// shared/src/commonMain/kotlin/com/example/rakah/model/Domain.kt
package com.example.rakah.model

import kotlinx.serialization.Serializable

enum class Posture { QIYAM, RUKU, JALSA, SUJOOD, UNKNOWN }

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
    val rakahCount: Int,
    val postureConfidence: Float,
    val lastEvent: String? = null
)
