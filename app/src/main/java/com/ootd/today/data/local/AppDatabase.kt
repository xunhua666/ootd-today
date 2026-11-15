package com.ootd.today.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * 应用数据库。
 *
 * version 从 1 开始。后续任何表结构变更都必须递增版本号并提供 Migration，
 * 或者在开发期用 destructiveMigration 清库重建（仅限未发布阶段）。
 */
@Database(
    entities = [
        WardrobeItemEntity::class,
        OutfitLogEntity::class,
        CommunityUserEntity::class,
        PostEntity::class,
        CommentEntity::class,
        CalendarPlanEntity::class
    ],
    version = 5,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun wardrobeItemDao(): WardrobeItemDao

    abstract fun outfitLogDao(): OutfitLogDao

    abstract fun communityUserDao(): CommunityUserDao

    abstract fun postDao(): PostDao

    abstract fun commentDao(): CommentDao
    abstract fun calendarPlanDao(): CalendarPlanDao

    companion object {
        const val DATABASE_NAME = "ootd_today.db"
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `wardrobe_items` ADD COLUMN `shapeJson` TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""CREATE TABLE IF NOT EXISTS `calendar_plans` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `dateKey` TEXT NOT NULL, `title` TEXT NOT NULL, `startMinute` INTEGER,
                    `occasion` TEXT NOT NULL, `location` TEXT NOT NULL, `note` TEXT NOT NULL,
                    `itemsSnapshot` TEXT NOT NULL, `completed` INTEGER NOT NULL)""")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_calendar_plans_dateKey` ON `calendar_plans` (`dateKey`)")
            }
        }

        /**
         * v1 -> v2：新增社区三张表。
         *
         * 用 Migration 而非 destructiveMigration：用户已经录入的衣橱与打卡
         * 记录是有价值的数据，升级不该被清空。
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `community_users` (
                        `id` INTEGER NOT NULL,
                        `nickname` TEXT NOT NULL,
                        `avatarEmoji` TEXT NOT NULL,
                        `bio` TEXT NOT NULL,
                        `isMe` INTEGER NOT NULL,
                        `isFollowing` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `posts` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `authorId` INTEGER NOT NULL,
                        `caption` TEXT NOT NULL,
                        `itemsSnapshot` TEXT NOT NULL,
                        `occasion` TEXT NOT NULL,
                        `likeCount` INTEGER NOT NULL,
                        `isLiked` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                    """
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_posts_authorId` ON `posts` (`authorId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_posts_createdAt` ON `posts` (`createdAt`)")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `comments` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `postId` INTEGER NOT NULL,
                        `authorId` INTEGER NOT NULL,
                        `content` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                    """
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_comments_postId` ON `comments` (`postId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_comments_authorId` ON `comments` (`authorId`)")
            }
        }

        /**
         * v2 -> v3：动态新增「配图」与「话题标签」。
         *
         * 两列都带默认值（NULL / 空串），因此旧动态无需回填即可继续读取：
         * 老动态自然就是「无配图、无话题」。
         *
         * 注意 SQLite 的 ALTER TABLE ADD COLUMN 不支持非常量默认值，
         * 但常量默认值（NULL、''）是完全支持的，因此这里可以安全使用。
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `posts` ADD COLUMN `photoPath` TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE `posts` ADD COLUMN `topics` TEXT NOT NULL DEFAULT ''")
            }
        }
    }
}
