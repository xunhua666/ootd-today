package com.ootd.today.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import com.ootd.today.data.model.ClothingCategory
import com.ootd.today.data.model.ItemStatus
import com.ootd.today.data.model.Occasion
import com.ootd.today.data.model.Season
import com.ootd.today.data.model.StyleTag

/**
 * 衣橱单品表。字段与领域模型 [com.ootd.today.data.model.WardrobeItem] 一一对应。
 */
@Entity(tableName = "wardrobe_items")
data class WardrobeItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: ClothingCategory,
    val subCategory: String,
    val imagePath: String?,
    val colorArgb: Long,
    val colorName: String,
    val warmthLevel: Int,
    val seasons: Set<Season>,
    val occasions: Set<Occasion>,
    val style: StyleTag?,
    val brand: String?,
    val price: Double?,
    val purchaseDate: Long?,
    val lastWornDate: Long?,
    val status: ItemStatus,
    val note: String?,
    val wearCount: Int,
    @ColumnInfo(defaultValue = "''") val shapeJson: String = ""
)

/**
 * 穿搭打卡表。
 *
 * [itemsSnapshot] 存的是**搭配快照**而非外键引用：
 * 打卡记录的是「那天穿了什么」，即使单品后来被删除/送人，
 * 历史也不该因此残缺。快照编解码见 [OutfitSnapshotCodec]。
 */
@Entity(tableName = "outfit_logs")
data class OutfitLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val occasion: Occasion,
    val score: Int,
    val reason: String,
    val itemsSnapshot: String,
    val mood: Int?,
    val note: String?
)
