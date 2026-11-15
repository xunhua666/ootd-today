package com.ootd.today.domain.stylist

import com.ootd.today.data.model.ClothingCategory
import com.ootd.today.data.model.ItemStatus
import com.ootd.today.data.model.Occasion
import com.ootd.today.data.model.Season
import com.ootd.today.data.model.StyleTag
import com.ootd.today.data.model.WardrobeItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * 大模型返回结果的解析测试。
 *
 * 重点覆盖「模型不守格式」的各种情况 —— 这是接入大模型时最容易线上出问题的地方：
 * 包代码块、夹带解释文字、字段缺失、甚至编造出衣橱里不存在的 id。
 */
class StylistAdviceParserTest {

    private val pool = listOf(
        item(1, "雾霾蓝卫衣", ClothingCategory.TOP),
        item(2, "直筒牛仔裤", ClothingCategory.BOTTOM),
        item(3, "德训小白鞋", ClothingCategory.SHOES)
    )

    @Test
    fun `parses a clean json response`() {
        val raw = """
            {"itemIds":[1,2,3],"headline":"蓝色系通勤","reason":"同色系显干净","tip":"鞋子保持白色"}
        """.trimIndent()

        val advice = StylistAdviceParser.parse(raw, pool)

        assertNotNull(advice)
        assertEquals(listOf(1L, 2L, 3L), advice!!.items.map { it.id })
        assertEquals("蓝色系通勤", advice.headline)
        assertEquals("同色系显干净", advice.reason)
        assertEquals("鞋子保持白色", advice.tip)
    }

    @Test
    fun `parses response wrapped in markdown code fence`() {
        // 实测中模型很爱加代码块，即使提示词里明确说了不要。
        val raw = """
            ```json
            {"itemIds":[1,2,3],"headline":"简洁","reason":"理由","tip":""}
            ```
        """.trimIndent()

        assertNotNull(StylistAdviceParser.parse(raw, pool))
    }

    @Test
    fun `parses response with surrounding prose`() {
        val raw = "好的，这是我的建议：\n" +
            """{"itemIds":[1,2,3],"headline":"简洁","reason":"理由","tip":""}""" +
            "\n希望你喜欢。"

        assertNotNull(StylistAdviceParser.parse(raw, pool))
    }

    @Test
    fun `fabricated item ids are dropped`() {
        // 模型编出衣橱里没有的 id 是最常见的越界行为，必须过滤掉。
        val raw = """{"itemIds":[1,999,2,3],"headline":"h","reason":"r","tip":""}"""

        val advice = StylistAdviceParser.parse(raw, pool)

        assertNotNull(advice)
        assertEquals(listOf(1L, 2L, 3L), advice!!.items.map { it.id })
    }

    @Test
    fun `duplicate ids are collapsed`() {
        val raw = """{"itemIds":[1,1,2,3],"headline":"h","reason":"r","tip":""}"""

        val advice = StylistAdviceParser.parse(raw, pool)

        assertNotNull(advice)
        assertEquals(3, advice!!.items.size)
    }

    @Test
    fun `returns null when too few valid items remain`() {
        // 只剩一件构不成搭配，宁可让上层回退到本地算法，也不要给半套。
        val raw = """{"itemIds":[1,999,888],"headline":"h","reason":"r","tip":""}"""
        assertNull(StylistAdviceParser.parse(raw, pool))
    }

    @Test
    fun `returns null when itemIds is missing`() {
        assertNull(StylistAdviceParser.parse("""{"headline":"h","reason":"r"}""", pool))
    }

    @Test
    fun `returns null on malformed text`() {
        assertNull(StylistAdviceParser.parse("模型今天不想干活", pool))
        assertNull(StylistAdviceParser.parse("", pool))
    }

    @Test
    fun `missing headline falls back to a default`() {
        val raw = """{"itemIds":[1,2,3],"reason":"理由","tip":""}"""

        val advice = StylistAdviceParser.parse(raw, pool)

        assertNotNull(advice)
        assertEquals("今日搭配", advice!!.headline)
    }

    @Test
    fun `blank tip becomes null`() {
        val raw = """{"itemIds":[1,2,3],"headline":"h","reason":"r","tip":"   "}"""
        assertNull(StylistAdviceParser.parse(raw, pool)!!.tip)
    }

    private fun item(id: Long, name: String, category: ClothingCategory) = WardrobeItem(
        id = id,
        name = name,
        category = category,
        subCategory = "",
        imagePath = null,
        colorArgb = 0xFF888888,
        colorName = "灰",
        warmthLevel = 3,
        seasons = Season.entries.toSet(),
        occasions = Occasion.entries.toSet(),
        style = StyleTag.MINIMAL,
        brand = null,
        price = null,
        purchaseDate = null,
        lastWornDate = null,
        status = ItemStatus.ACTIVE,
        note = null,
        wearCount = 0
    )
}