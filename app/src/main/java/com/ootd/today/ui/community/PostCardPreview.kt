package com.ootd.today.ui.community

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ootd.today.data.model.ClothingCategory
import com.ootd.today.data.model.CommunityUser
import com.ootd.today.data.model.ItemStatus
import com.ootd.today.data.model.Occasion
import com.ootd.today.data.model.Post
import com.ootd.today.data.model.Season
import com.ootd.today.data.model.StyleTag
import com.ootd.today.data.model.WardrobeItem
import com.ootd.today.ui.theme.OotdTodayTheme

/**
 * 动态卡片的视觉验证。
 *
 * 覆盖两种形态：
 * 1. **纯穿搭动态**（无配图）—— 色卡横排应成为视觉主体
 * 2. **带配图动态** —— 配图在上、色卡在下，层级应清晰不抢戏
 *
 * 同时验证含 @提及 与 #话题 的长文案不会被截断到看不清。
 */
@Preview(name = "动态卡片 · 两种形态", showBackground = true, widthDp = 380)
@Composable
private fun PostCardStatesPreview() {
    OotdTodayTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.padding(vertical = 12.dp)) {
                PostCard(
                    post = samplePost(
                        caption = "今天的通勤三件套，无脑但可靠 ☕️ #通勤穿搭",
                        photoPath = null,
                        items = listOf(
                            item(1, "米白针织衫", ClothingCategory.TOP, 0xFFEDE6D8),
                            item(2, "直筒西装裤", ClothingCategory.BOTTOM, 0xFF2B2B2B),
                            item(3, "乐福鞋", ClothingCategory.SHOES, 0xFF4A3226)
                        )
                    ),
                    onLikeClick = {},
                    onCommentClick = {},
                    onClick = {},
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                )
                PostCard(
                    post = samplePost(
                        caption = "这套是 @Kai 帮我挑的，颜色真的绝 #街头 #ootd",
                        photoPath = null,
                        items = listOf(
                            item(4, "oversize 卫衣", ClothingCategory.TOP, 0xFF9E9E9E),
                            item(5, "工装裤", ClothingCategory.BOTTOM, 0xFF3B3B3B),
                            item(6, "厚底运动鞋", ClothingCategory.SHOES, 0xFF1A1A1A),
                            item(7, "棒球帽", ClothingCategory.ACCESSORY, 0xFF00D9A5)
                        )
                    ),
                    onLikeClick = {},
                    onCommentClick = {},
                    onClick = {},
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .padding(top = 12.dp)
                )
            }
        }
    }
}

private fun samplePost(
    caption: String,
    photoPath: String?,
    items: List<WardrobeItem>
) = Post(
    id = 1,
    author = CommunityUser(
        id = 2,
        nickname = "阿茶",
        avatarEmoji = "🧋",
        bio = "极简主义",
        isMe = false,
        isFollowing = true
    ),
    items = items,
    caption = caption,
    occasion = Occasion.COMMUTE,
    photoPath = photoPath,
    topics = emptyList(),
    likeCount = 128,
    isLiked = false,
    commentCount = 3,
    createdAt = System.currentTimeMillis() - 2 * 60 * 60 * 1000
)

private fun item(
    id: Long,
    name: String,
    category: ClothingCategory,
    color: Long
) = WardrobeItem(
    id = id,
    name = name,
    category = category,
    subCategory = "",
    imagePath = null,
    colorArgb = color,
    colorName = "",
    warmthLevel = 2,
    seasons = Season.entries.toSet(),
    occasions = Occasion.entries.toSet(),
    style = StyleTag.MINIMAL,
    brand = null,
    price = null,
    purchaseDate = null,
    lastWornDate = null,
    status = ItemStatus.ACTIVE,
    note = null,
    wearCount = 0
)