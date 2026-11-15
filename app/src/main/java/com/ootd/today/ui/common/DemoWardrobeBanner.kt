package com.ootd.today.ui.common

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun DemoWardrobeBanner(onStart: () -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.primaryContainer) {
        Column(Modifier.padding(18.dp)) {
            Text("体验衣橱 · 示例内容", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text("这些衣服仅供体验，不计入个人统计。录入自己的衣服，开始建立专属衣橱。", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp, bottom = 10.dp))
            Button(onClick = onStart, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("开始建立我的衣橱") }
        }
    }
}
