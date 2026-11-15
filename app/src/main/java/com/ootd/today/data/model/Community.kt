package com.ootd.today.data.model

/**
 * 社区用户。
 *
 * @property id 用户 id。[ME_ID] 保留给「我自己」。
 * @property isMe 是否为当前用户。用于决定是否显示「删除」等自有操作。
 * @property isFollowing 是否已关注。P0 阶段是本地状态；
 *   接真后端后由服务端返回。
 */
data class CommunityUser(
    val id: Long,
    val nickname: String,
    val avatarEmoji: String,
    val bio: String,
    val isMe: Boolean = false,
    val isFollowing: Boolean = false
) {
    companion object {
        /** 当前用户的保留 id。 */
        const val ME_ID = 1L
    }
}

/**
 * 一条社区动态。
 *
 * @property items 动态里的穿搭单品。复用 [WardrobeItem]，展示所需的
 *   名称与主色都已包含，无需为社区单独造模型。
 * @property photoPath 用户单独上传的配图（本地私有目录路径）。
 *   与 [items] 是两回事：items 是结构化穿搭，photoPath 是氛围图/自拍。
 * @property topics 从 [caption] 解析出的话题标签（不含 # 符号，已去重）。
 *   冗余存储是为了让「按话题筛选」能直接走 SQL，而不必每次全表解析文本。
 * @property likeCount 点赞总数（含模拟的基础热度 + 本地点赞）。
 * @property isLiked 当前用户是否点过赞。
 * @property commentCount 评论数，由数据库实时统计，不做冗余存储以避免漂移。
 */
data class Post(
    val id: Long,
    val author: CommunityUser,
    val items: List<WardrobeItem>,
    val caption: String,
    val occasion: Occasion,
    val photoPath: String?,
    val topics: List<String>,
    val likeCount: Int,
    val isLiked: Boolean,
    val commentCount: Int,
    val createdAt: Long
) {
    /** 是否是我发布的动态。 */
    val isMine: Boolean get() = author.isMe

    /** 是否包含配图。 */
    val hasPhoto: Boolean get() = !photoPath.isNullOrBlank()
}

/**
 * 一条评论。
 */
data class Comment(
    val id: Long,
    val postId: Long,
    val author: CommunityUser,
    val content: String,
    val createdAt: Long
)