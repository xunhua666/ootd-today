package com.ootd.today.domain

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.graphics.scale
import androidx.core.net.toUri
import com.ootd.today.data.model.ClothingCategory
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.math.sqrt

/** 识别结果：预填的品类 + 主色。全部字段都允许用户后续修改。 */
data class RecognitionResult(
    val suggestedCategory: ClothingCategory?,
    val dominantColorArgb: Long?,
    /** ML Kit 给出的原始标签，用于调试与展示「AI 看到了什么」。 */
    val rawLabels: List<String>
)

/**
 * 单品识别器：拍照/选图后自动预填品类与主色。
 *
 * 两条独立通道：
 * 1. **品类**：ML Kit 离线图像打标（无需 key、不联网），把英文标签映射到 7 大品类；
 * 2. **主色**：本地像素统计（中位切色法），不依赖任何模型。
 *
 * 识别失败或置信度不足时返回 null 字段，UI 回退到手动选择，绝不阻塞录入流程。
 */
@Singleton
class ItemRecognizer @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val labeler by lazy {
        ImageLabeling.getClient(
            ImageLabelerOptions.Builder()
                .setConfidenceThreshold(MIN_CONFIDENCE)
                .build()
        )
    }

    /**
     * 对一张图片做识别。
     *
     * @param pathOrUri 私有目录路径或 content:// URI，二者都支持。
     */
    suspend fun recognize(pathOrUri: String): RecognitionResult? {
        val bitmap = decodeBitmap(pathOrUri) ?: return null

        // 两条通道互不依赖：任一失败不影响另一个的结果。
        val labels = runCatching { labelImage(scaleDown(bitmap)) }.getOrNull().orEmpty()
        val category = labels.firstNotNullOfOrNull { (label, _) -> LABEL_TO_CATEGORY[label] }
        val color = dominantColor(scaleDown(bitmap))

        return RecognitionResult(
            suggestedCategory = category,
            dominantColorArgb = color,
            rawLabels = labels.map { it.first }
        )
    }

    // ------------------------------------------------------------------
    // 品类通道：ML Kit
    // ------------------------------------------------------------------

    private suspend fun labelImage(bitmap: Bitmap): List<Pair<String, Float>> =
        suspendCancellableCoroutine { continuation ->
            val image = InputImage.fromBitmap(bitmap, 0)
            val task = labeler.process(image)
            task.addOnSuccessListener { results ->
                continuation.resumeWith(
                    Result.success(results.map { it.text to it.confidence })
                )
            }
            task.addOnFailureListener { continuation.resumeWith(Result.success(emptyList())) }
            // 任务被取消时让协程以取消异常结束，避免悬挂。
            task.addOnCanceledListener {
                continuation.cancel()
            }
        }

    // ------------------------------------------------------------------
    // 主色通道：本地像素统计
    // ------------------------------------------------------------------

    /**
     * 主色提取：降采样后做「量化计数」。
     *
     * 不用 k-means（实现重、慢），改用把 RGB 各通道右移 5 位做桶键计数，
     * 取票数最多的桶再还原成颜色。对衣物这类大面积纯色目标效果足够好，
     * 且单次耗时在毫秒级。
     */
    private fun dominantColor(bitmap: Bitmap): Long? = runCatching {
        val sampled = scaleDown(bitmap)
        val pixels = IntArray(sampled.width * sampled.height)
        sampled.getPixels(pixels, 0, sampled.width, 0, 0, sampled.width, sampled.height)

        val buckets = HashMap<Int, Int>()
        for (p in pixels) {
            // 跳过接近纯白/纯黑的背景像素，避免把白墙识别成主色。
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val b = p and 0xFF
            val max = maxOf(r, g, b)
            val min = minOf(r, g, b)
            val isNearWhite = min > 240
            val isNearBlack = max < 15
            if (isNearWhite || isNearBlack) continue

            val key = ((r shr 5) shl 6) or ((g shr 5) shl 3) or (b shr 5)
            buckets[key] = (buckets[key] ?: 0) + 1
        }

        val top = buckets.maxByOrNull { it.value } ?: return@runCatching null
        // 桶键还原：取桶区间中点，颜色更贴近肉眼观感。
        val key = top.key
        val r = (((key shr 6) and 0x7) * 32 + 16)
        val g = (((key shr 3) and 0x7) * 32 + 16)
        val b = ((key and 0x7) * 32 + 16)
        (0xFFL shl 24) or (r.toLong() shl 16) or (g.toLong() shl 8) or b.toLong()
    }.getOrNull()

    // ------------------------------------------------------------------
    // 工具
    // ------------------------------------------------------------------

    private fun decodeBitmap(pathOrUri: String): Bitmap? = runCatching {
        if (pathOrUri.startsWith("content://")) {
            context.contentResolver.openInputStream(pathOrUri.toUri())?.use {
                BitmapFactory.decodeStream(it)
            }
        } else {
            BitmapFactory.decodeFile(pathOrUri)
        }
    }.getOrNull()

    /** 等比缩放到最长边不超过 [SAMPLE_EDGE]，控制像素统计与模型输入的开销。 */
    private fun scaleDown(src: Bitmap): Bitmap {
        val longest = maxOf(src.width, src.height)
        if (longest <= SAMPLE_EDGE) return src
        val ratio = SAMPLE_EDGE.toFloat() / longest
        return src.scale(
            (src.width * ratio).toInt().coerceAtLeast(1),
            (src.height * ratio).toInt().coerceAtLeast(1)
        )
    }

    private companion object {
        /** 低于该置信度的标签直接丢弃，避免「0.3 的 T恤」误导预填。 */
        const val MIN_CONFIDENCE = 0.55f

        /** 像素统计的采样边长。 */
        const val SAMPLE_EDGE = 96

        /**
         * ML Kit 英文标签 → 品类映射表。
         *
         * 只收录映射把握高的标签；没命中就返回 null 让用户自己选，
         * 宁可空着也不乱猜。
         */
        val LABEL_TO_CATEGORY: Map<String, ClothingCategory> = mapOf(
            "T-shirt" to ClothingCategory.TOP,
            "Shirt" to ClothingCategory.TOP,
            "Sweater" to ClothingCategory.TOP,
            "Hoodie" to ClothingCategory.TOP,
            "Tank top" to ClothingCategory.TOP,
            "Jeans" to ClothingCategory.BOTTOM,
            "Shorts" to ClothingCategory.BOTTOM,
            "Skirt" to ClothingCategory.BOTTOM,
            "Trousers" to ClothingCategory.BOTTOM,
            "Dress" to ClothingCategory.ONE_PIECE,
            "Gown" to ClothingCategory.ONE_PIECE,
            "Coat" to ClothingCategory.OUTERWEAR,
            "Jacket" to ClothingCategory.OUTERWEAR,
            "Suit" to ClothingCategory.OUTERWEAR,
            "Footwear" to ClothingCategory.SHOES,
            "Shoe" to ClothingCategory.SHOES,
            "Sneakers" to ClothingCategory.SHOES,
            "Boot" to ClothingCategory.SHOES,
            "High heels" to ClothingCategory.SHOES,
            "Handbag" to ClothingCategory.BAG,
            "Backpack" to ClothingCategory.BAG,
            "Purse" to ClothingCategory.BAG,
            "Hat" to ClothingCategory.ACCESSORY,
            "Cap" to ClothingCategory.ACCESSORY,
            "Scarf" to ClothingCategory.ACCESSORY,
            "Sunglasses" to ClothingCategory.ACCESSORY,
            "Necklace" to ClothingCategory.ACCESSORY,
            "Watch" to ClothingCategory.ACCESSORY,
            "Socks" to ClothingCategory.ACCESSORY
        )
    }
}
