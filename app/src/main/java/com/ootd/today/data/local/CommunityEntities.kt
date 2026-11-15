package com.ootd.today.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.ootd.today.data.model.Occasion

/**
 * 社区用户表。
 *
 * P0 阶段同时承载「模拟的其他用户」与「我自己」：我自己这条记录以
 * [com.ootd.today.data.model.CommunityUser.ME_ID] 为 id 首次启动写入。
 */
@Entity(tableName = "community_users")
data class CommunityUserEntity(
    @PrimaryKey val id: Long,
    val nickname: String,
    val avatarEmoji: String,
    val bio: String,
    val isMe: Boolean,
    val isFollowing: Boolean
)

/**
 * 社区动态表。
 *
 * 穿搭用 [itemsSnapshot] 存快照（复用 [OutfitSnapshotCodec]），
 * 理由与打卡记录一致：单品日后被删除/修改，历史动态不应残缺。
 * 同时也让「社区数据」与「我的衣橱」解耦——将来接真后端时，
 * 别人的动态只带快照，本地没有对应单品也不会出问题。
 *
 * [topics] 是话题标签的冗余存储（逗号分隔，取自标题解析结果）。
 * 冗余的理由：让「按话题筛选动态」能直接走 SQL 索引，
 * 而不必把整张表读出来再逐条解析文本。
 */
@Entity(
    tableName = "posts",
    indices = [Index("authorId"), Index("createdAt")]
)
data class PostEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val authorId: Long,
    val caption: String,
    val itemsSnapshot: String,
    val occasion: Occasion,
    val likeCount: Int,
    val isLiked: Boolean,
    val createdAt: Long,
    /** 配图路径，null 表示纯穿搭动态。 */
    val photoPath: String? = null,
    /** 话题标签，逗号分隔（不含 # ）。 */
    val topics: String = ""
)

/**
 * 评论表。
 */
@Entity(
    tableName = "comments",
    indices = [Index("postId"), Index("authorId")]
)
data class CommentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val postId: Long,
    val authorId: Long,
    val content: String,
    val createdAt: Long
)

/** 评论数聚合查询结果。 */
data class PostCommentCount(
    val postId: Long,
    val count: Int
)