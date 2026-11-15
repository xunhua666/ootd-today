package com.ootd.today.data.repository

import com.ootd.today.data.local.OutfitLogDao
import com.ootd.today.data.local.OutfitSnapshotCodec
import com.ootd.today.data.local.WardrobeItemDao
import com.ootd.today.data.local.toEntity
import com.ootd.today.data.local.toModel
import com.ootd.today.data.model.OutfitLog
import com.ootd.today.data.model.WardrobeItem
import com.ootd.today.data.local.demoWardrobeItems
import com.ootd.today.data.prefs.SettingsRepository
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Room 版衣橱仓库，替换 P0 阶段的内存假实现。
 *
 * 与 [FakeWardrobeRepository] 实现同一接口，因此 ViewModel 与 UI 层零改动。
 *
 * 打卡时会同步累加单品的穿着次数与最后穿着日期，
 * 这是「闲置唤醒」加权与 P1 统计报表的数据来源。
 */
@Singleton
class RoomWardrobeRepository @Inject constructor(
    private val itemDao: WardrobeItemDao,
    private val logDao: OutfitLogDao,
    private val settingsRepository: SettingsRepository
) : WardrobeRepository {

    private val experienceItems by lazy { demoWardrobeItems() }

    override fun observeItems(): Flow<List<WardrobeItem>> = combine(
        itemDao.observeAll(), settingsRepository.settings
    ) { stored, settings ->
        // Upgrades retain all existing records, and an emptied personal wardrobe stays empty.
        if (stored.isNotEmpty() && settings.demoWardrobeEnabled) settingsRepository.startPersonalWardrobe()
        if (stored.isEmpty() && settings.demoWardrobeEnabled) experienceItems
        else stored.map { it.toModel() }
    }

    override suspend fun startPersonalWardrobe() = settingsRepository.startPersonalWardrobe()

    override suspend fun getItem(id: Long): WardrobeItem? =
        if (id < 0) observeItems().first().firstOrNull { it.id == id } else itemDao.getById(id)?.toModel()

    override suspend fun upsertItem(item: WardrobeItem): Long {
        require(!item.isDemo) { "Experience items cannot be saved as personal items" }
        val entity = item.toEntity()
        val result = if (item.id == 0L) {
            itemDao.insert(entity.copy(id = 0))
        } else {
            itemDao.update(entity)
            item.id
        }
        settingsRepository.startPersonalWardrobe()
        return result
    }

    override suspend fun deleteItem(id: Long) {
        itemDao.deleteById(id)
    }

    override fun observeLogs(): Flow<List<OutfitLog>> =
        logDao.observeAll().map { list -> list.map { it.toModel() } }

    override fun observeLogsBetween(from: Long, to: Long): Flow<List<OutfitLog>> =
        logDao.observeBetween(from, to).map { list -> list.map { it.toModel() } }

    override suspend fun addLog(log: OutfitLog): Long {
        require(log.outfit.items.none { it.isDemo }) { "Experience outfits do not count as personal logs" }
        val id = logDao.insert(log.toEntity())

        // 累加穿着次数：打卡里的单品 id 集合从快照解出，
        // 避免为取 id 再查一次表。
        val wornIds = log.outfit.items.map { it.id }.toSet()
        wornIds.forEach { wornId ->
            val current = itemDao.getById(wornId) ?: return@forEach
            itemDao.update(
                current.copy(
                    wearCount = current.wearCount + 1,
                    lastWornDate = log.timestamp
                )
            )
        }
        return id
    }

    override suspend fun deleteLog(id: Long) {
        logDao.deleteById(id)
    }

    override suspend fun recentlyWornItemIds(days: Int): Set<Long> {
        val cutoff = System.currentTimeMillis() - days * MILLIS_PER_DAY
        // 区间上界取「现在」，因为打卡时间戳就是当天零点，必然早于现在。
        return logDao.observeBetween(cutoff, System.currentTimeMillis())
            .map { list ->
                list.flatMap { log -> OutfitSnapshotCodec.decode(log.itemsSnapshot).map { it.id } }
                    .toSet()
            }
            .firstOrNullSafe() ?: emptySet()
    }

    private companion object {
        const val MILLIS_PER_DAY = 24L * 60 * 60 * 1000
    }
}

/** 取 Flow 首个值；Room 的 Flow 总会发射，这里仅作防御。 */
private suspend fun <T> Flow<T>.firstOrNullSafe(): T? = try {
    first()
} catch (_: Exception) {
    null
}
