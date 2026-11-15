package com.ootd.today.domain.stylist

/** Fixed artifacts: downloads and imports must both match this catalogue. */
data class LocalModel(
    val id: String,
    val title: String,
    val description: String,
    val repository: String,
    val revision: String,
    val filename: String,
    val bytes: Long,
    val sha256: String
) {
    val url: String get() = "https://huggingface.co/litert-community/$repository/resolve/$revision/$filename"
    val sourceUrl: String get() = "https://huggingface.co/litert-community/$repository/blob/$revision/$filename"
    val sizeLabel: String get() = "${bytes / 1_000_000} MB"

    companion object {
        val RECOMMENDED = LocalModel(
            "qwen3-17-int4", "Qwen3 · 1.7B 审美顾问", "中文搭配交流，兼顾建议质量与手机负担",
            "Qwen3-1.7B", "87603f0ff3fe74b9b77ed0477edb773b4b596f32",
            "Qwen3-1.7B_dynamic_wi4b32_afp32.litertlm", 977184032L,
            "2a72330b504a6596d53a2c00923c57173b10f1b83e6a541213ffec08d1fc5de8"
        )
        val ALL = listOf(RECOMMENDED)
        fun find(id: String): LocalModel = ALL.firstOrNull { it.id == id } ?: RECOMMENDED
    }
}
