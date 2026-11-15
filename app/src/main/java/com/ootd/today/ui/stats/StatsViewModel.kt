package com.ootd.today.ui.stats

import androidx.lifecycle.ViewModel
import com.ootd.today.data.model.ClothingCategory
import com.ootd.today.data.model.ItemStatus
import com.ootd.today.data.model.OutfitLog
import com.ootd.today.data.model.WardrobeItem
import com.ootd.today.data.repository.WardrobeRepository
import com.ootd.today.domain.StreakCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import androidx.lifecycle.viewModelScope
import com.ootd.today.domain.isIdle
import com.ootd.today.domain.rewearOpportunity
import com.ootd.today.domain.RewearOpportunity
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 色彩偏好条目：颜色名 + 件数 + 代表色 ARGB。 */
data class ColorStat(val name: String, val count: Int, val argb: Long)

/** 性价比条目：单品 + 单次穿着成本。 */
data class ValuePick(val item: WardrobeItem, val costPerWear: Double)

/** 统计页 UI 状态。全部字段由衣橱与打卡流派生，无独立存储。 */
data class StatsUiState(
    val isDemo: Boolean = false,
    val rewear: RewearOpportunity? = null,
    val totalItems: Int = 0,
    val activeItems: Int = 0,
    val retiredItems: Int = 0,
    val totalLogs: Int = 0,
    val streakDays: Int = 0,
    val mostWorn: List<WardrobeItem> = emptyList(),
    val idleItems: List<WardrobeItem> = emptyList(),
    val topColors: List<ColorStat> = emptyList(),
    val valuePicks: List<ValuePick> = emptyList(),
    val categoryCounts: List<Pair<ClothingCategory, Int>> = emptyList()
)

/**
 * 穿搭统计 ViewModel。
 *
 * 所有指标都是纯派生计算，输入只有「单品流 + 打卡流」，
 * 因此数据永远与衣橱/日历保持一致，不需要额外的同步逻辑。
 */
@HiltViewModel
class StatsViewModel @Inject constructor(
    private val wardrobeRepository: WardrobeRepository
) : ViewModel() {

    val uiState: StateFlow<StatsUiState> = combine(
        wardrobeRepository.observeItems(),
        wardrobeRepository.observeLogs()
    ) { items, logs ->
        buildState(items.filterNot { it.isDemo }, logs).copy(isDemo = items.any { it.isDemo })
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = StatsUiState()
    )

    private fun buildState(items: List<WardrobeItem>, logs: List<OutfitLog>): StatsUiState {
        val active = items.filter { it.status == ItemStatus.ACTIVE }

        // 最常穿：按穿着次数降序，取前 5。穿着 0 次的不进榜。
        val mostWorn = items.filter { it.wearCount > 0 }
            .sortedByDescending { it.wearCount }
            .take(5)

        // 闲置唤醒：在用但一次没穿过，或至少 30 天没有穿着记录。
        val now = System.currentTimeMillis()
        val idleItems = active.filter { it.isIdle(now) }
            .sortedBy { it.lastWornDate ?: 0 }

        // 色彩偏好：按颜色名计数取前 4，并带上该组里第一件单品的实际色值作代表色。
        val topColors = active.groupingBy { it.colorName }
            .eachCount()
            .map { (name, count) ->
                ColorStat(
                    name = name,
                    count = count,
                    argb = active.first { it.colorName == name }.colorArgb
                )
            }
            .sortedByDescending { it.count }
            .take(4)

        // 性价比：有价格且穿过的单品，按「单次穿着成本」升序，越低越值。
        val valuePicks = items
            .filter { it.price != null && it.wearCount > 0 }
            .mapNotNull { item ->
                val price = item.price ?: return@mapNotNull null
                ValuePick(item, price / item.wearCount)
            }
            .sortedBy { it.costPerWear }
            .take(3)

        // 品类分布：仅统计在用单品。
        val categoryCounts = active.groupingBy { it.category }
            .eachCount()
            .toList()
            .sortedBy { it.first.ordinal }

        return StatsUiState(
            rewear = rewearOpportunity(active),
            totalItems = items.size,
            activeItems = active.size,
            retiredItems = items.size - active.size,
            totalLogs = logs.size,
            streakDays = StreakCalculator.streakDays(logs.map { it.timestamp }),
            mostWorn = mostWorn,
            idleItems = idleItems,
            topColors = topColors,
            valuePicks = valuePicks,
            categoryCounts = categoryCounts
        )
    }

    fun startPersonalWardrobe(onDone: () -> Unit) { viewModelScope.launch { wardrobeRepository.startPersonalWardrobe(); onDone() } }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
