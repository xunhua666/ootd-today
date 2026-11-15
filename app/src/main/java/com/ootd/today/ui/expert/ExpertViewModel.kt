package com.ootd.today.ui.expert

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ootd.today.data.local.*
import com.ootd.today.data.model.*
import com.ootd.today.data.prefs.SettingsRepository
import com.ootd.today.data.prefs.UserSettings
import com.ootd.today.data.repository.LocalExpertRuntime
import com.ootd.today.data.repository.WardrobeRepository
import com.ootd.today.domain.OutfitRecommendEngine
import com.ootd.today.domain.stylist.*
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import java.util.Calendar
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class ExpertTurn(val user: Boolean, val text: String)
data class ExpertUiState(
    val models: List<ModelDownloadState> = LocalModel.ALL.map { ModelDownloadState(it) },
    val selected: LocalModel = LocalModel.RECOMMENDED,
    val busy: Boolean = false,
    val fileBusy: Boolean = false,
    val status: String = "",
    val error: String? = null,
    val messages: List<ExpertTurn> = emptyList(),
    val draftReply: String = "",
    val outfit: List<WardrobeItem> = emptyList(),
    val isDemo: Boolean = false
)

@HiltViewModel
class ExpertViewModel @Inject constructor(
    private val store: LocalModelStore,
    private val runtime: LocalExpertRuntime,
    private val wardrobe: WardrobeRepository,
    private val engine: OutfitRecommendEngine,
    private val settings: SettingsRepository
) : ViewModel() {
    val profile = settings.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserSettings())
    private val _state = MutableStateFlow(ExpertUiState(selected = store.selected()))
    val state = _state.asStateFlow()
    private var request: Job? = null
    private var weather: Weather? = null
    private var occasion = Occasion.COMMUTE
    private var requiredId: Long? = null
    init {
        viewModelScope.launch {
            while (isActive) {
                if (!_state.value.fileBusy) {
                    try {
                        val downloads = LocalModel.ALL.map { m -> store.state(m) }
                        _state.update { it.copy(models = downloads) }
                    }
                    catch (e: CancellationException) { throw e }
                    catch (e: Exception) { _state.update { it.copy(error = "读取模型状态失败：${e.message}") } }
                }
                delay(1500)
            }
        }
    }
    fun setContext(weather: Weather?, occasion: Occasion, requiredId: Long?) {
        this.weather = weather; this.occasion = occasion; this.requiredId = requiredId
    }
    fun saveProfile(name: String, preferences: String) {
        viewModelScope.launch { settings.setExpertProfile(name, preferences) }
    }
    fun select(model: LocalModel) {
        if (_state.value.busy || _state.value.fileBusy) return
        store.select(model)
        _state.update { it.copy(selected = model) }
    }
    private fun fileAction(action: suspend () -> Unit) {
        if (_state.value.busy || _state.value.fileBusy) return
        _state.update { it.copy(fileBusy = true, error = null) }
        viewModelScope.launch {
            try { action() }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { _state.update { it.copy(error = e.message ?: "操作失败，请重试") } }
            finally { _state.update { it.copy(fileBusy = false) } }
        }
    }
    fun download(model: LocalModel, wifiOnly: Boolean, mirror: Boolean) = fileAction { store.start(model, wifiOnly, mirror) }
    fun cancelDownload(model: LocalModel) = fileAction { store.cancel(model) }
    fun delete(model: LocalModel) = fileAction { store.delete(model) }
    fun import(model: LocalModel, uri: Uri) = fileAction { store.import(model, uri) }
    fun clearError() { _state.update { it.copy(error = null) } }
    fun stop() { request?.cancel() }
    fun clearChat() { if (!_state.value.busy) _state.update { it.copy(messages = emptyList(), outfit = emptyList(), draftReply = "") } }

    fun send(text: String) {
        val question = text.trim().take(500)
        val snapshot = _state.value
        if (question.isBlank() || snapshot.busy || snapshot.fileBusy) return
        if (profile.value.expertName.isBlank()) { _state.update { it.copy(error = "先给你的审美专家起个名字吧") }; return }
        if (snapshot.models.none { it.model == snapshot.selected && it.ready }) {
            _state.update { it.copy(error = "请先下载或导入所选模型") }; return
        }
        val history = snapshot.messages.map { it.user to it.text }
        _state.update { it.copy(busy = true, error = null, draftReply = "", messages = (it.messages + ExpertTurn(true, question)).takeLast(20)) }
        request = viewModelScope.launch {
            try {
                val items = wardrobe.observeItems().first().filter { it.status == ItemStatus.ACTIVE }
                if (items.isEmpty() && LocalExpertPrompt.needsWardrobe(question)) {
                    _state.update { it.copy(error = "衣橱还是空的，先录入几件真实单品，再来聊这套怎么搭；也可以先问通用配色。") }
                    return@launch
                }
                val month = Calendar.getInstance().get(Calendar.MONTH)
                val season = when(month) { 2,3,4 -> Season.SPRING; 5,6,7 -> Season.SUMMER; 8,9,10 -> Season.AUTUMN; else -> Season.WINTER }
                val outfit = weather?.let { engine.recommend(items, it, occasion, season, requiredItemId = requiredId)?.items }.orEmpty()
                _state.update { it.copy(outfit = outfit, isDemo = items.any { item -> item.isDemo }) }
                val userProfile = settings.settings.first()
                val answer = runtime.reply(
                    snapshot.selected, LocalExpertPrompt.system(userProfile.expertName, userProfile.expertPreferences), history,
                    LocalExpertPrompt.context(items, weather, occasion, outfit) + "\n用户问题：" + question + "\n/no_think",
                    onStatus = { status -> _state.update { it.copy(status = status) } },
                    onChunk = { chunk -> _state.update { it.copy(draftReply = chunk) } }
                )
                _state.update { it.copy(messages = (it.messages + ExpertTurn(false, answer)).takeLast(20), draftReply = "") }
            } catch (e: CancellationException) {
                val partial = _state.value.draftReply
                _state.update { it.copy(messages = if (partial.isBlank()) it.messages else it.messages + ExpertTurn(false, "$partial\n（已停止）"), draftReply = "", status = "已停止生成") }
                throw e
            } catch (e: LinkageError) {
                _state.update { it.copy(error = "当前设备不支持本地推理运行库，请使用64位安卓手机", draftReply = "") }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message ?: "本地模型暂时不可用，请重试", draftReply = "") }
            } finally { _state.update { it.copy(busy = false) } }
        }
    }
}
