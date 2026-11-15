package com.ootd.today.data.repository

import com.ootd.today.data.model.DeviceLocation

/**
 * 设备定位。
 *
 * 抽成接口的用意与天气一致：把「怎么拿到位置」和「拿位置干什么」分开，
 * 便于替换实现（例如将来改成高德/百度定位）以及单测时注入假数据。
 */
interface LocationRepository {

    /** 是否已获得定位权限。用于决定要不要弹权限申请。 */
    fun hasPermission(): Boolean

    /**
     * 取一次当前位置。
     *
     * @return 取不到（无权限、定位关闭、超时）时返回 null，调用方应回退到默认城市。
     *   刻意不抛异常：定位失败是常态，不该让首页崩掉。
     */
    suspend fun current(): DeviceLocation?
}