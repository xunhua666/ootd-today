package com.ootd.today.ui.community

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.ootd.today.data.model.Post
import com.ootd.today.ui.common.ItemSwatch
import com.ootd.today.ui.common.SocialText
import com.ootd.today.ui.theme.OotdColors
import com.ootd.today.ui.theme.PillShape
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * 动态卡片 —— 社区的核心展示单元。
 *
 * 布局刻意做成「作者信息 → 穿搭色卡横排 → 文案 → 互动栏」，
 * 让穿搭成为视觉主体：单品色卡横排展示，一眼能看清整套的配色关系。
 * 复用首页/衣橱同一套 [ItemSwatch]，保证色彩语言在全 App 一致。
 */
@Composable
fun PostCard(
    post: Post,
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onDeleteClick: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        // --- 作者行 ---
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(text = post.author.avatarEmoji, fontSize = 19.sp)
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = post.author.nickname,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${relativeTime(post.createdAt)} · ${post.occasion.emoji} ${post.occasion.label}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            // 仅自己的动态显示删除入口。
            if (onDeleteClick != null) {
                Box(
                    modifier = Modifier
                        .clip(PillShape)
                        .clickable(onClick = onDeleteClick)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "删除",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        // --- 配图（可选）---
        // 放在穿搭色卡之前：氛围图是视觉重心，先看到图再看穿搭明细，
        // 符合浏览社区的心理顺序。
        if (post.hasPhoto) {
            Spacer(Modifier.height(12.dp))
            AsyncImage(
                model = post.photoPath,
                contentDescription = "动态配图",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(14.dp)),
                contentScale = ContentScale.Crop
            )
        }

        // --- 穿搭色卡横排：视觉主体 ---
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            post.items.forEach { item ->
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ItemSwatch(
                        item = item,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(76.dp),
                        emojiSize = 30.sp,
                        shape = RoundedCornerShape(14.dp)
                    )
                }
            }
        }
        // 单品名称行，让色卡有明确语义。
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            post.items.forEach { item ->
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // --- 文案：@提及 与 #话题 高亮 ---
        if (post.caption.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            SocialText(
                text = post.caption,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(Modifier.height(12.dp))

        // --- 互动栏 ---
        Row(verticalAlignment = Alignment.CenterVertically) {
            InteractionButton(
                emoji = if (post.isLiked) "❤️" else "🤍",
                label = post.likeCount.toString(),
                active = post.isLiked,
                onClick = onLikeClick
            )
            Spacer(Modifier.width(16.dp))
            InteractionButton(
                emoji = "💬",
                label = post.commentCount.toString(),
                active = false,
                onClick = onCommentClick
            )
        }
    }
}

@Composable
private fun InteractionButton(
    emoji: String,
    label: String,
    active: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(PillShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = emoji, fontSize = 15.sp)
        Spacer(Modifier.width(5.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
            color = if (active) OotdColors.current.pink else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 相对时间：刚刚 / N分钟前 / N小时前 / N天前 / 具体日期。
 *
 * 社区类产品用相对时间更符合直觉；超过一周则退回绝对日期，
 * 因为「10天前」这种表述已经失去参考价值。
 */
internal fun relativeTime(millis: Long): String {
    val diff = System.currentTimeMillis() - millis
    val minute = TimeUnit.MINUTES.toMillis(1)
    val hour = TimeUnit.HOURS.toMillis(1)
    val day = TimeUnit.DAYS.toMillis(1)
    return when {
        diff < minute -> "刚刚"
        diff < hour -> "${diff / minute} 分钟前"
        diff < day -> "${diff / hour} 小时前"
        diff < 7 * day -> "${diff / day} 天前"
        else -> {
            val c = Calendar.getInstance().apply { timeInMillis = millis }
            "${c.get(Calendar.MONTH) + 1}月${c.get(Calendar.DAY_OF_MONTH)}日"
        }
    }
}