package com.ootd.today.domain

import com.ootd.today.data.model.ClothingCategory
import com.ootd.today.data.model.ItemStatus
import com.ootd.today.data.model.Occasion
import com.ootd.today.data.model.Season
import com.ootd.today.data.model.WardrobeItem
import com.ootd.today.data.model.Weather
import kotlin.math.abs

/**
 * 每日穿搭推荐引擎。
 *
 * 刻意不依赖任何 Android 类型，全部输入都是纯数据，因此可以直接跑 JVM 单元测试。
 *
 * 评分分两步，这是保证「好看」的关键：
 *
 * **第一步：单品分**（0..1，只看这一件自己合不合适）
 *
 * | 维度 | 权重 | 说明 |
 * |---|---|---|
 * | 保暖度匹配 | 0.34 | 气温推导目标保暖度，差值越大扣分越狠 |
 * | 季节匹配 | 0.14 | 与当前季节吻合度 |
 * | 闲置唤醒 | 0.12 | 穿着次数越少分越高，避免总推同几件 |
 * | 近期去重 | 0.10 | 近 N 天穿过的降权 |
 * | 场合通用性 | 0.30 | 适用场合越多越占优 |
 *
 * 场合是**硬性过滤**：单品适用场合不含目标场合则直接淘汰，而非降权。
 *
 * **第二步：组合分**（0..1，看几件放在一起的效果）
 *
 * | 成分 | 权重 | 说明 |
 * |---|---|---|
 * | 单品平均分 | 0.65 | 每件自己是否合适 |
 * | 整体审美分 | 0.35 | 配色与风格是否统一，见 [OutfitAesthetics] |
 *
 * 主体采用**枚举**而非「各大类各取最优再拼」：两件各自最合适的单品凑一起未必好看。
 * 包与配饰也在主体定下后再挑，且要求与整体配色合得来。
 *
 * 此外「上装 + 下装（或连体装）+ 鞋」是**硬性完整性约束**，不满足的组合直接不生成。
 *
 * @property recentDays 「近期去重」的回溯天数。
 */
class OutfitRecommendEngine(
    private val recentDays: Int = 7
) {

    /**
     * 生成一套推荐搭配。
     *
     * @param items 全部单品（内部会自动过滤掉已处理的）。
     * @param weather 当前天气。
     * @param occasion 目标场合。
     * @param currentSeason 当前季节，用于季节匹配打分。
     * @param recentlyWornIds 最近 [recentDays] 天穿过的单品 id，用于去重降权。
     * @param feedbackBoost 用户对单品的历史反馈权重（👍 为正、👎 为负），id -> 调整量。
     * @return 推荐结果；若衣橱不足以满足完整性约束则返回 null。
     */
    fun recommend(
        items: List<WardrobeItem>,
        weather: Weather,
        occasion: Occasion,
        currentSeason: Season,
        recentlyWornIds: Set<Long> = emptySet(),
        feedbackBoost: Map<Long, Double> = emptyMap(),
        requiredItemId: Long? = null
    ): OutfitCandidate? {
        // 已处理的单品不参与推荐。
        val pool = items.filter { it.status == ItemStatus.ACTIVE }
        if (pool.isEmpty()) return null
        val required = requiredItemId?.let { id -> pool.firstOrNull { it.id == id } ?: return null }

        val targetWarmth = warmthForTemperature(weather.temperatureC)

        // 每个大类先各自筛出得分靠前的若干件，作为组合搜索的原料。
        // 只留前 CANDIDATE_POOL 件：再往后分数已经很低，组合进去也选不上，
        // 却会让搜索空间白涨。
        val candidates: Map<ClothingCategory, List<Pair<WardrobeItem, Double>>> = pool
            .groupBy { it.category }
            .mapValues { (category, list) ->
                val choices = if (required?.category == category) list.filter { it.id == required.id } else list
                choices.map { item ->
                    item to scoreItem(
                        item = item,
                        targetWarmth = targetWarmth,
                        occasion = occasion,
                        currentSeason = currentSeason,
                        recentlyWornIds = recentlyWornIds,
                        feedbackBoost = feedbackBoost
                    )
                }
                    .filter { it.second >= MIN_ACCEPTABLE }
                    .sortedByDescending { it.second }
                    .take(CANDIDATE_POOL)
            }

        val coreRequired = required?.takeIf { it.category != ClothingCategory.BAG && it.category != ClothingCategory.ACCESSORY }?.id
        val core = bestCore(candidates, weather.temperatureC, coreRequired) ?: return null

        // 包与配饰在主体定下来之后再挑，而且要求它们跟整体合得来 ——
        // 各类别独立挑最容易出「一身冷色里塞个亮橙包」。
        val chosen = core.toMutableList()
        pickHarmonizing(
            candidates = candidates[ClothingCategory.BAG],
            core = chosen.map { it.first },
            threshold = MIN_ACCESSORY
        )?.let { chosen.add(it) }

        // 围巾等保暖型配饰在冷天放宽门槛，否则一件都出不来。
        val accessoryThreshold =
            if (weather.temperatureC < COLD_THRESHOLD) MIN_ACCEPTABLE else MIN_ACCESSORY
        pickHarmonizing(
            candidates = candidates[ClothingCategory.ACCESSORY]?.filter {
                // 配饰额外受保暖度硬约束：综合分里的保暖惩罚会被通用性奖励稀释，
                // 曾出现 28℃ 推荐羊绒围巾（保暖 4）的情况，这里直接按差值卡死。
                warmthWithin(it.first, targetWarmth, MAX_ACCESSORY_WARMTH_DIFF)
            },
            core = chosen.map { it.first },
            threshold = accessoryThreshold
        )?.let { chosen.add(it) }

        val finalItems = chosen.map { it.first }
        if (requiredItemId != null && finalItems.none { it.id == requiredItemId }) return null
        val aesthetic = OutfitAesthetics.score(finalItems)
        val baseScore = chosen.sumOf { it.second } / chosen.size

        return OutfitCandidate(
            items = finalItems,
            score = (blend(baseScore, aesthetic) * 100).toInt(),
            reason = buildReason(weather, occasion, finalItems)
        )
    }

    /**
     * 在主体组合里找最好的一套。
     *
     * 主体 = （上装 + 下装 或 一件连体装）+ 鞋 + 可选外套。
     *
     * 这里用**枚举**而不是各类别各取最优：搭配好不好看取决于几件放在一起的效果，
     * 单独最优的两件凑一起未必成立。候选量级是几十，枚举完全负担得起。
     */
    private fun bestCore(
        candidates: Map<ClothingCategory, List<Pair<WardrobeItem, Double>>>,
        temperatureC: Int,
        requiredItemId: Long? = null
    ): List<Pair<WardrobeItem, Double>>? {
        val shoes = candidates[ClothingCategory.SHOES].orEmpty()
        if (shoes.isEmpty()) return null

        val bodies: List<List<Pair<WardrobeItem, Double>>> = buildList {
            // 连衣裙/连体裤可以单件成立，无需再配上装下装。
            candidates[ClothingCategory.ONE_PIECE].orEmpty().forEach { add(listOf(it)) }
            val tops = candidates[ClothingCategory.TOP].orEmpty()
            val bottoms = candidates[ClothingCategory.BOTTOM].orEmpty()
            for (top in tops) for (bottom in bottoms) add(listOf(top, bottom))
        }
        // 无法满足「至少一件上装 + 一件下装」或连体装时不出推荐。
        if (bodies.isEmpty()) return null

        // 冷天要加外套；热天不加，避免推荐「大夏天穿羽绒服」。
        // 衣橱里没有合适外套时退化成「不加外套」，而不是因此一套都不出。
        val outerwears: List<Pair<WardrobeItem, Double>?> =
            if (needsOuterwear(temperatureC)) {
                candidates[ClothingCategory.OUTERWEAR].orEmpty().ifEmpty { listOf(null) }
            } else {
                listOf(null)
            }

        var best: List<Pair<WardrobeItem, Double>>? = null
        var bestTotal = Double.NEGATIVE_INFINITY

        for (body in bodies) {
            for (shoe in shoes) {
                for (outer in outerwears) {
                    val combination = body + shoe + listOfNotNull(outer)
                    if (requiredItemId != null && combination.none { it.first.id == requiredItemId }) continue
                    val base = combination.sumOf { it.second } / combination.size
                    val aesthetic = OutfitAesthetics.score(combination.map { it.first })
                    val total = blend(base, aesthetic)
                    if (total > bestTotal) {
                        bestTotal = total
                        best = combination
                    }
                }
            }
        }
        return best
    }

    /**
     * 把「单件平均分」与「整体审美分」合成最终分。
     *
     * 两者都要：只看前者会拼出各自合适但放一起难看的组合，
     * 只看后者会为了好看牺牲保暖与场合适宜性。
     */
    private fun blend(baseScore: Double, aesthetic: Double): Double =
        (baseScore * (1 - WEIGHT_AESTHETIC) + aesthetic * WEIGHT_AESTHETIC).coerceIn(0.0, 1.0)

    /**
     * 从候选中挑一件**与已选搭配最合得来**的。
     *
     * 不是单纯取分最高的：一个颜色突兀的包会让整套看起来没打理过，
     * 所以把「加入后的整体审美」也算进去。
     */
    private fun pickHarmonizing(
        candidates: List<Pair<WardrobeItem, Double>>?,
        core: List<WardrobeItem>,
        threshold: Double
    ): Pair<WardrobeItem, Double>? =
        candidates.orEmpty()
            .filter { it.second >= threshold }
            .maxByOrNull { (item, base) -> blend(base, OutfitAesthetics.score(core + item)) }

    /**
     * 随机搭配。
     *
     * 与 [recommend] 的区别：在各槽位的**得分前列**中随机抽取，而不是取最优，
     * 因此每次结果不同，但仍保证搭配合法且不至于离谱（不会随机到大夏天穿羽绒服）。
     *
     * @param candidatePoolSize 每个槽位参与随机的前 N 名。
     */
    fun random(
        items: List<WardrobeItem>,
        weather: Weather,
        occasion: Occasion,
        currentSeason: Season,
        recentlyWornIds: Set<Long> = emptySet(),
        candidatePoolSize: Int = 4
    ): OutfitCandidate? {
        val pool = items.filter { it.status == ItemStatus.ACTIVE }
        if (pool.isEmpty()) return null

        val targetWarmth = warmthForTemperature(weather.temperatureC)
        val scored = pool
            .groupBy { it.category }
            .mapValues { (_, list) ->
                list.map { item ->
                    item to scoreItem(
                        item, targetWarmth, occasion, currentSeason, recentlyWornIds
                    )
                }
                    .filter { it.second >= MIN_ACCEPTABLE }
                    .sortedByDescending { it.second }
                    .take(candidatePoolSize)
            }

        val onePiece = scored[ClothingCategory.ONE_PIECE]?.randomOrNull()
        val top = scored[ClothingCategory.TOP]?.randomOrNull()
        val bottom = scored[ClothingCategory.BOTTOM]?.randomOrNull()
        val shoes = scored[ClothingCategory.SHOES]?.randomOrNull() ?: return null

        // 完整性约束：上装+下装，或一件连体装。二者都抽到优先上下装组合，
        // 因为随机场景下「摇出连衣裙」的概率若太高会显得单调。
        val body = when {
            top != null && bottom != null -> listOf(top, bottom)
            onePiece != null -> listOf(onePiece)
            else -> return null
        }

        val chosen = (body + shoes).toMutableList()
        if (needsOuterwear(weather.temperatureC)) {
            scored[ClothingCategory.OUTERWEAR]?.randomOrNull()?.let { chosen.add(it) }
        }
        scored[ClothingCategory.BAG]?.randomOrNull()?.let { chosen.add(it) }
        // 随机路径同样受配饰保暖度硬约束：先过滤再抽，避免摇出「热天围巾」。
        scored[ClothingCategory.ACCESSORY]
            ?.filter { warmthWithin(it.first, targetWarmth, MAX_ACCESSORY_WARMTH_DIFF) }
            ?.randomOrNull()
            ?.let { chosen.add(it) }

        val finalItems = chosen.map { it.first }
        val aesthetic = OutfitAesthetics.score(finalItems)
        val baseScore = chosen.sumOf { it.second } / chosen.size

        return OutfitCandidate(
            items = finalItems,
            score = (blend(baseScore, aesthetic) * 100).toInt(),
            reason = "随机摇一摇 · ${occasion.label} · ${weather.temperatureC}℃"
        )
    }

    // ------------------------------------------------------------------
    // 单品打分
    // ------------------------------------------------------------------

    private fun scoreItem(
        item: WardrobeItem,
        targetWarmth: Int,
        occasion: Occasion,
        currentSeason: Season,
        recentlyWornIds: Set<Long>,
        feedbackBoost: Map<Long, Double> = emptyMap()
    ): Double {
        // 场合是硬性过滤：明确不适用的场合直接淘汰，而不是降权。
        // 例如睡衣不该被推荐去通勤。
        if (occasion !in item.occasions) return 0.0

        val warmth = warmthScore(item.warmthLevel, targetWarmth)
        val season = seasonScore(item.seasons, currentSeason)
        val idle = idleScore(item.wearCount)
        val recency = if (item.id in recentlyWornIds) RECENT_WORN_SCORE else 1.0
        val feedback = feedbackBoost[item.id] ?: 0.0

        val raw = warmth * WEIGHT_WARMTH +
            season * WEIGHT_SEASON +
            idle * WEIGHT_IDLE +
            recency * WEIGHT_RECENCY

        // 场合已经过硬性过滤，这里再给一点正向奖励，让「多场合通用」的单品略占优。
        val versatility = (item.occasions.size.toDouble() / Occasion.entries.size) * WEIGHT_VERSATILITY
        return (raw + versatility + feedback).coerceIn(0.0, 1.0)
    }

    /**
     * 保暖度匹配。
     *
     * 用非对称惩罚：**穿少了比穿多了更难受**。目标保暖度是 4（冷天）而单品只有 1，
     * 扣分要比目标 1（热天）而单品是 4 更狠。
     */
    private fun warmthScore(actual: Int, target: Int): Double {
        val diff = actual - target
        return when {
            diff == 0 -> 1.0
            diff > 0 -> (1.0 - diff * OVER_WARM_PENALTY).coerceAtLeast(0.05)
            else -> (1.0 - (-diff) * UNDER_WARM_PENALTY).coerceAtLeast(0.0)
        }
    }

    private fun seasonScore(seasons: Set<Season>, current: Season): Double = when {
        current in seasons -> 1.0
        seasons.isEmpty() -> 0.6 // 未标注季节视为四季可穿，给中等分。
        // 相邻季节（如春↔夏）仍有一定可用性。
        seasons.any { abs(it.ordinal - current.ordinal) == 1 } -> 0.55
        else -> 0.15
    }

    /**
     * 闲置唤醒：穿着次数越少，得分越高。
     *
     * 用对数衰减而非线性，避免「一次没穿过」的单品获得压倒性权重，
     * 导致算法总是推荐用户其实并不喜欢的新衣服。
     */
    private fun idleScore(wearCount: Int): Double {
        if (wearCount <= 0) return 1.0
        val normalized = 1.0 - (ln(1.0 + wearCount) / ln(1.0 + IDLE_NORMALIZER))
        return normalized.coerceIn(0.0, 1.0)
    }

    // ------------------------------------------------------------------
    // 温度 → 保暖度映射
    // ------------------------------------------------------------------

    /**
     * 由气温推导目标保暖度（1..5）。
     *
     * 分档依据日常穿衣经验：
     * - ≥28℃ 短袖短裤（1）
     * - 22~27℃ 单层长袖/薄T（2）
     * - 15~21℃ 长袖 + 薄外套（3）
     * - 8~14℃ 厚外套/毛衣（4）
     * - <8℃ 羽绒服级别（5）
     */
    fun warmthForTemperature(tempC: Int): Int = when {
        tempC >= 28 -> 1
        tempC >= 22 -> 2
        tempC >= 15 -> 3
        tempC >= 8 -> 4
        else -> 5
    }

    /** 气温低于该阈值时需要外套。 */
    private fun needsOuterwear(tempC: Int): Boolean = tempC < OUTERWEAR_THRESHOLD

    /** 单品保暖度与目标值的差是否在允许范围内。 */
    private fun warmthWithin(item: WardrobeItem, targetWarmth: Int, maxDiff: Int): Boolean =
        abs(item.warmthLevel - targetWarmth) <= maxDiff

    // ------------------------------------------------------------------
    // 推荐理由
    // ------------------------------------------------------------------

    private fun buildReason(
        weather: Weather,
        occasion: Occasion,
        chosen: List<WardrobeItem>
    ): String {
        val warmth = warmthForTemperature(weather.temperatureC)
        val tempDesc = when (warmth) {
            1 -> "天气炎热"
            2 -> "天气温暖"
            3 -> "温度适中"
            4 -> "有些凉意"
            else -> "天气寒冷"
        }
        val hasOuterwear = chosen.any { it.category == ClothingCategory.OUTERWEAR }
        val outerwearHint = if (hasOuterwear) "，加了外套保暖" else ""
        val rainHint = if (weather.condition.isRainy) " 记得带伞☔" else ""
        // 配色描述交给 OutfitAesthetics，与真正的评分标准同源，
        // 避免“说的”和“算的”是两套逻辑。
        val paletteDesc = OutfitAesthetics.describe(chosen)
        val shapeHint = SilhouetteRules.describe(chosen)?.let { " $it。" }.orEmpty()
        return "${weather.temperatureC}℃$tempDesc，适合${occasion.label}$outerwearHint，$paletteDesc。$shapeHint$rainHint"
    }

    private companion object {
        /** 每个大类参与组合搜索的候选数。再往后分数已很低，组合进来也选不上。 */
        const val CANDIDATE_POOL = 6

        // 权重总和为 1.0
        const val WEIGHT_WARMTH = 0.34
        const val WEIGHT_SEASON = 0.14
        const val WEIGHT_IDLE = 0.12
        const val WEIGHT_RECENCY = 0.10
        const val WEIGHT_VERSATILITY = 0.30

        /**
         * 整体审美在最终分里的占比。
         *
         * 定在 0.35：审美很重要，但不能压过保暖与场合适宜性 ——
         * 一套很好看的衣服在 5℃ 穿不住，就是算法失败。
         */
        const val WEIGHT_AESTHETIC = 0.35

        /** 非对称保暖惩罚：穿少了比穿多了更难受。 */
        const val UNDER_WARM_PENALTY = 0.28
        const val OVER_WARM_PENALTY = 0.16

        /** 低于此分数的单品不参与成组，用于硬性过滤掉明显不合适的。 */
        const val MIN_ACCEPTABLE = 0.22

        /** 包/配饰是可选件，门槛更高，避免每套都硬塞一个不搭的包。 */
        const val MIN_ACCESSORY = 0.45

        /** 闲置度归一化基准：穿着达到约 20 次即视为「常用款」，闲置分趋近 0。 */
        const val IDLE_NORMALIZER = 20.0

        /** 近 N 天穿过的单品得分。不完全归零，只是明显降权。 */
        const val RECENT_WORN_SCORE = 0.35

        /** 低于此温度需要外套。 */
        const val OUTERWEAR_THRESHOLD = 20

        /** 低于此温度视为冷天，放宽保暖配饰的准入门槛。 */
        const val COLD_THRESHOLD = 12

        /** 配饰保暖度与目标值的最大允许差值，防止热天推围巾、冷天推草帽。 */
        const val MAX_ACCESSORY_WARMTH_DIFF = 2

        private fun ln(x: Double) = kotlin.math.ln(x)
    }
}

/**
 * 推荐结果候选。
 *
 * @property items 构成这套搭配的单品。
 * @property score 综合得分 0..100。
 * @property reason 一句人话解释的推荐理由，直接展示在首页。
 * @property headline 一句话定调。本地算法不产出这个值，只有「沈砚」会给；
 *   为 null 时 UI 不展示标题行。
 */
data class OutfitCandidate(
    val items: List<WardrobeItem>,
    val score: Int,
    val reason: String,
    val headline: String? = null
)
