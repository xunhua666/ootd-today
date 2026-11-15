package com.ootd.today.data.model

/**
 * 经纬度。
 *
 * 只保留坐标，不掺地名 —— 地名来自反向地理编码，属于另一件事，
 * 混在一起会让「只想要坐标」的调用方被迫依赖一个用不上的字段。
 */
data class Coordinates(
    val latitude: Double,
    val longitude: Double
) {
    /** 保留 4 位小数（约 10 米精度），足够天气使用，也让日志可读。 */
    fun shortLabel(): String =
        "%.4f,%.4f".format(latitude, longitude)

    companion object {
        /**
         * 兜底坐标：杭州。
         *
         * 用户拒绝定位权限、或设备暂时取不到位置时使用，
         * 保证首页永远能显示天气而不是一片空白。
         */
        val DEFAULT = Coordinates(latitude = 30.2741, longitude = 120.1551)

        /** 与 [DEFAULT] 对应的城市名。 */
        const val DEFAULT_CITY = "杭州"
    }
}

/**
 * 一次定位结果。
 *
 * @property coordinates 坐标。
 * @property city 反向地理编码得到的城市名。可能为 null ——
 *   部分机型的地理编码服务不可用（尤其没有 Google 服务的国行 ROM），
 *   这时上游应回退成「当前位置」之类的说法，而不是显示空白。
 */
data class DeviceLocation(
    val coordinates: Coordinates,
    val city: String?
)

/**
 * 解析「显示什么地名」。
 *
 * 三种情形分开处理，关键是后两种不能混：
 * - 完全没有定位 → 用默认城市名（天气本身也是默认城市的）；
 * - 有定位但拿不到地名 → 「当前位置」。此时坐标是用户真实位置的，
 *   若沿用默认城市名就会显示「杭州·多云 9°」这种自相矛盾的结果；
 * - 有地名 → 用它。
 */
fun DeviceLocation?.resolveCityLabel(): String = when {
    this == null -> Coordinates.DEFAULT_CITY
    city.isNullOrBlank() -> CURRENT_LOCATION_LABEL
    else -> city
}

/** 拿不到地名时对用户的说法。 */
const val CURRENT_LOCATION_LABEL = "当前位置"