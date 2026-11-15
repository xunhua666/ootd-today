package com.ootd.today.data.model

/**
 * 一件衣橱单品。
 *
 * 日期字段统一用 epoch millis（`Long`）而非 `java.time.LocalDate`：
 * 项目 minSdk 24，而 `java.time` 需要 API 26 或开启 core library desugaring，
 * 用时间戳可以避免额外引入脱糖依赖。
 *
 * @property id 主键。
 * @property name 单品名称，如「雾霾蓝 oversize 卫衣」。
 * @property category 大类。
 * @property subCategory 细分款式，取自 [ClothingCategory.subCategories]。
 * @property imagePath 本地图片绝对路径；为 null 时用主色 + emoji 渲染占位图。
 * @property colorArgb 主色调 ARGB 值，如 `0xFF7C9CBF`。
 * @property colorName 主色调名称，如「雾霾蓝」。
 * @property warmthLevel 保暖度 1..5，推荐算法做温度匹配的核心依据。
 * @property seasons 适用季节，可多选。
 * @property occasions 适用场合，可多选。
 * @property style 风格标签，可为空。
 * @property brand 品牌，可为空。
 * @property price 购入价格，可为空；为 P1「闲置率/性价比」统计预留。
 * @property purchaseDate 购入日期，可为空。
 * @property lastWornDate 最后一次穿着日期，由打卡自动更新。
 * @property status 在用/已处理。
 * @property note 自定义备注。
 * @property wearCount 累计穿着次数，用于闲置唤醒加权。
 */
data class WardrobeItem(
    val id: Long,
    val name: String,
    val category: ClothingCategory,
    val subCategory: String,
    val imagePath: String?,
    val colorArgb: Long,
    val colorName: String,
    val warmthLevel: Int,
    val seasons: Set<Season>,
    val occasions: Set<Occasion>,
    val style: StyleTag?,
    val brand: String?,
    val price: Double?,
    val purchaseDate: Long?,
    val lastWornDate: Long?,
    val status: ItemStatus,
    val note: String?,
    val wearCount: Int,
    val shape: GarmentShape = GarmentShape()
) {
    /** Negative IDs are reserved for transient experience items, never saved to Room. */
    val isDemo: Boolean get() = id < 0

    companion object {
        /** 保暖度取值范围，录入表单与算法校验共用。 */
        val WARMTH_RANGE = 1..5
    }
}
