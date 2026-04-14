// androidApp/src/main/java/com/example/rakah/android/MainActivity.kt
package com.example.rakah.android

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LifecycleOwner
import com.example.rakah.android.camera.bindCamera
import com.example.rakah.android.ui.guidance.StepGuidanceStrip
import com.example.rakah.android.ui.overlay.PoseOverlay
import com.example.rakah.android.ui.theme.AppTheme
import com.example.rakah.android.vm.RakahViewModel
import com.example.rakah.model.PrayerStep
import com.example.rakah.model.PrayerType

class MainActivity : ComponentActivity() {

    private val vm: RakahViewModel by viewModels()
    private val requestPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            vm.onCameraPermissionResult(granted)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestPermission.launch(Manifest.permission.CAMERA)

        setContent {
            AppTheme {
                val lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current
                val ui = vm.ui.collectAsState()
                val cameraEnabled = vm.cameraEnabled.collectAsState()

                Surface {
                    Box(Modifier.fillMaxSize()) {
                        bindCamera(
                            enabled = cameraEnabled.value,
                            lifecycleOwner = lifecycleOwner,
                            analyzer = vm.analyzer,
                            analysisExecutor = vm.analysisExecutor,
                            onPreviewMirroredChanged = vm::onPreviewMirrorChanged
                        )

                        PoseOverlay(
                            width = ui.value.previewWidth,
                            height = ui.value.previewHeight,
                            keypoints = ui.value.keypointsForOverlay,
                            mirrorX = ui.value.previewMirrored
                        )

                        Column(
                            Modifier
                                .fillMaxWidth()
                                .align(Alignment.TopCenter)
                                .padding(top = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            PrayerSelectionRow(
                                selected = ui.value.prayer,
                                onSelect = vm::selectPrayer
                            )

                            Spacer(Modifier.height(8.dp))

                            StepGuidanceStrip(
                                previous = ui.value.previousStep,
                                current = ui.value.currentStep,
                                next = ui.value.nextStep,
                                prayer = ui.value.prayer,
                                currentRakah = ui.value.currentRakah,
                                totalRakah = ui.value.totalRakah
                            )

                            Spacer(Modifier.height(8.dp))

                            Text(
                                text = "Prayer: ${ui.value.prayer.label}",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Rakah: ${ui.value.currentRakah}/${ui.value.totalRakah} (completed ${ui.value.rakah})",
                                style = MaterialTheme.typography.headlineMedium
                            )
                            Text(
                                text = "Posture: ${ui.value.posture}",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "Current Step: ${ui.value.currentStep.pretty()}",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "Previous: ${ui.value.previousStep.prettyOrDash()}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Next: ${ui.value.nextStep.prettyOrDash()}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Confidence: ${(ui.value.postureConfidence * 100).toInt()}%",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            if (ui.value.lastEvent != null) {
                                Text(
                                    text = ui.value.lastEvent!!,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }

                            if (ui.value.nextStep == PrayerStep.SALAAM_RIGHT) {
                                Spacer(Modifier.height(8.dp))
                                Button(onClick = vm::onSalaamRight) {
                                    Text("Confirm Salaam Right")
                                }
                            }
                            if (ui.value.nextStep == PrayerStep.SALAAM_LEFT || ui.value.currentStep == PrayerStep.SALAAM_RIGHT) {
                                Spacer(Modifier.height(8.dp))
                                Button(onClick = vm::onSalaamLeft) {
                                    Text("Confirm Salaam Left")
                                }
                            }
                        }

                        if (!cameraEnabled.value) {
                            Text(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 32.dp),
                                text = "Camera permission is required",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PrayerSelectionRow(
    selected: PrayerType,
    onSelect: (PrayerType) -> Unit
) {
    val firstRow = listOf(PrayerType.FAJR, PrayerType.DHUHR, PrayerType.ASR)
    val secondRow = listOf(PrayerType.MAGHRIB, PrayerType.ISHA)

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        firstRow.forEach { prayer ->
            FilterChip(
                selected = selected == prayer,
                onClick = { onSelect(prayer) },
                label = { Text(prayer.label) }
            )
        }
    }

    Spacer(Modifier.height(6.dp))

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        secondRow.forEach { prayer ->
            FilterChip(
                selected = selected == prayer,
                onClick = { onSelect(prayer) },
                label = { Text(prayer.label) }
            )
        }
    }
}

private fun PrayerStep.pretty(): String =
    name.lowercase().split("_").joinToString(" ") { token -> token.replaceFirstChar { it.uppercase() } }

private fun PrayerStep?.prettyOrDash(): String = this?.pretty() ?: "-"
