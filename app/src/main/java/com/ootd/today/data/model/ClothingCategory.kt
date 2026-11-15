package com.ootd.today.data.model

/**
 * 单品大类。
 *
 * 每个大类都带有展示用的中文名与 emoji 图标。在 P0 阶段单品还没有真实照片时，
 * emoji 会配合单品主色一起渲染成色块占位图，保证界面依然有视觉信息量。
 *
 * @property label 中文名，用于筛选 Chip、详情页。
 * @property emoji 占位图标。
 * @property subCategories 该品类下的细分款式，录入时作为二级选择项。
 */
enum class ClothingCategory(
    val label: String,
    val emoji: String,
    val subCategories: List<String>
) {
    TOP("上装", "👕", listOf("T恤", "衬衫", "卫衣", "针织毛衣", "背心")),
    BOTTOM("下装", "👖", listOf("牛仔裤", "休闲裤", "西裤", "半裙", "短裤")),
    ONE_PIECE("连体", "👗", listOf("连衣裙", "连体裤")),
    OUTERWEAR("外套", "🧥", listOf("风衣", "大衣", "皮衣", "羽绒服", "西装外套", "夹克")),
    SHOES("鞋", "👟", listOf("运动鞋", "皮鞋", "靴子", "凉鞋", "高跟鞋")),
    BAG("包", "👜", listOf("双肩包", "单肩包", "手提包")),
    ACCESSORY("配饰", "🧢", listOf("帽子", "围巾", "墨镜", "项链", "手表", "袜子"))
}
