package com.ootd.today.data.repository

import com.ootd.today.data.model.OutfitLog
import com.ootd.today.data.model.WardrobeItem
import kotlinx.coroutines.flow.Flow

/**
 * 衣橱数据仓库。
 *
 * 所有界面只依赖这个接口，不感知底层是 Room 还是内存假数据。
 * P0 阶段由 `FakeWardrobeRepository` 提供内存假数据；
 * 后续换成 Room 实现时，只需新增实现类并改 DI 绑定，业务代码零改动。
 * 同理，未来接入云端同步也在这里扩展，UI 层无感。
 */
interface WardrobeRepository {

    /** End the experience without deleting any personal records. */
    suspend fun startPersonalWardrobe() {}

    /** 全部单品（含已处理），以 Flow 暴露以便自动刷新。 */
    fun observeItems(): Flow<List<WardrobeItem>>

    /** 按 id 查询单品，不存在返回 null。 */
    suspend fun getItem(id: Long): WardrobeItem?

    /** 新增或更新单品，返回其 id。 */
    suspend fun upsertItem(item: WardrobeItem): Long

    /** 删除单品。 */
    suspend fun deleteItem(id: Long)

    // --- 打卡记录 ---

    /** 全部打卡记录，按时间倒序。 */
    fun observeLogs(): Flow<List<OutfitLog>>

    /** 指定时间区间内的打卡记录，[from]、[to] 均为 epoch millis。 */
    fun observeLogsBetween(from: Long, to: Long): Flow<List<OutfitLog>>

    /** 新增一条打卡记录，返回其 id。 */
    suspend fun addLog(log: OutfitLog): Long

    /** 删除打卡记录。 */
    suspend fun deleteLog(id: Long)

    /**
     * 查询最近 [days] 天内出现过的单品 id 集合。
     * 推荐算法用它做「近期穿过的降权」。
     */
    suspend fun recentlyWornItemIds(days: Int): Set<Long>
}
