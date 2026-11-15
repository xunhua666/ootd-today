package com.ootd.today.data.repository

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import com.google.ai.edge.litertlm.*
import com.ootd.today.data.local.LocalModelStore
import com.ootd.today.domain.stylist.LocalModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

@Singleton
class LocalExpertRuntime @Inject constructor(
    @ApplicationContext private val context: Context,
    private val models: LocalModelStore
) {
    private val mutex = Mutex()

    suspend fun reply(
        model: LocalModel, system: String, history: List<Pair<Boolean, String>>, prompt: String,
        onStatus: (String) -> Unit, onChunk: (String) -> Unit
    ): String = mutex.withLock {
        withContext(Dispatchers.Default) {
            check(Build.SUPPORTED_ABIS.any { it == "arm64-v8a" || it == "x86_64" }) { "本地专家需要64位手机" }
            check(models.state(model).ready) { "请先完成模型下载与校验" }
            val memory = ActivityManager.MemoryInfo()
            (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(memory)
            check(!memory.lowMemory && memory.availMem > 1500L * 1024 * 1024) {
                "当前可用内存较少，请关闭其他大型应用后重试"
            }
            onStatus("正在唤醒本地专家，首次加载可能需要一些时间…")
            // A request owns its engine. Release native memory even after errors/cancellation.
            Engine(EngineConfig(
                modelPath = models.file(model).absolutePath,
                backend = Backend.CPU(threadCount = 4),
                maxNumTokens = 3072,
                cacheDir = context.cacheDir.absolutePath
            )).use { engine ->
                engine.initialize()
                coroutineContext.ensureActive()
                val config = ConversationConfig(
                    systemInstruction = Contents.of(system),
                    initialMessages = history.takeLast(2).map { (user, text) ->
                        if (user) Message.user(text.take(160)) else Message.model(text.take(220))
                    },
                    samplerConfig = SamplerConfig(topK = 40, topP = 0.85, temperature = 0.5),
                    thinkingConfig = ThinkingConfig(enableThinking = false),
                    maxOutputToken = 384
                )
                engine.createConversation(config).use { conversation ->
                    onStatus("正在斟酌配色与搭配细节…")
                    val result = StringBuilder()
                    conversation.sendMessageAsync(prompt).collect { message ->
                        coroutineContext.ensureActive()
                        result.append(message.toString())
                        onChunk(result.toString())
                    }
                    result.toString().trim().also { check(it.isNotBlank()) { "模型没有给出建议，请换个说法再试" } }
                }
            }
        }
    }
}
