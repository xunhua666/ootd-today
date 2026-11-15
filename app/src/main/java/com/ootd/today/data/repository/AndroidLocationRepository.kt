package com.ootd.today.data.repository

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.core.content.ContextCompat
import com.ootd.today.data.model.Coordinates
import com.ootd.today.data.model.DeviceLocation
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import kotlin.coroutines.resume

/**
 * 基于系统 [LocationManager] 的定位实现。
 *
 * **为什么不用 FusedLocationProviderClient**：它依赖 Google Play 服务，
 * 而国行小米等 ROM 通常不带 GMS，一调用就失败。这个项目的目标机型
 * （红米）正是这种情况，所以必须用系统自带的 LocationManager。
 *
 * 取位置分两步，兼顾速度与成功率：
 * 1. 先看系统缓存的上次位置（`getLastKnownLocation`），通常瞬间返回；
 * 2. 缓存太旧或没有，才主动请求一次定位，最多等 [FIX_TIMEOUT_MS]。
 *
 * 反向地理编码走系统 [Geocoder]。它在部分国行 ROM 上不可用，
 * 失败就返回 null，界面回退成「当前位置」—— 坐标本身仍然是对的，
 * 不影响天气。
 */
@Singleton
class AndroidLocationRepository @Inject constructor(
    @ApplicationContext private val context: Context
) : LocationRepository {

    private val locationManager: LocationManager?
        get() = context.getSystemService(LocationManager::class.java)

    override fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    override suspend fun current(): DeviceLocation? {
        if (!hasPermission()) return null

        val coordinates = resolveCoordinates() ?: return null
        return DeviceLocation(
            coordinates = coordinates,
            city = reverseGeocode(coordinates)
        )
    }

    private suspend fun resolveCoordinates(): Coordinates? {
        val manager = locationManager ?: return null

        // 缓存太旧就不用，否则可能拿到几天前在外地时的位置。
        cachedLocation(manager)?.let { return it.toCoordinates() }

        val fresh = requestFreshLocation(manager) ?: return null
        return fresh.toCoordinates()
    }

    @SuppressLint("MissingPermission")
    private fun cachedLocation(manager: LocationManager): Location? {
        val cutoff = System.currentTimeMillis() - CACHE_MAX_AGE_MS
        return PROVIDERS
            .mapNotNull { provider ->
                // 定位服务可能被系统关闭，任一 provider 不可用都属正常。
                runCatching { manager.getLastKnownLocation(provider) }.getOrNull()
            }
            .filter { it.time >= cutoff }
            .maxByOrNull { it.time }
    }

    @SuppressLint("MissingPermission")
    private suspend fun requestFreshLocation(manager: LocationManager): Location? {
        val provider = PROVIDERS.firstOrNull {
            runCatching { manager.isProviderEnabled(it) }.getOrDefault(false)
        } ?: return null

        return withTimeoutOrNull(FIX_TIMEOUT_MS) {
            suspendCancellableCoroutine { continuation ->
                val listener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        runCatching { manager.removeUpdates(this) }
                        if (continuation.isActive) continuation.resume(location)
                    }

                    // 以下三个在 API 30+ 有默认实现，这里显式留空只为兼容旧版本的低层次行为。
                    override fun onProviderEnabled(provider: String) = Unit
                    override fun onProviderDisabled(provider: String) = Unit
                    @Deprecated("Deprecated in API 29")
                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
                }

                val registered = runCatching {
                    manager.requestLocationUpdates(provider, 0L, 0f, listener)
                }.isSuccess

                if (!registered && continuation.isActive) {
                    continuation.resume(null)
                }

                // 协程被取消（含超时）时解绑监听，避免泄漏。
                continuation.invokeOnCancellation {
                    runCatching { manager.removeUpdates(listener) }
                }
            }
        }
    }

    /**
     * 坐标 -> 城市名。
     *
     * 先试系统 [Geocoder]，失败再走一个免密钥的在线服务。
     *
     * 为什么要两层：系统 Geocoder 在国行 ROM（无 Google 服务）上经常
     * 返回空列表或直接不可用，而目标机型正是红米这类。只靠系统的话，
     * 用户会看到「当前位置」——坐标是对的但看着很含糊。
     *
     * 在线服务是最后手段，仍失败就返回 null，界面显示「当前位置」。
     */
    private suspend fun reverseGeocode(coordinates: Coordinates): String? {
        systemGeocode(coordinates)?.let { return normalizeCityName(it) }
        return httpGeocode(coordinates)?.let { normalizeCityName(it) }
    }

    private suspend fun systemGeocode(coordinates: Coordinates): String? =
        withContext(Dispatchers.IO) {
            runCatching {
                @Suppress("DEPRECATION")
                val addresses = Geocoder(context, Locale.getDefault())
                    .getFromLocation(coordinates.latitude, coordinates.longitude, 1)

                addresses?.firstOrNull()?.let { address ->
                    // 逐级放宽：城市 -> 区县 -> 省级。
                    address.locality ?: address.subAdminArea ?: address.adminArea
                }
            }.getOrNull()
        }

    private suspend fun httpGeocode(coordinates: Coordinates): String? =
        withContext(Dispatchers.IO) {
            runCatching {
                val url = "https://api.bigdatacloud.net/data/reverse-geocode-client" +
                    "?latitude=${coordinates.latitude}&longitude=${coordinates.longitude}" +
                    "&localityLanguage=zh"
                val json = HttpJson.get(url) ?: return@runCatching null
                val obj = JSONObject(json)
                // city 对直辖市是「北京市」，对普通城市是「杭州市」；
                // 部分区域为空，退到一级行政区。
                obj.optString("city").takeIf { it.isNotBlank() }
                    ?: obj.optString("principalSubdivision").takeIf { it.isNotBlank() }
            }.getOrNull()
        }

    /** 去掉尾部的「市」，跟默认城市名「杭州」保持同一种写法。 */
    private fun normalizeCityName(raw: String): String =
        raw.trim().removeSuffix("市").ifBlank { raw.trim() }

    private fun Location.toCoordinates() = Coordinates(latitude, longitude)

    private companion object {
        /** 优先级从高到低：GPS 更准，网络定位更快。 */
        val PROVIDERS = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER
        )

        /** 缓存位置超过这个时长就不再采用。 */
        const val CACHE_MAX_AGE_MS = 30 * 60 * 1000L

        /** 主动定位的最长等待。天气不值得让用户等太久。 */
        const val FIX_TIMEOUT_MS = 5_000L
    }
}