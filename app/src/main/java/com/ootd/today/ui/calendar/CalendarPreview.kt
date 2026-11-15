package com.ootd.today.ui.calendar

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ootd.today.data.model.ClothingCategory
import com.ootd.today.data.model.DailyWeather
import com.ootd.today.data.model.ItemStatus
import com.ootd.today.data.model.Occasion
import com.ootd.today.data.model.Outfit
import com.ootd.today.data.model.OutfitLog
import com.ootd.today.data.model.Season
import com.ootd.today.data.model.StyleTag
import com.ootd.today.data.model.WardrobeItem
import com.ootd.today.data.model.WeatherCondition
import com.ootd.today.ui.theme.OotdTodayTheme
import java.util.Calendar

/**
 * 日历排版验证。
 *
 * 重点看两处以前出问题的地方：
 * 1. 星期表头与下面的格子是否**逐列对齐**（旧版表头用了整宽等分、
 *    格子却减去了间距，导致越往右偏得越多）；
 * 2. 格子里的日期、天气、色点是否互不重叠（旧版色条会顶到日期上）。
 */
@Preview(name = "日历 · 2026年9月", showBackground = true, widthDp = 380, heightDp = 760)
@Composable
private fun CalendarMonthPreview() {
    OotdTodayTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.padding(vertical = 12.dp)) {
                MonthCalendar(
                    monthStart = monthStartOf(2026, Calendar.SEPTEMBER),
                    logsByDay = sampleLogsByDay(),
                    weatherByDay = sampleWeatherByDay(),
                    onPrevious = {},
                    onNext = {},
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )
            }
        }
    }
}

@Preview(name = "记录卡片 · 带天气", showBackground = true, widthDp = 380)
@Composable
private fun CalendarLogCardPreview() {
    OotdTodayTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.padding(16.dp)) {
                LogCard(
                    log = sampleLog(day = 19, occasion = Occasion.COMMUTE),
                    weather = DailyWeather("2026-09-19", WeatherCondition.CLOUDY, 31, 23),
                    onDelete = {},
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// --- 预览用假数据 ---

private fun monthStartOf(year: Int, month: Int): Long = Calendar.getInstance().apply {
    set(year, month, 1, 0, 0, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis

/** 只在 19 号有打卡，用来验证「有打卡 / 无打卡」两种格子形态的对比。 */
private fun sampleLogsByDay(): Map<Int, OutfitLog> = mapOf(
    19 to sampleLog(day = 19, occasion = Occasion.COMMUTE)
)

/**
 * 给整月都造天气，但只有 19 号有打卡 —— 正好验证
 * 「没打卡的日子不显示天气」这条规则是否生效。
 */
private fun sampleWeatherByDay(): Map<Int, DailyWeather> =
    (1..30).associateWith { day ->
        DailyWeather(
            dateKey = "2026-09-${day.toString().padStart(2, '0')}",
            condition = when (day % 4) {
                0 -> WeatherCondition.SUNNY
                1 -> WeatherCondition.CLOUDY
                2 -> WeatherCondition.OVERCAST
                else -> WeatherCondition.RAIN
            },
            highC = 26 + day % 6,
            lowC = 18 + day % 5
        )
    }

private fun sampleLog(day: Int, occasion: Occasion): OutfitLog {
    val items = listOf(
        item(1, "条纹法式衬衫", ClothingCategory.TOP, 0xFFE8E3D9),
        item(2, "垂坠感阔腿西裤", ClothingCategory.BOTTOM, 0xFF2B2B2B),
        item(3, "德训小白鞋", ClothingCategory.SHOES, 0xFFFAFAFA),
        item(4, "尼龙双肩包", ClothingCategory.BAG, 0xFF2F3542)
    )
    return OutfitLog(
        id = day.toLong(),
        timestamp = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, day, 9, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis,
        outfit = Outfit(
            id = day.toLong(),
            items = items,
            occasion = occasion,
            score = 72,
            reason = "温度适中，适合通勤"
        ),
        mood = null,
        note = null
    )
}

private fun item(
    id: Long,
    name: String,
    category: ClothingCategory,
    color: Long
) = WardrobeItem(
    id = id,
    name = name,
    category = category,
    subCategory = "",
    imagePath = null,
    colorArgb = color,
    colorName = "",
    warmthLevel = 2,
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