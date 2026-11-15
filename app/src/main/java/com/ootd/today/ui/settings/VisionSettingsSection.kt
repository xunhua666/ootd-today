package com.ootd.today.ui.settings

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.ootd.today.data.prefs.VisionSettings

@Composable
fun VisionSettingsSection(settings: VisionSettings, saving: Boolean, message: String?, onSave: (String, String, String) -> Unit, onClearKey: () -> Unit) {
    var address by rememberSaveable(settings.baseUrl) { mutableStateOf(settings.baseUrl) }
    var model by rememberSaveable(settings.model) { mutableStateOf(settings.model) }
    var key by remember { mutableStateOf("") } // Never put an unencrypted key in saved instance state.
    LaunchedEffect(settings.apiKey) { if (settings.apiKey.isNotBlank() && key.trim() == settings.apiKey) key = "" }
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("照片版型识别 · 云端视觉", style = MaterialTheme.typography.titleMedium)
        Text("推荐 Qwen3-VL-Plus。识别后确认版型，搭配继续由手机本地完成。每次识别会调用你配置的服务，可能产生服务商费用。", style = MaterialTheme.typography.bodySmall)
        Text(if (settings.isReady) "已配置 · 在录入／编辑单品中使用" else "尚未配置视觉服务", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
        OutlinedTextField(address, { address = it }, label = { Text("视觉服务接口地址") }, placeholder = { Text("从服务商控制台复制 Base URL") }, modifier = Modifier.fillMaxWidth(), singleLine = true, enabled = !saving)
        Text("请使用与你的 API Key 同地域的接口地址。百炼新业务空间的地址含业务空间 ID，请从控制台复制完整地址。", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(model, { model = it }, label = { Text("视觉模型名称") }, modifier = Modifier.fillMaxWidth(), singleLine = true, enabled = !saving)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SuggestionChip(onClick = { model = "qwen3-vl-plus" }, label = { Text("Plus · 优先质量") })
            SuggestionChip(onClick = { model = "qwen3-vl-flash" }, label = { Text("Flash · 轻量调用") })
        }
        OutlinedTextField(key, { key = it }, label = { Text("视觉服务 API Key") }, placeholder = { Text(if (settings.apiKey.isNotBlank()) "已加密保存，留空沿用" else "填写服务商提供的 API Key") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), singleLine = true, enabled = !saving)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { onSave(address, model, key.ifBlank { settings.apiKey }) }, enabled = !saving) { Text(if (saving) "保存中…" else "保存视觉配置") }
            if (settings.apiKey.isNotBlank()) TextButton(onClick = { key = ""; onClearKey() }, enabled = !saving) { Text("清除 Key") }
        }
        message?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary) }
        TextButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, "https://help.aliyun.com/zh/model-studio/vision".toUri())) }) { Text("查看官方配置说明") }
        Text("只在你点击确认后上传当前所选衣物照片；结果不会自动替换原标签，需你确认。API Key 在手机中加密保存。", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
