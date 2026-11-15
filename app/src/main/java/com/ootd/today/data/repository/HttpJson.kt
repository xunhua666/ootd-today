package com.ootd.today.data.repository

import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * 极简 HTTP 工具。
 *
 * 项目只与几个只读接口和一个大模型接口通信，为此引入 Retrofit/OkHttp
 * 并不划算，用 [HttpURLConnection] 足够。各数据源共用这里的超时与异常处理，
 * 避免各写一份、行为不一致。
 */
internal object HttpJson {

    /** 天气这类「锦上添花」的数据用短超时，不该让用户等。 */
    const val TIMEOUT_SHORT_MS = 4_000

    /**
     * 大模型调用用长超时。
     *
     * 生成一套搭配通常要 5–20 秒，超时设短了会把正常请求也掐掉。
     * 45 秒是「足够宽松但不至于让用户以为卡死」的折中。
     */
    const val TIMEOUT_LONG_MS = 45_000

    /** GET，成功返回响应体，任何异常或非 200 都返回 null。 */
    fun get(url: String, timeoutMs: Int = TIMEOUT_SHORT_MS): String? {
        val connection = openConnection(url, timeoutMs) ?: return null
        return try {
            connection.requestMethod = "GET"
            readBody(connection)
        } catch (_: Exception) {
            null
        } finally {
            runCatching { connection.disconnect() }
        }
    }

    /**
     * POST JSON，返回响应体。
     *
     * 失败时返回 null，但**错误响应体会尽量带回来** —— 大模型接口的报错
     * （如 key 无效、额度不足、模型名写错）都写在响应体里，
     * 不读出来就没法排查。
     */
    fun postJson(
        url: String,
        body: String,
        headers: Map<String, String> = emptyMap(),
        timeoutMs: Int = TIMEOUT_LONG_MS
    ): String? {
        val connection = openConnection(url, timeoutMs) ?: return null
        return try {
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            headers.forEach { (k, v) -> connection.setRequestProperty(k, v) }

            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { it.write(body) }

            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            stream?.bufferedReader()?.use { it.readText() }
        } catch (_: Exception) {
            null
        } finally {
            runCatching { connection.disconnect() }
        }
    }

    private fun openConnection(url: String, timeoutMs: Int): HttpURLConnection? =
        runCatching {
            (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = timeoutMs
                readTimeout = timeoutMs
            }
        }.getOrNull()

    private fun readBody(connection: HttpURLConnection): String? =
        if (connection.responseCode == HttpURLConnection.HTTP_OK) {
            connection.inputStream.bufferedReader().use { it.readText() }
        } else {
            null
        }
}