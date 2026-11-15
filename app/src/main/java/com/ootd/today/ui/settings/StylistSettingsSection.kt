package com.ootd.today.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.ootd.today.data.prefs.StylistSettings
import com.ootd.today.data.prefs.StylistVendor
import com.ootd.today.domain.stylist.StylistPersona
import com.ootd.today.ui.theme.OotdColors
import com.ootd.today.ui.theme.PillShape

/**
 * 设置页的「审美专家」配置区。
 *
 * 这里必须把**数据外发**讲清楚 —— 这是整个应用里唯一一处把用户数据
 * 送出设备的地方，用户有权在开启前知道具体会传什么。
 */
@Composable
fun StylistSettingsSection(
    settings: StylistSettings,
    onVendorSelected: (StylistVendor) -> Unit,
    onApiKeyChanged: (String) -> Unit,
    onUploadPhotosChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "可选 · 云端审美服务",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "使用自己的模型账号，在首页主动请求云端搭配。下载本地专家无需填写这里。",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(14.dp))

        Text(
            text = "服务商",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StylistVendor.entries.forEach { vendor ->
                val selected = settings.baseUrl == vendor.baseUrl
                Box(
                    modifier = Modifier
                        .clip(PillShape)
                        .background(
                            if (selected) OotdColors.current.brandGradientStart
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .clickable { onVendorSelected(vendor) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = vendor.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        color = if (selected) Color.White
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        ApiKeyField(
            currentKey = settings.apiKey,
            onSave = onApiKeyChanged
        )

        Spacer(Modifier.height(6.dp))
        Text(
            text = "当前模型：${settings.model}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "允许发送单品照片",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "开启后专家能看到衣服的真实样子，建议质量更高",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = settings.uploadPhotos,
                onCheckedChange = onUploadPhotosChanged
            )
        }

        Spacer(Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(12.dp)
        ) {
            val photoPart = if (settings.uploadPhotos) "以及单品照片" else "（不上传照片）"
            Text(
                text = "当你主动请求云端专家时，会把当前衣橱的单品信息" +
                    "（名称、品类、颜色、保暖度、场合）$photoPart" +
                    "发送给你选择的模型服务商。除此之外不会发送任何数据；" +
                    "打卡、关注、点赞等行为都不上传。",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * API Key 输入。
 *
 * 已保存的 Key 不回显明文，只显示尾号 —— 这样用户能确认「填过了」，
 * 又不会在别人看到屏幕时泄露。
 */
@Composable
private fun ApiKeyField(
    currentKey: String,
    onSave: (String) -> Unit
) {
    var input by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf(false) }

    if (!editing && currentKey.isNotBlank()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "已配置 ····${currentKey.takeLast(4)}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "更换",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { editing = true }
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = "清除",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.clickable {
                    onSave("")
                    editing = false
                }
            )
        }
        return
    }

    OutlinedTextField(
        value = input,
        onValueChange = { input = it },
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text("粘贴 API Key", style = MaterialTheme.typography.bodySmall) },
        singleLine = true,
        shape = MaterialTheme.shapes.small,
        visualTransformation = PasswordVisualTransformation(),
        trailingIcon = {
            Text(
                text = "保存",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(end = 12.dp)
                    .clickable(enabled = input.isNotBlank()) {
                        onSave(input)
                        input = ""
                        editing = false
                    }
            )
        }
    )
}
