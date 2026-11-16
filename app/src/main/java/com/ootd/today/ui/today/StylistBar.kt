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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ootd.today.R
import com.ootd.today.ui.theme.OotdColors
import com.ootd.today.ui.theme.PillShape

/** 追问快选。都是日常最可能提出的几种，避免用户从空白输入框开始想。 */
private val QUICK_PROMPT_RES = listOf(
    R.string.stylist_quick_too_plain,
    R.string.stylist_quick_change_bottom,
    R.string.stylist_quick_formal,
    R.string.stylist_quick_casual,
    R.string.stylist_quick_too_busy
)

/**
 * 专家入口条。
 *
 * 两种状态分开设计：
 * - 还没请过专家 → 一个明确的按钮「请专家看看」；
 * - 已经是专家的方案 → 直接展开追问输入与快选。
 *
 * 这比"永远显示一个输入框"好：第一次看到的用户不会对着输入框发呆，
 * 已经用过的人则少点一次。
 *
 * @param name 用户自定义的专家名；为空时回退到本地化的默认称呼。
 */
@Composable
fun StylistBar(
    ready: Boolean,
    consulting: Boolean,
    fromStylist: Boolean,
    onAsk: () -> Unit,
    onFollowUp: (String) -> Unit,
    modifier: Modifier = Modifier,
    name: String = ""
) {
    val displayName = name.ifBlank { stringResource(R.string.today_expert_default_name) }

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
                    text = displayName,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = when {
                        !ready -> stringResource(R.string.stylist_status_not_ready)
                        consulting -> stringResource(R.string.stylist_status_consulting)
                        fromStylist -> stringResource(R.string.stylist_status_followup)
                        else -> stringResource(R.string.stylist_status_idle)
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
                    text = if (consulting) stringResource(R.string.stylist_asking)
                    else stringResource(R.string.stylist_ask, displayName),
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
                Text(stringResource(R.string.stylist_followup_placeholder), style = MaterialTheme.typography.bodySmall)
            },
            singleLine = true,
            shape = MaterialTheme.shapes.small,
            enabled = !consulting,
            trailingIcon = {
                Text(
                    text = stringResource(R.string.stylist_send),
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
            QUICK_PROMPT_RES.forEach { promptRes ->
                val prompt = stringResource(promptRes)
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
 * 用插画头像而不是 emoji：emoji 在这个应用里已经大量用于品类和场合，
 * 再拿来做人物头像会混淆语义。
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