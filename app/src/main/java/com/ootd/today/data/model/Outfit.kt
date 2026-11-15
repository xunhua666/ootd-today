package com.ootd.today.data.model

/**
 * 一套完整搭配。
 *
 * @property id 主键。
 * @property items 构成这套搭配的单品。必须满足完整性约束：
 *   至少含一件上装或连体装，且含下装（连体装可豁免）与鞋；外套/包/配饰可选。
 * @property occasion 这套搭配针对的场合。
 * @property score 推荐得分 0..100，由各维度加权汇总而来。
 * @property reason 推荐理由，用一句人话解释为什么推这套。
 */
data class Outfit(
    val id: Long,
    val items: List<WardrobeItem>,
    val occasion: Occasion,
    val score: Int,
    val reason: String
) {
    /** 是否已包含下装或连体装，用于完整性校验。 */
    val hasBottomOrOnePiece: Boolean
        get() = items.any { it.category == ClothingCategory.BOTTOM || it.category == ClothingCategory.ONE_PIECE }

    /** 按大类归组，便于 UI 分区渲染（上装/下装/鞋/配饰…）。 */
    val itemsByCategory: Map<ClothingCategory, List<WardrobeItem>>
        get() = items.groupBy { it.category }
}

/**
 * 一次穿搭打卡记录。
 *
 * @property id 主键。
 * @property timestamp 打卡日期，epoch millis（取当天零点，便于按天归组）。
 * @property outfit 当天穿的搭配。
 * @property mood 心情评分 1..5，可为空。
 * @property note 打卡备注，可为空。
 */
data class OutfitLog(
    val id: Long,
    val timestamp: Long,
    val outfit: Outfit,
    val mood: Int?,
    val note: String?
)
