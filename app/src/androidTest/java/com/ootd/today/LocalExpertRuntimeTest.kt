package com.ootd.today

import android.net.Uri
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ootd.today.data.local.LocalModelStore
import com.ootd.today.data.repository.LocalExpertRuntime
import com.ootd.today.domain.stylist.LocalModel
import com.ootd.today.domain.stylist.LocalExpertPrompt
import java.io.File
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout

/** Push the fixed official model to the preview app's external files before running. */
@RunWith(AndroidJUnit4::class)
class LocalExpertRuntimeTest {
    private fun exercise(model: LocalModel) = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val input = File(context.getExternalFilesDir(null), "qa-${model.id}.litertlm")
        assumeTrue("QA model must be supplied separately", input.exists())
        val store = LocalModelStore(context)
        store.import(model, Uri.fromFile(input))
        assertTrue(store.state(model).ready)
        val runtime = LocalExpertRuntime(context, store)
        val chunks = mutableListOf<String>()
        val started = System.currentTimeMillis()
        val answer = withTimeout(240000) {
            runtime.reply(model, LocalExpertPrompt.system("小栗", "喜欢简约，穿着舒适"), emptyList(),
                "请点评一套通勤搭配：白色衬衫、深蓝色直筒裤、白色运动鞋。天气20℃，晴。只给两个具体建议，80字以内。/no_think",
                onStatus = {}, onChunk = { chunks.add(it) })
        }
        assertTrue(answer.any { it.code in 0x4e00..0x9fff })
        assertTrue(chunks.isNotEmpty())
        File(context.getExternalFilesDir(null), "qa-${model.id}-reply.txt").writeText("elapsedMs=${System.currentTimeMillis()-started}\n$answer")
    }
    @Test fun localRecommendedModelGeneratesChineseAdvice() { exercise(LocalModel.RECOMMENDED) }
}
