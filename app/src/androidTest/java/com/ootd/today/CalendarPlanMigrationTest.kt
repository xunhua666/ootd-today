package com.ootd.today

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ootd.today.data.local.*
import com.ootd.today.data.model.CalendarPlan
import com.ootd.today.data.repository.CalendarPlanRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CalendarPlanMigrationTest {
    @Test fun upgradesV3WithoutLosingLogsAndPersistsPlanLifecycle() = runBlocking {
        val instrument = InstrumentationRegistry.getInstrumentation()
        val context = instrument.targetContext
        val name = "calendar-migration-test.db"
        context.deleteDatabase(name)
        val schema = JSONObject(instrument.context.assets.open("com.ootd.today.data.local.AppDatabase/3.json").bufferedReader().use { it.readText() }).getJSONObject("database")
        context.openOrCreateDatabase(name, 0, null).use { old ->
            val entities = schema.getJSONArray("entities")
            for (i in 0 until entities.length()) {
                val entity = entities.getJSONObject(i)
                old.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", entity.getString("tableName")))
                val indices = entity.optJSONArray("indices")
                if (indices != null) for (j in 0 until indices.length()) old.execSQL(indices.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}", entity.getString("tableName")))
            }
            val setup = schema.getJSONArray("setupQueries")
            for (i in 0 until setup.length()) old.execSQL(setup.getString(i))
            old.execSQL("INSERT INTO outfit_logs (id,timestamp,occasion,score,reason,itemsSnapshot,mood,note) VALUES (9901,0,'COMMUTE',80,'migration-record','',NULL,'keep-me')")
            old.version = 3
        }
        fun open() = Room.databaseBuilder(context, AppDatabase::class.java, name).addMigrations(AppDatabase.MIGRATION_3_4, AppDatabase.MIGRATION_4_5).build()
        var db = open()
        try {
            db.openHelper.readableDatabase.query("SELECT note FROM outfit_logs WHERE id=9901").use { c -> assertTrue(c.moveToFirst()); assertEquals("keep-me", c.getString(0)) }
            var repository = CalendarPlanRepository(db.calendarPlanDao())
            val item = demoWardrobeItems().first().copy(id = 101, name = "已选外套")
            repository.save(CalendarPlan(dateKey = "2026-11-06", title = "画展约会", startMinute = 570, note = "带好门票", items = listOf(item)))
            val saved = repository.observeAll().first().single()
            assertEquals(101L, saved.items.single().id)
            repository.complete(saved.id, true)
            db.close(); db = open(); repository = CalendarPlanRepository(db.calendarPlanDao())
            val restored = repository.observeAll().first().single()
            assertTrue(restored.completed)
            assertEquals("09:30", restored.timeLabel)
            assertEquals("已选外套", restored.items.single().name)
            repository.save(restored.copy(title = "编辑后的画展", dateKey = "2026-12-01"))
            assertEquals("2026-12-01", repository.observeAll().first().single().dateKey)
            repository.delete(saved.id)
            assertTrue(repository.observeAll().first().isEmpty())
            db.openHelper.readableDatabase.query("SELECT COUNT(*) FROM outfit_logs WHERE id=9901").use { c -> c.moveToFirst(); assertEquals(1, c.getInt(0)) }
        } finally { db.close(); context.deleteDatabase(name) }
    }
}
