// androidApp/src/main/java/com/example/rakah/android/vm/RakahViewModel.kt
package com.example.rakah.android.vm

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.lifecycle.AndroidViewModel
import com.example.rakah.android.ml.MoveNetInterpreter
import com.example.rakah.android.ml.toBitmap
import com.example.rakah.classifier.DefaultPoseClassifier
import com.example.rakah.classifier.Thresholds
import com.example.rakah.engine.RakahEngine
import com.example.rakah.fsm.PostureSmoother
import com.example.rakah.fsm.RakahFSM
import com.example.rakah.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

data class UiState(
    val rakah: Int = 0,
    val posture: Posture = Posture.UNKNOWN,
    val postureConfidence: Float = 0f,
    val lastEvent: String? = null,
    val previewWidth: Int = 0,
    val previewHeight: Int = 0,
    val keypointsForOverlay: Map<String, Keypoint> = emptyMap()
)

class RakahViewModel(app: Application) : AndroidViewModel(app) {

    val analysisExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    private var moveNet: MoveNetInterpreter? = null
    private val standingCalib = StandingCalib(initialHeadToAnkle = 300f)
    private val engine = RakahEngine(
        classifier = DefaultPoseClassifier(Thresholds(), standingCalib),
        smoother = PostureSmoother(confirmFrames = 6),
        fsm = RakahFSM()
    )

    private val _cameraEnabled = MutableStateFlow(false)
    val cameraEnabled: StateFlow<Boolean> = _cameraEnabled

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui

    val analyzer = MoveNetAnalyzer { bmp, rotationDegrees ->
        processFrame(bmp, rotationDegrees)
    }

    fun onCameraPermissionResult(granted: Boolean) {
        _cameraEnabled.value = granted
    }

    override fun onCleared() {
        super.onCleared()
        analysisExecutor.execute {
            moveNet?.close()
            moveNet = null
        }
        analysisExecutor.shutdown()
    }

    private fun processFrame(rgb: Bitmap, rotationDegrees: Int) {
        // Rotate to camera-upright using analyzer-provided rotation
        val upright = if (rotationDegrees % 360 != 0) rgb.rotate(rotationDegrees) else rgb

        // Inference: keypoints are normalized to the center crop used by MoveNet.
        val output = interpreter().run(upright)

        val w = upright.width
        val h = upright.height

        val keypoints = mutableMapOf<String, Keypoint>()
        for (i in 0 until 17) {
            val y = output.cropTop + output.keypoints[i][0].coerceIn(0f, 1f) * output.cropSize
            val x = output.cropLeft + output.keypoints[i][1].coerceIn(0f, 1f) * output.cropSize
            val s = output.keypoints[i][2]
            keypoints[KEYPOINT_NAMES[i]] = Keypoint(KEYPOINT_NAMES[i], x, y, s)
        }

        val frame = PoseFrame(
            keypoints = keypoints,
            width = w,
            height = h,
            timestampMs = System.currentTimeMillis()
        )

        val result = engine.onFrame(frame)

        _ui.value = _ui.value.copy(
            rakah = result.rakahCount,
            posture = result.currentPosture,
            postureConfidence = result.postureConfidence,
            lastEvent = result.lastEvent,
            previewWidth = w,
            previewHeight = h,
            keypointsForOverlay = keypoints
        )
    }

    @Synchronized
    private fun interpreter(): MoveNetInterpreter {
        val existing = moveNet
        if (existing != null) return existing
        val created = MoveNetInterpreter(getApplication())
        moveNet = created
        return created
    }

    companion object {
        private val KEYPOINT_NAMES = arrayOf(
            "nose", "left_eye", "right_eye", "left_ear", "right_ear",
            "left_shoulder", "right_shoulder", "left_elbow", "right_elbow",
            "left_wrist", "right_wrist", "left_hip", "right_hip",
            "left_knee", "right_knee", "left_ankle", "right_ankle"
        )
    }
}

// Simple bitmap rotate helper
private fun Bitmap.rotate(degrees: Int): Bitmap {
    val m = Matrix().apply { postRotate(degrees.toFloat()) }
    return Bitmap.createBitmap(this, 0, 0, width, height, m, true)
}

class MoveNetAnalyzer(
    private val onBitmapReady: (Bitmap, Int) -> Unit
) : ImageAnalysis.Analyzer {
    override fun analyze(image: ImageProxy) {
        try {
            val bmp = image.toBitmap()
            onBitmapReady(bmp, image.imageInfo.rotationDegrees)
        } finally {
            image.close()
        }
    }
}
