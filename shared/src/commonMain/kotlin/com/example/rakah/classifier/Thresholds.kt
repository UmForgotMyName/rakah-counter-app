// shared/src/commonMain/kotlin/com/example/rakah/classifier/Thresholds.kt
package com.example.rakah.classifier

data class PostureThresholds(
    val qiyamHipRatio: Float = 0.70f,
    val qiyamKneeAngleMin: Float = 155f,
    val qiyamBackInclineMax: Float = 20f,

    val rukuHipRatioMin: Float = 0.50f,
    val rukuHipRatioMax: Float = 0.68f,
    val rukuBackInclineMin: Float = 35f,
    val rukuBackInclineMax: Float = 70f,

    val jalsaHipRatioMin: Float = 0.30f,
    val jalsaHipRatioMax: Float = 0.55f,
    val jalsaKneeAngleMax: Float = 120f,

    val sujoodHipRatioMax: Float = 0.35f
)

data class Thresholds(
    val enter: PostureThresholds = PostureThresholds(),
    // Exit thresholds are intentionally a bit looser to reduce rapid toggling near boundaries.
    val exit: PostureThresholds = PostureThresholds(
        qiyamHipRatio = 0.66f,
        qiyamKneeAngleMin = 150f,
        qiyamBackInclineMax = 24f,
        rukuHipRatioMin = 0.46f,
        rukuHipRatioMax = 0.72f,
        rukuBackInclineMin = 30f,
        rukuBackInclineMax = 75f,
        jalsaHipRatioMin = 0.26f,
        jalsaHipRatioMax = 0.60f,
        jalsaKneeAngleMax = 130f,
        sujoodHipRatioMax = 0.40f
    )
)
