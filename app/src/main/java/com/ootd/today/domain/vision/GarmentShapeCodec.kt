package com.ootd.today.domain.vision

import com.ootd.today.data.model.*
import org.json.JSONObject

object GarmentShapeCodec {
    fun encode(shape: GarmentShape): String = JSONObject().put("fit", shape.fit.name).put("length", shape.length.name)
        .put("rise", shape.rise.name).put("cut", shape.cut.name).put("shoulder", shape.shoulder.name).put("confirmed", shape.confirmed).toString()
    fun decode(raw: String): GarmentShape = runCatching { fromJson(JSONObject(raw)) }.getOrDefault(GarmentShape())
    fun fromJson(json: JSONObject) = GarmentShape(
        enumOrUnknown<GarmentFit>(json.optString("fit")), enumOrUnknown<GarmentLength>(json.optString("length")),
        enumOrUnknown<WaistRise>(json.optString("rise")), enumOrUnknown<GarmentCut>(json.optString("cut")),
        enumOrUnknown<ShoulderShape>(json.optString("shoulder")), json.optBoolean("confirmed", false)
    )
    private inline fun <reified T : Enum<T>> enumOrUnknown(value: String): T = enumValues<T>().firstOrNull { it.name == value } ?: enumValues<T>().first()
}
