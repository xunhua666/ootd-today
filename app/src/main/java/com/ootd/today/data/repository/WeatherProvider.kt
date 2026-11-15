package com.ootd.today.data.repository

import com.ootd.today.data.model.Coordinates
import com.ootd.today.data.model.Weather

/**
 * 天气数据源。
 *
 * 坐标由调用方传入而不是在实现里写死：定位是另一件事，
 * 数据源只负责「给我这个地方的天气」。这样换城市、换数据源互不影响。
 */
interface WeatherProvider {

    /**
     * 获取指定地点的当前天气。
     *
     * @param coordinates 目标坐标。
     * @param cityLabel 用于展示的地名（来自定位的反向地理编码，
     *   或兜底的默认城市）。数据源不自己编地名。
     * @return 天气快照；获取失败返回 null，由调用方决定降级展示。
     */
    suspend fun current(coordinates: Coordinates, cityLabel: String): Weather?
}