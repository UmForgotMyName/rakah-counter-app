// shared/src/commonMain/kotlin/com/example/rakah/fsm/RakahFSM.kt
package com.example.rakah.fsm

import com.example.rakah.model.*

class RakahFSM(initialPrayer: PrayerType = PrayerType.FAJR) {
    private var prayer: PrayerType = initialPrayer
    private var spec: PrayerSpec = prayer.spec()

    private var completedRakah: Int = 0
    private var lastStablePosture: Posture = Posture.UNKNOWN
    private var currentStep: PrayerStep = PrayerStep.QIYAM
    private var previousStep: PrayerStep? = null
    private var hasStarted = false

    fun setPrayer(prayerType: PrayerType): RakahResult {
        prayer = prayerType
        spec = prayer.spec()

        completedRakah = 0
        lastStablePosture = Posture.UNKNOWN
        currentStep = PrayerStep.QIYAM
        previousStep = null
        hasStarted = true

        return snapshot(confidence = 1f, event = "Prayer set to ${prayer.label}")
    }

    fun markSalaamRight(): RakahResult {
        val event = if (expectedNextStep() == PrayerStep.SALAAM_RIGHT) {
            val transitionEvent = advanceIfExpected(PrayerStep.SALAAM_RIGHT)
            transitionEvent ?: "Salaam right"
        } else {
            null
        }
        return snapshot(confidence = 1f, event = event)
    }

    fun markSalaamLeft(): RakahResult {
        var event: String? = null
        if (expectedNextStep() == PrayerStep.SALAAM_LEFT) {
            advanceIfExpected(PrayerStep.SALAAM_LEFT)
            previousStep = currentStep
            currentStep = PrayerStep.DONE
            event = "Prayer complete"
        }
        return snapshot(confidence = 1f, event = event)
    }

    fun onStablePosture(p: Posture, confidence: Float = 1f): RakahResult {
        if (p == Posture.UNKNOWN) return snapshot(confidence.coerceIn(0f, 1f), null)

        lastStablePosture = p
        val candidate = mapPostureToStep(p) ?: return snapshot(confidence.coerceIn(0f, 1f), null)

        val event = if (!hasStarted) {
            hasStarted = true
            currentStep = candidate
            null
        } else {
            advanceIfExpected(candidate)
        }

        return snapshot(confidence.coerceIn(0f, 1f), event)
    }

    private fun advanceIfExpected(candidate: PrayerStep): String? {
        if (candidate == currentStep) return null
        val expected = expectedNextStep() ?: return null
        if (candidate != expected) return null

        var event: String? = null
        if (currentStep == PrayerStep.SECOND_SUJOOD &&
            (candidate == PrayerStep.QIYAM || candidate == PrayerStep.TASHAHHUD)
        ) {
            if (completedRakah < spec.totalRakah) {
                completedRakah += 1
                event = "Rakah++ -> $completedRakah/${spec.totalRakah}"
            }
        }

        previousStep = currentStep
        currentStep = candidate
        return event
    }

    private fun mapPostureToStep(p: Posture): PrayerStep? = when (p) {
        Posture.QIYAM -> if (currentStep == PrayerStep.RUKU) PrayerStep.QAWMAH else PrayerStep.QIYAM
        Posture.RUKU -> PrayerStep.RUKU
        Posture.SUJOOD -> {
            if (currentStep == PrayerStep.JALSA_BETWEEN_SUJOOD || currentStep == PrayerStep.SECOND_SUJOOD) {
                PrayerStep.SECOND_SUJOOD
            } else {
                PrayerStep.SUJOOD
            }
        }
        Posture.JALSA -> {
            when {
                currentStep == PrayerStep.TASHAHHUD -> PrayerStep.TASHAHHUD
                currentStep == PrayerStep.SECOND_SUJOOD && shouldEnterTashahhudAfterThisRakah() -> {
                    PrayerStep.TASHAHHUD
                }
                else -> PrayerStep.JALSA_BETWEEN_SUJOOD
            }
        }
        Posture.UNKNOWN -> null
    }

    private fun shouldEnterTashahhudAfterThisRakah(): Boolean {
        val upcomingCompleted = (completedRakah + 1).coerceAtMost(spec.totalRakah)
        return upcomingCompleted in spec.tashahhudAfterRakah
    }

    private fun expectedNextStep(): PrayerStep? = when (currentStep) {
        PrayerStep.QIYAM -> PrayerStep.RUKU
        PrayerStep.RUKU -> PrayerStep.QAWMAH
        PrayerStep.QAWMAH -> PrayerStep.SUJOOD
        PrayerStep.SUJOOD -> PrayerStep.JALSA_BETWEEN_SUJOOD
        PrayerStep.JALSA_BETWEEN_SUJOOD -> PrayerStep.SECOND_SUJOOD
        PrayerStep.SECOND_SUJOOD -> {
            val upcomingCompleted = (completedRakah + 1).coerceAtMost(spec.totalRakah)
            if (upcomingCompleted in spec.tashahhudAfterRakah) PrayerStep.TASHAHHUD else PrayerStep.QIYAM
        }
        PrayerStep.TASHAHHUD -> {
            if (completedRakah >= spec.totalRakah) PrayerStep.SALAAM_RIGHT else PrayerStep.QIYAM
        }
        PrayerStep.SALAAM_RIGHT -> PrayerStep.SALAAM_LEFT
        PrayerStep.SALAAM_LEFT -> PrayerStep.DONE
        PrayerStep.DONE -> null
    }

    private fun snapshot(confidence: Float, event: String?): RakahResult {
        val activeRakah = when (currentStep) {
            PrayerStep.SALAAM_RIGHT, PrayerStep.SALAAM_LEFT, PrayerStep.DONE -> spec.totalRakah
            else -> (completedRakah + 1).coerceAtMost(spec.totalRakah)
        }

        return RakahResult(
            currentPosture = lastStablePosture,
            rakahCount = completedRakah,
            postureConfidence = confidence,
            lastEvent = event,
            prayer = prayer,
            totalRakah = spec.totalRakah,
            currentRakah = activeRakah,
            currentStep = currentStep,
            previousStep = previousStep,
            nextStep = expectedNextStep()
        )
    }
}
