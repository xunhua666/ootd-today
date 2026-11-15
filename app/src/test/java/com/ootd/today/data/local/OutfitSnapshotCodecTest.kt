package com.ootd.today.data.local

import com.ootd.today.data.model.ClothingCategory
import com.ootd.today.data.model.ItemStatus
import com.ootd.today.data.model.Occasion
import com.ootd.today.data.model.Season
import com.ootd.today.data.model.StyleTag
import com.ootd.today.data.model.WardrobeItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 搭配快照编解码测试。
 *
 * 快照是打卡历史的持久化格式，一旦损坏历史就残缺，
 * 因此往返一致性与容错性都必须有测试兜底。
 */
class OutfitSnapshotCodecTest {

    @Test
    fun `encode then decode preserves display fields`() {
        val items = listOf(
            item(id = 1, name = "雾霾蓝卫衣", category = ClothingCategory.TOP, color = 0xFF7C9CBF),
            item(id = 2, name = "直筒牛仔裤", category = ClothingCategory.BOTTOM, color = 0xFF3A5F8A),
            item(id = 3, name = "德训小白鞋", category = ClothingCategory.SHOES, color = 0xFFFAFAFA)
        )

        val snapshot = OutfitSnapshotCodec.encode(items)
        val decoded = OutfitSnapshotCodec.decode(snapshot)

        assertEquals(items.size, decoded.size)
        items.zip(decoded).forEach { (original, restored) ->
            assertEquals(original.id, restored.id)
            assertEquals(original.name, restored.name)
            assertEquals(original.category, restored.category)
            assertEquals(original.colorArgb, restored.colorArgb)
        }
    }

    @Test
    fun `names containing commas and spaces survive round trip`() {
        // 快照内部用控制字符分隔，用户输入的逗号/空格不应破坏格式。
        val items = listOf(
            item(id = 7, name = "条纹, 带空格 与逗号", category = ClothingCategory.ONE_PIECE)
        )
        val decoded = OutfitSnapshotCodec.decode(OutfitSnapshotCodec.encode(items))
        assertEquals("条纹, 带空格 与逗号", decoded.single().name)
    }

    @Test
    fun `empty list encodes to empty string and decodes back empty`() {
        val snapshot = OutfitSnapshotCodec.encode(emptyList())
        assertEquals("", snapshot)
        assertTrue(OutfitSnapshotCodec.decode(snapshot).isEmpty())
    }

    @Test
    fun `corrupted snapshot is skipped rather than throwing`() {
        // 字段数不足的单元应被跳过，而不是让整页崩溃。
        val good = OutfitSnapshotCodec.encode(
            listOf(item(id = 1, name = "正常", category = ClothingCategory.TOP))
        )
        val corrupted = "garbage" + Char(0x1F) + good

        val decoded = OutfitSnapshotCodec.decode(corrupted)
        assertEquals(1, decoded.size)
        assertEquals("正常", decoded.single().name)
    }

    @Test
    fun `unknown category in snapshot is skipped`() {
        val fields = listOf("1", "名字", "NOT_A_CATEGORY", "T恤", "123", "灰", "0")
            .joinToString(Char(0x1E).toString())
        assertTrue(OutfitSnapshotCodec.decode(fields).isEmpty())
    }

    // ------------------------------------------------------------------
    // 格式兼容性：新增字段必须不破坏旧数据
    // ------------------------------------------------------------------

    @Test
    fun `photo path survives round trip`() {
        val items = listOf(
            item(id = 1, name = "带照片的", category = ClothingCategory.TOP)
                .copy(imagePath = "/data/user/0/com.ootd.today/files/photos/item_1.jpg")
        )
        val decoded = OutfitSnapshotCodec.decode(OutfitSnapshotCodec.encode(items))
        assertEquals(
            "/data/user/0/com.ootd.today/files/photos/item_1.jpg",
            decoded.single().imagePath
        )
    }

    @Test
    fun `item without photo decodes imagePath to null`() {
        val items = listOf(item(id = 1, name = "无照片", category = ClothingCategory.TOP))
        val decoded = OutfitSnapshotCodec.decode(OutfitSnapshotCodec.encode(items))
        assertNull(decoded.single().imagePath)
    }

    @Test
    fun `legacy v1 snapshot without photo field is still readable`() {
        // 模拟 v1 时代的 7 字段快照（无 imagePath）。
        // 这条测试锁住「新增字段追加在末尾 + getOrNull 读默认值」的演进约定，
        // 防止以后有人改动字段顺序导致老用户的打卡历史读不出来。
        val legacy = listOf("1", "旧数据", "TOP", "T恤", "16711680", "红", "3")
            .joinToString(Char(0x1E).toString())

        val decoded = OutfitSnapshotCodec.decode(legacy)
        assertEquals(1, decoded.size)
        assertEquals("旧数据", decoded.single().name)
        assertEquals(3, decoded.single().wearCount)
        assertNull(decoded.single().imagePath)
    }

    private fun item(
        id: Long,
        name: String,
        category: ClothingCategory,
        color: Long = 0xFF888888
    ) = WardrobeItem(
        id = id,
        name = name,
        category = category,
        subCategory = "测试款",
        imagePath = null,
        colorArgb = color,
        colorName = "灰",
        warmthLevel = 2,
        seasons = setOf(Season.SPRING),
        occasions = setOf(Occasion.COMMUTE),
        style = StyleTag.MINIMAL,
        brand = null,
        price = null,
        purchaseDate = null,
        lastWornDate = null,
        status = ItemStatus.ACTIVE,
        note = null,
        wearCount = 3
    )
}
