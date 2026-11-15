package com.ootd.today.domain

import com.ootd.today.data.model.ClothingCategory
import com.ootd.today.data.model.ItemStatus
import com.ootd.today.data.model.Occasion
import com.ootd.today.data.model.Season
import com.ootd.today.data.model.StyleTag
import com.ootd.today.data.model.WardrobeItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 整套搭配的审美评分测试。
 *
 * 用纯色构造用例（红 0°、黄 60°、绿 120°、青 180°、蓝 240°），
 * 色相是整数，断言才好写也好看懂。
 */
class OutfitAestheticsTest {

    // ------------------------------------------------------------------
    // 配色
    // ------------------------------------------------------------------

    @Test
    fun `all neutral colors score full palette`() {
        // 黑白灰随便搭都成立，不该因为「没颜色」被扣分。
        val items = listOf(
            item(0xFFFFFFFF.toInt(), StyleTag.MINIMAL),
            item(0xFF808080.toInt(), StyleTag.MINIMAL),
            item(0xFF000000.toInt(), StyleTag.MINIMAL)
        )
        assertEquals(1.0, OutfitAesthetics.score(items), 0.001)
    }

    @Test
    fun `same hue family beats awkward middle ground`() {
        // 红 + 橙（差 30°）明显好过 红 + 黄绿（差 90°，不上不下）。
        val sameHue = listOf(item(RED, StyleTag.STREET), item(ORANGE, StyleTag.STREET))
        val awkward = listOf(item(RED, StyleTag.STREET), item(CHARTREUSE, StyleTag.STREET))

        assertTrue(
            "同色系应高于尴尬区，实际 ${OutfitAesthetics.score(sameHue)} vs ${OutfitAesthetics.score(awkward)}",
            OutfitAesthetics.score(sameHue) > OutfitAesthetics.score(awkward)
        )
    }

    @Test
    fun `clash colors are rewarded`() {
        // 红 + 青（互补）在本应用的活泼调性里是加分项，应高于尴尬区。
        val clash = listOf(item(RED, StyleTag.STREET), item(CYAN, StyleTag.STREET))
        val awkward = listOf(item(RED, StyleTag.STREET), item(CHARTREUSE, StyleTag.STREET))

        assertTrue(OutfitAesthetics.score(clash) > OutfitAesthetics.score(awkward))
    }

    @Test
    fun `too many color families are penalised`() {
        // 四个色系 vs 三个色系：都算撞色，但前者更花。
        val three = listOf(
            item(RED, StyleTag.STREET),
            item(GREEN, StyleTag.STREET),
            item(BLUE, StyleTag.STREET)
        )
        val four = listOf(
            item(RED, StyleTag.STREET),
            item(YELLOW, StyleTag.STREET),
            item(CYAN, StyleTag.STREET),
            item(MAGENTA, StyleTag.STREET)
        )

        assertTrue(
            "色系越多应越低，实际 ${OutfitAesthetics.score(three)} vs ${OutfitAesthetics.score(four)}",
            OutfitAesthetics.score(three) > OutfitAesthetics.score(four)
        )
    }

    @Test
    fun `neutral pieces do not count toward color families`() {
        // 红 + 黑 + 白，实际只有一个色系，应与「只有红」一样干净。
        val withNeutrals = listOf(
            item(RED, StyleTag.MINIMAL),
            item(0xFF000000.toInt(), StyleTag.MINIMAL),
            item(0xFFFFFFFF.toInt(), StyleTag.MINIMAL)
        )
        assertEquals(1.0, OutfitAesthetics.score(withNeutrals), 0.001)
    }

    // ------------------------------------------------------------------
    // 风格
    // ------------------------------------------------------------------

    @Test
    fun `matching styles score higher than mixed styles`() {
        val coherent = listOf(item(BLACK, StyleTag.STREET), item(BLACK, StyleTag.STREET))
        val mixed = listOf(item(BLACK, StyleTag.SWEET), item(BLACK, StyleTag.TECHWEAR))

        assertTrue(OutfitAesthetics.score(coherent) > OutfitAesthetics.score(mixed))
    }

    @Test
    fun `three clashing styles score worst`() {
        val two = listOf(item(BLACK, StyleTag.STREET), item(BLACK, StyleTag.SWEET))
        val three = listOf(
            item(BLACK, StyleTag.STREET),
            item(BLACK, StyleTag.SWEET),
            item(BLACK, StyleTag.TECHWEAR)
        )

        assertTrue(OutfitAesthetics.score(two) > OutfitAesthetics.score(three))
    }

    @Test
    fun `missing styles do not count as coherent`() {
        // 全都没填风格时不能判满分，否则引擎会偏好「没有风格信息」的单品。
        val unknown = listOf(item(BLACK, style = null), item(BLACK, style = null))
        val known = listOf(item(BLACK, StyleTag.STREET), item(BLACK, StyleTag.STREET))

        assertTrue(OutfitAesthetics.score(known) > OutfitAesthetics.score(unknown))
    }

    // ------------------------------------------------------------------
    // 边界
    // ------------------------------------------------------------------

    @Test
    fun `single item has no combination to judge`() {
        // 一件单品（如只有连衣裙）没有「搭配」可言，给满分避免干扰。
        assertEquals(1.0, OutfitAesthetics.score(listOf(item(RED, StyleTag.SWEET))), 0.001)
    }

    @Test
    fun `empty list scores full`() {
        assertEquals(1.0, OutfitAesthetics.score(emptyList()), 0.001)
    }

    // ------------------------------------------------------------------
    // 文字描述
    // ------------------------------------------------------------------

    @Test
    fun `describe talks about palette not about score`() {
        assertEquals(
            "配色干净",
            OutfitAesthetics.describe(listOf(item(BLACK, StyleTag.MINIMAL), item(WHITE, StyleTag.MINIMAL)))
        )
        assertEquals(
            "同色系层次",
            OutfitAesthetics.describe(listOf(item(RED, StyleTag.MINIMAL), item(ORANGE, StyleTag.MINIMAL)))
        )
        assertEquals(
            "撞色大胆",
            OutfitAesthetics.describe(listOf(item(RED, StyleTag.MINIMAL), item(CYAN, StyleTag.MINIMAL)))
        )
    }

    // ------------------------------------------------------------------
    // 数据工厂
    // ------------------------------------------------------------------

    private fun item(color: Int, style: StyleTag?) = WardrobeItem(
        id = color.toLong(),
        name = "测试单品",
        category = ClothingCategory.TOP,
        subCategory = "",
        imagePath = null,
        colorArgb = color.toLong() and 0xFFFFFFFFL,
        colorName = "",
        warmthLevel = 2,
        seasons = Season.entries.toSet(),
        occasions = Occasion.entries.toSet(),
        style = style,
        brand = null,
        price = null,
        purchaseDate = null,
        lastWornDate = null,
        status = ItemStatus.ACTIVE,
        note = null,
        wearCount = 0
    )

    private companion object {
        const val RED = 0xFFFF0000.toInt()
        const val ORANGE = 0xFFFF8000.toInt()   // 约 30°
        const val YELLOW = 0xFFFFFF00.toInt()   // 60°
        const val CHARTREUSE = 0xFF80FF00.toInt() // 约 90°
        const val GREEN = 0xFF00FF00.toInt()    // 120°
        const val CYAN = 0xFF00FFFF.toInt()     // 180°
        const val BLUE = 0xFF0000FF.toInt()     // 240°
        const val MAGENTA = 0xFFFF00FF.toInt()  // 300°
        const val BLACK = 0xFF000000.toInt()
        const val WHITE = 0xFFFFFFFF.toInt()
    }
}