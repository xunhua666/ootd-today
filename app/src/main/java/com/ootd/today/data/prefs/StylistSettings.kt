package com.ootd.today.data.prefs

/**
 * 「审美专家」的配置。
 *
 * @property apiKey 用户自己的 API Key。空串表示未配置，此时「问沈砚」不可用。
 * @property baseUrl 服务商的接口地址（OpenAI 兼容格式）。
 * @property model 模型名。
 * @property uploadPhotos 是否允许把单品照片一并发送。
 *   默认开启（用户已确认），但保留开关 —— 这是应用「数据不出设备」
 *   定位下唯一一处数据外发，应当让用户能随时收回。
 */
data class StylistSettings(
    val apiKey: String = "",
    val baseUrl: String = StylistVendor.DEEPSEEK.baseUrl,
    val model: String = StylistVendor.DEEPSEEK.defaultModel,
    val uploadPhotos: Boolean = true
) {
    /** 是否已具备调用条件。 */
    val isReady: Boolean get() = apiKey.isNotBlank() && baseUrl.isNotBlank() && model.isNotBlank()
}

/**
 * 服务商预设。
 *
 * 这几家的对话接口都兼容 OpenAI 格式，因此换服务商只是换个地址和模型名。
 * 把它们列出来是为了让用户少查文档 —— baseUrl 写错是接入时最常见的坑。
 */
enum class StylistVendor(
    val label: String,
    val baseUrl: String,
    val defaultModel: String
) {
    DEEPSEEK("DeepSeek", "https://api.deepseek.com/v1", "deepseek-chat"),
    QWEN("通义千问", "https://dashscope.aliyuncs.com/compatible-mode/v1", "qwen-plus"),
    ZHIPU("智谱 GLM", "https://open.bigmodel.cn/api/paas/v4", "glm-4-flash"),
    OPENAI("OpenAI", "https://api.openai.com/v1", "gpt-4o-mini");

    companion object {
        /** 按 baseUrl 反查预设，用于在设置页回显当前选中项。 */
        fun matchByUrl(baseUrl: String): StylistVendor? =
            entries.firstOrNull { it.baseUrl == baseUrl }
    }
}