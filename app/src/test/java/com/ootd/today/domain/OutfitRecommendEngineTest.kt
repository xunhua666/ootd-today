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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 推荐引擎单元测试。
 *
 * 引擎刻意不依赖任何 Android 类型（配色用自写 RGB→HSV），
 * 因此这些测试是纯 JVM 测试，无需 Robolectric，跑得极快。
 */
class OutfitRecommendEngineTest {

    private val engine = OutfitRecommendEngine(recentDays = 7)

    // ------------------------------------------------------------------
    // 温度 → 保暖度映射
    // ------------------------------------------------------------------

    @Test
    fun `warmth mapping follows documented bands`() {
        assertEquals(1, engine.warmthForTemperature(30))
        assertEquals(1, engine.warmthForTemperature(28))
        assertEquals(2, engine.warmthForTemperature(25))
        assertEquals(3, engine.warmthForTemperature(18))
        assertEquals(4, engine.warmthForTemperature(10))
        assertEquals(5, engine.warmthForTemperature(-3))
    }

    // ------------------------------------------------------------------
    // 完整性约束
    // ------------------------------------------------------------------

    @Test
    fun `returns null when wardrobe cannot form a complete outfit`() {
        // 只有上装，没有下装/连体装和鞋，无法满足完整性约束。
        val items = listOf(top(id = 1))
        val result = engine.recommend(items, coldWeather(), Occasion.COMMUTE, Season.WINTER)
        assertNull(result)
    }

    @Test
    fun `one piece alone satisfies body requirement`() {
        val items = listOf(
            onePiece(id = 1),
            shoes(id = 2)
        )
        val result = engine.recommend(items, warmWeather(), Occasion.DATE, Season.SUMMER)
        assertNotNull(result)
        assertTrue(result!!.items.any { it.category == ClothingCategory.ONE_PIECE })
    }

    @Test
    fun `complete outfit contains top bottom and shoes`() {
        val items = listOf(top(id = 1), bottom(id = 2), shoes(id = 3))
        val result = engine.recommend(items, mildWeather(), Occasion.COMMUTE, Season.SPRING)
        assertNotNull(result)
        val categories = result!!.items.map { it.category }.toSet()
        assertTrue(categories.contains(ClothingCategory.TOP))
        assertTrue(categories.contains(ClothingCategory.BOTTOM))
        assertTrue(categories.contains(ClothingCategory.SHOES))
    }

    // ------------------------------------------------------------------
    // 场合硬性过滤
    // ------------------------------------------------------------------

    @Test
    fun `items not matching occasion are excluded`() {
        // 所有单品都只标注「居家」，却请求「正式」场合 → 硬性淘汰，无推荐。
        val homeOnly = listOf(
            top(id = 1, occasions = setOf(Occasion.HOME)),
            bottom(id = 2, occasions = setOf(Occasion.HOME)),
            shoes(id = 3, occasions = setOf(Occasion.HOME))
        )
        val result = engine.recommend(homeOnly, mildWeather(), Occasion.FORMAL, Season.SPRING)
        assertNull(result)
    }

    // ------------------------------------------------------------------
    // 已处理单品不参与推荐
    // ------------------------------------------------------------------

    @Test
    fun `retired items are never recommended`() {
        val items = listOf(
            top(id = 1, status = ItemStatus.RETIRED),
            bottom(id = 2, status = ItemStatus.RETIRED),
            shoes(id = 3, status = ItemStatus.RETIRED)
        )
        val result = engine.recommend(items, mildWeather(), Occasion.COMMUTE, Season.SPRING)
        assertNull(result)
    }

    // ------------------------------------------------------------------
    // 温度适配：冷天加外套、热天不加
    // ------------------------------------------------------------------

    @Test
    fun `cold weather adds outerwear`() {
        val items = listOf(
            top(id = 1, warmth = 3),
            bottom(id = 2, warmth = 3),
            shoes(id = 3, warmth = 2),
            outerwear(id = 4, warmth = 4)
        )
        val result = engine.recommend(items, coldWeather(), Occasion.COMMUTE, Season.WINTER)
        assertNotNull(result)
        assertTrue(result!!.items.any { it.category == ClothingCategory.OUTERWEAR })
    }

    @Test
    fun `hot weather never adds outerwear`() {
        val items = listOf(
            top(id = 1, warmth = 1),
            bottom(id = 2, warmth = 1),
            shoes(id = 3, warmth = 1),
            outerwear(id = 4, warmth = 5)
        )
        val result = engine.recommend(items, hotWeather(), Occasion.COMMUTE, Season.SUMMER)
        assertNotNull(result)
        assertTrue(result!!.items.none { it.category == ClothingCategory.OUTERWEAR })
    }

    // ------------------------------------------------------------------
    // 近期去重降权
    // ------------------------------------------------------------------

    @Test
    fun `recently worn items lose to fresh alternatives`() {
        // 两件几乎相同的上装，其中一件近期穿过 → 应优先推另一件。
        val wornTop = top(id = 1, name = "常穿的T")
        val freshTop = top(id = 2, name = "新T")
        val items = listOf(wornTop, freshTop, bottom(id = 3), shoes(id = 4))

        val result = engine.recommend(
            items = items,
            weather = mildWeather(),
            occasion = Occasion.COMMUTE,
            currentSeason = Season.SPRING,
            recentlyWornIds = setOf(1L)
        )
        assertNotNull(result)
        val chosenTop = result!!.items.first { it.category == ClothingCategory.TOP }
        assertEquals(2L, chosenTop.id)
    }

    // ------------------------------------------------------------------
    // 反馈调权
    // ------------------------------------------------------------------

    @Test
    fun `positive feedback promotes an item`() {
        val disliked = top(id = 1, name = "被嫌弃的")
        val liked = top(id = 2, name = "被喜欢的")
        val items = listOf(disliked, liked, bottom(id = 3), shoes(id = 4))

        val result = engine.recommend(
            items = items,
            weather = mildWeather(),
            occasion = Occasion.COMMUTE,
            currentSeason = Season.SPRING,
            feedbackBoost = mapOf(2L to 0.3, 1L to -0.3)
        )
        assertNotNull(result)
        val chosenTop = result!!.items.first { it.category == ClothingCategory.TOP }
        assertEquals(2L, chosenTop.id)
    }

    // ------------------------------------------------------------------
    // 随机搭配的合法性
    // ------------------------------------------------------------------

    @Test
    fun `random outfit still satisfies completeness`() {
        val items = listOf(
            top(id = 1), top(id = 2),
            bottom(id = 3), bottom(id = 4),
            shoes(id = 5), shoes(id = 6)
        )
        repeat(20) {
            val result = engine.random(items, mildWeather(), Occasion.COMMUTE, Season.SPRING)
            assertNotNull("第 $it 次随机应产出合法搭配", result)
            val categories = result!!.items.map { it.category }.toSet()
            assertTrue(categories.contains(ClothingCategory.SHOES))
            assertTrue(
                categories.contains(ClothingCategory.TOP) ||
                    categories.contains(ClothingCategory.ONE_PIECE)
            )
        }
    }

    // ------------------------------------------------------------------
    // 配饰保暖度硬约束（回归测试：曾出现 28℃ 推羊绒围巾）
    // ------------------------------------------------------------------

    @Test
    fun `hot weather never recommends warm accessories`() {
        val items = listOf(
            top(id = 1, warmth = 1),
            bottom(id = 2, warmth = 1),
            shoes(id = 3, warmth = 1),
            // 保暖 4 的围巾：热天目标保暖 1，差值 3 > 允许 2，必须被排除。
            accessory(id = 4, warmth = 4)
        )
        val result = engine.recommend(items, hotWeather(), Occasion.COMMUTE, Season.SUMMER)
        assertNotNull(result)
        assertTrue(result!!.items.none { it.category == ClothingCategory.ACCESSORY })
    }

    @Test
    fun `cold weather still allows warm accessories`() {
        val items = listOf(
            top(id = 1, warmth = 4),
            bottom(id = 2, warmth = 4),
            shoes(id = 3, warmth = 4),
            accessory(id = 4, warmth = 4)
        )
        val result = engine.recommend(items, coldWeather(), Occasion.COMMUTE, Season.WINTER)
        assertNotNull(result)
        assertTrue(result!!.items.any { it.category == ClothingCategory.ACCESSORY })
    }

    @Test
    fun `random outfit respects accessory warmth constraint`() {
        val items = listOf(
            top(id = 1, warmth = 1),
            bottom(id = 2, warmth = 1),
            shoes(id = 3, warmth = 1),
            accessory(id = 4, warmth = 5)
        )
        repeat(10) {
            val result = engine.random(items, hotWeather(), Occasion.COMMUTE, Season.SUMMER)
            assertNotNull(result)
            assertTrue(result!!.items.none { it.category == ClothingCategory.ACCESSORY })
        }
    }

    // ------------------------------------------------------------------
    // 测试数据工厂
    // ------------------------------------------------------------------

    @Test
    fun `required top cannot be replaced by a dress or competing top`() {
        val items = listOf(top(1),top(2),bottom(3),shoes(4),onePiece(5))
        val result = engine.recommend(items,mildWeather(),Occasion.COMMUTE,Season.SPRING,requiredItemId=1)
        assertNotNull(result)
        assertTrue(result!!.items.any { it.id == 1L })
        assertTrue(result.items.none { it.id == 2L || it.id == 5L })
    }

    @Test
    fun `required item still obeys occasion availability and retired status`() {
        val items = listOf(top(1,occasions=setOf(Occasion.HOME)),top(2),bottom(3),shoes(4))
        assertNull(engine.recommend(items,mildWeather(),Occasion.COMMUTE,Season.SPRING,requiredItemId=1))
        assertNull(engine.recommend(items,mildWeather(),Occasion.COMMUTE,Season.SPRING,requiredItemId=999))
        val retired = items.map { if(it.id==2L) it.copy(status=ItemStatus.RETIRED) else it }
        assertNull(engine.recommend(retired,mildWeather(),Occasion.COMMUTE,Season.SPRING,requiredItemId=2))
    }

    @Test
    fun `required winter outerwear cannot force an outfit on a hot day`() {
        val items = listOf(top(1,warmth=1),bottom(2,warmth=1),shoes(3,warmth=1),outerwear(4,warmth=5))
        assertNull(engine.recommend(items,hotWeather(),Occasion.COMMUTE,Season.SUMMER,requiredItemId=4))
    }

    @Test
    fun `required warm accessory still respects temperature constraints`() {
        val items = listOf(top(1,warmth=1),bottom(2,warmth=1),shoes(3,warmth=1),accessory(4,warmth=5))
        assertNull(engine.recommend(items,hotWeather(),Occasion.COMMUTE,Season.SUMMER,requiredItemId=4))
    }

    private fun top(
        id: Long,
        name: String = "上装$id",
        warmth: Int = 2,
        occasions: Set<Occasion> = Occasion.entries.toSet(),
        status: ItemStatus = ItemStatus.ACTIVE
    ) = item(id, name, ClothingCategory.TOP, warmth, occasions, status)

    private fun bottom(
        id: Long,
        warmth: Int = 2,
        occasions: Set<Occasion> = Occasion.entries.toSet(),
        status: ItemStatus = ItemStatus.ACTIVE
    ) = item(id, "下装$id", ClothingCategory.BOTTOM, warmth, occasions, status)

    private fun onePiece(id: Long, warmth: Int = 2) =
        item(id, "连衣裙$id", ClothingCategory.ONE_PIECE, warmth, Occasion.entries.toSet())

    private fun shoes(
        id: Long,
        warmth: Int = 2,
        occasions: Set<Occasion> = Occasion.entries.toSet(),
        status: ItemStatus = ItemStatus.ACTIVE
    ) = item(id, "鞋$id", ClothingCategory.SHOES, warmth, occasions, status)

    private fun outerwear(id: Long, warmth: Int = 4) =
        item(id, "外套$id", ClothingCategory.OUTERWEAR, warmth, Occasion.entries.toSet())

    private fun accessory(id: Long, warmth: Int = 2) =
        item(id, "配饰$id", ClothingCategory.ACCESSORY, warmth, Occasion.entries.toSet())

    private fun item(
        id: Long,
        name: String,
        category: ClothingCategory,
        warmth: Int,
        occasions: Set<Occasion>,
        status: ItemStatus = ItemStatus.ACTIVE
    ) = WardrobeItem(
        id = id,
        name = name,
        category = category,
        subCategory = "测试款",
        imagePath = null,
        colorArgb = 0xFF888888,
        colorName = "灰",
        warmthLevel = warmth,
        seasons = Season.entries.toSet(),
        occasions = occasions,
        style = StyleTag.MINIMAL,
        brand = null,
        price = null,
        purchaseDate = null,
        lastWornDate = null,
        status = status,
        note = null,
        wearCount = 0
    )

    private fun coldWeather() = weather(5)
    private fun mildWeather() = weather(18)
    private fun warmWeather() = weather(24)
    private fun hotWeather() = weather(32)

    private fun weather(temp: Int) = Weather(
        city = "杭州",
        temperatureC = temp,
        condition = WeatherCondition.SUNNY,
        highC = temp + 3,
        lowC = temp - 4,
        humidity = 50,
        aqi = 60
    )
}
