package com.ootd.today.data.repository

import com.ootd.today.data.model.Coordinates
import com.ootd.today.data.model.DailyWeather

/**
 * 逐日天气数据源。
 *
 * 与 [WeatherProvider]（提供「当前」天气）分开，因为两者的使用场景不同：
 * 首页要的是此刻的温湿度，日历要的是「某段日期区间内每天的高低温」。
 * 合成一个接口会让两边都拿到用不上的字段。
 */
interface DailyWeatherProvider {

    /**
     * 查询指定地点、指定日期区间的逐日天气。
     *
     * @param coordinates 目标坐标。
     * @param fromDate 起始日期，格式 `yyyy-MM-dd`。
     * @param toDate 结束日期，格式 `yyyy-MM-dd`。
     * @return 日期键 -> 天气摘要。失败或超出可查范围时返回空 Map，
     *   调用方应把它当作「没有天气信息」而非错误。
     */
    suspend fun daily(
        coordinates: Coordinates,
        fromDate: String,
        toDate: String
    ): Map<String, DailyWeather>
}