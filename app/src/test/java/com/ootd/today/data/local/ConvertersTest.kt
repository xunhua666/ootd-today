package com.ootd.today.data.local

import com.ootd.today.data.model.ClothingCategory
import com.ootd.today.data.model.ItemStatus
import com.ootd.today.data.model.Occasion
import com.ootd.today.data.model.Season
import com.ootd.today.data.model.StyleTag
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Room 类型转换器往返测试。
 *
 * 转换器是数据库与领域模型之间的唯一桥梁，格式一旦写错，
 * 存进去的数据读出来就是坏的，且难以事后修复，因此必须测试。
 */
class ConvertersTest {

    private val converters = Converters()

    @Test
    fun `seasons round trip`() {
        val seasons = setOf(Season.SPRING, Season.AUTUMN, Season.WINTER)
        assertEquals(seasons, converters.stringToSeasons(converters.seasonsToString(seasons)))
    }

    @Test
    fun `empty season set round trips to empty`() {
        assertEquals(emptySet<Season>(), converters.stringToSeasons(converters.seasonsToString(emptySet())))
    }

    @Test
    fun `occasions round trip`() {
        val occasions = setOf(Occasion.COMMUTE, Occasion.DATE, Occasion.SPORT, Occasion.VACATION)
        assertEquals(occasions, converters.stringToOccasions(converters.occasionsToString(occasions)))
    }

    @Test
    fun `category round trip`() {
        ClothingCategory.entries.forEach { category ->
            assertEquals(category, converters.stringToCategory(converters.categoryToString(category)))
        }
    }

    @Test
    fun `status round trip`() {
        ItemStatus.entries.forEach { status ->
            assertEquals(status, converters.stringToStatus(converters.statusToString(status)))
        }
    }

    @Test
    fun `style round trip including null`() {
        StyleTag.entries.forEach { tag ->
            assertEquals(tag, converters.stringToStyle(converters.styleToString(tag)))
        }
        assertNull(converters.stringToStyle(converters.styleToString(null)))
    }

    @Test
    fun `single occasion round trip`() {
        Occasion.entries.forEach { occasion ->
            assertEquals(occasion, converters.stringToOccasion(converters.occasionToString(occasion)))
        }
    }
}
