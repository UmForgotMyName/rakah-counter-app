// shared/src/commonMain/kotlin/com/example/rakah/engine/RakahEngine.kt
package com.example.rakah.engine

import com.example.rakah.classifier.DefaultPoseClassifier
import com.example.rakah.fsm.PostureSmoother
import com.example.rakah.fsm.RakahFSM
import com.example.rakah.model.*

class RakahEngine(
    private val classifier: DefaultPoseClassifier,
    private val smoother: PostureSmoother,
    private val fsm: RakahFSM
) {
    private var lastStableForClassifier: Posture = Posture.UNKNOWN
    private var lastResult: RakahResult = fsm.setPrayer(PrayerType.FAJR)

    fun setPrayer(prayer: PrayerType): RakahResult {
        lastStableForClassifier = Posture.UNKNOWN
        lastResult = fsm.setPrayer(prayer)
        return lastResult
    }

    fun markSalaamRight(): RakahResult {
        lastResult = fsm.markSalaamRight()
        return lastResult
    }

    fun markSalaamLeft(): RakahResult {
        lastResult = fsm.markSalaamLeft()
        return lastResult
    }

    fun onFrame(frame: PoseFrame): RakahResult {
        val raw = classifier.classify(frame, previousStable = lastStableForClassifier)
        val stable = smoother.onRaw(raw.posture)
        lastStableForClassifier = stable
        lastResult = fsm.onStablePosture(stable, confidence = raw.confidence)
        return lastResult
    }
}
