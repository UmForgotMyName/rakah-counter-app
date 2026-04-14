package com.example.rakah.engine

import com.example.rakah.classifier.DefaultPoseClassifier
import com.example.rakah.classifier.Thresholds
import com.example.rakah.fsm.PostureSmoother
import com.example.rakah.fsm.RakahFSM
import com.example.rakah.model.PoseFrame
import com.example.rakah.model.Posture
import com.example.rakah.model.PrayerType
import com.example.rakah.model.RakahResult
import com.example.rakah.model.StandingCalib

/**
 * Cross-platform shared entry point for prayer guidance.
 *
 * Android can keep feeding PoseFrame via [onFrame].
 * iOS can feed classified posture via [onClassifiedPosture] from any native pose detector.
 */
class PrayerSessionController(
    initialPrayer: PrayerType = PrayerType.FAJR,
    standingHeadToAnklePx: Float = 300f,
    confirmFrames: Int = 6,
    thresholds: Thresholds = Thresholds()
) {
    private val engine = RakahEngine(
        classifier = DefaultPoseClassifier(
            cfg = thresholds,
            calib = StandingCalib(initialHeadToAnkle = standingHeadToAnklePx)
        ),
        smoother = PostureSmoother(confirmFrames = confirmFrames),
        fsm = RakahFSM(initialPrayer = initialPrayer)
    )

    private var latest: RakahResult = engine.setPrayer(initialPrayer)

    fun current(): RakahResult = latest

    fun setPrayer(prayer: PrayerType): RakahResult {
        latest = engine.setPrayer(prayer)
        return latest
    }

    fun onFrame(frame: PoseFrame): RakahResult {
        latest = engine.onFrame(frame)
        return latest
    }

    fun onClassifiedPosture(posture: Posture, confidence: Float = 1f): RakahResult {
        latest = engine.onClassifiedPosture(posture, confidence)
        return latest
    }

    fun markSalaamRight(): RakahResult {
        latest = engine.markSalaamRight()
        return latest
    }

    fun markSalaamLeft(): RakahResult {
        latest = engine.markSalaamLeft()
        return latest
    }
}

/**
 * Swift-friendly facade that uses string inputs/outputs for easy direct iOS integration.
 */
class PrayerSessionFacade(initialPrayer: String = PrayerType.FAJR.name) {
    private val controller = PrayerSessionController(initialPrayer = parsePrayer(initialPrayer))
    private var latest = controller.current()

    fun availablePrayers(): List<String> = PrayerType.entries.map { it.name }

    fun availablePostures(): List<String> = Posture.entries
        .filter { it != Posture.UNKNOWN }
        .map { it.name }

    fun current(): GuidanceSnapshot = latest.toSnapshot()

    fun setPrayer(prayer: String): GuidanceSnapshot {
        latest = controller.setPrayer(parsePrayer(prayer))
        return latest.toSnapshot()
    }

    fun onDetectedPosture(posture: String, confidence: Float): GuidanceSnapshot {
        latest = controller.onClassifiedPosture(parsePosture(posture), confidence)
        return latest.toSnapshot()
    }

    fun markSalaamRight(): GuidanceSnapshot {
        latest = controller.markSalaamRight()
        return latest.toSnapshot()
    }

    fun markSalaamLeft(): GuidanceSnapshot {
        latest = controller.markSalaamLeft()
        return latest.toSnapshot()
    }

    private fun parsePrayer(value: String): PrayerType =
        PrayerType.entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: PrayerType.FAJR

    private fun parsePosture(value: String): Posture =
        Posture.entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: Posture.UNKNOWN
}

data class GuidanceSnapshot(
    val prayer: String,
    val currentStep: String,
    val previousStep: String?,
    val nextStep: String?,
    val currentRakah: Int,
    val totalRakah: Int,
    val completedRakah: Int,
    val posture: String,
    val confidence: Float,
    val lastEvent: String?
)

private fun RakahResult.toSnapshot(): GuidanceSnapshot =
    GuidanceSnapshot(
        prayer = prayer.name,
        currentStep = currentStep.name,
        previousStep = previousStep?.name,
        nextStep = nextStep?.name,
        currentRakah = currentRakah,
        totalRakah = totalRakah,
        completedRakah = rakahCount,
        posture = currentPosture.name,
        confidence = postureConfidence,
        lastEvent = lastEvent
    )

