package com.ootd.today

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.ootd.today.data.local.DatabaseSeeder
import com.ootd.today.data.prefs.SettingsRepository
import com.ootd.today.di.ApplicationScope
import com.ootd.today.notification.ReminderScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * 应用入口，Hilt 的根容器。
 *
 * 已在 AndroidManifest 中通过 `android:name` 注册。
 *
 * 实现 [Configuration.Provider] 是为了让 WorkManager 使用 Hilt 的
 * WorkerFactory，这样 [com.ootd.today.notification.DailyOutfitWorker]
 * 才能通过构造器注入拿到仓库与引擎。
 */
@HiltAndroidApp
class OotdApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var seeder: DatabaseSeeder

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var reminderScheduler: ReminderScheduler

    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        // 准备社区体验内容；衣橱示例由仓库以临时数据提供。
        // 放在应用级作用域里异步执行，不阻塞首帧渲染。
        applicationScope.launch { seeder.seedIfEmpty() }

        // 按用户设置恢复每日推送调度。
        // WorkManager 的任务在卸载重装后会丢失，因此每次启动都对齐一次。
        applicationScope.launch {
            reminderScheduler.reschedule(settingsRepository.settings.first())
        }
    }
}
