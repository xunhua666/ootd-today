package com.ootd.today.data.repository

import com.ootd.today.data.local.CommentDao
import com.ootd.today.data.local.CommentEntity
import com.ootd.today.data.local.CommunityUserDao
import com.ootd.today.data.local.OutfitSnapshotCodec
import com.ootd.today.data.local.PostDao
import com.ootd.today.data.local.PostEntity
import com.ootd.today.data.local.toModel
import com.ootd.today.data.model.Comment
import com.ootd.today.data.model.CommunityUser
import com.ootd.today.data.model.Occasion
import com.ootd.today.data.model.Post
import com.ootd.today.data.model.WardrobeItem
import com.ootd.today.domain.SocialTextParser
import com.ootd.today.domain.TopicAggregator
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/**
 * 社区仓库的本地实现（P0）。
 *
 * 数据存 Room，并内置一批模拟用户与动态，让社区一打开就有内容。
 * 所有交互（发布/点赞/评论/关注）都写入数据库并即时反映到 UI，
 * 因此体验与真实后端一致，差别仅在于「没有别人能看见」。
 *
 * 实现要点：动态列表需要同时关联作者与评论数，用 [combine] 把
 * 三条流（动态 / 用户 / 评论计数）合并，避免在 UI 层做 N+1 查询。
 */
@Singleton
class RoomCommunityRepository @Inject constructor(
    private val userDao: CommunityUserDao,
    private val postDao: PostDao,
    private val commentDao: CommentDao
) : CommunityRepository {

    override suspend fun currentUser(): CommunityUser =
        userDao.getById(CommunityUser.ME_ID)?.toModel()
        // 兜底：极端情况下（用户表被清空）也能给出一个可用的「我」。
            ?: CommunityUser(
                id = CommunityUser.ME_ID,
                nickname = "我",
                avatarEmoji = "🙋",
                bio = "",
                isMe = true
            )

    override suspend fun updateCurrentUser(nickname: String, avatarEmoji: String) {
        userDao.updateProfile(CommunityUser.ME_ID, nickname, avatarEmoji)
    }

    override fun observeFeed(): Flow<List<Post>> = combine(
        postDao.observeFeed(),
        userDao.observeAll(),
        commentDao.observeCounts()
    ) { posts, users, counts ->
        val userMap = users.associateBy({ it.id }, { it.toModel() })
        val countMap = counts.associate { it.postId to it.count }
        posts.mapNotNull { entity ->
            // 作者缺失（如用户被删）时跳过该条，而不是抛异常或显示空白作者。
            val author = userMap[entity.authorId] ?: return@mapNotNull null
            entity.toModel(author = author, commentCount = countMap[entity.id] ?: 0)
        }
    }

    override fun observePostsByUser(userId: Long): Flow<List<Post>> = combine(
        postDao.observeByAuthor(userId),
        userDao.observeAll(),
        commentDao.observeCounts()
    ) { posts, users, counts ->
        val userMap = users.associateBy({ it.id }, { it.toModel() })
        val countMap = counts.associate { it.postId to it.count }
        posts.mapNotNull { entity ->
            val author = userMap[entity.authorId] ?: return@mapNotNull null
            entity.toModel(author = author, commentCount = countMap[entity.id] ?: 0)
        }
    }

    override fun observePost(postId: Long): Flow<Post?> = combine(
        postDao.observeById(postId),
        userDao.observeAll(),
        commentDao.observeCounts()
    ) { entity, users, counts ->
        val post = entity ?: return@combine null
        val author = users.firstOrNull { it.id == post.authorId }?.toModel()
            ?: return@combine null
        post.toModel(author = author, commentCount = counts.firstOrNull { it.postId == postId }?.count ?: 0)
    }

    override suspend fun createPost(
        items: List<WardrobeItem>,
        caption: String,
        occasion: Occasion,
        photoPath: String?
    ): Long {
        val me = currentUser()
        val trimmedCaption = caption.trim()
        return postDao.insert(
            PostEntity(
                authorId = me.id,
                caption = trimmedCaption,
                itemsSnapshot = OutfitSnapshotCodec.encode(items),
                occasion = occasion,
                // 新发布的动态从 0 赞起步；模拟数据的初始热度在播种时写入。
                likeCount = 0,
                isLiked = false,
                createdAt = System.currentTimeMillis(),
                photoPath = photoPath,
                // 话题在此处解析并冗余存储，让后续「按话题筛选」能走 SQL，
                // 而不必每次把全表读出来再逐条解析文本。
                topics = SocialTextParser.extractTopics(trimmedCaption).joinToString(",")
            )
        )
    }

    override fun observePostsByTopic(tag: String): Flow<List<Post>> = combine(
        postDao.observeByTopic(tag),
        userDao.observeAll(),
        commentDao.observeCounts()
    ) { posts, users, counts ->
        val userMap = users.associateBy({ it.id }, { it.toModel() })
        val countMap = counts.associate { it.postId to it.count }
        posts.mapNotNull { entity ->
            val author = userMap[entity.authorId] ?: return@mapNotNull null
            entity.toModel(author = author, commentCount = countMap[entity.id] ?: 0)
        }
    }

    override fun observeTrendingTopics(limit: Int): Flow<List<Pair<String, Int>>> =
        postDao.observeAllTopicStrings().map { topicStrings ->
            // 聚合规则（忽略大小写、保留首次写法、稳定排序）在 TopicAggregator 里，
            // 那边有独立单测覆盖。
            TopicAggregator.rank(topicStrings, limit)
        }

    override suspend fun deletePost(postId: Long) {
        // 先删评论再删动态，避免留下孤儿评论。
        commentDao.deleteByPost(postId)
        postDao.deleteById(postId)
    }

    override suspend fun toggleLike(postId: Long) {
        val post = postDao.getById(postId) ?: return
        val nextLiked = !post.isLiked
        postDao.setLiked(
            id = postId,
            liked = nextLiked,
            delta = if (nextLiked) 1 else -1
        )
    }

    override fun observeComments(postId: Long): Flow<List<Comment>> = combine(
        commentDao.observeByPost(postId),
        userDao.observeAll()
    ) { comments, users ->
        val userMap = users.associateBy({ it.id }, { it.toModel() })
        comments.mapNotNull { entity ->
            val author = userMap[entity.authorId] ?: return@mapNotNull null
            entity.toModel(author)
        }
    }

    override suspend fun addComment(postId: Long, content: String) {
        val trimmed = content.trim()
        if (trimmed.isEmpty()) return
        val me = currentUser()
        commentDao.insert(
            CommentEntity(
                postId = postId,
                authorId = me.id,
                content = trimmed,
                createdAt = System.currentTimeMillis()
            )
        )
    }

    override suspend fun deleteComment(commentId: Long) {
        commentDao.deleteById(commentId)
    }

    override fun observeUsers(): Flow<List<CommunityUser>> =
        userDao.observeAll().map { list -> list.map { it.toModel() } }

    override suspend fun setFollowing(userId: Long, following: Boolean) {
        userDao.setFollowing(userId, following)
    }

}

