package com.ootd.today.domain.stylist

import java.io.File
import org.junit.Assert.*
import org.junit.Test
import com.ootd.today.data.model.*

class LocalExpertTest {
    @Test fun wardrobeQuestionsAreDistinguishedFromGeneralColorAdvice() {
        assertTrue(LocalExpertPrompt.needsWardrobe("点评今天这套"))
        assertTrue(LocalExpertPrompt.needsWardrobe("根据我的衣橱配色"))
        assertFalse(LocalExpertPrompt.needsWardrobe("教我一组通勤配色"))
    }
    @Test fun integrityRejectsTruncationAndSameSizeCorruption() {
        val file = File.createTempFile("model-integrity", ".litertlm")
        val tiny = LocalModel.RECOMMENDED.copy(bytes = 3, sha256 = "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad")
        try {
            file.writeText("abc"); assertTrue(ModelIntegrity.verify(file, tiny))
            file.writeText("ab"); assertFalse(ModelIntegrity.verify(file, tiny))
            file.writeText("xyz"); assertFalse(ModelIntegrity.verify(file, tiny))
        } finally { file.delete() }
    }
    @Test fun integrityHonorsCancellationDuringHashing() {
        val file = File.createTempFile("model-cancel", ".litertlm")
        try {
            file.writeBytes(ByteArray(300000))
            var interrupted = false
            try { ModelIntegrity.verify(file, LocalModel.RECOMMENDED.copy(bytes = file.length())) { throw InterruptedException() } }
            catch (_: InterruptedException) { interrupted = true }
            assertTrue(interrupted)
        } finally { file.delete() }
    }
    @Test fun noWeatherOrInventoryCannotInventAnOutfit() {
        val text = LocalExpertPrompt.context(emptyList(), null, Occasion.COMMUTE, emptyList())
        assertTrue(text.contains("天气：未提供"))
        assertTrue(text.contains("无法组成完整搭配"))
        assertFalse(text.contains("℃"))
    }
    @Test fun personaUsesUserNameAndExplicitlyLimitsVision() {
        val prompt = LocalExpertPrompt.system("小栗", "喜欢复古，平底鞋")
        assertTrue(prompt.contains("小栗"))
        assertTrue(prompt.contains("喜欢复古，平底鞋"))
        assertTrue(prompt.contains("没有看到照片"))
        assertFalse(prompt.contains("沈砚"))
    }
}
