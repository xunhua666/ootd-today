package com.ootd.today.data.local

import com.ootd.today.data.model.ClothingCategory
import com.ootd.today.data.model.CommunityUser
import com.ootd.today.data.model.ItemStatus
import com.ootd.today.data.model.Occasion
import com.ootd.today.data.model.Season
import com.ootd.today.data.model.StyleTag
import com.ootd.today.data.model.WardrobeItem
import java.util.concurrent.TimeUnit

/** 模拟社区用户。id 从 2 开始，[CommunityUser.ME_ID]（1）保留给「我自己」。 */
internal fun seedCommunityUsers(): List<CommunityUserEntity> = listOf(
    CommunityUserEntity(
        id = CommunityUser.ME_ID,
        nickname = "我",
        avatarEmoji = "🙋",
        bio = "记录每一天的穿搭",
        isMe = true,
        isFollowing = false
    ),
    CommunityUserEntity(
        id = 2, nickname = "阿茶", avatarEmoji = "🧋",
        bio = "极简主义 · 三件单品出门", isMe = false, isFollowing = true
    ),
    CommunityUserEntity(
        id = 3, nickname = "小满", avatarEmoji = "🌾",
        bio = "复古控，爱淘二手", isMe = false, isFollowing = false
    ),
    CommunityUserEntity(
        id = 4, nickname = "Kai", avatarEmoji = "🛹",
        bio = "街头 / 机能，色不过三", isMe = false, isFollowing = true
    ),
    CommunityUserEntity(
        id = 5, nickname = "柚子", avatarEmoji = "🍊",
        bio = "甜酷女孩，偏爱撞色", isMe = false, isFollowing = false
    ),
    CommunityUserEntity(
        id = 6, nickname = "林深", avatarEmoji = "🪵",
        bio = "通勤党，衬衫收藏家", isMe = false, isFollowing = false
    )
)

/**
 * 模拟社区动态。
 *
 * 时间戳相对「当前时间」往回推导，因此无论何时安装，
 * 动态流看起来都是新鲜的（而不是一堆很久以前的时间）。
 */
internal fun seedPosts(now: Long = System.currentTimeMillis()): List<PostEntity> {
    val hour = TimeUnit.HOURS.toMillis(1)
    return listOf(
        PostEntity(
            authorId = 2, occasion = Occasion.COMMUTE,
            caption = "今天的通勤三件套，无脑但可靠 ☕️ #通勤穿搭",
            itemsSnapshot = OutfitSnapshotCodec.encode(
                listOf(
                    fakeItem(101, "米白针织衫", ClothingCategory.TOP, 0xFFEDE6D8),
                    fakeItem(102, "直筒西装裤", ClothingCategory.BOTTOM, 0xFF2B2B2B),
                    fakeItem(103, "乐福鞋", ClothingCategory.SHOES, 0xFF4A3226)
                )
            ),
            likeCount = 128, isLiked = false, createdAt = now - 2 * hour,
            topics = "通勤穿搭"
        ),
        PostEntity(
            authorId = 4, occasion = Occasion.VACATION,
            caption = "周末街头，黑灰之外加了点荧光 #街头 #ootd",
            itemsSnapshot = OutfitSnapshotCodec.encode(
                listOf(
                    fakeItem(201, "oversize 卫衣", ClothingCategory.TOP, 0xFF9E9E9E),
                    fakeItem(202, "工装裤", ClothingCategory.BOTTOM, 0xFF3B3B3B),
                    fakeItem(203, "厚底运动鞋", ClothingCategory.SHOES, 0xFF1A1A1A),
                    fakeItem(204, "棒球帽", ClothingCategory.ACCESSORY, 0xFF00D9A5)
                )
            ),
            likeCount = 342, isLiked = true, createdAt = now - 5 * hour,
            topics = "街头,ootd"
        ),
        PostEntity(
            authorId = 5, occasion = Occasion.DATE,
            caption = "玫粉 + 湖蓝，今天想被看见 💗 #约会穿搭 #撞色",
            itemsSnapshot = OutfitSnapshotCodec.encode(
                listOf(
                    fakeItem(301, "短款针织开衫", ClothingCategory.TOP, 0xFFFF3D8B),
                    fakeItem(302, "高腰半裙", ClothingCategory.BOTTOM, 0xFF2B5CE6),
                    fakeItem(303, "玛丽珍鞋", ClothingCategory.SHOES, 0xFF1A1A1A)
                )
            ),
            likeCount = 256, isLiked = false, createdAt = now - 9 * hour,
            topics = "约会穿搭,撞色"
        ),
        PostEntity(
            authorId = 3, occasion = Occasion.VACATION,
            caption = "二手市集淘的复古皮衣，越穿越有味道 #复古 #二手",
            itemsSnapshot = OutfitSnapshotCodec.encode(
                listOf(
                    fakeItem(401, "白T", ClothingCategory.TOP, 0xFFF5F5F5),
                    fakeItem(402, "水洗牛仔裤", ClothingCategory.BOTTOM, 0xFF6B8CAE),
                    fakeItem(403, "皮质机车夹克", ClothingCategory.OUTERWEAR, 0xFF4A3226),
                    fakeItem(404, "切尔西靴", ClothingCategory.SHOES, 0xFF3B2B23)
                )
            ),
            likeCount = 187, isLiked = false, createdAt = now - 26 * hour,
            topics = "复古,二手"
        ),
        PostEntity(
            authorId = 6, occasion = Occasion.FORMAL,
            caption = "见客户的一天，衬衫 + 西裤不会出错 @林深 #通勤穿搭",
            itemsSnapshot = OutfitSnapshotCodec.encode(
                listOf(
                    fakeItem(501, "浅蓝条纹衬衫", ClothingCategory.TOP, 0xFFCFE0EE),
                    fakeItem(502, "垂坠西裤", ClothingCategory.BOTTOM, 0xFF33384A),
                    fakeItem(503, "皮鞋", ClothingCategory.SHOES, 0xFF2A1F18),
                    fakeItem(504, "皮质公文包", ClothingCategory.BAG, 0xFF5C3A22)
                )
            ),
            likeCount = 96, isLiked = false, createdAt = now - 30 * hour,
            topics = "通勤穿搭"
        ),
        PostEntity(
            authorId = 2, occasion = Occasion.HOME,
            caption = "宅家也要舒服又好看，亚麻套装 yyds #居家 #舒适",
            itemsSnapshot = OutfitSnapshotCodec.encode(
                listOf(
                    fakeItem(601, "亚麻衬衫", ClothingCategory.TOP, 0xFFD9C7A7),
                    fakeItem(602, "亚麻阔腿裤", ClothingCategory.BOTTOM, 0xFFC4B08C)
                )
            ),
            likeCount = 74, isLiked = false, createdAt = now - 50 * hour,
            topics = "居家,舒适"
        )
    )
}

/** 模拟动态里的评论。postId 与 [seedPosts] 的插入顺序（1 起）对应。 */
internal fun seedComments(now: Long = System.currentTimeMillis()): List<CommentEntity> {
    val hour = TimeUnit.HOURS.toMillis(1)
    return listOf(
        CommentEntity(postId = 2, authorId = 5, content = "荧光绿的帽子好绝！", createdAt = now - 4 * hour),
        CommentEntity(postId = 2, authorId = 2, content = "这个色不过三我做不到 😂", createdAt = now - 3 * hour),
        CommentEntity(postId = 1, authorId = 6, content = "求针织衫链接", createdAt = now - 1 * hour),
        CommentEntity(postId = 3, authorId = 4, content = "撞色很大胆，好看", createdAt = now - 8 * hour)
    )
}

/**
 * 构造一条仅用于社区展示的「假单品」。
 *
 * 这些单品不属于我的衣橱（id 从 100 起，与真实单品不冲突），
 * 只是为了让动态里的穿搭有名称和主色可渲染。
 */
private fun fakeItem(
    id: Long,
    name: String,
    category: ClothingCategory,
    color: Long
): WardrobeItem = WardrobeItem(
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