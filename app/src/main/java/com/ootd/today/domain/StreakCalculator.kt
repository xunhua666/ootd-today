package com.ootd.today.domain

import java.util.Calendar

/**
 * 连续打卡天数计算。
 *
 * 从 [OutfitRecommendEngine] 同样的设计哲学出发：纯函数、无 Android 依赖、
 * 可 JVM 单测。时间戳入参 + `now` 可注入，测试里能精确构造「跨天/断签」场景。
 */
object StreakCalculator {

    /**
     * 计算连续打卡天数。
     *
     * 规则：从「今天」往前数连续有打卡的天数；若今天还没打卡，
     * 允许从昨天起算——否则用户早上打开统计永远看到 0 天，体验很差。
     *
     * @param timestamps 打卡时间戳列表，无需排序、允许同天多条。
     * @param now 当前时间，可注入以便测试。
     */
    fun streakDays(timestamps: List<Long>, now: Long = System.currentTimeMillis()): Int {
        if (timestamps.isEmpty()) return 0
        val days = timestamps.map { dayKey(it) }.toSet()

        val calendar = Calendar.getInstance().apply { timeInMillis = now }
        if (dayKey(calendar.timeInMillis) !in days) {
            calendar.add(Calendar.DAY_OF_YEAR, -1)
            if (dayKey(calendar.timeInMillis) !in days) return 0
        }

        var streak = 0
        while (dayKey(calendar.timeInMillis) in days) {
            streak++
            calendar.add(Calendar.DAY_OF_YEAR, -1)
        }
        return streak
    }

    /** 把时间戳归一成「年月日」整数，用于按天去重与比较。 */
    fun dayKey(millis: Long): Int {
        val c = Calendar.getInstance().apply { timeInMillis = millis }
        return c.get(Calendar.YEAR) * 10000 +
            (c.get(Calendar.MONTH) + 1) * 100 +
            c.get(Calendar.DAY_OF_MONTH)
    }
}
