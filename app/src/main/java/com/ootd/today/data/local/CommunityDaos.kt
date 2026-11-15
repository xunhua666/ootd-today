package com.ootd.today.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * 社区用户 DAO。
 */
@Dao
interface CommunityUserDao {

    @Query("SELECT * FROM community_users")
    fun observeAll(): Flow<List<CommunityUserEntity>>

    @Query("SELECT * FROM community_users WHERE id = :id")
    suspend fun getById(id: Long): CommunityUserEntity?

    @Query("SELECT COUNT(*) FROM community_users")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(users: List<CommunityUserEntity>)

    @Query("UPDATE community_users SET isFollowing = :following WHERE id = :id")
    suspend fun setFollowing(id: Long, following: Boolean)

    @Query("UPDATE community_users SET nickname = :nickname, avatarEmoji = :emoji WHERE id = :id")
    suspend fun updateProfile(id: Long, nickname: String, emoji: String)
}

/**
 * 社区动态 DAO。
 */
@Dao
interface PostDao {

    @Query("SELECT * FROM posts ORDER BY createdAt DESC")
    fun observeFeed(): Flow<List<PostEntity>>

    /** 我发布的动态。 */
    @Query("SELECT * FROM posts WHERE authorId = :authorId ORDER BY createdAt DESC")
    fun observeByAuthor(authorId: Long): Flow<List<PostEntity>>

    /**
     * 按话题筛选。
     *
     * 用 LIKE 匹配「,标签,」形式的包裹串，避免 `#ootd` 误匹配 `#ootd2026`：
     * 存储时已用逗号拼接，这里把待查标签也用逗号包起来再匹配。
     */
    @Query(
        """
        SELECT * FROM posts
        WHERE ',' || topics || ',' LIKE '%,' || :tag || ',%'
        ORDER BY createdAt DESC
        """
    )
    fun observeByTopic(tag: String): Flow<List<PostEntity>>

    /** 全部出现过的话题（逗号拼接的原始串），供「热门话题」聚合。 */
    @Query("SELECT topics FROM posts WHERE topics != ''")
    fun observeAllTopicStrings(): Flow<List<String>>

    @Query("SELECT * FROM posts WHERE id = :id")
    suspend fun getById(id: Long): PostEntity?

    /** 单条动态的响应式查询，用于详情页跟随点赞/评论实时刷新。 */
    @Query("SELECT * FROM posts WHERE id = :id")
    fun observeById(id: Long): Flow<PostEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(post: PostEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(posts: List<PostEntity>)

    @Query("DELETE FROM posts WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM posts")
    suspend fun count(): Int

    /**
     * 点赞 / 取消点赞。
     *
     * 同一条 UPDATE 里同时改计数与状态，保证两者永远一致；
     * 计数用 MAX(0, ...) 兜底，避免取消点赞把总数压成负数。
     */
    @Query(
        """
        UPDATE posts SET
            isLiked = :liked,
            likeCount = MAX(0, likeCount + :delta)
        WHERE id = :id
        """
    )
    suspend fun setLiked(id: Long, liked: Boolean, delta: Int)
}

/**
 * 评论 DAO。
 */
@Dao
interface CommentDao {

    @Query("SELECT * FROM comments WHERE postId = :postId ORDER BY createdAt ASC")
    fun observeByPost(postId: Long): Flow<List<CommentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(comment: CommentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(comments: List<CommentEntity>)

    @Query("DELETE FROM comments WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM comments WHERE postId = :postId")
    suspend fun deleteByPost(postId: Long)

    /** 各动态的评论数。用聚合查询实时统计，不做冗余字段以避免计数漂移。 */
    @Query("SELECT postId, COUNT(*) AS count FROM comments GROUP BY postId")
    fun observeCounts(): Flow<List<PostCommentCount>>
}