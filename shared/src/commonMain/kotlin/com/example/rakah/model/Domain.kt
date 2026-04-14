// shared/src/commonMain/kotlin/com/example/rakah/model/Domain.kt
package com.example.rakah.model

import kotlinx.serialization.Serializable

enum class Posture { QIYAM, RUKU, JALSA, SUJOOD, UNKNOWN }

enum class PrayerType(val label: String) {
    FAJR("Fajr"),
    DHUHR("Dhuhr"),
    ASR("Asr"),
    MAGHRIB("Maghrib"),
    ISHA("Isha")
}

data class PrayerSpec(
    val totalRakah: Int,
    val tashahhudAfterRakah: Set<Int>
)

fun PrayerType.spec(): PrayerSpec = when (this) {
    PrayerType.FAJR -> PrayerSpec(totalRakah = 2, tashahhudAfterRakah = setOf(2))
    PrayerType.DHUHR -> PrayerSpec(totalRakah = 4, tashahhudAfterRakah = setOf(2, 4))
    PrayerType.ASR -> PrayerSpec(totalRakah = 4, tashahhudAfterRakah = setOf(2, 4))
    PrayerType.MAGHRIB -> PrayerSpec(totalRakah = 3, tashahhudAfterRakah = setOf(2, 3))
    PrayerType.ISHA -> PrayerSpec(totalRakah = 4, tashahhudAfterRakah = setOf(2, 4))
}

enum class PrayerStep {
    QIYAM,
    RUKU,
    QAWMAH,
    SUJOOD,
    JALSA_BETWEEN_SUJOOD,
    SECOND_SUJOOD,
    TASHAHHUD,
    SALAAM_RIGHT,
    SALAAM_LEFT,
    DONE
}

@Serializable
data class Keypoint(
    val name: String,
    val x: Float,   // image coords in pixels
    val y: Float,
    val score: Float
)

data class PoseFrame(
    val keypoints: Map<String, Keypoint>,
    val width: Int,
    val height: Int,
    val timestampMs: Long
)

class StandingCalib(
    initialHeadToAnkle: Float,
    private val emaAlpha: Float = 0.12f,
    private val minAcceptedPx: Float = 80f,
    private val maxAcceptedPx: Float = 2000f
) {
    var headToAnkle: Float = initialHeadToAnkle
        private set

    fun updateFromStanding(observedHeadToAnkle: Float) {
        if (observedHeadToAnkle !in minAcceptedPx..maxAcceptedPx) return
        val a = emaAlpha.coerceIn(0.01f, 1f)
        headToAnkle = headToAnkle * (1f - a) + observedHeadToAnkle * a
    }
}

data class RakahResult(
    val currentPosture: Posture,
    val rakahCount: Int, // completed rakah
    val postureConfidence: Float,
    val lastEvent: String? = null,
    val prayer: PrayerType = PrayerType.FAJR,
    val totalRakah: Int = 2,
    val currentRakah: Int = 1,
    val currentStep: PrayerStep = PrayerStep.QIYAM,
    val previousStep: PrayerStep? = null,
    val nextStep: PrayerStep? = PrayerStep.RUKU
)
