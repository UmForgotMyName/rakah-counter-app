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
import com.example.rakah.android.ui.overlay.PoseOverlay
import com.example.rakah.android.ui.theme.AppTheme
import com.example.rakah.android.vm.RakahViewModel

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
                            analysisExecutor = vm.analysisExecutor
                        )

                        PoseOverlay(
                            width = ui.value.previewWidth,
                            height = ui.value.previewHeight,
                            keypoints = ui.value.keypointsForOverlay
                        )

                        Column(
                            Modifier
                                .fillMaxWidth()
                                .align(Alignment.TopCenter)
                                .padding(top = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Rakah: ${ui.value.rakah}",
                                style = MaterialTheme.typography.headlineMedium
                            )
                            Text(
                                text = "Posture: ${ui.value.posture}",
                                style = MaterialTheme.typography.bodyLarge
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
