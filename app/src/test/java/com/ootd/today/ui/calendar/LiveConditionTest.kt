package com.ootd.today.ui.calendar

import com.ootd.today.data.model.DailyWeather
import com.ootd.today.data.model.WeatherCondition
import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

/**
 * 「今天用实况、其余用逐日」的替换逻辑测试。
 *
 * 这件事容易改坏的地方有两个：
 * 1. 手一抖把所有日期都换成实况，历史就全错了；
 * 2. 顺手把高低温也换成实况的瞬时值，丢掉当天区间。
 * 两条都固定下来。
 */
class LiveConditionTest {

    private val october = monthStartOf(2026, Calendar.OCTOBER)
    private val now = at(2026, Calendar.OCTOBER, 20)

    @Test
    fun `only today gets the live condition`() {
        val weather = mapOf(
            19 to day(19, WeatherCondition.RAIN),
            20 to day(20, WeatherCondition.RAIN),
            21 to day(21, WeatherCondition.RAIN)
        )

        val result = weather.withLiveConditionForToday(october, WeatherCondition.CLOUDY, now)

        assertEquals(WeatherCondition.RAIN, result[19]?.condition)
        assertEquals(WeatherCondition.CLOUDY, result[20]?.condition)
        assertEquals(WeatherCondition.RAIN, result[21]?.condition)
    }

    @Test
    fun `temperature range is preserved`() {
        val weather = mapOf(20 to day(20, WeatherCondition.RAIN, high = 31, low = 20))

        val result = weather.withLiveConditionForToday(october, WeatherCondition.SUNNY, now)

        assertEquals(31, result[20]?.highC)
        assertEquals(20, result[20]?.lowC)
    }

    @Test
    fun `other months are untouched`() {
        // 看的是 9 月，即使当天有实况也不该改 9 月里的任何一天。
        val september = monthStartOf(2026, Calendar.SEPTEMBER)
        val weather = mapOf(20 to day(20, WeatherCondition.RAIN))

        val result = weather.withLiveConditionForToday(september, WeatherCondition.SUNNY, now)

        assertEquals(WeatherCondition.RAIN, result[20]?.condition)
    }

    @Test
    fun `no live condition leaves data as is`() {
        val weather = mapOf(20 to day(20, WeatherCondition.RAIN))

        val result = weather.withLiveConditionForToday(october, null, now)

        assertSame(weather, result)
    }

    @Test
    fun `today missing from data does not invent an entry`() {
        // 逐日接口没返回今天（可能失败或超范围）时，不该凭空造一条。
        val weather = mapOf(19 to day(19, WeatherCondition.RAIN))

        val result = weather.withLiveConditionForToday(october, WeatherCondition.SUNNY, now)

        assertEquals(setOf(19), result.keys)
    }

    private fun day(
        date: Int,
        condition: WeatherCondition,
        high: Int = 25,
        low: Int = 15
    ) = DailyWeather(
        dateKey = "2026-10-${date.toString().padStart(2, '0')}",
        condition = condition,
        highC = high,
        lowC = low
    )

    private fun monthStartOf(year: Int, month: Int): Long = Calendar.getInstance().apply {
        set(year, month, 1, 0, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun at(year: Int, month: Int, day: Int): Long = Calendar.getInstance().apply {
        set(year, month, day, 12, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}