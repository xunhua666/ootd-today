package com.ootd.today.data.local

import com.ootd.today.data.model.ClothingCategory
import com.ootd.today.data.model.ItemStatus
import com.ootd.today.data.model.WardrobeItem
import com.ootd.today.domain.vision.GarmentShapeCodec

/**
 * 搭配快照编解码器。
 *
 * 打卡记录存的是「那天穿了什么」的快照，而不是外键引用：
 * 单品日后被删除或送人，历史也不该因此残缺。
 *
 * 编码格式：单品之间用单元分隔符连接，单品内部字段用组分隔符连接。
 * 两个分隔符都是不可打印的控制字符，绝不会与用户输入的文本冲突。
 * 快照只保留展示所需的字段（id、名称、品类、主色、照片路径等），
 * 不存完整对象，避免格式随领域模型演进而频繁破坏旧数据。
 *
 * **格式演进约定**：新增字段一律**追加在末尾**，解码时用 `getOrNull`
 * 读取并给出默认值。这样旧数据（字段数更少）无需迁移即可继续读取。
 */
object OutfitSnapshotCodec {

    /** 单元分隔符 US（Unit Separator）。 */
    private val ITEM_SEPARATOR = Char(0x1F).toString()

    /** 组分隔符 RS（Record Separator）。 */
    private val FIELD_SEPARATOR = Char(0x1E).toString()

    /** 快照必须包含的最小字段数（后续新增的字段不算在内）。 */
    private const val MIN_FIELDS = 7

    fun encode(items: List<WardrobeItem>): String = items.joinToString(ITEM_SEPARATOR) { item ->
        listOf(
            item.id.toString(),
            item.name,
            item.category.name,
            item.subCategory,
            item.colorArgb.toString(),
            item.colorName,
            item.wearCount.toString(),
            // 照片路径。为空时写空串（分隔符会自然产生空字段），
            // 这样解码侧统一用 takeIf { isNotEmpty() } 还原成 null。
            item.imagePath.orEmpty(),
            GarmentShapeCodec.encode(item.shape)
        ).joinToString(FIELD_SEPARATOR)
    }

    /**
     * 解码快照。
     *
     * 对损坏/旧格式数据采取宽容策略：跳过无法解析的单元而不是抛异常，
     * 因为打卡历史属于「尽力展示」的数据，不该让整页崩溃。
     */
    fun decode(snapshot: String): List<WardrobeItem> = snapshot
        .split(ITEM_SEPARATOR)
        .filter { it.isNotBlank() }
        .mapNotNull { unit ->
            val fields = unit.split(FIELD_SEPARATOR)
            if (fields.size < MIN_FIELDS) return@mapNotNull null
            val category = runCatching { ClothingCategory.valueOf(fields[2]) }.getOrNull()
                ?: return@mapNotNull null
            WardrobeItem(
                id = fields[0].toLongOrNull() ?: return@mapNotNull null,
                name = fields[1],
                category = category,
                subCategory = fields[3],
                // v2 新增：旧快照只有 7 个字段，getOrNull 返回 null，
                // 自动回退到「无照片」的旧行为，无需数据迁移。
                imagePath = fields.getOrNull(7)?.takeIf { it.isNotEmpty() },
                colorArgb = fields[4].toLongOrNull() ?: return@mapNotNull null,
                colorName = fields[5],
                shape = GarmentShapeCodec.decode(fields.getOrNull(8).orEmpty()),
                warmthLevel = 0,
                seasons = emptySet(),
                occasions = emptySet(),
                style = null,
                brand = null,
                price = null,
                purchaseDate = null,
                lastWornDate = null,
                status = ItemStatus.ACTIVE,
                note = null,
                wearCount = fields[6].toIntOrNull() ?: 0
            )
        }
}
