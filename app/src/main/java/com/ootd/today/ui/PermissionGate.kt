package com.ootd.today.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.ootd.today.data.prefs.SettingsRepository
import kotlinx.coroutines.flow.first

/**
 * 首次启动的通知权限请求门。
 *
 * Android 13+ 发通知需要运行时权限。只在「用户开启了每日推送且尚未授权」时
 * 弹一次系统授权框；拒绝后不再打扰，改由设置页引导去系统设置开启。
 *
 * 用 [remember] 保证同一次页面生命周期内只触发一次，
 * 避免每次重组都重复弹框。
 */
@Composable
fun RememberNotificationPermissionGate(settingsRepository: SettingsRepository) {
    val context = LocalContext.current
    var asked by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* 结果不处理：拒绝则由设置页兜底引导 */ }

    LaunchedEffect(Unit) {
        if (asked) return@LaunchedEffect
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return@LaunchedEffect

        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) return@LaunchedEffect

        // 只在用户确实开启了推送功能时才请求，避免「没开推送却被要权限」的困惑。
        val reminderOn = settingsRepository.settings.first().dailyReminderEnabled
        if (!reminderOn) return@LaunchedEffect

        asked = true
        launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
