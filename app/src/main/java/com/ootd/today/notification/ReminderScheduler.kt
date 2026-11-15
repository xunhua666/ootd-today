package com.ootd.today.notification

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.ootd.today.data.prefs.UserSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 每日推送调度器。
 *
 * WorkManager 的周期任务最小间隔是 15 分钟且不支持「每天定点」，
 * 因此采用业界通用做法：**24 小时周期 + 首次执行时间对齐到用户设定的时刻**。
 * 首次延迟 = 从现在到下一个设定时刻的差值，之后每 24 小时重复一次。
 *
 * 设置变更时调用 [reschedule] 用 REPLACE 策略重建任务，旧任务自动作废。
 */
@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /** 按当前设置重新调度；开关关闭时取消任务。 */
    fun reschedule(settings: UserSettings) {
        val workManager = WorkManager.getInstance(context)
        if (!settings.dailyReminderEnabled) {
            workManager.cancelUniqueWork(UNIQUE_NAME)
            return
        }

        val initialDelay = millisUntilNext(settings.reminderHour, settings.reminderMinute)
        val request = PeriodicWorkRequestBuilder<DailyOutfitWorker>(
            REPEAT_INTERVAL_HOURS,
            TimeUnit.HOURS
        )
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .build()

        workManager.enqueueUniquePeriodicWork(
            UNIQUE_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    /** 计算从现在到下一个「hour:minute」的毫秒差。 */
    private fun millisUntilNext(hour: Int, minute: Int): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        // 今天的时刻已过则推到明天。
        if (!target.after(now)) {
            target.add(Calendar.DAY_OF_YEAR, 1)
        }
        return target.timeInMillis - now.timeInMillis
    }

    companion object {
        const val UNIQUE_NAME = "daily_outfit_reminder"
        private const val REPEAT_INTERVAL_HOURS = 24L
    }
}
