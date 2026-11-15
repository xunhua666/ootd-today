package com.ootd.today.data.local

import com.ootd.today.data.model.Comment
import com.ootd.today.data.model.CommunityUser
import com.ootd.today.data.model.Post

/** 用户实体 -> 领域模型。 */
fun CommunityUserEntity.toModel(): CommunityUser = CommunityUser(
    id = id,
    nickname = nickname,
    avatarEmoji = avatarEmoji,
    bio = bio,
    isMe = isMe,
    isFollowing = isFollowing
)

/** 用户领域模型 -> 实体。 */
fun CommunityUser.toEntity(): CommunityUserEntity = CommunityUserEntity(
    id = id,
    nickname = nickname,
    avatarEmoji = avatarEmoji,
    bio = bio,
    isMe = isMe,
    isFollowing = isFollowing
)

/**
 * 动态实体 -> 领域模型。
 *
 * @param author 作者，由调用方从用户表关联后传入。
 * @param commentCount 评论数，由调用方用聚合查询结果传入。
 */
fun PostEntity.toModel(author: CommunityUser, commentCount: Int): Post = Post(
    id = id,
    author = author,
    items = OutfitSnapshotCodec.decode(itemsSnapshot),
    caption = caption,
    occasion = occasion,
    photoPath = photoPath,
    // topics 以逗号拼接存储，这里还原成列表。
    topics = topics.split(",").filter { it.isNotBlank() },
    likeCount = likeCount,
    isLiked = isLiked,
    commentCount = commentCount,
    createdAt = createdAt
)

/** 评论实体 -> 领域模型。 */
fun CommentEntity.toModel(author: CommunityUser): Comment = Comment(
    id = id,
    postId = postId,
    author = author,
    content = content,
    createdAt = createdAt
)