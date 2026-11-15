package com.ootd.today.data.prefs

import java.net.URI

data class VisionSettings(val baseUrl: String = "", val model: String = "qwen3-vl-plus", val apiKey: String = "") {
    fun validationError(): String? = when {
        apiKey.isBlank() -> "请先配置视觉服务 API Key"
        model.isBlank() || model.length > 100 -> "请填写视觉模型名称"
        !runCatching { URI(baseUrl).let { it.scheme == "https" && !it.host.isNullOrBlank() && it.userInfo == null && it.rawQuery == null && it.fragment == null } }.getOrDefault(false) -> "请填写服务商控制台提供的 HTTPS 接口地址"
        else -> null
    }
    val isReady: Boolean get() = validationError() == null
    val endpoint: String get() = baseUrl.trimEnd('/').let { if (it.endsWith("/chat/completions")) it else "$it/chat/completions" }
}
