package com.ootd.today.domain.vision

import com.ootd.today.data.model.*
import com.ootd.today.data.prefs.VisionSettings
import com.ootd.today.data.local.OutfitSnapshotCodec
import com.ootd.today.data.local.demoWardrobeItems
import com.ootd.today.domain.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class VisionProtocolTest {
    private fun result(vararg updates: Pair<String, Any>) = JSONObject("""{"isGarment":true,"ambiguous":false,"category":"BOTTOM","name":"高腰阔腿裤","fit":"RELAXED","length":"LONG","rise":"HIGH","cut":"WIDE","shoulder":"UNKNOWN","confidence":"MEDIUM","evidence":"裤腿轮廓较宽","warning":"腰高仍建议试穿确认"}""").apply { updates.forEach { put(it.first, it.second) } }.toString()
    @Test fun extractsVisibleTagsButRequiresUserConfirmation() {
        val parsed = VisionProtocol.parse("```json\n${result()}\n```")
        assertEquals(GarmentCut.WIDE, parsed.shape.cut)
        assertEquals(WaistRise.HIGH, parsed.shape.rise)
        assertFalse(parsed.shape.confirmed)
    }
    @Test fun unknownOrLowConfidenceDoesNotInventShape() {
        assertTrue(VisionProtocol.parse(result("confidence" to "LOW")).shape.labels.isEmpty())
        assertEquals(GarmentCut.UNKNOWN, VisionProtocol.parse(result("cut" to "a made-up cut")).shape.cut)
        assertEquals(GarmentFit.UNKNOWN, VisionProtocol.parse(result("fit" to JSONObject.NULL)).shape.fit)
    }
    @Test fun irrelevantDimensionsAreRemoved() {
        val top = VisionProtocol.parse(result("category" to "TOP", "shoulder" to "DROPPED")).shape
        assertEquals(GarmentCut.UNKNOWN, top.cut); assertEquals(WaistRise.UNKNOWN, top.rise)
        assertEquals(ShoulderShape.DROPPED, top.shoulder)
    }
    @Test fun refusesNonGarmentAndAmbiguousSubjects() {
        for (raw in listOf(result("isGarment" to false), result("ambiguous" to true), result("category" to "PERSON"))) {
            assertTrue(runCatching { VisionProtocol.parse(raw) }.isFailure)
        }
    }
    @Test fun payloadContainsOnePhotoAndJsonModeWithoutCredentials() {
        val payload = JSONObject(VisionProtocol.payload("qwen3-vl-plus", "sample-jpeg"))
        val messages = payload.getJSONArray("messages")
        assertEquals(1, messages.length())
        val content = messages.getJSONObject(0).getJSONArray("content")
        assertEquals(2, content.length())
        assertEquals("data:image/jpeg;base64,sample-jpeg", content.getJSONObject(1).getJSONObject("image_url").getString("url"))
        assertEquals("json_object", payload.getJSONObject("response_format").getString("type"))
        assertFalse(payload.getBoolean("enable_thinking")); assertFalse(payload.has("apiKey"))
    }
    @Test fun extractsARealChatCompletionsEnvelope() {
        val envelope = JSONObject().put("choices", org.json.JSONArray().put(JSONObject().put("message", JSONObject().put("content", result())))).toString()
        assertEquals("高腰阔腿裤", VisionProtocol.extract(envelope).name)
        assertTrue(runCatching { VisionProtocol.extract("""{"error":{"message":"request denied"}}""") }.isFailure)
    }
    @Test fun malformedSnapshotsFallBackAndConfirmedTagsRoundTrip() {
        assertEquals(GarmentShape(), GarmentShapeCodec.decode("old record"))
        val shape = GarmentShape(fit = GarmentFit.RELAXED, length = GarmentLength.CROPPED, confirmed = true)
        assertEquals(shape, GarmentShapeCodec.decode(GarmentShapeCodec.encode(shape)))
        val item = demoWardrobeItems().first().copy(id = 123, shape = shape)
        assertEquals(shape, OutfitSnapshotCodec.decode(OutfitSnapshotCodec.encode(listOf(item))).single().shape)
    }
    @Test fun onlyConfirmedShapesAffectRecommendations() {
        val pool = demoWardrobeItems()
        val top = pool.first { it.category == ClothingCategory.TOP }.copy(shape = GarmentShape(fit = GarmentFit.OVERSIZED, confirmed = true))
        val narrow = pool.first { it.category == ClothingCategory.BOTTOM }.copy(shape = GarmentShape(cut = GarmentCut.TAPERED, confirmed = true))
        val wide = narrow.copy(shape = GarmentShape(cut = GarmentCut.WIDE, fit = GarmentFit.OVERSIZED, confirmed = true))
        assertTrue(OutfitAesthetics.score(listOf(top, narrow)) > OutfitAesthetics.score(listOf(top, wide)))
        assertNull(SilhouetteRules.score(listOf(top.copy(shape = top.shape.copy(confirmed = false)), narrow)))
        assertTrue(SilhouetteRules.describe(listOf(top, narrow))!!.contains("上宽下利落"))
    }
    @Test fun croppedTopAndHighWaistHaveSpecificAdvice() {
        val pool = demoWardrobeItems()
        val top = pool.first { it.category == ClothingCategory.TOP }.copy(shape = GarmentShape(length = GarmentLength.CROPPED, confirmed = true))
        val bottom = pool.first { it.category == ClothingCategory.BOTTOM }.copy(shape = GarmentShape(rise = WaistRise.HIGH, confirmed = true))
        assertNotNull(SilhouetteRules.score(listOf(top, bottom)))
        assertTrue(SilhouetteRules.describe(listOf(top, bottom))!!.contains("腰线"))
    }
    @Test fun confirmedOuterwearShapesAlsoInfluenceComposition() {
        val pool = demoWardrobeItems()
        val outer = pool.first { it.category == ClothingCategory.OUTERWEAR }.copy(shape = GarmentShape(length = GarmentLength.CROPPED, confirmed = true))
        val bottom = pool.first { it.category == ClothingCategory.BOTTOM }.copy(shape = GarmentShape(rise = WaistRise.HIGH, confirmed = true))
        assertNotNull(SilhouetteRules.score(listOf(outer, bottom)))
        assertTrue(SilhouetteRules.describe(listOf(outer, bottom))!!.contains("腰线"))
    }
    @Test fun visionConfigRequiresHttpsAndHandlesBothBaseAndEndpoint() {
        assertNotNull(VisionSettings("http://example.com", "qwen3-vl-plus", "test").validationError())
        assertNotNull(VisionSettings("https://{WorkspaceId}.example.com", "qwen3-vl-plus", "test").validationError())
        assertNull(VisionSettings("https://example.com/v1", "qwen3-vl-plus", "test").validationError())
        assertEquals("https://example.com/v1/chat/completions", VisionSettings("https://example.com/v1/chat/completions/").endpoint)
    }
}
