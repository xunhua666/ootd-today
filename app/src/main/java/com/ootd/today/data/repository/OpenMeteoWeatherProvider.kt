package com.ootd.today.data.repository

import com.ootd.today.data.model.Coordinates
import com.ootd.today.data.model.Weather
import com.ootd.today.data.model.WeatherCondition
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * 真实天气数据源：Open-Meteo。
 *
 * 选它的理由：**免费且不需要申请 API key**，无需用户做任何配置就能用上真实数据。
 *
 * 坐标由调用方传入（来自设备定位），这里不再写死城市。
 * 空气质量走 Open-Meteo 的独立 air-quality 接口，取美标 AQI。
 *
 * 任何一步失败都返回 null，由 [ResilientWeatherProvider] 回退到模拟数据，
 * 保证弱网/无网环境下首页永远有内容。
 */
@Singleton
class OpenMeteoWeatherProvider @Inject constructor() : WeatherProvider {

    override suspend fun current(
        coordinates: Coordinates,
        cityLabel: String
    ): Weather? = withContext(Dispatchers.IO) {
        runCatching { fetch(coordinates, cityLabel) }.getOrNull()
    }

    private fun fetch(coordinates: Coordinates, cityLabel: String): Weather? {
        val forecastJson = httpGet(forecastUrl(coordinates)) ?: return null
        val root = JSONObject(forecastJson)
        val current = root.optJSONObject("current") ?: return null
        val daily = root.optJSONObject("daily") ?: return null

        val temp = current.optDouble("temperature_2m", Double.NaN)
        if (temp.isNaN()) return null
        val humidity = current.optInt("relative_humidity_2m", 50)
        val code = current.optInt("weather_code", 0)

        val highs = daily.optJSONArray("temperature_2m_max")
        val lows = daily.optJSONArray("temperature_2m_min")
        val high = highs?.optDouble(0, temp + 3)?.toInt() ?: (temp.toInt() + 3)
        val low = lows?.optDouble(0, temp - 4)?.toInt() ?: (temp.toInt() - 4)

        // 空气质量是「锦上添花」的数据：单独失败不应拖垮整次请求。
        val aqi = fetchAqi(coordinates) ?: DEFAULT_AQI

        return Weather(
            // 地名由调用方给（来自定位），这里不编。
            city = cityLabel,
            temperatureC = temp.toInt(),
            condition = conditionFor(code),
            highC = high,
            lowC = low,
            humidity = humidity,
            aqi = aqi,
            isLive = true
        )
    }

    private fun fetchAqi(coordinates: Coordinates): Int? = runCatching {
        val json = httpGet(airQualityUrl(coordinates)) ?: return@runCatching null
        JSONObject(json).optJSONObject("current")?.optInt("us_aqi")?.takeIf { it > 0 }
    }.getOrNull()

    private fun httpGet(url: String): String? = HttpJson.get(url)

    companion object {
        private const val DEFAULT_AQI = 50

        /**
         * 拼当前天气请求。坐标由调用方传入，因此这里只做字符串拼装，
         * 不再持有任何写死的城市坐标。
         */
        private fun forecastUrl(coordinates: Coordinates): String =
            "https://api.open-meteo.com/v1/forecast" +
                "?latitude=${coordinates.latitude}&longitude=${coordinates.longitude}" +
                "&current=temperature_2m,relative_humidity_2m,weather_code" +
                "&daily=temperature_2m_max,temperature_2m_min" +
                "&forecast_days=1&timezone=auto"

        private fun airQualityUrl(coordinates: Coordinates): String =
            "https://air-quality-api.open-meteo.com/v1/air-quality" +
                "?latitude=${coordinates.latitude}&longitude=${coordinates.longitude}" +
                "&current=us_aqi&timezone=auto"

        /**
         * WMO 天气代码 → 应用内天气状况。
         *
         * 只区分展示需要的五档，不做过细映射：UI 上只是一个 emoji + 一个词。
         */
        fun conditionFor(code: Int): WeatherCondition = when (code) {
            0 -> WeatherCondition.SUNNY
            1, 2 -> WeatherCondition.CLOUDY
            3, 45, 48 -> WeatherCondition.OVERCAST
            71, 73, 75, 77, 85, 86 -> WeatherCondition.SNOW
            else -> if (code >= 51) WeatherCondition.RAIN else WeatherCondition.CLOUDY
        }
    }
}
