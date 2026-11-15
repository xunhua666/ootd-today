package com.ootd.today.data.repository

import com.ootd.today.data.model.Coordinates
import com.ootd.today.data.model.Weather
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 带兜底的天气源：优先真实数据，失败回退到离线模拟。
 *
 * DI 里把 [WeatherProvider] 绑定到本类，因此 ViewModel 完全无感；
 * 以后要换成和风天气等其它真实源，只需替换 [remote] 的实现。
 *
 * 回退刻意「静默」：不抛异常、不弹提示，由 [Weather.isLive] 把数据来源
 * 透明地交给 UI 标注，避免弱网时打扰用户。
 */
@Singleton
class ResilientWeatherProvider @Inject constructor(
    private val remote: OpenMeteoWeatherProvider,
    private val fallback: FakeWeatherProvider
) : WeatherProvider {

    override suspend fun current(coordinates: Coordinates, cityLabel: String): Weather? =
        remote.current(coordinates, cityLabel)
            ?: fallback.current(coordinates, cityLabel)
}