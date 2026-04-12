// shared/src/commonMain/kotlin/com/example/rakah/fsm/RakahFSM.kt
package com.example.rakah.fsm

import com.example.rakah.model.*

class RakahFSM {
    private var seenFirstSujood = false
    private var seenJalsaAfterSujood = false
    private var rakah = 0
    private var lastStable = Posture.UNKNOWN

    fun onStablePosture(p: Posture, confidence: Float = 1f): RakahResult {
        var event: String? = null
        if (p != lastStable) {
            when (p) {
                Posture.SUJOOD -> {
                    if (!seenFirstSujood) {
                        seenFirstSujood = true
                        seenJalsaAfterSujood = false
                    }
                }
                Posture.JALSA -> if (seenFirstSujood) seenJalsaAfterSujood = true
                Posture.QIYAM -> {
                    if (seenFirstSujood && seenJalsaAfterSujood) {
                        rakah += 1
                        event = "Rakah++ -> $rakah"
                    }
                    seenFirstSujood = false
                    seenJalsaAfterSujood = false
                }
                else -> Unit
            }
            lastStable = p
        }
        return RakahResult(
            currentPosture = lastStable,
            rakahCount = rakah,
            postureConfidence = confidence.coerceIn(0f, 1f),
            lastEvent = event
        )
    }
}
