package com.ootd.today.data.model

import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar
import java.util.Locale

class CalendarPlanTest {
    @Test fun invalidAndLeapDatesAreValidatedStrictly() {
        assertTrue(PlanDates.isValid("2028-02-29"))
        assertFalse(PlanDates.isValid("2026-02-29"))
        assertFalse(PlanDates.isValid("2026-04-31"))
        assertFalse(PlanDates.isValid("2026-11-00"))
        assertFalse(PlanDates.isValid("2026-1-01"))
    }
    @Test fun gridUsesMondayFirstAndHandlesSixWeekMonths() {
        val nov = PlanDates.monthCells(PlanDates.calendar("2026-11-01").timeInMillis)
        assertEquals(6, nov.indexOf(1))
        assertEquals((1..30).toList(), nov.filterNotNull())
        val march = PlanDates.monthCells(PlanDates.calendar("2025-03-01").timeInMillis)
        assertEquals(42, march.size)
        assertEquals(31, march.filterNotNull().last())
    }
    @Test fun dateKeysStayAsciiUnderDifferentLocale() {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("ar"))
            assertEquals("2026-11-04", PlanDates.key(PlanDates.calendar("2026-11-04").timeInMillis))
        } finally { Locale.setDefault(original) }
    }
    @Test fun draftNeedsTitleAndValidTimeButClothesAreOptional() {
        val draft = CalendarPlan(dateKey = "2026-11-04")
        assertNotNull(draft.validationError())
        assertNull(draft.copy(title = "画展").validationError())
        assertNotNull(draft.copy(title = "画展", startMinute = 1440).validationError())
        assertNotNull(draft.copy(title = "画展", dateKey = "2026-02-30").validationError())
        assertEquals("全天", draft.timeLabel)
        assertEquals("09:05", draft.copy(startMinute = 545).timeLabel)
    }
}
