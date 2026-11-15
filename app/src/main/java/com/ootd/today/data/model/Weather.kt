package com.ootd.today.data.model

/**
 * 天气状况。
 *
 * @property isRainy 是否有降水，用于推荐时提示带伞、避免浅色鞋。
 */
enum class WeatherCondition(
    val label: String,
    val emoji: String,
    val isRainy: Boolean
) {
    SUNNY("晴", "☀️", false),
    CLOUDY("多云", "⛅", false),
    OVERCAST("阴", "☁️", false),
    RAIN("雨", "🌧", true),
    SNOW("雪", "🌨", true)
}

/**
 * 当前天气快照。
 *
 * @property city 城市名。P0 阶段写死为「杭州」，不做定位。
 * @property temperatureC 当前气温（摄氏度）。
 * @property condition 天气状况。
 * @property highC 当日最高温。
 * @property lowC 当日最低温。
 * @property humidity 相对湿度百分比 0..100。
 * @property aqi 空气质量指数。
 * @property isLive 是否来自真实天气源。false 表示离线回退的模拟数据，
 *   UI 会据此标注「实时 / 模拟」，避免用户把假数据当真。
 */
data class Weather(
    val city: String,
    val temperatureC: Int,
    val condition: WeatherCondition,
    val highC: Int,
    val lowC: Int,
    val humidity: Int,
    val aqi: Int,
    val isLive: Boolean = false
)

/**
 * 某一天的天气摘要，用于日历页回看「那天什么天穿的那套」。
 *
 * @property dateKey 日期键，格式 `yyyy-MM-dd`，与 Open-Meteo 返回的格式一致。
 */
data class DailyWeather(
    val dateKey: String,
    val condition: WeatherCondition,
    val highC: Int,
    val lowC: Int
) {
    /** 用于日历格子展示的简写，例如 `26°`。 */
    val temperatureLabel: String get() = "${highC}°"
}
