// shared/src/commonMain/kotlin/com/example/rakah/fsm/PostureSmoother.kt
package com.example.rakah.fsm

import com.example.rakah.model.Posture

class PostureSmoother(private val confirmFrames: Int = 6) {
    private var pending: Posture = Posture.UNKNOWN
    private var stable: Posture = Posture.UNKNOWN
    private var count = 0

    fun onRaw(p: Posture): Posture {
        if (p == pending) count++ else { pending = p; count = 1 }
        if (count >= confirmFrames && pending != stable) stable = pending
        return stable
    }
}
