// shared/src/commonMain/kotlin/com/example/rakah/classifier/DefaultPoseClassifier.kt
package com.example.rakah.classifier

import com.example.rakah.math.*
import com.example.rakah.model.*

data class PoseClassification(
    val posture: Posture,
    val confidence: Float
)

class DefaultPoseClassifier(
    private val cfg: Thresholds,
    private val calib: StandingCalib,
    private val minRequiredKeypointScore: Float = 0.20f
) {
    fun classify(frame: PoseFrame, previousStable: Posture = Posture.UNKNOWN): PoseClassification {
        val kp = frame.keypoints

        val lHipKp = kp["left_hip"] ?: return unknown()
        val rHipKp = kp["right_hip"] ?: return unknown()
        val lHip = Pt(lHipKp.x, lHipKp.y)
        val rHip = Pt(rHipKp.x, rHipKp.y)
        val hips = mid(lHip, rHip)

        val lAnkKp = kp["left_ankle"] ?: return unknown()
        val rAnkKp = kp["right_ankle"] ?: return unknown()
        val lAnk = Pt(lAnkKp.x, lAnkKp.y)
        val rAnk = Pt(rAnkKp.x, rAnkKp.y)
        val ankles = mid(lAnk, rAnk)

        val noseKp = kp["nose"] ?: return unknown()
        val nose = Pt(noseKp.x, noseKp.y)

        val lKneeKp = kp["left_knee"] ?: return unknown()
        val rKneeKp = kp["right_knee"] ?: return unknown()
        val lKnee = Pt(lKneeKp.x, lKneeKp.y)
        val rKnee = Pt(rKneeKp.x, rKneeKp.y)

        val lShKp = kp["left_shoulder"] ?: return unknown()
        val rShKp = kp["right_shoulder"] ?: return unknown()
        val lSh = Pt(lShKp.x, lShKp.y)
        val rSh = Pt(rShKp.x, rShKp.y)
        val shoulders = mid(lSh, rSh)

        val keypointScores = listOf(
            lHipKp.score, rHipKp.score, lAnkKp.score, rAnkKp.score, noseKp.score,
            lKneeKp.score, rKneeKp.score, lShKp.score, rShKp.score
        )
        val minScore = keypointScores.minOrNull() ?: 0f
        val confidence = keypointScores.average().toFloat().coerceIn(0f, 1f)
        if (minScore < minRequiredKeypointScore) {
            return PoseClassification(Posture.UNKNOWN, confidence)
        }

        val lKneeAng = angleAt(lKnee, lHip, lAnk)
        val rKneeAng = angleAt(rKnee, rHip, rAnk)

        val backIncline = inclineFromVertical(shoulders, hips)

        val hipToAnk = ankles.y - hips.y
        val headToAnk = ankles.y - nose.y
        val headToAnkRef = calib.headToAnkle.coerceAtLeast(1f)
        // normalize hip height by calibrated standing height
        val hipRatio = (hipToAnk / headToAnkRef).coerceIn(0f, 1.5f)

        // Opportunistic calibration while person appears upright and visible.
        val looksStanding =
            lKneeAng > cfg.enter.qiyamKneeAngleMin &&
                rKneeAng > cfg.enter.qiyamKneeAngleMin &&
                backIncline < cfg.enter.qiyamBackInclineMax
        if (looksStanding) {
            calib.updateFromStanding(headToAnk)
        }

        // First try to keep the previous stable class with looser exit thresholds.
        val keepPrevious = when (previousStable) {
            Posture.QIYAM -> isQiyam(hipRatio, lKneeAng, rKneeAng, backIncline, cfg.exit)
            Posture.RUKU -> isRuku(hipRatio, backIncline, cfg.exit)
            Posture.JALSA -> isJalsa(hipRatio, lKneeAng, rKneeAng, nose.y, hips.y, cfg.exit)
            Posture.SUJOOD -> isSujood(hipRatio, nose.y, lKnee.y, rKnee.y, cfg.exit)
            Posture.UNKNOWN -> false
        }
        if (keepPrevious) return PoseClassification(previousStable, confidence)

        if (isQiyam(hipRatio, lKneeAng, rKneeAng, backIncline, cfg.enter)) {
            return PoseClassification(Posture.QIYAM, confidence)
        }
        if (isSujood(hipRatio, nose.y, lKnee.y, rKnee.y, cfg.enter)) {
            return PoseClassification(Posture.SUJOOD, confidence)
        }
        if (isJalsa(hipRatio, lKneeAng, rKneeAng, nose.y, hips.y, cfg.enter)) {
            return PoseClassification(Posture.JALSA, confidence)
        }
        if (isRuku(hipRatio, backIncline, cfg.enter)) {
            return PoseClassification(Posture.RUKU, confidence)
        }

        return PoseClassification(Posture.UNKNOWN, confidence)
    }

    private fun unknown(): PoseClassification = PoseClassification(Posture.UNKNOWN, 0f)

    private fun isQiyam(
        hipRatio: Float,
        lKneeAng: Float,
        rKneeAng: Float,
        backIncline: Float,
        t: PostureThresholds
    ): Boolean {
        return hipRatio > t.qiyamHipRatio &&
            lKneeAng > t.qiyamKneeAngleMin &&
            rKneeAng > t.qiyamKneeAngleMin &&
            backIncline < t.qiyamBackInclineMax
    }

    private fun isSujood(
        hipRatio: Float,
        noseY: Float,
        leftKneeY: Float,
        rightKneeY: Float,
        t: PostureThresholds
    ): Boolean {
        return hipRatio < t.sujoodHipRatioMax &&
            noseY > leftKneeY &&
            noseY > rightKneeY
    }

    private fun isJalsa(
        hipRatio: Float,
        lKneeAng: Float,
        rKneeAng: Float,
        noseY: Float,
        hipsY: Float,
        t: PostureThresholds
    ): Boolean {
        return hipRatio in t.jalsaHipRatioMin..t.jalsaHipRatioMax &&
            (lKneeAng < t.jalsaKneeAngleMax || rKneeAng < t.jalsaKneeAngleMax) &&
            noseY > hipsY
    }

    private fun isRuku(
        hipRatio: Float,
        backIncline: Float,
        t: PostureThresholds
    ): Boolean {
        return hipRatio in t.rukuHipRatioMin..t.rukuHipRatioMax &&
            backIncline in t.rukuBackInclineMin..t.rukuBackInclineMax
    }
}
