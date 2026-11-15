package com.ootd.today.data.local

import androidx.room.*
import com.ootd.today.data.model.Occasion
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "calendar_plans", indices = [Index("dateKey")])
data class CalendarPlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateKey: String,
    val title: String,
    val startMinute: Int?,
    val occasion: Occasion,
    val location: String,
    val note: String,
    val itemsSnapshot: String,
    val completed: Boolean
)

@Dao
interface CalendarPlanDao {
    @Query("SELECT * FROM calendar_plans ORDER BY dateKey, COALESCE(startMinute, -1), id")
    fun observeAll(): Flow<List<CalendarPlanEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(plan: CalendarPlanEntity): Long
    @Query("UPDATE calendar_plans SET completed = :completed WHERE id = :id")
    suspend fun complete(id: Long, completed: Boolean)
    @Query("DELETE FROM calendar_plans WHERE id = :id")
    suspend fun delete(id: Long)
}
