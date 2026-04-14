// androidApp/src/main/java/com/example/rakah/android/camera/CameraBinding.kt
package com.example.rakah.android.camera

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.rakah.android.vm.MoveNetAnalyzer
import java.util.concurrent.Executor

@Composable
fun bindCamera(
    enabled: Boolean,
    lifecycleOwner: LifecycleOwner,
    analyzer: MoveNetAnalyzer,
    analysisExecutor: Executor,
    onPreviewMirroredChanged: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var previewView: PreviewView? by remember { mutableStateOf(null) }

    AndroidView(modifier = Modifier, factory = { ctx ->
        PreviewView(ctx).apply { this.scaleType = PreviewView.ScaleType.FILL_CENTER }
    }, update = { pv ->
        previewView = pv
    })

    DisposableEffect(enabled, lifecycleOwner, analyzer, analysisExecutor, previewView) {
        val surface = previewView
        if (!enabled || surface == null) {
            onDispose { }
        } else {
            val listener = Runnable {
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(surface.surfaceProvider)
                }

                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                    .build()
                    .also { it.setAnalyzer(analysisExecutor, analyzer) }

                val cameraSelector = if (cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                } else {
                    CameraSelector.DEFAULT_BACK_CAMERA
                }
                onPreviewMirroredChanged(cameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA)
                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, analysis)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            cameraProviderFuture.addListener(listener, ContextCompat.getMainExecutor(context))

            onDispose {
                runCatching {
                    cameraProviderFuture.get().unbindAll()
                }
            }
        }
    }
}
