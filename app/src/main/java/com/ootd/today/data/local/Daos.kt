package com.ootd.today.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * 衣橱单品 DAO。
 *
 * 查询全部返回 [Flow]，Room 会在表变化时自动重新发射，
 * 因此 UI 层无需手动刷新，增删改后列表自动更新。
 */
@Dao
interface WardrobeItemDao {

    @Query("SELECT * FROM wardrobe_items ORDER BY id ASC")
    fun observeAll(): Flow<List<WardrobeItemEntity>>

    @Query("SELECT * FROM wardrobe_items WHERE id = :id")
    suspend fun getById(id: Long): WardrobeItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: WardrobeItemEntity): Long

    @Update
    suspend fun update(entity: WardrobeItemEntity)

    @Delete
    suspend fun delete(entity: WardrobeItemEntity)

    @Query("DELETE FROM wardrobe_items WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM wardrobe_items")
    suspend fun count(): Int
}

/**
 * 打卡记录 DAO。
 *
 * 区间查询用闭区间 [from]..[to]，与 ViewModel 里「当天零点 ~ 当天 23:59:59」的取法配套。
 */
@Dao
interface OutfitLogDao {

    @Query("SELECT * FROM outfit_logs ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<OutfitLogEntity>>

    @Query("SELECT * FROM outfit_logs WHERE timestamp BETWEEN :from AND :to ORDER BY timestamp DESC")
    fun observeBetween(from: Long, to: Long): Flow<List<OutfitLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: OutfitLogEntity): Long

    @Query("DELETE FROM outfit_logs WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM outfit_logs")
    suspend fun count(): Int
}
