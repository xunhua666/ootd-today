package com.ootd.today.domain.stylist

import com.ootd.today.data.model.Occasion
import com.ootd.today.data.model.WardrobeItem
import com.ootd.today.data.model.Weather
import com.ootd.today.data.prefs.SettingsRepository
import com.ootd.today.data.repository.StylistProvider
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

/**
 * 「审美专家」的调用编排。
 *
 * 把「构造请求 → 调模型 → 解析 → 决定成败」串起来，
 * 让 ViewModel 只需要一次调用、处理一个结果。
 *
 * 设计上最重要的一点：**任何失败都返回 null 而不是抛异常**。
 * 大模型不可用（没配 Key、断网、额度用尽、返回格式不对）都属常态，
 * 上层应静默回退到本地算法，用户不该看到报错。
 */
@Singleton
class StylistService @Inject constructor(
    private val provider: StylistProvider,
    private val settingsRepository: SettingsRepository
) {

    /** 是否已配置好，供 UI 决定要不要展示「问沈砚」入口。 */
    suspend fun isReady(): Boolean = provider.isConfigured()

    /**
     * 请「沈砚」给一套搭配。
     *
     * @param items 用户衣橱里的全部在用单品。
     * @param followUp 追问的修改要求；首次请求传 null。
     * @return 建议；任何环节失败都返回 null，调用方应回退本地算法。
     */
    suspend fun advise(
        items: List<WardrobeItem>,
        weather: Weather,
        occasion: Occasion,
        followUp: String? = null
    ): StylistAdvice? {
        if (items.size < MIN_ITEMS_REQUIRED) return null

        val uploadPhotos = runCatching {
            settingsRepository.settings.first().stylist.uploadPhotos
        }.getOrDefault(false)

        val request = StylistPromptBuilder.build(
            items = items,
            weather = weather,
            occasion = occasion,
            includePhotos = uploadPhotos,
            followUp = followUp
        )

        val name = settingsRepository.settings.first().expertName
        val namedRequest = if (name.isBlank()) request else request.copy(systemPrompt = "你的名字是「${name.take(16)}」。\n" + request.systemPrompt)
        val raw = provider.complete(namedRequest) ?: return null

        // 解析时把**发送给模型的那批单品**作为合法集合，
        // 模型编造出的 id 会被过滤掉。
        return StylistAdviceParser.parse(raw, items)
    }

    /**
     * 处理追问。
     *
     * 先把口语整理成明确指令（本地意图识别，不消耗请求），
     * 再带着这条指令重新请求。
     */
    suspend fun followUp(
        items: List<WardrobeItem>,
        weather: Weather,
        occasion: Occasion,
        userText: String
    ): StylistAdvice? {
        val normalized = StylistIntent.normalize(userText)
        if (normalized.isBlank()) return null
        return advise(
            items = items,
            weather = weather,
            occasion = occasion,
            followUp = StylistPersona.followUpInstruction(normalized)
        )
    }

    private companion object {
        /** 少于这个件数连一套都凑不出，不必浪费一次请求。 */
        const val MIN_ITEMS_REQUIRED = 3
    }
}
