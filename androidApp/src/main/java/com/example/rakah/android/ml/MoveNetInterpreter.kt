// androidApp/src/main/java/com/example/rakah/android/ml/MoveNetInterpreter.kt
package com.example.rakah.android.ml

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.support.common.FileUtil
import java.nio.ByteBuffer
import java.nio.ByteOrder

data class MoveNetOutput(
    val keypoints: Array<FloatArray>,
    val sourceWidth: Int,
    val sourceHeight: Int,
    val cropLeft: Int,
    val cropTop: Int,
    val cropSize: Int
)

class MoveNetInterpreter(
    context: Context,
    private val modelAssetName: String = "movenet_lightning_singlepose_f32.tflite",
    private val inputSize: Int = 192
) {
    private val interpreter: Interpreter
    private var gpuDelegate: GpuDelegate? = null

    private val inputBuffer: ByteBuffer =
        ByteBuffer.allocateDirect(1 * inputSize * inputSize * 3 * 4).order(ByteOrder.nativeOrder())

    private val output3d: Array<Array<FloatArray>> =
        Array(1) { Array(17) { FloatArray(3) } }
    private val output4d: Array<Array<Array<FloatArray>>> =
        Array(1) { Array(1) { Array(17) { FloatArray(3) } } }
    private val use4dOutput: Boolean

    init {
        val options = Interpreter.Options()
        val compat = CompatibilityList()
        if (compat.isDelegateSupportedOnThisDevice) {
            gpuDelegate = GpuDelegate()
            options.addDelegate(gpuDelegate)
        } else {
            options.setNumThreads(4)
            options.setUseXNNPACK(true)
        }
        val model = FileUtil.loadMappedFile(context, modelAssetName)
        interpreter = Interpreter(model, options)
        val shape = interpreter.getOutputTensor(0).shape()
        use4dOutput = shape.contentEquals(intArrayOf(1, 1, 17, 3))
        val supports3d = shape.contentEquals(intArrayOf(1, 17, 3))
        require(use4dOutput || supports3d) {
            "Unsupported MoveNet output shape: ${shape.joinToString(prefix = "[", postfix = "]")}"
        }
    }

    fun run(bitmap: Bitmap): MoveNetOutput {
        val crop = cropCenter(bitmap)
        val resized = Bitmap.createScaledBitmap(crop.bitmap, inputSize, inputSize, true)
        bitmapToBuffer(resized, inputBuffer)
        val keypoints = if (use4dOutput) {
            interpreter.run(inputBuffer, output4d)
            output4d[0][0]
        } else {
            interpreter.run(inputBuffer, output3d)
            output3d[0]
        }
        return MoveNetOutput(
            keypoints = keypoints,
            sourceWidth = bitmap.width,
            sourceHeight = bitmap.height,
            cropLeft = crop.left,
            cropTop = crop.top,
            cropSize = crop.size
        )
    }

    fun close() {
        interpreter.close()
        gpuDelegate?.close()
        gpuDelegate = null
    }

    private data class CenterCrop(val bitmap: Bitmap, val left: Int, val top: Int, val size: Int)

    private fun cropCenter(src: Bitmap): CenterCrop {
        val w = src.width
        val h = src.height
        val size = minOf(w, h)
        val x = (w - size) / 2
        val y = (h - size) / 2
        return CenterCrop(
            bitmap = Bitmap.createBitmap(src, x, y, size, size),
            left = x,
            top = y,
            size = size
        )
    }

    private fun bitmapToBuffer(bmp: Bitmap, buffer: ByteBuffer) {
        buffer.rewind()
        val pixels = IntArray(bmp.width * bmp.height)
        bmp.getPixels(pixels, 0, bmp.width, 0, 0, bmp.width, bmp.height)
        var idx = 0
        for (y in 0 until bmp.height) {
            for (x in 0 until bmp.width) {
                val p = pixels[idx++]
                buffer.putFloat((p shr 16 and 0xFF) / 255f)
                buffer.putFloat((p shr 8 and 0xFF) / 255f)
                buffer.putFloat((p and 0xFF) / 255f)
            }
        }
        buffer.rewind()
    }
}
