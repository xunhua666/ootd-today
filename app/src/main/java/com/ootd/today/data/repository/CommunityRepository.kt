package com.ootd.today.data.repository

import com.ootd.today.data.model.Comment
import com.ootd.today.data.model.CommunityUser
import com.ootd.today.data.model.Occasion
import com.ootd.today.data.model.Post
import com.ootd.today.data.model.WardrobeItem
import kotlinx.coroutines.flow.Flow

/**
 * 社区数据仓库 —— 后端无关的唯一契约。
 *
 * P0 由 [RoomCommunityRepository] 提供「本地 + 模拟数据」实现，
 * 社区的所有 UI 与交互都真实可用（发布、点赞、评论、关注、个人主页）。
 *
 * **接真后端时**：新增一个实现类（如 `FirebaseCommunityRepository`），
 * 在 DI 里改一行绑定即可，ViewModel 与 UI 层零改动。
 * 为此接口刻意设计成不含任何本地存储细节（没有 DAO、没有 Room 类型）。
 */
interface CommunityRepository {

    /** 当前用户。用于发布动态时署名、判断「是否是我发的」。 */
    suspend fun currentUser(): CommunityUser

    /** 更新当前用户资料（昵称、头像）。 */
    suspend fun updateCurrentUser(nickname: String, avatarEmoji: String)

    /** 社区动态流，按时间倒序。 */
    fun observeFeed(): Flow<List<Post>>

    /** 某个用户发布的动态。 */
    fun observePostsByUser(userId: Long): Flow<List<Post>>

    /** 单条动态，用于详情页。 */
    fun observePost(postId: Long): Flow<Post?>

    /**
     * 发布一条动态。
     *
     * @param items 搭配里的单品。内部会转成快照存储，
     *   因此单品日后被删除也不影响这条动态的展示。
     * @param photoPath 配图的本地路径，null 表示纯穿搭动态。
     * @param caption 文案，内部会自动解析并冗余存储其中的 #话题。
     */
    suspend fun createPost(
        items: List<WardrobeItem>,
        caption: String,
        occasion: Occasion,
        photoPath: String? = null
    ): Long

    /** 按话题筛选动态（标签不含 # 符号，匹配忽略大小写）。 */
    fun observePostsByTopic(tag: String): Flow<List<Post>>

    /**
     * 热门话题排行。
     *
     * @param limit 取前几名。
     * @return 标签 -> 出现次数，按次数降序。
     */
    fun observeTrendingTopics(limit: Int = 8): Flow<List<Pair<String, Int>>>

    /** 删除动态（同时清理其评论，避免留下孤儿数据）。 */
    suspend fun deletePost(postId: Long)

    /** 切换点赞状态。 */
    suspend fun toggleLike(postId: Long)

    /** 某条动态的评论，按时间正序。 */
    fun observeComments(postId: Long): Flow<List<Comment>>

    /** 发表评论。 */
    suspend fun addComment(postId: Long, content: String)

    /** 删除评论（仅本人可删，由 UI 层控制入口）。 */
    suspend fun deleteComment(commentId: Long)

    /** 全部社区用户，用于「发现同好」列表。 */
    fun observeUsers(): Flow<List<CommunityUser>>

    /** 关注 / 取关。 */
    suspend fun setFollowing(userId: Long, following: Boolean)
}