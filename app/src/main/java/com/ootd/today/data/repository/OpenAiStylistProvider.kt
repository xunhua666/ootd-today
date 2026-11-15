package com.ootd.today.data.repository

import android.util.Base64
import com.ootd.today.data.prefs.SettingsRepository
import com.ootd.today.domain.stylist.StylistRequest
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * OpenAI 兼容格式的大模型实现。
 *
 * DeepSeek、通义千问、智谱、OpenAI 的对话接口都遵循同一套请求/响应格式，
 * 因此一份实现即可覆盖，换服务商只改设置里的地址与模型名。
 *
 * 失败一律返回 null（不抛异常）：大模型不可用、额度用尽、Key 失效
 * 都属常见情况，调用方会回退到本地算法。
 */
@Singleton
class OpenAiStylistProvider @Inject constructor(
    private val settingsRepository: SettingsRepository
) : StylistProvider {

    /**
     * 是否已配置好。
     *
     * 每次实时读 DataStore 而不是缓存 —— 用户可能在设置页刚填完 Key
     * 就返回首页，缓存的旧值会让这次判断出错。
     * 读一次 DataStore 开销很小，且这里是挂起函数，不阻塞主线程。
     */
    override suspend fun isConfigured(): Boolean = runCatching {
        settingsRepository.settings.first().stylist.isReady
    }.getOrDefault(false)

    override suspend fun complete(request: StylistRequest): String? = withContext(Dispatchers.IO) {
        val settings = settingsRepository.settings.first().stylist
        if (!settings.isReady) return@withContext null

        val payload = buildPayload(
            model = settings.model,
            request = request,
            includeImages = settings.uploadPhotos
        )

        val url = settings.baseUrl.trimEnd('/') + "/chat/completions"
        val raw = HttpJson.postJson(
            url = url,
            body = payload,
            headers = mapOf("Authorization" to "Bearer ${settings.apiKey}")
        ) ?: return@withContext null

        extractContent(raw)
    }

    // ------------------------------------------------------------------
    // 请求构造
    // ------------------------------------------------------------------

    private fun buildPayload(
        model: String,
        request: StylistRequest,
        includeImages: Boolean
    ): String {
        val userMessage = if (includeImages) {
            buildMultimodalMessage(request)
        } else {
            JSONObject().put("role", "user").put("content", request.userText)
        }

        // 系统提示词、单品清单、追问分成三条消息：把追问放在最后，
        // 模型对「最后一条用户消息」的服从度最高。
        val messages = JSONArray().apply {
            put(JSONObject().put("role", "system").put("content", request.systemPrompt))
            put(userMessage)
            request.followUp?.let {
                put(JSONObject().put("role", "user").put("content", it))
            }
        }

        return JSONObject()
            .put("model", model)
            .put("messages", messages)
            // 温度稍高一点，避免每天给出一模一样的搭配。
            .put("temperature", 0.7)
            .put("max_tokens", 700)
            .toString()
    }

    /** 多模态消息：一段文字 + 若干张照片。 */
    private fun buildMultimodalMessage(request: StylistRequest): JSONObject {
        val parts = JSONArray().apply {
            put(
                JSONObject()
                    .put("type", "text")
                    .put("text", request.userText + "\n附带的照片按上面的顺序对应前几件单品。")
            )
            request.imagePaths.forEach { path ->
                encodeImage(path)?.let { dataUri ->
                    put(
                        JSONObject()
                            .put("type", "image_url")
                            .put("image_url", JSONObject().put("url", dataUri))
                    )
                }
            }
        }
        return JSONObject().put("role", "user").put("content", parts)
    }

    /**
     * 读图并转成 data URI。
     *
     * 超过体积上限的直接跳过：单张几 MB 的照片会把请求体撑爆，
     * 而且模型也看不出更多细节。
     */
    private fun encodeImage(path: String): String? = runCatching {
        val file = File(path)
        if (!file.exists() || file.length() > MAX_IMAGE_BYTES) return@runCatching null
        val base64 = Base64.encodeToString(file.readBytes(), Base64.NO_WRAP)
        "data:image/jpeg;base64,$base64"
    }.getOrNull()

    // ------------------------------------------------------------------
    // 响应解析
    // ------------------------------------------------------------------

    /** 从 OpenAI 格式的响应里取出正文。 */
    private fun extractContent(raw: String): String? = runCatching {
        JSONObject(raw)
            .optJSONArray("choices")
            ?.optJSONObject(0)
            ?.optJSONObject("message")
            ?.optString("content")
            ?.takeIf { it.isNotBlank() }
    }.getOrNull()

    private companion object {
        /** 单张照片上限，约 1.5 MB。 */
        const val MAX_IMAGE_BYTES = 1_500_000L
    }
}