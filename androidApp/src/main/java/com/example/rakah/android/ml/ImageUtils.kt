// androidApp/src/main/java/com/example/rakah/android/ml/ImageUtils.kt
package com.example.rakah.android.ml

import android.graphics.Bitmap
import android.graphics.ImageFormat
import androidx.camera.core.ImageProxy

// CameraX can provide RGBA_8888 for ImageAnalysis. We parse it directly to avoid YUV->JPEG conversion.
fun ImageProxy.toBitmap(): Bitmap {
    require(format == ImageFormat.RGBA_8888) {
        "Expected RGBA_8888 analysis format. Got: $format"
    }
    val plane = planes.first()
    val buffer = plane.buffer
    val rowStride = plane.rowStride
    val pixelStride = plane.pixelStride
    val out = IntArray(width * height)
    var outIndex = 0
    for (y in 0 until height) {
        val rowStart = y * rowStride
        for (x in 0 until width) {
            val i = rowStart + x * pixelStride
            val a = buffer.get(i).toInt() and 0xFF
            val r = buffer.get(i + 1).toInt() and 0xFF
            val g = buffer.get(i + 2).toInt() and 0xFF
            val b = buffer.get(i + 3).toInt() and 0xFF
            out[outIndex++] = (a shl 24) or (r shl 16) or (g shl 8) or b
        }
    }
    return Bitmap.createBitmap(out, width, height, Bitmap.Config.ARGB_8888)
}
