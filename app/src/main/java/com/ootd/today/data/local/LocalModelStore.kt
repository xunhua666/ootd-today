package com.ootd.today.data.local

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import com.ootd.today.domain.stylist.LocalModel
import com.ootd.today.domain.stylist.ModelIntegrity
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

data class ModelDownloadState(
    val model: LocalModel,
    val ready: Boolean = false,
    val downloading: Boolean = false,
    val received: Long = 0,
    val message: String = "尚未下载"
)

@Singleton
class LocalModelStore @Inject constructor(@ApplicationContext private val context: Context) {
    private val downloads = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    private val prefs = context.getSharedPreferences("local_models", Context.MODE_PRIVATE)
    private val lock = Mutex()
    private val root get() = File(requireNotNull(context.getExternalFilesDir(null)), "expert_models").apply { mkdirs() }
    fun file(model: LocalModel) = File(root, model.id + ".litertlm")
    private fun partial(model: LocalModel) = File(root, model.id + ".part")
    fun selected(): LocalModel = LocalModel.find(prefs.getString("selected", null).orEmpty())
    fun select(model: LocalModel) { prefs.edit().putString("selected", model.id).apply() }
    private fun key(model: LocalModel) = "download_${model.id}"

    suspend fun start(model: LocalModel, wifiOnly: Boolean, mirror: Boolean = false) = withContext(Dispatchers.IO) {
        lock.withLock {
            check(root.usableSpace > model.bytes + 256L * 1024 * 1024) { "存储空间不足，请先留出 ${model.sizeLabel} 和额外 256 MB 空间" }
            cancelLocked(model)
            val url = if (mirror) model.url.replace("https://huggingface.co/", "https://hf-mirror.com/") else model.url
            val request = DownloadManager.Request(Uri.parse(url))
                .setTitle("下载审美专家 · ${model.title}")
                .setDescription("下载完成后可离线交流")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setAllowedOverMetered(!wifiOnly)
                .setAllowedOverRoaming(false)
                .setDestinationUri(Uri.fromFile(partial(model)))
            if (wifiOnly) request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI)
            val id = downloads.enqueue(request)
            prefs.edit().putLong(key(model), id).remove("error_${model.id}").commit()
        }
    }

    suspend fun cancel(model: LocalModel) = withContext(Dispatchers.IO) {
        lock.withLock { cancelLocked(model) }
    }
    private fun cancelLocked(model: LocalModel) {
        val id = prefs.getLong(key(model), -1)
        if (id != -1L) downloads.remove(id)
        partial(model).delete()
        prefs.edit().remove(key(model)).remove("error_${model.id}").commit()
    }
    suspend fun delete(model: LocalModel) = withContext(Dispatchers.IO) {
        lock.withLock {
            cancelLocked(model)
            check(!file(model).exists() || file(model).delete()) { "删除失败，请重试" }
            prefs.edit().remove("verified_${model.id}").commit()
        }
    }

    suspend fun state(model: LocalModel): ModelDownloadState = withContext(Dispatchers.IO) {
        lock.withLock {
            if (file(model).length() == model.bytes && prefs.getBoolean("verified_${model.id}", false))
                return@withLock ModelDownloadState(model, ready = true, received = model.bytes, message = "已校验 · 可离线使用")
            val id = prefs.getLong(key(model), -1)
            if (id == -1L) return@withLock ModelDownloadState(model, message = prefs.getString("error_${model.id}", null) ?: "尚未下载")
            downloads.query(DownloadManager.Query().setFilterById(id)).use { cursor ->
                if (!cursor.moveToFirst()) {
                    prefs.edit().remove(key(model)).commit()
                    return@withLock ModelDownloadState(model, message = "下载已被移除，可重新下载")
                }
                val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                val received = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                when (status) {
                    DownloadManager.STATUS_SUCCESSFUL -> {
                        val valid = verify(partial(model), model)
                        if (valid && partial(model).renameTo(file(model))) {
                            prefs.edit().remove(key(model)).putBoolean("verified_${model.id}", true).commit()
                            ModelDownloadState(model, ready = true, received = model.bytes, message = "已校验 · 可离线使用")
                        } else {
                            cancelLocked(model)
                            val error = "文件校验失败，请重新下载或导入官方文件"
                            prefs.edit().putString("error_${model.id}", error).commit()
                            ModelDownloadState(model, message = error)
                        }
                    }
                    DownloadManager.STATUS_FAILED -> {
                        cancelLocked(model)
                        val error = "下载失败，请检查网络后重试，也可导入官方文件"
                        prefs.edit().putString("error_${model.id}", error).commit()
                        ModelDownloadState(model, message = error)
                    }
                    else -> {
                        val reason = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))
                        val message = when {
                            status == DownloadManager.STATUS_PENDING -> "等待下载，请保持所选网络连接"
                            status == DownloadManager.STATUS_PAUSED && reason == DownloadManager.PAUSED_QUEUED_FOR_WIFI -> "等待 Wi-Fi 连接"
                            status == DownloadManager.STATUS_PAUSED -> "等待网络，系统将自动重试"
                            else -> "正在下载，完成后校验文件"
                        }
                        ModelDownloadState(model, downloading = true, received = received, message = message)
                    }
                }
            }
        }
    }

    suspend fun import(model: LocalModel, uri: Uri) = withContext(Dispatchers.IO) {
        lock.withLock {
            check(root.usableSpace > model.bytes + 64L * 1024 * 1024) { "存储空间不足" }
            cancelLocked(model)
            try {
                requireNotNull(context.contentResolver.openInputStream(uri)).use { input ->
                    partial(model).outputStream().buffered().use { output ->
                        val buffer = ByteArray(256 * 1024)
                        var total = 0L
                        while (true) {
                            coroutineContext.ensureActive()
                            val count = input.read(buffer)
                            if (count < 0) break
                            total += count
                            check(total <= model.bytes) { "请选择与当前模型对应的官方 .litertlm 文件" }
                            output.write(buffer, 0, count)
                        }
                    }
                }
                check(verify(partial(model), model)) { "文件与所选模型不符，或文件不完整" }
                check(partial(model).renameTo(file(model))) { "保存文件失败" }
                prefs.edit().putBoolean("verified_${model.id}", true).commit()
            } finally { partial(model).delete() }
        }
    }

    private suspend fun verify(file: File, model: LocalModel): Boolean {
        val task = coroutineContext
        return ModelIntegrity.verify(file, model) { task.ensureActive() }
    }
}
