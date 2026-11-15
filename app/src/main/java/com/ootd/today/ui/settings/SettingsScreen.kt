package com.ootd.today.ui.settings

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ootd.today.notification.NotificationHelper

/** 小时候选：整点 + 半点，覆盖绝大多数使用场景，避免引入完整时间选择器。 */
private val HOUR_OPTIONS = listOf(6, 7, 8, 9, 10, 12, 18, 20, 21)
private val MINUTE_OPTIONS = listOf(0, 15, 30, 45)

/**
 * 设置页。
 *
 * P1 阶段只承载「每日推送」相关设置；主题、城市、数据导出等留待后续。
 * Android 13+ 首次开启推送时会请求通知权限。
 */
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onOpenExpert: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val settings by viewModel.uiState.collectAsStateWithLifecycle()
    val visionMessage by viewModel.visionMessage.collectAsStateWithLifecycle()
    val visionSaving by viewModel.visionSaving.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // 通知权限请求（Android 13+）。拒绝后引导去系统设置。
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            // 用户拒绝：跳系统通知设置页，让用户手动开启。
            // ACTION_APP_NOTIFICATION_SETTINGS 需 API 26+，低版本回退到应用详情页。
            runCatching {
                val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                } else {
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        .setData("package:${context.packageName}".toUri())
                }
                context.startActivity(intent)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Text("‹", fontSize = 22.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(10.dp))
            Text(
                text = "设置",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Spacer(Modifier.height(20.dp))

        com.ootd.today.ui.expert.ExpertEntry(settings.expertName, onOpenExpert)
        Spacer(Modifier.height(16.dp))

        SectionCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "每日穿搭推送",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "每天早上提醒今天穿什么",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = settings.dailyReminderEnabled,
                    onCheckedChange = { enabled ->
                        viewModel.setReminderEnabled(enabled)
                        // 开启时若系统未授权（Android 13+），当场请求。
                        if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                )
            }

            if (settings.dailyReminderEnabled) {
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "推送时间",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                TimePickerRow(
                    hour = settings.reminderHour,
                    minute = settings.reminderMinute,
                    onHourPick = { viewModel.setReminderTime(it, settings.reminderMinute) },
                    onMinutePick = { viewModel.setReminderTime(settings.reminderHour, it) }
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "提示：小米/MIUI 需在「应用信息 → 自启动」中允许本应用，否则后台任务可能被系统杀掉导致推送不准时。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // --- 审美专家 ---
        SectionCard { VisionSettingsSection(settings.vision, visionSaving, visionMessage, viewModel::saveVisionSettings, viewModel::clearVisionKey) }
        Spacer(Modifier.height(16.dp))
        SectionCard {
            StylistSettingsSection(
                settings = settings.stylist,
                onVendorSelected = viewModel::setStylistVendor,
                onApiKeyChanged = viewModel::setStylistApiKey,
                onUploadPhotosChanged = viewModel::setStylistUploadPhotos
            )
        }

        Spacer(Modifier.height(16.dp))

        SectionCard {
            Text(
                text = "数据存储",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "衣橱与打卡记录全部保存在本机 SQLite 数据库，平时不联网。" +
                    "本地专家对话不上传。仅当你在首页主动请求云端专家时，才会把单品信息发给所选模型服务商；" +
                    "照片版型识别会在你确认后把所选衣物照片发给视觉服务。基础品类和主色识别在本机进行。卸载应用会清除本机数据。",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(16.dp))

        SectionCard {
            Text(
                text = "通知渠道",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "渠道 ID：${NotificationHelper.CHANNEL_ID}。可在系统通知设置中单独调整该渠道的声音与重要程度。",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(110.dp))
    }
}

@Composable
private fun SectionCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        content()
    }
}

/** 时间选择：小时 + 分钟两组 Chip。 */
@Composable
private fun TimePickerRow(
    hour: Int,
    minute: Int,
    onHourPick: (Int) -> Unit,
    onMinutePick: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HOUR_OPTIONS.forEach { h ->
                TimeChip(
                    label = "${h}点",
                    selected = h == hour,
                    onClick = { onHourPick(h) }
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MINUTE_OPTIONS.forEach { m ->
                TimeChip(
                    label = if (m == 0) "整" else "${m}分",
                    selected = m == minute,
                    onClick = { onMinutePick(m) }
                )
            }
        }
    }
}

@Composable
private fun TimeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(MaterialTheme.shapes.small)
            .background(
                if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
