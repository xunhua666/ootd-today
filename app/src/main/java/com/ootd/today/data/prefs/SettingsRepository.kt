package com.ootd.today.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ootd.today.data.crypto.SecretStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** DataStore 实例，进程内单例。 */
private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "ootd_settings"
)

/** 用户设置。 */
data class UserSettings(
    val vision: VisionSettings = VisionSettings(),
    val expertName: String = "",
    val expertPreferences: String = "",
    val demoWardrobeEnabled: Boolean = true,
    /** 是否开启每日穿搭推送。 */
    val dailyReminderEnabled: Boolean = DEFAULT_REMINDER_ENABLED,
    /** 推送小时（0..23）。 */
    val reminderHour: Int = DEFAULT_REMINDER_HOUR,
    /** 推送分钟（0..59）。 */
    val reminderMinute: Int = DEFAULT_REMINDER_MINUTE,
    /** 「审美专家」的配置。 */
    val stylist: StylistSettings = StylistSettings()
) {
    companion object {
        const val DEFAULT_REMINDER_ENABLED = true
        const val DEFAULT_REMINDER_HOUR = 7
        const val DEFAULT_REMINDER_MINUTE = 30
    }
}

/**
 * 用户设置仓库，基于 DataStore Preferences。
 *
 * 只存轻量偏好（推送、专家配置）。衣橱数据在 Room，二者职责分离。
 *
 * API Key 不直接存明文：写入前经 [SecretStore] 加密，读出时解密。
 * 这样即使备份文件被导出，Key 也不会以明文形式暴露。
 */
@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val secretStore: SecretStore
) {

    private object Keys {
        val VISION_API_KEY = stringPreferencesKey("vision_api_key")
        val VISION_BASE_URL = stringPreferencesKey("vision_base_url")
        val VISION_MODEL = stringPreferencesKey("vision_model")
        val EXPERT_NAME = stringPreferencesKey("expert_name")
        val EXPERT_PREFERENCES = stringPreferencesKey("expert_preferences")
        val DEMO_WARDROBE_ENABLED = booleanPreferencesKey("demo_wardrobe_enabled")
        val REMINDER_ENABLED = booleanPreferencesKey("daily_reminder_enabled")
        val REMINDER_HOUR = intPreferencesKey("reminder_hour")
        val REMINDER_MINUTE = intPreferencesKey("reminder_minute")

        // 专家配置
        val STYLIST_API_KEY = stringPreferencesKey("stylist_api_key")
        val STYLIST_BASE_URL = stringPreferencesKey("stylist_base_url")
        val STYLIST_MODEL = stringPreferencesKey("stylist_model")
        val STYLIST_UPLOAD_PHOTOS = booleanPreferencesKey("stylist_upload_photos")
    }

    val settings: Flow<UserSettings> = context.settingsDataStore.data.map { prefs ->
        val vendor = StylistVendor.DEEPSEEK
        UserSettings(
            vision = VisionSettings(prefs[Keys.VISION_BASE_URL].orEmpty(), prefs[Keys.VISION_MODEL] ?: "qwen3-vl-plus", secretStore.decrypt(prefs[Keys.VISION_API_KEY].orEmpty())),
            expertName = prefs[Keys.EXPERT_NAME].orEmpty(),
            expertPreferences = prefs[Keys.EXPERT_PREFERENCES].orEmpty(),
            demoWardrobeEnabled = prefs[Keys.DEMO_WARDROBE_ENABLED] ?: true,
            dailyReminderEnabled = prefs[Keys.REMINDER_ENABLED]
                ?: UserSettings.DEFAULT_REMINDER_ENABLED,
            reminderHour = prefs[Keys.REMINDER_HOUR] ?: UserSettings.DEFAULT_REMINDER_HOUR,
            reminderMinute = prefs[Keys.REMINDER_MINUTE] ?: UserSettings.DEFAULT_REMINDER_MINUTE,
            stylist = StylistSettings(
                apiKey = secretStore.decrypt(prefs[Keys.STYLIST_API_KEY].orEmpty()),
                baseUrl = prefs[Keys.STYLIST_BASE_URL] ?: vendor.baseUrl,
                model = prefs[Keys.STYLIST_MODEL] ?: vendor.defaultModel,
                uploadPhotos = prefs[Keys.STYLIST_UPLOAD_PHOTOS] ?: true
            )
        )
    }

    suspend fun startPersonalWardrobe() {
        context.settingsDataStore.edit { it[Keys.DEMO_WARDROBE_ENABLED] = false }
    }

    suspend fun setVisionSettings(baseUrl: String, model: String, apiKey: String) {
        val encrypted = secretStore.encrypt(apiKey.trim())
        context.settingsDataStore.edit {
            it[Keys.VISION_BASE_URL] = baseUrl.trim().trimEnd('/')
            it[Keys.VISION_MODEL] = model.trim()
            if (encrypted.isNullOrEmpty()) it.remove(Keys.VISION_API_KEY) else it[Keys.VISION_API_KEY] = encrypted
        }
    }

    suspend fun setExpertProfile(name: String, preferences: String) {
        context.settingsDataStore.edit {
            it[Keys.EXPERT_NAME] = name.trim().take(16)
            it[Keys.EXPERT_PREFERENCES] = preferences.trim().take(300)
        }
    }

    suspend fun setReminderEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.REMINDER_ENABLED] = enabled }
    }

    suspend fun setReminderTime(hour: Int, minute: Int) {
        context.settingsDataStore.edit {
            it[Keys.REMINDER_HOUR] = hour.coerceIn(0, 23)
            it[Keys.REMINDER_MINUTE] = minute.coerceIn(0, 59)
        }
    }

    // ------------------------------------------------------------------
    // 审美专家
    // ------------------------------------------------------------------

    /** 保存 API Key。传空串表示清除。 */
    suspend fun setStylistApiKey(apiKey: String) {
        val encrypted = secretStore.encrypt(apiKey.trim())
        context.settingsDataStore.edit {
            if (encrypted.isNullOrEmpty()) it.remove(Keys.STYLIST_API_KEY)
            else it[Keys.STYLIST_API_KEY] = encrypted
        }
    }

    /**
     * 切换服务商。
     *
     * 同时写入该服务商的默认模型名 —— 用户切到别家时若还留着上一家的
     * 模型名，请求必然失败，这是接入时很容易踩的坑。
     */
    suspend fun setStylistVendor(vendor: StylistVendor) {
        context.settingsDataStore.edit {
            it[Keys.STYLIST_BASE_URL] = vendor.baseUrl
            it[Keys.STYLIST_MODEL] = vendor.defaultModel
        }
    }

    /** 手动指定 baseUrl 与模型名（预设之外的服务商走这条路）。 */
    suspend fun setStylistEndpoint(baseUrl: String, model: String) {
        context.settingsDataStore.edit {
            it[Keys.STYLIST_BASE_URL] = baseUrl.trim()
            it[Keys.STYLIST_MODEL] = model.trim()
        }
    }

    /** 是否允许上传单品照片。 */
    suspend fun setStylistUploadPhotos(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.STYLIST_UPLOAD_PHOTOS] = enabled }
    }
}
