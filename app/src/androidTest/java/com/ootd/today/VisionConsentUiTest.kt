package com.ootd.today

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.ootd.today.data.model.*
import com.ootd.today.domain.vision.GarmentVisionResult
import com.ootd.today.ui.add.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class VisionConsentUiTest {
    @get:Rule val rule = createComposeRule()
    @Test fun photoIsOnlySubmittedAfterAnExplicitSecondConfirmation() {
        var calls = 0
        rule.setContent { MaterialTheme {
            GarmentShapeSection(AddItemUiState(imagePath = "local-test-photo.jpg"), true, "vision.example.invalid", {}, { calls++ }, {}, {}, {}, {})
        } }
        assertEquals(0, calls)
        rule.onNodeWithText("云端识别这张照片").performClick()
        rule.onNodeWithText("确认上传并识别").assertExists()
        assertEquals(0, calls)
        rule.onNodeWithText("取消").performClick()
        assertEquals(0, calls)
        rule.onNodeWithText("云端识别这张照片").performClick()
        rule.onNodeWithText("确认上传并识别").performClick()
        assertEquals(1, calls)
    }
    @Test fun recognitionIsNotAppliedBeforeReview() {
        var applied = 0; var discarded = 0
        val result = GarmentVisionResult(ClothingCategory.TOP, "短款夹克", GarmentShape(length = GarmentLength.CROPPED), "可见短衣摆", "松紧不确定")
        rule.setContent { MaterialTheme {
            GarmentShapeSection(AddItemUiState(imagePath = "local-test-photo.jpg", visionDraft = result), true, "vision.example.invalid", {}, {}, {}, {}, { applied++ }, { discarded++ })
        } }
        rule.onNodeWithText("先确认，再参与搭配").assertExists()
        assertEquals(0, applied)
        rule.onNodeWithText("采用结果并继续").performClick()
        assertEquals(1, applied); assertEquals(0, discarded)
    }
}
