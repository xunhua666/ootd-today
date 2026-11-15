package com.ootd.today.data.repository

import com.ootd.today.data.model.Coordinates
import com.ootd.today.data.model.DailyWeather
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * 逐日天气数据源，同样是 Open-Meteo。
 *
 * 用 forecast 接口的 `start_date` / `end_date` 参数一次拿整段区间，
 * 而不是逐天请求 —— 日历翻一页只需一次网络往返。
 *
 * 可查范围是官方限制：过去约 3 个月到未来约 2 周。超出范围接口会报错，
 * 这里统一吞掉并返回空 Map，日历照常显示，只是没有天气而已。
 */
@Singleton
class OpenMeteoDailyWeatherProvider @Inject constructor() : DailyWeatherProvider {

    override suspend fun daily(
        coordinates: Coordinates,
        fromDate: String,
        toDate: String
    ): Map<String, DailyWeather> = withContext(Dispatchers.IO) {
        runCatching { fetch(coordinates, fromDate, toDate) }.getOrDefault(emptyMap())
    }

    private fun fetch(
        coordinates: Coordinates,
        fromDate: String,
        toDate: String
    ): Map<String, DailyWeather> {
        val url = BASE_URL +
            "?latitude=${coordinates.latitude}&longitude=${coordinates.longitude}" +
            "&daily=weather_code,temperature_2m_max,temperature_2m_min" +
            // timezone=auto 让 Open-Meteo 按坐标自己判断时区，
            // 比写死上海更经得住用户跑到别的时区。
            "&timezone=auto" +
            "&start_date=$fromDate&end_date=$toDate"

        val body = HttpJson.get(url) ?: return emptyMap()
        val daily = JSONObject(body).optJSONObject("daily") ?: return emptyMap()

        val dates = daily.optJSONArray("time") ?: return emptyMap()
        val codes = daily.optJSONArray("weather_code")
        val highs = daily.optJSONArray("temperature_2m_max")
        val lows = daily.optJSONArray("temperature_2m_min")

        val result = mutableMapOf<String, DailyWeather>()
        for (i in 0 until dates.length()) {
            val dateKey = dates.optString(i).takeIf { it.isNotBlank() } ?: continue
            result[dateKey] = DailyWeather(
                dateKey = dateKey,
                condition = OpenMeteoWeatherProvider.conditionFor(codes?.optInt(i) ?: 0),
                highC = highs?.optDouble(i, Double.NaN)?.takeIf { !it.isNaN() }?.toInt() ?: 0,
                lowC = lows?.optDouble(i, Double.NaN)?.takeIf { !it.isNaN() }?.toInt() ?: 0
            )
        }
        return result
    }

    private companion object {
        const val BASE_URL = "https://api.open-meteo.com/v1/forecast"
    }
}