package com.ootd.today.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.ootd.today.data.model.Coordinates
import com.ootd.today.data.model.DailyWeather
import com.ootd.today.data.model.OutfitLog
import com.ootd.today.data.model.Weather
import com.ootd.today.data.model.WeatherCondition
import com.ootd.today.data.model.resolveCityLabel
import com.ootd.today.data.model.CalendarPlan
import com.ootd.today.data.model.PlanDates
import com.ootd.today.data.model.WardrobeItem
import com.ootd.today.data.model.ItemStatus
import com.ootd.today.data.repository.CalendarPlanRepository
import com.ootd.today.data.repository.DailyWeatherProvider
import com.ootd.today.data.repository.LocationRepository
import com.ootd.today.data.repository.WardrobeRepository
import com.ootd.today.data.repository.WeatherProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

/**
 * 日历页 ViewModel。
 *
 * 两件事：按月亮出打卡记录，以及按月亮出天气。
 * 天气按月缓存，翻回看过的月份不会再打网络请求。
 */
@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val wardrobeRepository: WardrobeRepository,
    private val dailyWeatherProvider: DailyWeatherProvider,
    private val weatherProvider: WeatherProvider,
    private val locationRepository: LocationRepository,
    private val plans: CalendarPlanRepository,
    private val savedState: SavedStateHandle,
) : ViewModel() {

    /** 当前展示月份的起始时间戳。 */
    private val selectedDate = savedState.getStateFlow("selected_date", formatDate(System.currentTimeMillis()))
    private val displayedMonth = MutableStateFlow(startOfMonth(PlanDates.calendar(selectedDate.value).timeInMillis))
    private val editor = MutableStateFlow(PlanEditorState())

    /** 已取到的天气，key 是 `yyyy-MM-dd`。跨月累加，不做淘汰（数据量很小）。 */
    private val weatherCache = MutableStateFlow<Map<String, DailyWeather>>(emptyMap())

    /**
     * 今天的实况天气。
     *
     * 逐日接口给的 `weather_code` 是「当天最严重的天气」，会偏悲观 ——
     * 实测中今天实况是多云，逐日接口却给了「雨」，导致日历跟首页对不上。
     * 因此今天这一天改用实况，其余日期仍然用逐日数据。
     */
    private val liveWeather = MutableStateFlow<Weather?>(null)

    /** 已经发起过请求的月份，避免翻月时重复请求。 */
    private val requestedMonths = mutableSetOf<String>()

    init {
        viewModelScope.launch {
            displayedMonth.collectLatest { monthStart ->
                loadWeather(monthStart)
            }
        }
        viewModelScope.launch { loadLiveWeather() }
    }

    private suspend fun loadLiveWeather() {
        val deviceLocation = locationRepository.current()
        val coordinates = deviceLocation?.coordinates ?: Coordinates.DEFAULT
        liveWeather.value = weatherProvider.current(coordinates, deviceLocation.resolveCityLabel())
    }

    private suspend fun loadWeather(monthStart: Long) {
        val monthKey = monthKeyOf(monthStart)
        if (!requestedMonths.add(monthKey)) return

        // 日历的天气也跟随定位；取不到就退回默认坐标。
        val coordinates = locationRepository.current()?.coordinates ?: Coordinates.DEFAULT
        val from = formatDate(monthStart)
        val to = formatDate(endOfMonth(monthStart))
        val fetched = dailyWeatherProvider.daily(coordinates, from, to)

        if (fetched.isEmpty()) {
            // 失败就允许下次翻回来时重试，不把失败也当成"已请求过"。
            requestedMonths.remove(monthKey)
            return
        }
        weatherCache.value = weatherCache.value + fetched
    }

    private val monthState: StateFlow<CalendarUiState> = combine(
        wardrobeRepository.observeLogs(),
        displayedMonth,
        weatherCache,
        liveWeather
    ) { logs, monthStart, weather, live ->
        val monthEnd = endOfMonth(monthStart)
        val inMonth = logs.filter { it.timestamp in monthStart..monthEnd }

        // 同一天有多条时，格子缩略图和天气都取最新那条。
        val byDay = inMonth
            .groupBy { dayOfMonth(it.timestamp) }
            .mapValues { (_, list) -> list.maxByOrNull { it.timestamp } ?: list.first() }

        // 天气按「几号」重排，让 UI 直接用天号取，不必关心日期格式。
        val weatherByDay = weather
            .filterKeys { it.startsWith(monthKeyOf(monthStart)) }
            .mapNotNull { (dateKey, day) ->
                dateKey.substringAfterLast('-').toIntOrNull()?.let { it to day }
            }
            .toMap()
            .withLiveConditionForToday(monthStart, live?.condition)

        CalendarUiState(
            isLoading = false,
            monthStart = monthStart,
            logsByDay = byDay,
            totalLogs = logs.size,
            currentDayLogs = inMonth.sortedByDescending { it.timestamp },
            weatherByDay = weatherByDay
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = CalendarUiState(monthStart = displayedMonth.value)
    )

    val uiState = combine(monthState, selectedDate, plans.observeAll(), wardrobeRepository.observeItems(), editor) { month, date, allPlans, items, edit ->
        val monthPlans = allPlans.filter { it.dateKey.startsWith(monthKeyOf(month.monthStart)) }
        month.copy(
            selectedDate = date,
            selectedLogs = month.currentDayLogs.filter { formatDate(it.timestamp) == date },
            selectedPlans = monthPlans.filter { it.dateKey == date }.sortedWith(compareBy<CalendarPlan> { it.completed }.thenBy { it.startMinute ?: -1 }.thenBy { it.id }),
            plansByDay = monthPlans.groupBy { it.dateKey.takeLast(2).toInt() },
            monthPlanCount = monthPlans.size,
            pendingPlanCount = monthPlans.count { !it.completed },
            wardrobe = items.filter { !it.isDemo && it.status == ItemStatus.ACTIVE },
            editor = edit
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), CalendarUiState(selectedDate = selectedDate.value, monthStart = displayedMonth.value))

    fun selectDate(date: String) {
        if (!PlanDates.isValid(date)) return
        savedState["selected_date"] = date
        displayedMonth.value = startOfMonth(PlanDates.calendar(date).timeInMillis)
    }

    fun today() = selectDate(formatDate(System.currentTimeMillis()))

    fun openPlan(plan: CalendarPlan? = null) { editor.value = PlanEditorState(draft = plan ?: CalendarPlan(dateKey = selectedDate.value)) }
    fun closeEditor() { if (!editor.value.saving) editor.value = PlanEditorState() }
    fun savePlan(plan: CalendarPlan) {
        if (editor.value.saving) return
        val error = plan.validationError()
        if (error != null) { editor.update { it.copy(error = error) }; return }
        editor.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            try { plans.save(plan); selectDate(plan.dateKey); editor.value = PlanEditorState() }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { editor.update { it.copy(saving = false, error = "保存失败，请重试") } }
        }
    }
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message
    fun clearMessage() { _message.value = null }
    fun completePlan(plan: CalendarPlan) = planAction { plans.complete(plan.id, !plan.completed) }
    fun deletePlan(id: Long) = planAction { plans.delete(id) }
    private fun planAction(action: suspend () -> Unit) {
        viewModelScope.launch {
            try { action() }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { _message.value = "操作未成功，请稍后重试" }
        }
    }

    fun previousMonth() {
        selectDate(formatDate(shiftMonth(-1)))
    }

    fun nextMonth() {
        selectDate(formatDate(shiftMonth(1)))
    }

    private fun shiftMonth(delta: Int): Long = startOfMonth(
        Calendar.getInstance().apply {
            timeInMillis = displayedMonth.value
            add(Calendar.MONTH, delta)
        }.timeInMillis
    )

    fun deleteLog(id: Long) {
        viewModelScope.launch { wardrobeRepository.deleteLog(id) }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

/**
 * 把「今天」那天的天气状况换成实况。
 *
 * 只在看当前月份、且实况已拿到时替换，并且**只换状况、不动高低温**——
 * 高低温是全天的事实数据，换成实况那个瞬时值反而丢信息。
 *
 * 在 ViewModel 里替换而不是让 UI 判断，好处是格子与下方记录卡片
 * 读的是同一份数据，不会出现「格子多云、卡片下雨」这种自相矛盾。
 *
 * @param now 当前时间，可注入以便测试。
 */
internal fun Map<Int, DailyWeather>.withLiveConditionForToday(
    monthStart: Long,
    liveCondition: WeatherCondition?,
    now: Long = System.currentTimeMillis()
): Map<Int, DailyWeather> {
    if (liveCondition == null) return this
    if (monthKeyOf(monthStart) != monthKeyOf(now)) return this

    val today = dayOfMonth(now)
    val existing = this[today] ?: return this
    return this + (today to existing.copy(condition = liveCondition))
}

/** 日历页 UI 状态。 */
data class CalendarUiState(
    val selectedDate: String = "",
    val selectedLogs: List<OutfitLog> = emptyList(),
    val selectedPlans: List<CalendarPlan> = emptyList(),
    val plansByDay: Map<Int, List<CalendarPlan>> = emptyMap(),
    val monthPlanCount: Int = 0,
    val pendingPlanCount: Int = 0,
    val wardrobe: List<WardrobeItem> = emptyList(),
    val editor: PlanEditorState = PlanEditorState(),
    val isLoading: Boolean = true,
    val monthStart: Long = 0L,
    /** key = 该月的第几天（1..31），value = 当天最新的一条打卡。 */
    val logsByDay: Map<Int, OutfitLog> = emptyMap(),
    val totalLogs: Int = 0,
    /** 当前月份的全部打卡记录，按时间倒序。 */
    val currentDayLogs: List<OutfitLog> = emptyList(),
    /** key = 该月的第几天，value = 当天天气。取不到时该天就是没有。 */
    val weatherByDay: Map<Int, DailyWeather> = emptyMap()
)

data class PlanEditorState(val draft: CalendarPlan? = null, val saving: Boolean = false, val error: String? = null)

// --- 时间工具 ---

/**
 * 格式化为 `yyyy-MM-dd`。
 *
 * 手写而不用 SimpleDateFormat：后者不是线程安全的，而这些函数会在
 * flow 的 combine 转换里被调用，执行线程不确定，共享一个实例会出问题。
 */
internal fun formatDate(millis: Long): String {
    val c = Calendar.getInstance().apply { timeInMillis = millis }
    val month = c.get(Calendar.MONTH) + 1
    val day = c.get(Calendar.DAY_OF_MONTH)
    return "${c.get(Calendar.YEAR)}-" +
        month.toString().padStart(2, '0') + "-" +
        day.toString().padStart(2, '0')
}

private fun monthKeyOf(millis: Long): String = formatDate(millis).substring(0, 7)

private fun startOfMonth(millis: Long): Long = Calendar.getInstance().apply {
    timeInMillis = millis
    set(Calendar.DAY_OF_MONTH, 1)
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis

private fun endOfMonth(monthStartMillis: Long): Long = Calendar.getInstance().apply {
    timeInMillis = monthStartMillis
    set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
    set(Calendar.HOUR_OF_DAY, 23)
    set(Calendar.MINUTE, 59)
    set(Calendar.SECOND, 59)
    set(Calendar.MILLISECOND, 999)
}.timeInMillis

private fun dayOfMonth(millis: Long): Int = Calendar.getInstance().apply {
    timeInMillis = millis
}.get(Calendar.DAY_OF_MONTH)
