package com.ootd.today.ui.today

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ootd.today.R
import com.ootd.today.data.model.Coordinates
import com.ootd.today.data.model.Occasion
import com.ootd.today.data.model.Outfit
import com.ootd.today.data.model.OutfitLog
import com.ootd.today.data.model.Season
import com.ootd.today.data.model.WardrobeItem
import com.ootd.today.data.model.Weather
import com.ootd.today.data.model.resolveCityLabel
import com.ootd.today.data.repository.LocationRepository
import com.ootd.today.data.repository.WardrobeRepository
import com.ootd.today.data.repository.WeatherProvider
import com.ootd.today.domain.OutfitCandidate
import com.ootd.today.domain.OutfitRecommendEngine
import com.ootd.today.domain.stylist.StylistAdvice
import com.ootd.today.domain.stylist.StylistService

import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import java.util.Calendar
import javax.inject.Inject

/** 「今日穿搭」页的 UI 状态。 */
data class TodayUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isDemo: Boolean = false,
    val requiredItem: WardrobeItem? = null,
    val weather: Weather? = null,
    val occasion: Occasion = Occasion.COMMUTE,
    val recommendation: OutfitCandidate? = null,
    /** 衣橱不足以生成合法搭配时置为 true，UI 据此引导用户去录入单品。 */
    val wardrobeTooSmall: Boolean = false,
    /** 用户点👍/👎 累积的反馈权重，影响本次会话内的后续推荐。 */
    val feedback: Map<Long, Double> = emptyMap(),
    /**
     * 天气是否来自设备定位。
     *
     * false 表示没拿到定位（未授权 / 定位关闭 / 取不到），
     * 此时用的是默认城市，UI 会给一个「用我的位置」的入口。
     */
    val usingDeviceLocation: Boolean = false,
    /** 当前推荐是否由「沈砚」给出（false 表示走的是本地算法）。 */
    val fromStylist: Boolean = false,
    /** 沈砚的一句话定调，本地推荐时为 null。 */
    val stylistHeadline: String? = null,
    /** 是否已配置好专家（配了 API Key），决定追问入口是否可用。 */
    val stylistReady: Boolean = false,
    /** 正在等专家回应，UI 显示「沈砚正在看你的衣橱…」。 */
    val isConsulting: Boolean = false,
    /** 追问失败时的提示，展示后自动清空。 */
    val stylistError: String? = null
)

/**
 * 「今日穿搭」页 ViewModel。
 *
 * 依赖全部通过构造器注入，不直接依赖具体实现类，
 * 因此后续把 [WardrobeRepository] 换成 Room 实现、[WeatherProvider] 换成真实
 * 天气 API 时，这个类无需任何改动。
 */
@HiltViewModel
class TodayViewModel @Inject constructor(
    private val wardrobeRepository: WardrobeRepository,
    private val weatherProvider: WeatherProvider,
    private val locationRepository: LocationRepository,
    private val engine: OutfitRecommendEngine,
    private val stylistService: StylistService,
    private val settingsRepository: com.ootd.today.data.prefs.SettingsRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val expertProfile = settingsRepository.settings.stateIn(
        viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), com.ootd.today.data.prefs.UserSettings()
    )

    private val _uiState = MutableStateFlow(TodayUiState())
    val uiState: StateFlow<TodayUiState> = _uiState.asStateFlow()

    /** 缓存最近一次的天气与单品，供场合切换/摇一摇复用，避免重复加载。 */
    private var cachedWeather: Weather? = null
    private var cachedItems: List<WardrobeItem> = emptyList()
    private var cachedRecentIds: Set<Long> = emptySet()

    init {
        refresh()
        // Keep the home recommendation in sync after adding or editing clothes.
        viewModelScope.launch {
            wardrobeRepository.observeItems().collect { items ->
                if (items != cachedItems) {
                    cachedItems = items
                    _uiState.update { it.copy(isDemo = items.any { item -> item.isDemo }) }
                    cachedWeather?.let { selectOccasion(_uiState.value.occasion) }
                }
            }
        }
    }

    /** 重新加载天气与衣橱，并生成今日推荐。 */
    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // 先定位，再拿天气。定位失败不是错误，会退回默认城市。
            val deviceLocation = locationRepository.current()
            val coordinates = deviceLocation?.coordinates ?: Coordinates.DEFAULT
            val cityLabel = deviceLocation.resolveCityLabel()

            val weather = weatherProvider.current(coordinates, cityLabel)
            val items = wardrobeRepository.observeItems().first()
            val recentIds = wardrobeRepository.recentlyWornItemIds(RECENT_DEDUPE_DAYS)

            cachedWeather = weather
            cachedItems = items
            cachedRecentIds = recentIds

            if (weather == null) {
                _uiState.update { it.copy(isLoading = false, weather = null) }
                return@launch
            }

            val candidate = engine.recommend(
                items = items,
                weather = weather,
                occasion = _uiState.value.occasion,
                currentSeason = currentSeason(),
                recentlyWornIds = recentIds,
                feedbackBoost = _uiState.value.feedback,
                requiredItemId = _uiState.value.requiredItem?.id
            )

            // 在 update 外先取好：update 内部是 CAS 重试循环，
            // 把挂起调用放进去会被重复执行。
            val stylistReady = stylistService.isReady()

            _uiState.update {
                it.copy(
                    isLoading = false,
                    weather = weather,
                    isDemo = items.any { item -> item.isDemo },
                    recommendation = candidate,
                    wardrobeTooSmall = candidate == null,
                    usingDeviceLocation = deviceLocation != null,
                    // 首页默认只走本地算法，专家由用户主动点「请沈砚看看」触发。
                    fromStylist = false,
                    stylistHeadline = null,
                    stylistReady = stylistReady
                )
            }
        }
    }

    /**
     * 主动请「沈砚」给一套搭配。
     *
     * 只在用户点击时调用 —— 每次调用都产生费用，不做自动触发。
     * 失败时静默保留当前推荐，只给一行轻提示。
     */
    fun askStylist() {
        val weather = cachedWeather ?: return
        if (_uiState.value.isConsulting || _uiState.value.requiredItem != null) return

        viewModelScope.launch {
            _uiState.update { it.copy(isConsulting = true, stylistError = null) }

            val advice = stylistService.advise(
                items = cachedItems,
                weather = weather,
                occasion = _uiState.value.occasion
            )

            if (advice == null) {
                _uiState.update {
                    it.copy(
                        isConsulting = false,
                        stylistError = context.getString(R.string.vm_stylist_unavailable)
                    )
                }
                return@launch
            }

            val candidate = advice.toCandidate()
            _uiState.update {
                it.copy(
                    isConsulting = false,
                    recommendation = candidate,
                    wardrobeTooSmall = false,
                    fromStylist = true,
                    stylistHeadline = advice.headline
                )
            }
        }
    }

    /**
     * 向沈砚追问。
     *
     * 例如「换条裤子」「太素了」「正式点」。意图在本地先整理一遍，
     * 再带着明确指令重新请求。
     */
    fun askStylistFollowUp(text: String) {
        val weather = cachedWeather ?: return
        if (text.isBlank() || _uiState.value.isConsulting || _uiState.value.requiredItem != null) return

        viewModelScope.launch {
            _uiState.update { it.copy(isConsulting = true, stylistError = null) }

            val advice = stylistService.followUp(
                items = cachedItems,
                weather = weather,
                occasion = _uiState.value.occasion,
                userText = text
            )

            if (advice == null) {
                _uiState.update {
                    it.copy(
                        isConsulting = false,
                        stylistError = context.getString(R.string.vm_stylist_not_understood)
                    )
                }
                return@launch
            }

            _uiState.update {
                it.copy(
                    isConsulting = false,
                    recommendation = advice.toCandidate(),
                    wardrobeTooSmall = false,
                    fromStylist = true,
                    stylistHeadline = advice.headline
                )
            }
        }
    }

    /** 提示已展示，清空避免重复弹出。 */
    fun clearStylistError() {
        _uiState.update { it.copy(stylistError = null) }
    }

    /** 切换场合后立刻用缓存数据重算推荐，无需重新加载。 */
    fun selectOccasion(occasion: Occasion) {
        val weather = cachedWeather ?: return
        val candidate = engine.recommend(
            items = cachedItems,
            weather = weather,
            occasion = occasion,
            currentSeason = currentSeason(),
            recentlyWornIds = cachedRecentIds,
            feedbackBoost = _uiState.value.feedback,
            requiredItemId = _uiState.value.requiredItem?.id
        )
        _uiState.update {
            it.copy(occasion = occasion, recommendation = candidate, wardrobeTooSmall = candidate == null, fromStylist = false, stylistHeadline = null)
        }
    }

    /** 换一套：随机生成，但仍受温度与场合约束。 */
    fun shuffle() {
        val weather = cachedWeather ?: return
        if (_uiState.value.requiredItem != null) {
            selectOccasion(_uiState.value.occasion)
            return
        }
        val candidate = engine.random(
            items = cachedItems,
            weather = weather,
            occasion = _uiState.value.occasion,
            currentSeason = currentSeason(),
            recentlyWornIds = cachedRecentIds
        )
        // 摇不出合法组合时保留上一套，避免界面突然空掉。
        if (candidate != null) {
            _uiState.update { it.copy(recommendation = candidate, wardrobeTooSmall = false) }
        }
    }

    /**
     * 记录用户对当前推荐的反馈并重算。
     *
     * 👍 给组合内每件单品加权，👎 降权，让后续推荐逐渐贴近用户口味。
     * P0 只保存在内存（会话级），持久化到 DataStore 属于 P1。
     */
    fun rateCurrent(liked: Boolean) {
        val rated = _uiState.value.recommendation?.items ?: return
        val delta = if (liked) FEEDBACK_STEP else -FEEDBACK_STEP

        val nextFeedback = _uiState.value.feedback.toMutableMap()
        rated.forEach { item ->
            nextFeedback[item.id] = ((nextFeedback[item.id] ?: 0.0) + delta)
                .coerceIn(-FEEDBACK_CLAMP, FEEDBACK_CLAMP)
        }

        val weather = cachedWeather ?: return
        val candidate = engine.recommend(
            items = cachedItems,
            weather = weather,
            occasion = _uiState.value.occasion,
            currentSeason = currentSeason(),
            recentlyWornIds = cachedRecentIds,
            feedbackBoost = nextFeedback,
            requiredItemId = _uiState.value.requiredItem?.id
        )
        _uiState.update {
            it.copy(feedback = nextFeedback, recommendation = candidate, wardrobeTooSmall = candidate == null)
        }
    }

    /** 采纳当前推荐并写入今日打卡记录。 */
    fun adoptCurrent(onDone: (Boolean) -> Unit = {}) {
        if (_uiState.value.isSaving) return
        val candidate = _uiState.value.recommendation
        if (candidate == null || candidate.items.any { it.isDemo }) {
            onDone(false)
            return
        }
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            try {
            val outfit = Outfit(
                id = 0L,
                items = candidate.items,
                occasion = _uiState.value.occasion,
                score = candidate.score,
                reason = candidate.reason
            )
            wardrobeRepository.addLog(
                OutfitLog(
                    id = 0L,
                    timestamp = startOfToday(),
                    outfit = outfit,
                    mood = null,
                    note = null
                )
            )
            // 打卡后近期去重集合已变化，刷新缓存。
            cachedRecentIds = wardrobeRepository.recentlyWornItemIds(RECENT_DEDUPE_DAYS)
            onDone(true)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                onDone(false)
            } finally {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    fun startPersonalWardrobe(onDone: () -> Unit) {
        viewModelScope.launch { wardrobeRepository.startPersonalWardrobe(); onDone() }
    }

    fun focusOnItem(id: Long) {
        viewModelScope.launch {
            val item = wardrobeRepository.getItem(id) ?: return@launch
            val occasion = if (_uiState.value.occasion in item.occasions) _uiState.value.occasion
                else item.occasions.firstOrNull() ?: _uiState.value.occasion
            _uiState.update { it.copy(requiredItem = item, occasion = occasion) }
            if (cachedWeather != null) selectOccasion(occasion)
        }
    }

    fun clearFocusItem() {
        _uiState.update { it.copy(requiredItem = null) }
        if (cachedWeather != null) selectOccasion(_uiState.value.occasion)
    }

    /** 根据当前月份推断季节。 */
    private fun currentSeason(): Season = when (Calendar.getInstance().get(Calendar.MONTH)) {
        Calendar.MARCH, Calendar.APRIL, Calendar.MAY -> Season.SPRING
        Calendar.JUNE, Calendar.JULY, Calendar.AUGUST -> Season.SUMMER
        Calendar.SEPTEMBER, Calendar.OCTOBER, Calendar.NOVEMBER -> Season.AUTUMN
        else -> Season.WINTER
    }

    /** 当天零点的 epoch millis，用于按天归组打卡记录。 */
    private fun startOfToday(): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private companion object {
        /** 「近期去重」回溯天数。 */
        const val RECENT_DEDUPE_DAYS = 7
        /** 单次👍/👎 对单品得分的影响量。 */
        const val FEEDBACK_STEP = 0.06
        /** 反馈权重上下限，防止用户狂点导致某件单品被永久封杀或永远置顶。 */
        const val FEEDBACK_CLAMP = 0.30
    }
}

/**
 * 把「沈砚」的建议转成与本地算法一致的推荐结果。
 *
 * 两者共用同一个展示模型，UI 层不必区分来源 —— 只需看
 * `fromStylist` 决定要不要显示专家的标识。
 *
 * @return 得分统一给 90：大模型不返回可信的数值评分，
 *   与其编一个不如不显示分数，但卡片需要一个值，这里取高分表示
 *   「这是经过专门挑选的」。
 */
private fun StylistAdvice.toCandidate(): OutfitCandidate = OutfitCandidate(
    items = items,
    score = STYLIST_SCORE,
    reason = toDisplayText(),
    headline = headline
)

/** 专家推荐的展示分数。 */
private const val STYLIST_SCORE = 90
