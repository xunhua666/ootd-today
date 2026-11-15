package com.ootd.today.ui.today

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ootd.today.domain.stylist.StylistPersona
import com.ootd.today.ui.theme.OotdColors
import com.ootd.today.ui.theme.PillShape

/** 追问快选。都是日常最可能提出的几种，避免用户从空白输入框开始想。 */
private val QUICK_PROMPTS = listOf(
    "太素了", "换条裤子", "正式点", "休闲一点", "太花了"
)

/**
 * 「沈砚」入口条。
 *
 * 两种状态分开设计：
 * - 还没请过专家 → 一个明确的按钮「请沈砚看看」；
 * - 已经是专家的方案 → 直接展开追问输入与快选。
 *
 * 这比"永远显示一个输入框"好：第一次看到的用户不会对着输入框发呆，
 * 已经用过的人则少点一次。
 */
@Composable
fun StylistBar(
    ready: Boolean,
    consulting: Boolean,
    fromStylist: Boolean,
    onAsk: () -> Unit,
    onFollowUp: (String) -> Unit,
    modifier: Modifier = Modifier,
    name: String = "审美专家"
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surface)
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StylistAvatar(active = fromStylist)
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = when {
                        !ready -> "还没配置 · 去「我的 → 设置」填入 Key"
                        consulting -> "正在看你的衣橱…"
                        fromStylist -> "对这套有意见？直接说"
                        else -> "让编辑帮你挑一套"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (consulting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        if (!ready) return@Column

        if (!fromStylist) {
            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(PillShape)
                    .background(OotdColors.current.brandGradientStart)
                    .clickable(enabled = !consulting, onClick = onAsk)
                    .padding(vertical = 11.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (consulting) "稍等…" else "请${name}看看（云端）",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White
                )
            }
            return@Column
        }

        // 已经是专家方案 → 展开追问。
        Spacer(Modifier.height(10.dp))
        var text by remember { mutableStateOf("") }

        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text("想怎么改？比如「换条裤子」", style = MaterialTheme.typography.bodySmall)
            },
            singleLine = true,
            shape = MaterialTheme.shapes.small,
            enabled = !consulting,
            trailingIcon = {
                Text(
                    text = "发送",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (text.isBlank() || consulting) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .clickable(enabled = text.isNotBlank() && !consulting) {
                            onFollowUp(text)
                            text = ""
                        }
                )
            }
        )

        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            QUICK_PROMPTS.forEach { prompt ->
                Box(
                    modifier = Modifier
                        .clip(PillShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable(enabled = !consulting) { onFollowUp(prompt) }
                        .padding(horizontal = 11.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = prompt,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * 专家头像。
 *
 * 用字母「沈」而不是 emoji：emoji 在这个应用里已经大量用于品类和场合，
 * 再拿来做人物头像会混淆语义。中文字面反而更像一个署名。
 */
@Composable
fun StylistAvatar(active: Boolean, modifier: Modifier = Modifier) {
    val background = if (active) OotdColors.current.brandGradientStart
    else MaterialTheme.colorScheme.surfaceVariant

    Box(
        modifier = modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(background)
            .then(
                if (active) Modifier.border(2.dp, OotdColors.current.pink, CircleShape)
                else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        com.ootd.today.ui.expert.ExpertMascot(Modifier.size(36.dp))
    }
}
