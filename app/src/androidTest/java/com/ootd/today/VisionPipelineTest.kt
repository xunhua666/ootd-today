package com.ootd.today

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.media.ExifInterface
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ootd.today.data.local.*
import com.ootd.today.data.model.*
import com.ootd.today.data.repository.CloudGarmentVision
import com.ootd.today.domain.vision.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first

@RunWith(AndroidJUnit4::class)
class VisionPipelineTest {
    @Test fun imageUploadCorrectsOrientationBoundsSizeAndRemovesMetadata() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val input = File(context.cacheDir, "vision-test-original.jpg")
        val output = File(context.cacheDir, "vision-test-upload.jpg")
        try {
            val bitmap = Bitmap.createBitmap(2560, 1280, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.GREEN) }
            input.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }; bitmap.recycle()
            ExifInterface(input.path).apply { setAttribute(ExifInterface.TAG_ORIENTATION, "6"); setAttribute(ExifInterface.TAG_GPS_LATITUDE, "30/1,0/1,0/1"); saveAttributes() }
            val bytes = CloudGarmentVision.prepareJpeg(input.path)
            output.writeBytes(bytes)
            val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            assertEquals(640, decoded.width); assertEquals(1280, decoded.height); decoded.recycle()
            assertTrue(bytes.size < 2_500_000)
            assertNull(ExifInterface(output.path).getAttribute(ExifInterface.TAG_GPS_LATITUDE))
        } finally { input.delete(); output.delete() }
    }
    @Test fun migratesV4PreservesWardrobeAndPlansThenPersistsShape() = runBlocking {
        val instrument = InstrumentationRegistry.getInstrumentation(); val context = instrument.targetContext
        val name = "vision-migration-test.db"; context.deleteDatabase(name)
        val schema = JSONObject(instrument.context.assets.open("com.ootd.today.data.local.AppDatabase/4.json").bufferedReader().use { it.readText() }).getJSONObject("database")
        context.openOrCreateDatabase(name, 0, null).use { old ->
            val entities = schema.getJSONArray("entities")
            for (i in 0 until entities.length()) {
                val entity = entities.getJSONObject(i); val table = entity.getString("tableName")
                old.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", table))
                val indices = entity.optJSONArray("indices")
                if (indices != null) for (j in 0 until indices.length()) old.execSQL(indices.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}", table))
            }
            val setup = schema.getJSONArray("setupQueries"); for (i in 0 until setup.length()) old.execSQL(setup.getString(i))
            old.execSQL("INSERT INTO wardrobe_items (id,name,category,subCategory,imagePath,colorArgb,colorName,warmthLevel,seasons,occasions,style,brand,price,purchaseDate,lastWornDate,status,note,wearCount) VALUES (123,'保留的外套','OUTERWEAR','夹克',NULL,4278190080,'黑',3,'AUTUMN','COMMUTE',NULL,NULL,NULL,NULL,NULL,'ACTIVE',NULL,4)")
            old.execSQL("INSERT INTO calendar_plans (id,dateKey,title,startMinute,occasion,location,note,itemsSnapshot,completed) VALUES (42,'2026-11-06','保留的日程',NULL,'COMMUTE','','','',0)")
            old.version = 4
        }
        fun open() = Room.databaseBuilder(context, AppDatabase::class.java, name).addMigrations(AppDatabase.MIGRATION_4_5).build()
        var db = open()
        try {
            val before = db.wardrobeItemDao().getById(123)!!.toModel()
            assertEquals(4, before.wearCount); assertEquals(GarmentShape(), before.shape)
            assertEquals("保留的日程", db.calendarPlanDao().observeAll().first().single().title)
            val result = VisionProtocol.parse("""{"isGarment":true,"ambiguous":false,"category":"OUTERWEAR","name":"短款宽松夹克","fit":"RELAXED","length":"CROPPED","confidence":"HIGH","evidence":"下摆较短"}""")
            val confirmed = before.copy(shape = result.shape.copy(confirmed = true))
            db.wardrobeItemDao().update(confirmed.toEntity()); db.close(); db = open()
            val restored = db.wardrobeItemDao().getById(123)!!.toModel()
            assertTrue(restored.shape.confirmed); assertEquals(GarmentLength.CROPPED, restored.shape.length)
            assertEquals(4, restored.wearCount)
        } finally { db.close(); context.deleteDatabase(name) }
    }
}
