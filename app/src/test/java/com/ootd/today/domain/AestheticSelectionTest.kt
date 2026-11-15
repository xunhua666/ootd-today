package com.ootd.today.domain

import com.ootd.today.data.model.ClothingCategory
import com.ootd.today.data.model.ItemStatus
import com.ootd.today.data.model.Occasion
import com.ootd.today.data.model.Season
import com.ootd.today.data.model.StyleTag
import com.ootd.today.data.model.WardrobeItem
import com.ootd.today.data.model.Weather
import com.ootd.today.data.model.WeatherCondition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 验证「审美」真的参与决策，而不只是算了个分放着。
 *
 * 两个层次：
 * 1. 单件分相同时，选出配色/风格更协调的那套；
 * 2. 协调与不协调的两套搭配，最终得分应拉开差距。
 */
class AestheticSelectionTest {

    private val engine = OutfitRecommendEngine()

    @Test
    fun `picks the harmonious bottom when single item scores are equal`() {
        // 两件下装除了颜色完全一样，所以单件分相同 —— 决定权全在审美。
        val items = listOf(
            top(1, BLUE),
            bottom(2, GREEN),       // 与蓝相差 120°，落在最难看的那一段
            bottom(3, NEAR_BLUE),   // 与蓝同一色系
            shoes(4, BLACK)
        )

        val result = engine.recommend(
            items = items,
            weather = weather(),
            occasion = Occasion.COMMUTE,
            currentSeason = Season.SPRING
        )

        assertNotNull(result)
        val chosen = result!!.items.first { it.category == ClothingCategory.BOTTOM }
        assertEquals("应选与上装同色系的那件", 3L, chosen.id)
    }

    @Test
    fun `style coherence breaks ties too`() {
        // 两件下装单件分相同，一件与上装同风格、一件完全相反。
        val items = listOf(
            top(1, BLACK, style = StyleTag.SWEET),
            bottom(2, BLACK, style = StyleTag.TECHWEAR),
            bottom(3, BLACK, style = StyleTag.SWEET),
            shoes(4, BLACK, style = StyleTag.SWEET)
        )

        val result = engine.recommend(
            items = items,
            weather = weather(),
            occasion = Occasion.COMMUTE,
            currentSeason = Season.SPRING
        )

        assertNotNull(result)
        val chosen = result!!.items.first { it.category == ClothingCategory.BOTTOM }
        assertEquals("应选与整体风格一致的那件", 3L, chosen.id)
    }

    @Test
    fun `aesthetic makes the final score reflect the palette`() {
        val harmonious = engine.recommend(
            items = listOf(
                top(1, BLUE),
                bottom(2, NEAR_BLUE),
                shoes(3, BLACK)
            ),
            weather = weather(),
            occasion = Occasion.COMMUTE,
            currentSeason = Season.SPRING
        )
        val clashing = engine.recommend(
            items = listOf(
                top(1, BLUE),
                bottom(2, CHARTREUSE), // 90°，最不协调
                shoes(3, BLACK)
            ),
            weather = weather(),
            occasion = Occasion.COMMUTE,
            currentSeason = Season.SPRING
        )

        assertNotNull(harmonious)
        assertNotNull(clashing)
        assertTrue(
            "协调的配色得分应更高：${harmonious!!.score} vs ${clashing!!.score}",
            harmonious.score > clashing.score
        )
    }

    @Test
    fun `accessory is chosen to harmonise with the core`() {
        // 两个包单件分相同，一个跟主体同色系、一个颜色突兀。
        val items = listOf(
            top(1, BLUE),
            bottom(2, NEAR_BLUE),
            shoes(3, BLACK),
            bag(4, ORANGE),      // 与蓝撞得很突兀
            bag(5, NEAR_BLUE)    // 与主体一致
        )

        val result = engine.recommend(
            items = items,
            weather = weather(),
            occasion = Occasion.COMMUTE,
            currentSeason = Season.SPRING
        )

        assertNotNull(result)
        val chosen = result!!.items.firstOrNull { it.category == ClothingCategory.BAG }
        assertNotNull("应当选出一个包", chosen)
        assertEquals("应选与整体配色合得来的包", 5L, chosen!!.id)
    }

    // ------------------------------------------------------------------
    // 数据工厂：除了要考察的维度，其余一律相同，保证对比公平
    // ------------------------------------------------------------------

    private fun top(id: Long, color: Int, style: StyleTag = StyleTag.MINIMAL) =
        item(id, "上装", ClothingCategory.TOP, color, style)

    private fun bottom(id: Long, color: Int, style: StyleTag = StyleTag.MINIMAL) =
        item(id, "下装", ClothingCategory.BOTTOM, color, style)

    private fun shoes(id: Long, color: Int, style: StyleTag = StyleTag.MINIMAL) =
        item(id, "鞋", ClothingCategory.SHOES, color, style)

    private fun bag(id: Long, color: Int, style: StyleTag = StyleTag.MINIMAL) =
        item(id, "包", ClothingCategory.BAG, color, style)

    private fun item(
        id: Long,
        name: String,
        category: ClothingCategory,
        color: Int,
        style: StyleTag
    ) = WardrobeItem(
        id = id,
        name = name,
        category = category,
        subCategory = "",
        imagePath = null,
        colorArgb = color.toLong() and 0xFFFFFFFFL,
        colorName = "",
        // 20℃ 的目标保暖度是 3，全部给 3，消除保暖带来的分差。
        warmthLevel = 3,
        seasons = setOf(Season.SPRING),
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

    /** 20℃：目标保暖度 3，且不需要外套，排除外套干扰。 */
    private fun weather() = Weather(
        city = "测试",
        temperatureC = 20,
        condition = WeatherCondition.SUNNY,
        highC = 23,
        lowC = 16,
        humidity = 50,
        aqi = 60
    )

    private companion object {
        const val ORANGE = 0xFFFF8000.toInt()     // 约 30°
        const val CHARTREUSE = 0xFF80FF00.toInt() // 约 90°
        const val GREEN = 0xFF00FF00.toInt()      // 120°
        const val BLUE = 0xFF0000FF.toInt()       // 240°
        const val NEAR_BLUE = 0xFF0040FF.toInt()  // 约 225°，与蓝同色系
        const val BLACK = 0xFF000000.toInt()
    }
}