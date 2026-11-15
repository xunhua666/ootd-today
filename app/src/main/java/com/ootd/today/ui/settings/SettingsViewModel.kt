package com.ootd.today.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ootd.today.data.prefs.SettingsRepository
import com.ootd.today.data.prefs.StylistVendor
import com.ootd.today.data.prefs.UserSettings
import com.ootd.today.notification.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.MutableStateFlow
import com.ootd.today.data.prefs.VisionSettings
import kotlinx.coroutines.launch

/**
 * 设置页 ViewModel。
 *
 * 每次修改设置后立刻 [ReminderScheduler.reschedule]，
 * 保证开关与时间变更即时生效，不需要重启 App。
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val reminderScheduler: ReminderScheduler
) : ViewModel() {
    val visionMessage = MutableStateFlow<String?>(null)
    val visionSaving = MutableStateFlow(false)
    fun saveVisionSettings(baseUrl: String, model: String, apiKey: String) {
        if (visionSaving.value) return
        val config = VisionSettings(baseUrl.trim(), model.trim(), apiKey.trim())
        val error = config.validationError()
        if (error != null) { visionMessage.value = error; return }
        visionSaving.value = true
        viewModelScope.launch {
            try { settingsRepository.setVisionSettings(config.baseUrl, config.model, config.apiKey); visionMessage.value = "已保存，只有你确认识别照片时才会上传" }
            catch (_: Exception) { visionMessage.value = "保存失败，请重试" }
            finally { visionSaving.value = false }
        }
    }
    fun clearVisionKey() {
        viewModelScope.launch {
            val old = settingsRepository.settings.first().vision
            settingsRepository.setVisionSettings(old.baseUrl, old.model, "")
            visionMessage.value = "视觉服务 Key 已清除"
        }
    }

    val uiState: StateFlow<UserSettings> = settingsRepository.settings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = UserSettings()
        )

    fun setReminderEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setReminderEnabled(enabled)
            // 写入后重新读取，确保调度器拿到的是落盘后的最新值。
            reminderScheduler.reschedule(settingsRepository.settings.first())
        }
    }

    fun setReminderTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            settingsRepository.setReminderTime(hour, minute)
            reminderScheduler.reschedule(settingsRepository.settings.first())
        }
    }

    // ------------------------------------------------------------------
    // 审美专家
    // ------------------------------------------------------------------

    fun setStylistVendor(vendor: StylistVendor) {
        viewModelScope.launch { settingsRepository.setStylistVendor(vendor) }
    }

    fun setStylistApiKey(apiKey: String) {
        viewModelScope.launch { settingsRepository.setStylistApiKey(apiKey) }
    }

    fun setStylistUploadPhotos(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setStylistUploadPhotos(enabled) }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
