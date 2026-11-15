package com.ootd.today.data.repository

import com.ootd.today.data.local.*
import com.ootd.today.data.model.CalendarPlan
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.map

@Singleton
class CalendarPlanRepository @Inject constructor(private val dao: CalendarPlanDao) {
    fun observeAll() = dao.observeAll().map { rows -> rows.map { row ->
        CalendarPlan(row.id, row.dateKey, row.title, row.startMinute, row.occasion, row.location,
            row.note, OutfitSnapshotCodec.decode(row.itemsSnapshot), row.completed)
    } }
    suspend fun save(plan: CalendarPlan) {
        require(plan.validationError() == null) { plan.validationError().orEmpty() }
        dao.save(CalendarPlanEntity(plan.id, plan.dateKey, plan.title.trim(), plan.startMinute, plan.occasion,
            plan.location.trim(), plan.note.trim(), OutfitSnapshotCodec.encode(plan.items.distinctBy { it.id }), plan.completed))
    }
    suspend fun complete(id: Long, completed: Boolean) = dao.complete(id, completed)
    suspend fun delete(id: Long) = dao.delete(id)
}
