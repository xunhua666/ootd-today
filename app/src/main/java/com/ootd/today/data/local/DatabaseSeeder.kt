package com.ootd.today.data.local

import javax.inject.Inject
import javax.inject.Singleton

/** Only community fixtures are persisted; the experience wardrobe lives outside Room. */
@Singleton
class DatabaseSeeder @Inject constructor(
    private val userDao: CommunityUserDao,
    private val postDao: PostDao,
    private val commentDao: CommentDao
) {

    suspend fun seedIfEmpty() {
        seedCommunity()
    }

    private suspend fun seedCommunity() {
        // 用户表必须写入（即使动态已有），否则「我」这条记录缺失会导致发布失败。
        val usersEmpty = userDao.count() == 0
        if (usersEmpty) userDao.insertAll(seedCommunityUsers())

        if (postDao.count() > 0) return

        // 动态用自增 id，插入后才能知道各条的真实 id，
        // 因此逐条插入并收集 id，再把评论挂到对应的动态上。
        val posts = seedPosts()
        val insertedIds = posts.map { postDao.insert(it) }

        val comments = seedComments()
        // 播种评论里的 postId 是「插入顺序」（1 起），这里映射成真实 id。
        val remapped = comments.mapNotNull { comment ->
            val realId = insertedIds.getOrNull((comment.postId - 1).toInt()) ?: return@mapNotNull null
            comment.copy(id = 0, postId = realId)
        }
        if (remapped.isNotEmpty()) commentDao.insertAll(remapped)
    }
}