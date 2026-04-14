// androidApp/src/main/java/com/example/rakah/android/ui/overlay/PoseOverlay.kt
package com.example.rakah.android.ui.overlay

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.example.rakah.model.Keypoint

private val EDGES = listOf(
    "left_shoulder" to "right_shoulder",
    "left_hip" to "right_hip",
    "left_shoulder" to "left_elbow",
    "left_elbow" to "left_wrist",
    "right_shoulder" to "right_elbow",
    "right_elbow" to "right_wrist",
    "left_hip" to "left_knee",
    "left_knee" to "left_ankle",
    "right_hip" to "right_knee",
    "right_knee" to "right_ankle",
    "left_shoulder" to "left_hip",
    "right_shoulder" to "right_hip"
)

@Composable
fun PoseOverlay(
    width: Int,
    height: Int,
    keypoints: Map<String, Keypoint>,
    mirrorX: Boolean = false
) {
    Canvas(Modifier.fillMaxSize()) {
        // scale input pixel coords (width x height) to current canvas size
        val sx = if (width > 0) size.width / width else 1f
        val sy = if (height > 0) size.height / height else 1f
        fun mapX(rawX: Float): Float {
            val x = rawX * sx
            return if (mirrorX) size.width - x else x
        }

        fun pt(name: String): Offset? =
            keypoints[name]?.let { Offset(mapX(it.x), it.y * sy) }

        // bones
        for ((a, b) in EDGES) {
            val pa = pt(a); val pb = pt(b)
            if (pa != null && pb != null) {
                drawLine(
                    color = Color.Cyan,
                    start = pa,
                    end = pb,
                    strokeWidth = 6f
                )
            }
        }

        // joints
        keypoints.values.forEach { kp ->
            drawCircle(
                color = Color.Magenta,
                center = Offset(mapX(kp.x), kp.y * sy),
                radius = 8f
            )
        }
    }
}
