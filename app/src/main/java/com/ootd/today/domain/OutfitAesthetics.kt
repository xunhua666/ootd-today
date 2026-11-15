package com.ootd.today.domain

import com.ootd.today.data.model.WardrobeItem

/**
 * 整套搭配的审美评分。
 *
 * 之前每件单品是**独立**打分的，再取各类别最高分拼起来 ——
 * 两件各自最合适的单品凑在一起未必好看。这个对象负责回答
 * 「这几件放在一起好不好看」，供引擎在组合之间做取舍。
 *
 * 只看两件事，都是搭配里最直观的：
 *
 * 1. **配色**（权重 0.6）：色相关系 + 用了几个色系。
 *    三个色系以内是经典规则，超过就显花；
 *    同色系、邻近色、撞色都算好看的方案，中间那段「不上不下」最难看。
 * 2. **风格**（权重 0.4）：风格标签是否统一。
 *    甜美配机能会把两件好衣服穿成没想法。
 *
 * 保暖、场合、季节这些属于「合不合适」，不在这里评 —— 那些由引擎负责。
 */
object OutfitAesthetics {

    /** 配色在总分里的占比。 */
    private const val WEIGHT_PALETTE = 0.6

    /** 风格在总分里的占比。 */
    private const val WEIGHT_STYLE = 0.4

    /** 色相在这个角度内视为同一色系。 */
    private const val SAME_HUE_RANGE = 40f

    /** 邻近色的上限。 */
    private const val ANALOGOUS_RANGE = 75f

    /** 色相差超过这个值算撞色 —— 本应用的调性里这是加分项。 */
    private const val CLASH_RANGE = 150f

    /** 色系数量超过这个值开始扣分。 */
    private const val IDEAL_COLOR_COUNT = 3

    /**
     * 给整套搭配打 0..1 的分。
     *
     * @param items 构成这套搭配的单品。少于两件时没有「搭配」可言，
     *   直接给满分，避免影响单件场景（如只有连衣裙）。
     */
    fun score(items: List<WardrobeItem>): Double {
        if (items.size < 2) return 1.0

        val palette = paletteScore(items.map { it.colorArgb.toInt() })
        val style = styleScore(items)
        val base = palette * WEIGHT_PALETTE + style * WEIGHT_STYLE
        val proportions = SilhouetteRules.score(items)
        return (if (proportions == null) base else base * .85 + proportions * .15).coerceIn(0.0, 1.0)
    }

    /**
     * 用一句人话描述这套搭配的审美特点，用于推荐理由。
     */
    fun describe(items: List<WardrobeItem>): String {
        val colors = items.map { it.colorArgb.toInt() }.filter { ColorMath.isChromatic(it) }
        if (colors.size < 2) return "配色干净"

        return when (val spread = ColorMath.circularSpread(colors.map { ColorMath.toHsv(it).hue })) {
            in 0f..SAME_HUE_RANGE -> "同色系层次"
            in SAME_HUE_RANGE..ANALOGOUS_RANGE -> "邻近色柔和"
            in CLASH_RANGE..360f -> "撞色大胆"
            else -> if (spread < 115f) "配色克制" else "三色平衡"
        }
    }

    // ------------------------------------------------------------------
    // 配色
    // ------------------------------------------------------------------

    /**
     * 配色分 = 色相协调度 × 0.65 + 色系数量纪律 × 0.35。
     *
     * 两者缺一不可：只算协调度的话，四五个相近颜色会拿高分，
     * 但身上挂五个颜色本身就不好看。
     */
    private fun paletteScore(colors: List<Int>): Double {
        val chromatic = colors.filter { ColorMath.isChromatic(it) }
        // 全中性色（黑白灰）或只有一个彩色：天然好搭，直接满分。
        if (chromatic.size < 2) return 1.0

        val hues = chromatic.map { ColorMath.toHsv(it).hue }
        val harmony = hueHarmony(ColorMath.circularSpread(hues))
        val discipline = colorCountDiscipline(ColorMath.clusterHues(hues).size)

        return (harmony * 0.65 + discipline * 0.35).coerceIn(0.0, 1.0)
    }

    /**
     * 色相协调度。
     *
     * 分段依据的是常见的配色经验：
     * - 同色系（跨度小）：高级感最强，但要注意层次；
     * - 邻近色：柔和自然；
     * - 撞色（跨度大）：本应用主打活泼撞色，是明确的加分项；
     * - 中间那段（约 75°~150°）既不够统一也不够有冲击力，最难看。
     */
    private fun hueHarmony(spread: Float): Double = when {
        spread <= SAME_HUE_RANGE -> 0.95
        spread <= ANALOGOUS_RANGE -> 0.85
        spread < 115f -> 0.45
        spread < CLASH_RANGE -> 0.70
        else -> 0.90
    }

    /**
     * 色系数量纪律：三个以内是经典规则。
     *
     * 中性色不计入 —— 它们的作用正是衬托，黑白灰再多也不乱。
     */
    private fun colorCountDiscipline(clusterCount: Int): Double = when {
        clusterCount <= IDEAL_COLOR_COUNT -> 1.0
        clusterCount == IDEAL_COLOR_COUNT + 1 -> 0.55
        else -> 0.2
    }

    // ------------------------------------------------------------------
    // 风格
    // ------------------------------------------------------------------

    /**
     * 风格统一度。
     *
     * 没有任何风格标签时给 0.75 —— 不算好也不算差。
     * 不能给满分，否则「全部没填风格」会被误判成「风格很统一」，
     * 让引擎偏好那些没有风格信息的单品。
     */
    private fun styleScore(items: List<WardrobeItem>): Double {
        val styles = items.mapNotNull { it.style }.distinct()
        return when (styles.size) {
            0 -> 0.75
            1 -> 1.0
            2 -> 0.6
            else -> 0.25
        }
    }
}
