package com.ootd.today.data.local

import androidx.room.TypeConverter
import com.ootd.today.data.model.ClothingCategory
import com.ootd.today.data.model.ItemStatus
import com.ootd.today.data.model.Occasion
import com.ootd.today.data.model.Season
import com.ootd.today.data.model.StyleTag

/**
 * Room 类型转换器。
 *
 * SQLite 只能存基础类型，枚举与枚举集合需要序列化。
 * 集合用逗号拼接枚举名存储，可读性好、便于日后用 SQL 直接排查数据。
 */
class Converters {

    @TypeConverter
    fun seasonsToString(value: Set<Season>): String = value.joinToString(",") { it.name }

    @TypeConverter
    fun stringToSeasons(value: String): Set<Season> = value
        .split(",")
        .filter { it.isNotBlank() }
        .map { Season.valueOf(it) }
        .toSet()

    @TypeConverter
    fun occasionsToString(value: Set<Occasion>): String = value.joinToString(",") { it.name }

    @TypeConverter
    fun stringToOccasions(value: String): Set<Occasion> = value
        .split(",")
        .filter { it.isNotBlank() }
        .map { Occasion.valueOf(it) }
        .toSet()

    @TypeConverter
    fun categoryToString(value: ClothingCategory): String = value.name

    @TypeConverter
    fun stringToCategory(value: String): ClothingCategory = ClothingCategory.valueOf(value)

    @TypeConverter
    fun statusToString(value: ItemStatus): String = value.name

    @TypeConverter
    fun stringToStatus(value: String): ItemStatus = ItemStatus.valueOf(value)

    @TypeConverter
    fun styleToString(value: StyleTag?): String? = value?.name

    @TypeConverter
    fun stringToStyle(value: String?): StyleTag? = value?.let { StyleTag.valueOf(it) }

    @TypeConverter
    fun occasionToString(value: Occasion): String = value.name

    @TypeConverter
    fun stringToOccasion(value: String): Occasion = Occasion.valueOf(value)

    /**
     * 话题标签列表 <-> 逗号分隔字符串。
     *
     * 话题内容由 [com.ootd.today.domain.SocialTextParser] 保证不含逗号
     * （解析时遇到逗号即终止），因此可以安全地用逗号作分隔符。
     * 写入前统一去掉空串，避免出现连续逗号。
     */
    @TypeConverter
    fun topicsToString(value: List<String>): String =
        value.filter { it.isNotBlank() }.joinToString(",")

    @TypeConverter
    fun stringToTopics(value: String): List<String> =
        value.split(",").filter { it.isNotBlank() }
}
