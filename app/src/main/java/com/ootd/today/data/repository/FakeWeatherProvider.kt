package com.ootd.today.data.repository

import com.ootd.today.data.model.Coordinates
import com.ootd.today.data.model.Weather
import com.ootd.today.data.model.WeatherCondition
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/**
 * 离线兜底的天气数据。
 *
 * 只在真实数据源拿不到数据时才用（无网、接口挂掉）。
 *
 * 温度与天气按当天日期做确定性推导，所以同一天多次打开看到的一致，
 * 不同日期则自动变化 —— 总比每天都是一模一样的假数据好一点。
 *
 * 地名用调用方给的 [cityLabel]，不自己编：用户在北京时
 * 显示「北京·晴（模拟）」比显示「杭州」诚实。
 */
@Singleton
class FakeWeatherProvider @Inject constructor() : WeatherProvider {

    override suspend fun current(coordinates: Coordinates, cityLabel: String): Weather {
        val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        // 在 8~28℃ 之间做正弦式季节波动，叠加按天的确定性抖动。
        val base = 18 + (10 * kotlin.math.sin(dayOfYear / 58.0)).toInt()
        val jitter = abs(dayOfYear * 7 % 5) - 2
        val temp = (base + jitter).coerceIn(-5, 38)

        val condition = when (dayOfYear % 5) {
            0 -> WeatherCondition.RAIN
            1 -> WeatherCondition.CLOUDY
            2 -> WeatherCondition.SUNNY
            3 -> WeatherCondition.OVERCAST
            else -> WeatherCondition.SUNNY
        }

        return Weather(
            city = cityLabel,
            temperatureC = temp,
            condition = condition,
            highC = temp + 3,
            lowC = temp - 4,
            humidity = 45 + (dayOfYear % 40),
            aqi = 30 + (dayOfYear % 90)
        )
    }
}
