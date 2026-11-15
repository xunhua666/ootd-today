package com.ootd.today.data.model

import java.util.Calendar

data class CalendarPlan(
    val id: Long = 0,
    val dateKey: String,
    val title: String = "",
    val startMinute: Int? = null,
    val occasion: Occasion = Occasion.COMMUTE,
    val location: String = "",
    val note: String = "",
    val items: List<WardrobeItem> = emptyList(),
    val completed: Boolean = false
) {
    val timeLabel: String get() = startMinute?.let { "%02d:%02d".format(it / 60, it % 60) } ?: "全天"
    fun validationError(): String? = when {
        title.isBlank() -> "给日程起一个名字吧"
        title.length > 60 -> "日程标题最多 60 字"
        !PlanDates.isValid(dateKey) -> "请选择有效日期"
        startMinute != null && startMinute !in 0..1439 -> "请选择有效时间"
        location.length > 100 || note.length > 500 -> "地点或备注过长"
        items.any { it.isDemo } -> "体验单品不能保存到个人日程"
        else -> null
    }
}

/** Date-only keys retain the intended calendar day when a user changes time zone. */
object PlanDates {
    fun calendar(key: String): Calendar = Calendar.getInstance().apply {
        isLenient = false
        clear()
        val parts = key.split('-')
        require(parts.size == 3)
        set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt(), 12, 0, 0)
        timeInMillis // Validate real month lengths, including leap years.
    }
    fun isValid(key: String): Boolean = Regex("[1-9][0-9]{3}-[0-9]{2}-[0-9]{2}").matches(key) && runCatching { calendar(key) }.isSuccess
    fun key(millis: Long): String = Calendar.getInstance().run {
        timeInMillis = millis
        "%04d-%02d-%02d".format(java.util.Locale.ROOT, get(Calendar.YEAR), get(Calendar.MONTH) + 1, get(Calendar.DAY_OF_MONTH))
    }
    fun monthCells(monthStart: Long): List<Int?> {
        val month = Calendar.getInstance().apply { timeInMillis = monthStart; set(Calendar.DAY_OF_MONTH, 1) }
        val leading = (month.get(Calendar.DAY_OF_WEEK) + 5) % 7
        return buildList {
            repeat(leading) { add(null) }
            for (day in 1..month.getActualMaximum(Calendar.DAY_OF_MONTH)) add(day)
            while (size % 7 != 0) add(null)
        }
    }
}
