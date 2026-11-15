package com.ootd.today

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class PlannerUiTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test fun dragDoesNotOpenChatAndPlansCanBeCreatedAndCompleted() {
        rule.onNodeWithText("日历").performClick()
        val avatar = rule.onNodeWithContentDescription("悬浮审美专家")
        val before = avatar.fetchSemanticsNode().boundsInRoot
        avatar.performTouchInput { swipe(center, center + Offset(-180f, -230f), durationMillis = 700) }
        val after = avatar.fetchSemanticsNode().boundsInRoot
        assertTrue(after.left < before.left - 100f)
        assertTrue(after.top < before.top - 100f)
        rule.onNodeWithText("手机本地运行 · 对话不上传").assertDoesNotExist()
        avatar.performClick()
        rule.onNodeWithText("手机本地运行 · 对话不上传").assertIsDisplayed()
        rule.onNodeWithContentDescription("返回").performClick()
        val returned = avatar.fetchSemanticsNode().boundsInRoot
        assertEquals(after.left, returned.left, 2f)
        assertEquals(after.top, returned.top, 2f)
        rule.onNodeWithText("新建日程").performScrollTo().performClick()
        val title = "QA-画展约会"
        rule.onNodeWithText("日程名称").performTextInput(title)
        rule.onNodeWithText("保存日程").performClick()
        rule.waitUntil(10000) { rule.onAllNodesWithText(title).fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText(title).performScrollTo().assertIsDisplayed()
        rule.onNodeWithContentDescription("完成日程").performClick()
        rule.onNodeWithContentDescription("标记日程未完成").assertExists()
        rule.onNodeWithContentDescription("日程更多操作", useUnmergedTree = true).performClick()
        rule.onNodeWithText("删除日程").performClick()
        rule.onNodeWithText("删除").performClick()
        rule.waitUntil(10000) { rule.onAllNodesWithText(title).fetchSemanticsNodes().isEmpty() }
    }
}
