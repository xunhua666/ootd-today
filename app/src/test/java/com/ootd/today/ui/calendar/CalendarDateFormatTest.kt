package com.ootd.today.ui.calendar

import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 日期格式测试。
 *
 * 这个格式直接拼进天气接口的 URL 参数。一旦月份或日期少补了零
 * （`2026-1-5` 而不是 `2026-01-05`），接口会直接返回 400，
 * 而界面上只是「这天没有天气」—— 这种失败很难一眼看出来，
 * 所以值得单独锁住。
 */
class CalendarDateFormatTest {

    @Test
    fun `single digit month and day are zero padded`() {
        assertEquals("2026-01-05", formatDate(at(2026, Calendar.JANUARY, 5)))
        assertEquals("2026-09-09", formatDate(at(2026, Calendar.SEPTEMBER, 9)))
    }

    @Test
    fun `double digit month and day are unchanged`() {
        assertEquals("2026-10-20", formatDate(at(2026, Calendar.OCTOBER, 20)))
        assertEquals("2026-12-31", formatDate(at(2026, Calendar.DECEMBER, 31)))
    }

    @Test
    fun `day is unaffected by time of day`() {
        // 同一天的凌晨和深夜必须给出同一个日期键，
        // 否则跨零点打卡会让天气对不上。
        assertEquals("2026-10-20", formatDate(at(2026, Calendar.OCTOBER, 20, hour = 0)))
        assertEquals("2026-10-20", formatDate(at(2026, Calendar.OCTOBER, 20, hour = 23)))
    }

    @Test
    fun `month key prefix matches the api format`() {
        // ViewModel 用前 7 位做「这个月」的判断，必须与接口的 yyyy-MM 一致。
        val date = formatDate(at(2026, Calendar.SEPTEMBER, 19))
        assertEquals("2026-09", date.substring(0, 7))
    }

    private fun at(year: Int, month: Int, day: Int, hour: Int = 9): Long =
        Calendar.getInstance().apply {
            set(year, month, day, hour, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
}