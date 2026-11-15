package com.ootd.today.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 地名回退逻辑测试。
 *
 * 这条逻辑看着简单，但很容易写成「拿不到地名就退回默认城市」——
 * 那样会出现「杭州·多云 9°」这种自相矛盾的显示（坐标其实是北京的），
 * 实测中就真的踩过一次，所以固定下来。
 */
class CityLabelTest {

    @Test
    fun `no location at all falls back to default city`() {
        val noLocation: DeviceLocation? = null
        assertEquals(Coordinates.DEFAULT_CITY, noLocation.resolveCityLabel())
    }

    @Test
    fun `coordinates without city name shows current location`() {
        // 关键用例：有坐标说明定位成功，此时绝不能用默认城市名。
        val located = DeviceLocation(Coordinates(39.9042, 116.4074), city = null)
        assertEquals(CURRENT_LOCATION_LABEL, located.resolveCityLabel())
    }

    @Test
    fun `blank city name is treated as missing`() {
        val located = DeviceLocation(Coordinates(39.9042, 116.4074), city = "   ")
        assertEquals(CURRENT_LOCATION_LABEL, located.resolveCityLabel())
    }

    @Test
    fun `resolved city name is used as is`() {
        val located = DeviceLocation(Coordinates(39.9042, 116.4074), city = "北京")
        assertEquals("北京", located.resolveCityLabel())
    }

    @Test
    fun `default coordinates point at the fallback city`() {
        // 兜底坐标与兜底城市名必须对应同一个地方，
        // 否则又会出现「城市名与实际天气不符」。
        assertEquals(30.2741, Coordinates.DEFAULT.latitude, 0.001)
        assertEquals(120.1551, Coordinates.DEFAULT.longitude, 0.001)
        assertEquals("杭州", Coordinates.DEFAULT_CITY)
    }
}