package com.ootd.today.notification

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ootd.today.data.model.Coordinates
import com.ootd.today.data.model.Occasion
import com.ootd.today.data.model.Season
import com.ootd.today.data.model.resolveCityLabel
import com.ootd.today.data.repository.LocationRepository
import com.ootd.today.data.repository.WardrobeRepository
import com.ootd.today.data.repository.WeatherProvider
import com.ootd.today.domain.OutfitRecommendEngine
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.Calendar
import kotlinx.coroutines.flow.first

/**
 * 每日穿搭推送 Worker。
 *
 * 在后台跑一遍推荐算法，把「今天穿什么 + 为什么」浓缩成一条通知。
 * 用 [HiltWorker] + 构造器注入，与页面 ViewModel 共用同一套仓库与引擎，
 * 保证推送内容和 App 内推荐完全一致。
 */
@HiltWorker
class DailyOutfitWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val wardrobeRepository: WardrobeRepository,
    private val weatherProvider: WeatherProvider,
    private val locationRepository: LocationRepository,
    private val engine: OutfitRecommendEngine,
    private val notificationHelper: NotificationHelper
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            // 后台任务的定位可能与前台行为不同（用户可能在另一个城市），
            // 因此这里也重新解析一次，而不是复用首页缓存。
            val deviceLocation = locationRepository.current()
            val coordinates = deviceLocation?.coordinates ?: Coordinates.DEFAULT
            val cityLabel = deviceLocation.resolveCityLabel()

            val weather = weatherProvider.current(coordinates, cityLabel) ?: return Result.retry()
            val items = wardrobeRepository.observeItems().first()
            val recentIds = wardrobeRepository.recentlyWornItemIds(RECENT_DEDUPE_DAYS)

            val candidate = engine.recommend(
                items = items,
                weather = weather,
                occasion = Occasion.COMMUTE,
                currentSeason = currentSeason(),
                recentlyWornIds = recentIds
            )

            if (candidate == null) {
                // 衣橱不足以成组：提醒用户去录入，而不是发一条空推荐。
                notificationHelper.showDailyOutfit(
                    title = "今日穿搭",
                    body = "衣橱里的单品还不够搭一套，点开录入几件吧 🧺"
                )
            } else {
                notificationHelper.showDailyOutfit(
                    title = "今日穿搭 · ${candidate.score} 分",
                    body = candidate.reason
                )
            }
            Result.success()
        } catch (e: Exception) {
            // 偶发失败（如数据库忙）交给 WorkManager 重试。
            Result.retry()
        }
    }

    private fun currentSeason(): Season = when (Calendar.getInstance().get(Calendar.MONTH)) {
        Calendar.MARCH, Calendar.APRIL, Calendar.MAY -> Season.SPRING
        Calendar.JUNE, Calendar.JULY, Calendar.AUGUST -> Season.SUMMER
        Calendar.SEPTEMBER, Calendar.OCTOBER, Calendar.NOVEMBER -> Season.AUTUMN
        else -> Season.WINTER
    }

    private companion object {
        const val RECENT_DEDUPE_DAYS = 7
    }
}
