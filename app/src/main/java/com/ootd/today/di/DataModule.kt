package com.ootd.today.di

import android.content.Context
import androidx.room.Room
import com.ootd.today.data.local.AppDatabase
import com.ootd.today.data.local.CommentDao
import com.ootd.today.data.local.CommunityUserDao
import com.ootd.today.data.local.OutfitLogDao
import com.ootd.today.data.local.PostDao
import com.ootd.today.data.local.WardrobeItemDao
import com.ootd.today.data.repository.AndroidLocationRepository
import com.ootd.today.data.repository.CommunityRepository
import com.ootd.today.data.repository.DailyWeatherProvider
import com.ootd.today.data.repository.LocationRepository
import com.ootd.today.data.repository.OpenMeteoDailyWeatherProvider
import com.ootd.today.data.repository.OpenMeteoWeatherProvider
import com.ootd.today.data.repository.OpenAiStylistProvider
import com.ootd.today.data.repository.ResilientWeatherProvider
import com.ootd.today.data.repository.RoomCommunityRepository
import com.ootd.today.data.repository.RoomWardrobeRepository
import com.ootd.today.data.repository.StylistProvider
import com.ootd.today.data.repository.WardrobeRepository
import com.ootd.today.data.repository.WeatherProvider
import com.ootd.today.domain.OutfitRecommendEngine
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * 数据层依赖绑定。
 *
 * 衣橱仓库已从内存假实现切换到 Room 实现：界面层只依赖 [WardrobeRepository]
 * 接口，因此这次替换没有改动任何 ViewModel 或 UI 代码。
 *
 * [WeatherProvider] 仍绑定假天气实现；接真实天气 API 时只改这一处绑定。
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindWardrobeRepository(
        impl: RoomWardrobeRepository
    ): WardrobeRepository

    /**
     * 天气绑定到「真实源 + 离线兜底」的组合实现。
     * 换成其它真实天气 API 时，只需替换 [ResilientWeatherProvider] 的 remote 参数。
     */
    @Binds
    @Singleton
    abstract fun bindWeatherProvider(
        impl: ResilientWeatherProvider
    ): WeatherProvider

    /**
     * 逐日天气（日历用）。
     *
     * 没有做「离线模拟」兜底：日历里缺天气只是少一行小字，
     * 不值得为它编造假数据 —— 那反而会让人误以为那天真是这么个天气。
     */
    @Binds
    @Singleton
    abstract fun bindDailyWeatherProvider(
        impl: OpenMeteoDailyWeatherProvider
    ): DailyWeatherProvider

    /**
     * 设备定位。用系统 LocationManager 而非 FusedLocationProvider ——
     * 后者依赖 Google Play 服务，国行小米等机型没有 GMS，会直接失败。
     */
    @Binds
    @Singleton
    abstract fun bindLocationRepository(
        impl: AndroidLocationRepository
    ): LocationRepository

    /**
     * 审美专家的大模型数据源。
     *
     * 绑定到 OpenAI 兼容实现；未配置 Key 时它自己的 isConfigured 会返回 false，
     * 调用方据此走本地算法。以后换服务商只需改设置里的地址与模型名，不必换实现。
     */
    @Binds
    @Singleton
    abstract fun bindStylistProvider(
        impl: OpenAiStylistProvider
    ): StylistProvider

    /**
     * 社区仓库。P0 绑定本地实现；接真后端时只改这一行。
     * 接口不含任何存储细节，因此 UI 与 ViewModel 不需要任何改动。
     */
    @Binds
    @Singleton
    abstract fun bindCommunityRepository(
        impl: RoomCommunityRepository
    ): CommunityRepository

    companion object {

        /**
         * 推荐引擎是无状态的纯计算对象，做成单例复用即可。
         * 「近期去重回溯 7 天」在这里集中配置。
         */
        @Provides
        @Singleton
        fun provideRecommendEngine(): OutfitRecommendEngine = OutfitRecommendEngine(recentDays = 7)

        @Provides
        @Singleton
        fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
            Room.databaseBuilder(
                context,
                AppDatabase::class.java,
                AppDatabase.DATABASE_NAME
            )
                // v1 -> v2 新增社区表；v2 -> v3 动态新增配图与话题。
                // 用 Migration 而非清库，以免用户在升级时丢掉已录入的数据。
                .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4, AppDatabase.MIGRATION_4_5)
                .build()

        @Provides
        fun provideWardrobeItemDao(db: AppDatabase): WardrobeItemDao = db.wardrobeItemDao()

        @Provides
        fun provideOutfitLogDao(db: AppDatabase): OutfitLogDao = db.outfitLogDao()

        @Provides
        fun provideCalendarPlanDao(db: AppDatabase): com.ootd.today.data.local.CalendarPlanDao = db.calendarPlanDao()

        @Provides
        fun provideCommunityUserDao(db: AppDatabase): CommunityUserDao = db.communityUserDao()

        @Provides
        fun providePostDao(db: AppDatabase): PostDao = db.postDao()

        @Provides
        fun provideCommentDao(db: AppDatabase): CommentDao = db.commentDao()

        /**
         * 应用级协程作用域，生命周期与进程一致。
         * 用于首次启动播种等「不属于任何页面」的后台任务。
         */
        @Provides
        @Singleton
        @ApplicationScope
        fun provideApplicationScope(): CoroutineScope =
            CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}

/** 标记应用级协程作用域，避免与其它 CoroutineScope 注入混淆。 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope
