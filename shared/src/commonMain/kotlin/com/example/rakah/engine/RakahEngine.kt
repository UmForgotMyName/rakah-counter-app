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

    fun onFrame(frame: PoseFrame): RakahResult {
        val raw = classifier.classify(frame, previousStable = lastStableForClassifier)
        val stable = smoother.onRaw(raw.posture)
        lastStableForClassifier = stable
        return fsm.onStablePosture(stable, confidence = raw.confidence)
    }
}
