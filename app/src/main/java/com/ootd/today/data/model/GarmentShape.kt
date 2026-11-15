package com.ootd.today.data.model

enum class GarmentFit(val label: String) { UNKNOWN("不确定"), FITTED("修身"), REGULAR("合体"), RELAXED("宽松"), OVERSIZED("廓形宽大") }
enum class GarmentLength(val label: String) { UNKNOWN("不确定"), CROPPED("短款"), REGULAR("常规长度"), LONG("长款") }
enum class WaistRise(val label: String) { UNKNOWN("不确定"), LOW("低腰"), MID("中腰"), HIGH("高腰") }
enum class GarmentCut(val label: String) { UNKNOWN("不确定"), SKINNY("紧身"), STRAIGHT("直筒"), TAPERED("锥形"), WIDE("阔腿"), FLARED("微喇／喇叭"), A_LINE("A字") }
enum class ShoulderShape(val label: String) { UNKNOWN("不确定"), SET_IN("正肩"), DROPPED("落肩"), RAGLAN("插肩") }

data class GarmentShape(
    val fit: GarmentFit = GarmentFit.UNKNOWN,
    val length: GarmentLength = GarmentLength.UNKNOWN,
    val rise: WaistRise = WaistRise.UNKNOWN,
    val cut: GarmentCut = GarmentCut.UNKNOWN,
    val shoulder: ShoulderShape = ShoulderShape.UNKNOWN,
    val confirmed: Boolean = false
) {
    val labels: List<String> get() = buildList {
        if (fit != GarmentFit.UNKNOWN) add(fit.label)
        if (length != GarmentLength.UNKNOWN) add(length.label)
        if (rise != WaistRise.UNKNOWN) add(rise.label)
        if (cut != GarmentCut.UNKNOWN) add(cut.label)
        if (shoulder != ShoulderShape.UNKNOWN) add(shoulder.label)
    }
    val summary: String get() = if (confirmed) labels.joinToString("、").ifBlank { "版型待补充" } else "版型未确认"
    fun forCategory(category: ClothingCategory): GarmentShape = when (category) {
        ClothingCategory.TOP, ClothingCategory.OUTERWEAR -> copy(rise = WaistRise.UNKNOWN, cut = GarmentCut.UNKNOWN)
        ClothingCategory.BOTTOM -> copy(shoulder = ShoulderShape.UNKNOWN)
        ClothingCategory.ONE_PIECE -> copy(rise = WaistRise.UNKNOWN, cut = if (cut == GarmentCut.A_LINE) cut else GarmentCut.UNKNOWN)
        else -> GarmentShape(confirmed = confirmed)
    }
}
