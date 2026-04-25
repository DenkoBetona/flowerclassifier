package com.denko.flowerclassifier

import android.content.Context
import android.graphics.Bitmap
import org.pytorch.executorch.EValue
import org.pytorch.executorch.Module
import org.pytorch.executorch.Tensor
import java.io.File
import java.io.FileOutputStream
import kotlin.math.exp

data class Prediction(val className: String, val confidence: Float)

class FlowerClassifier(context: Context) {

    private val module: Module
    private val classes = listOf("daisy", "dandelion", "rose", "sunflower", "tulip")

    private val displayNames = mapOf(
        "daisy" to "маргаритка",
        "dandelion" to "глухарче",
        "rose" to "роза",
        "sunflower" to "слънчоглед",
        "tulip" to "лале"
    )
    private val imageSize = 128
    private val mean = floatArrayOf(0.485f, 0.456f, 0.406f)
    private val std = floatArrayOf(0.229f, 0.224f, 0.225f)

    init {
        // Module.load иска filepath, не InputStream → копираме от assets до cache
        val modelFile = File(context.filesDir, "flowers.pte")
        if (!modelFile.exists()) {
            context.assets.open("flowers.pte").use { input ->
                FileOutputStream(modelFile).use { output -> input.copyTo(output) }
            }
        }
        module = Module.load(modelFile.absolutePath)
    }

    fun classify(bitmap: Bitmap): List<Prediction> {
        // 1. Resize до 128x128
        val resized = Bitmap.createScaledBitmap(bitmap, imageSize, imageSize, true)

        // 2. Bitmap → float[] в CHW layout с ImageNet normalization
        val pixels = IntArray(imageSize * imageSize)
        resized.getPixels(pixels, 0, imageSize, 0, 0, imageSize, imageSize)

        val floats = FloatArray(3 * imageSize * imageSize)
        val channelSize = imageSize * imageSize
        for (i in 0 until channelSize) {
            val px = pixels[i]
            val r = ((px shr 16) and 0xFF) / 255.0f
            val g = ((px shr 8) and 0xFF) / 255.0f
            val b = (px and 0xFF) / 255.0f
            floats[i] = (r - mean[0]) / std[0]
            floats[i + channelSize] = (g - mean[1]) / std[1]
            floats[i + 2 * channelSize] = (b - mean[2]) / std[2]
        }

        // 3. Inference
        val inputTensor = Tensor.fromBlob(
            floats,
            longArrayOf(1, 3, imageSize.toLong(), imageSize.toLong())
        )
        val output = module.forward(EValue.from(inputTensor))
        val logits = output[0].toTensor().dataAsFloatArray

        // 4. Softmax → probabilities
        val maxLogit = logits.max()
        val exps = logits.map { exp((it - maxLogit).toDouble()).toFloat() }
        val sum = exps.sum()
        val probs = exps.map { it / sum }

        // 5. Върни sorted top predictions
        return classes.mapIndexed { i, name ->
            Prediction(displayNames[name] ?: name, probs[i])
        }.sortedByDescending { it.confidence }
    }
}