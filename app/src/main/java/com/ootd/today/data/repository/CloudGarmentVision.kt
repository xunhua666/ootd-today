package com.ootd.today.data.repository

import android.graphics.*
import android.media.ExifInterface
import android.util.Base64
import com.ootd.today.data.prefs.SettingsRepository
import com.ootd.today.domain.vision.*
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext

/** Only called after a user explicitly confirms uploading the current photo. */
@Singleton
class CloudGarmentVision @Inject constructor(private val settings: SettingsRepository) {
    suspend fun analyze(path: String): GarmentVisionResult = withContext(Dispatchers.IO) {
        val config = settings.settings.first().vision
        config.validationError()?.let { error(it) }
        val image = prepareJpeg(path)
        coroutineContext.ensureActive()
        val payload = VisionProtocol.payload(config.model, Base64.encodeToString(image, Base64.NO_WRAP))
        val connection = URL(config.endpoint).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 15000; connection.readTimeout = 60000
            connection.instanceFollowRedirects = false
            connection.requestMethod = "POST"; connection.doOutput = true
            connection.setRequestProperty("Authorization", "Bearer ${config.apiKey}")
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connection.outputStream.bufferedWriter(Charsets.UTF_8).use { it.write(payload) }
            val code = connection.responseCode
            check(code in 200..299) { VisionProtocol.httpError(code) }
            val body = connection.inputStream.bufferedReader().use { reader ->
                val result = StringBuilder(); val buffer = CharArray(4096)
                while (true) {
                    coroutineContext.ensureActive()
                    val count = reader.read(buffer); if (count < 0) break
                    result.append(buffer, 0, count)
                    check(result.length <= 1_000_000) { "视觉服务返回内容过大" }
                }
                result.toString()
            }
            VisionProtocol.extract(body)
        } finally { connection.disconnect() }
    }
    companion object {
        /** Bounded decode, EXIF orientation correction, no original EXIF/location in upload. */
        internal fun prepareJpeg(path: String): ByteArray {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, bounds)
            require(bounds.outWidth > 0 && bounds.outHeight > 0) { "照片无法读取，请重新选择" }
            var sample = 1
            while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 2048) sample *= 2
            val decoded = requireNotNull(BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })) { "照片无法读取" }
            var oriented: Bitmap? = null
            var scaled: Bitmap? = null
            var flattened: Bitmap? = null
            try {
                val orientation = runCatching { ExifInterface(path).getAttributeInt(ExifInterface.TAG_ORIENTATION, 1) }.getOrDefault(1)
                val transform = Matrix().apply {
                    when (orientation) {
                        2 -> setScale(-1f, 1f); 3 -> setRotate(180f); 4 -> setScale(1f, -1f)
                        5 -> { setRotate(90f); postScale(-1f, 1f) }; 6 -> setRotate(90f)
                        7 -> { setRotate(270f); postScale(-1f, 1f) }; 8 -> setRotate(270f)
                    }
                }
                oriented = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, transform, true)
                val ratio = minOf(1f, 1280f / maxOf(oriented.width, oriented.height))
                scaled = Bitmap.createScaledBitmap(oriented, (oriented.width * ratio).toInt().coerceAtLeast(1), (oriented.height * ratio).toInt().coerceAtLeast(1), true)
                flattened = Bitmap.createBitmap(scaled.width, scaled.height, Bitmap.Config.ARGB_8888)
                Canvas(flattened).apply { drawColor(Color.WHITE); drawBitmap(scaled, 0f, 0f, null) }
                return ByteArrayOutputStream().use { output ->
                    flattened.compress(Bitmap.CompressFormat.JPEG, 85, output)
                    output.toByteArray().also { require(it.size <= 2_500_000) { "照片压缩后仍然过大，请换图" } }
                }
            } finally { listOfNotNull(decoded, oriented, scaled, flattened).distinct().forEach { it.recycle() } }
        }
    }
}
