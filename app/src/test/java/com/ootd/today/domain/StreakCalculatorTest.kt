package com.ootd.today.domain

import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 连续打卡天数测试。
 *
 * `now` 可注入，因此能精确构造「今天已打卡 / 今天未打卡 / 断签」等场景，
 * 不依赖真实系统时间，测试稳定可重复。
 */
class StreakCalculatorTest {

    private val base = dayOf(2026, Calendar.SEPTEMBER, 19, hour = 10)

    @Test
    fun `empty logs give zero streak`() {
        assertEquals(0, StreakCalculator.streakDays(emptyList(), now = base))
    }

    @Test
    fun `streak counts consecutive days ending today`() {
        val logs = listOf(
            dayOf(2026, Calendar.SEPTEMBER, 19),
            dayOf(2026, Calendar.SEPTEMBER, 18),
            dayOf(2026, Calendar.SEPTEMBER, 17)
        )
        assertEquals(3, StreakCalculator.streakDays(logs, now = base))
    }

    @Test
    fun `multiple logs same day count once`() {
        val logs = listOf(
            dayOf(2026, Calendar.SEPTEMBER, 19, hour = 8),
            dayOf(2026, Calendar.SEPTEMBER, 19, hour = 20),
            dayOf(2026, Calendar.SEPTEMBER, 18)
        )
        assertEquals(2, StreakCalculator.streakDays(logs, now = base))
    }

    @Test
    fun `missing today falls back to yesterday as streak start`() {
        // 今天还没打卡，但昨天和前天打了 → 连续 2 天。
        val logs = listOf(
            dayOf(2026, Calendar.SEPTEMBER, 18),
            dayOf(2026, Calendar.SEPTEMBER, 17)
        )
        assertEquals(2, StreakCalculator.streakDays(logs, now = base))
    }

    @Test
    fun `no log today nor yesterday gives zero`() {
        val logs = listOf(dayOf(2026, Calendar.SEPTEMBER, 16))
        assertEquals(0, StreakCalculator.streakDays(logs, now = base))
    }

    @Test
    fun `gap breaks the streak`() {
        // 19、18 打了，17 断了，16 打了 → 只算 2 天。
        val logs = listOf(
            dayOf(2026, Calendar.SEPTEMBER, 19),
            dayOf(2026, Calendar.SEPTEMBER, 18),
            dayOf(2026, Calendar.SEPTEMBER, 16)
        )
        assertEquals(2, StreakCalculator.streakDays(logs, now = base))
    }

    @Test
    fun `unsorted input still works`() {
        val logs = listOf(
            dayOf(2026, Calendar.SEPTEMBER, 17),
            dayOf(2026, Calendar.SEPTEMBER, 19),
            dayOf(2026, Calendar.SEPTEMBER, 18)
        )
        assertEquals(3, StreakCalculator.streakDays(logs, now = base))
    }

    private fun dayOf(year: Int, month: Int, day: Int, hour: Int = 9): Long =
        Calendar.getInstance().apply {
            set(year, month, day, hour, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
}
