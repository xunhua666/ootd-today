package com.ootd.today.ui.community

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ootd.today.data.model.CommunityUser
import com.ootd.today.ui.common.EmptyPlaceholder
import com.ootd.today.ui.theme.OotdColors
import com.ootd.today.ui.theme.PillShape

/**
 * 社区动态流页。
 *
 * 顶部：筛选（全部 / 关注 / 我的）+ 「发现同好」横向列表
 * 主体：动态卡片列表
 */
@Composable
fun CommunityScreen(
    viewModel: CommunityViewModel,
    onPostClick: (Long) -> Unit,
    onPublishClick: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {
        // --- 顶栏 ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
                text = "穿搭社区",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )
            // 发布入口
            Box(
                modifier = Modifier
                    .clip(PillShape)
                    .background(OotdColors.current.brandGradientStart)
                    .clickable(onClick = onPublishClick)
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            ) {
                Text(
                    text = "＋ 发布",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        // --- 筛选 ---
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FeedFilter.entries.forEach { option ->
                FilterChip(
                    selected = state.filter == option,
                    onClick = { viewModel.selectFilter(option) },
                    label = { Text(option.label, style = MaterialTheme.typography.labelMedium) },
                    shape = PillShape,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // --- 发现同好 ---
        if (state.users.isNotEmpty()) {
            Text(
                text = "发现同好",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(Modifier.height(8.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.users, key = { it.id }) { user ->
                    UserChip(
                        user = user,
                        onToggleFollow = { viewModel.toggleFollow(user.id, !user.isFollowing) }
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
        }

        // --- 热门话题 ---
        // 只在「全部」视图下展示：筛选「我的/关注」时，话题条会干扰对当前视图的认知。
        if (state.trendingTopics.isNotEmpty() && state.filter == FeedFilter.ALL) {
            Text(
                text = if (state.activeTopic == null) "热门话题" else "正在看 #${state.activeTopic}（再点一下取消）",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(Modifier.height(8.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.trendingTopics.size) { index ->
                    val (tag, count) = state.trendingTopics[index]
                    val active = state.activeTopic.equals(tag, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .clip(PillShape)
                            .background(
                                if (active) OotdColors.current.brandGradientStart
                                else MaterialTheme.colorScheme.surface
                            )
                            .clickable { viewModel.selectTopic(tag) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "#$tag · $count",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                            color = if (active) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
        }

        // --- 动态流 ---
        if (state.posts.isEmpty()) {
            EmptyPlaceholder(
                emoji = when {
                    state.activeTopic != null -> "🏷"
                    state.filter == FeedFilter.MINE -> "✍️"
                    state.filter == FeedFilter.FOLLOWING -> "🔍"
                    else -> "🌐"
                },
                message = when {
                    state.activeTopic != null -> "#${state.activeTopic} 下还没有动态"
                    state.filter == FeedFilter.MINE -> "还没发布过动态\n点右上角分享你的今日穿搭"
                    state.filter == FeedFilter.FOLLOWING -> "关注的人还没发动态"
                    else -> "社区还没有内容"
                }
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.posts, key = { it.id }) { post ->
                    PostCard(
                        post = post,
                        onLikeClick = { viewModel.toggleLike(post.id) },
                        onCommentClick = { onPostClick(post.id) },
                        onClick = { onPostClick(post.id) },
                        onDeleteClick = if (post.isMine) {
                            { viewModel.deletePost(post.id) }
                        } else {
                            null
                        }
                    )
                }
            }
        }
    }
}

/** 「发现同好」里的用户条目：头像 + 昵称 + 关注按钮。 */
@Composable
private fun UserChip(
    user: CommunityUser,
    onToggleFollow: () -> Unit
) {
    Column(
        modifier = Modifier.width(72.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(text = user.avatarEmoji, fontSize = 24.sp)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = user.nickname,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .clip(PillShape)
                .background(
                    if (user.isFollowing) MaterialTheme.colorScheme.surfaceVariant
                    else OotdColors.current.pink
                )
                .clickable(onClick = onToggleFollow)
                .padding(horizontal = 10.dp, vertical = 3.dp)
        ) {
            Text(
                text = if (user.isFollowing) "已关注" else "关注",
                style = MaterialTheme.typography.labelSmall,
                color = if (user.isFollowing) MaterialTheme.colorScheme.onSurfaceVariant else Color.White
            )
        }
    }
}