package com.ootd.today.domain

/**
 * 颜色计算工具。
 *
 * 手写 RGB→HSV 而不用 `android.graphics.Color.colorToHSV`，
 * 是为了让搭配相关的算法保持「纯 JVM」，可以直接跑单元测试。
 */
internal object ColorMath {

    /**
     * 饱和度低于此值视为中性色。
     *
     * 定在 0.30 而不是更低的 0.18：驼色（0xFFB8A88A，约 0.25）、
     * 燕麦色这类大地色在穿搭里就是中性色，几乎百搭。阈值取低了会把它们
     * 当成彩色，于是「蓝色系 + 驼色围巾」会被判成撞色，色系计数也被虚增。
     *
     * 而真正的彩色（雾霾蓝 0.35、中蓝 0.58）仍在阈值之上，不受影响。
     */
    const val NEUTRAL_SATURATION = 0.30f

    /** 色相与饱和度的打包结果。 */
    data class Hsv(val hue: Float, val saturation: Float)

    fun toHsv(argb: Int): Hsv {
        val r = (argb shr 16 and 0xFF) / 255f
        val g = (argb shr 8 and 0xFF) / 255f
        val b = (argb and 0xFF) / 255f

        val max = maxOf(r, g, b)
        val min = minOf(r, g, b)
        val delta = max - min

        val hue = when {
            delta == 0f -> 0f
            max == r -> 60f * (((g - b) / delta) % 6f)
            max == g -> 60f * (((b - r) / delta) + 2f)
            else -> 60f * (((r - g) / delta) + 4f)
        }.let { if (it < 0f) it + 360f else it }

        val saturation = if (max == 0f) 0f else delta / max
        return Hsv(hue, saturation)
    }

    /** 是否为彩色（非中性色）。 */
    fun isChromatic(argb: Int): Boolean = toHsv(argb).saturation > NEUTRAL_SATURATION

    /**
     * 一组色相在色环上占据的弧长（0..360）。
     *
     * 算法：把色相排序后找最大的空隙，360 减去它即为包含所有色相的最短弧段。
     * 这样能正确处理跨越 0° 的情况（例如 350° 与 10° 其实只差 20°）。
     */
    fun circularSpread(hues: List<Float>): Float {
        if (hues.size < 2) return 0f
        val sorted = hues.sorted()
        var largestGap = 0f
        for (i in 1 until sorted.size) {
            largestGap = maxOf(largestGap, sorted[i] - sorted[i - 1])
        }
        // 首尾之间那条跨越 0° 的空隙也要算上。
        largestGap = maxOf(largestGap, 360f - sorted.last() + sorted.first())
        return 360f - largestGap
    }

    /**
     * 把色相按邻近程度聚成若干簇。
     *
     * 用途是回答「这套衣服一共用了几个色系」—— 3° 与 8° 算同一个色系，
     * 不能当成两种颜色。同样要处理跨 0° 的环形情况。
     */
    fun clusterHues(hues: List<Float>, threshold: Float = 30f): List<List<Float>> {
        if (hues.isEmpty()) return emptyList()

        val sorted = hues.sorted()
        val clusters = mutableListOf<MutableList<Float>>()
        var current = mutableListOf(sorted.first())

        for (i in 1 until sorted.size) {
            if (sorted[i] - sorted[i - 1] <= threshold) {
                current.add(sorted[i])
            } else {
                clusters.add(current)
                current = mutableListOf(sorted[i])
            }
        }
        clusters.add(current)

        // 环形收尾：首簇与尾簇可能本是同一个色系（如 355° 和 5°）。
        if (clusters.size > 1) {
            val first = clusters.first().first()
            val last = clusters.last().last()
            if (360f - last + first <= threshold) {
                val wrapped = clusters.removeAt(clusters.size - 1)
                clusters[0] = (wrapped + clusters[0]).toMutableList()
            }
        }
        return clusters
    }
}