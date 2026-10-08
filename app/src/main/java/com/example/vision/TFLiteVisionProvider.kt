package com.example.vision

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.example.model.PlantAnalysisResult
import com.example.model.Prediction
import com.example.util.PlantLabelParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import kotlin.math.exp
import kotlin.math.max

class TFLiteVisionProvider : VisionProvider {

    override val name: String = "PlantVillage MobileNet TFLite"

    companion object {
        private const val TAG = "TFLiteVisionProvider"
        private const val MODEL_FILE = "plant_disease_model.tflite"
        private const val INPUT_SIZE = 224
        private const val NUM_CLASSES = 38
        private const val TOP_K = 3
    }

    private var interpreter: Interpreter? = null

    private fun getInterpreter(context: Context): Interpreter {
        return interpreter ?: synchronized(this) {
            interpreter ?: run {
                val fileDescriptor = context.assets.openFd(MODEL_FILE)
                val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
                val fileChannel = inputStream.channel
                val startOffset = fileDescriptor.startOffset
                val declaredLength = fileDescriptor.declaredLength
                val modelBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
                
                val options = Interpreter.Options().apply {
                    setNumThreads(4)
                }
                Interpreter(modelBuffer, options).also { interpreter = it }
            }
        }
    }

    override suspend fun analyze(context: Context, imageUri: Uri): PlantAnalysisResult = withContext(Dispatchers.IO) {
        Log.i(TAG, "Analysis request started for URI: $imageUri")

        // 1. Image preprocessing and validation
        val bitmap = decodeSampledBitmapFromUri(context, imageUri, 1024, 1024)
            ?: throw IllegalArgumentException("Could not decode image from URI.")
        Log.i(TAG, "Image prepared. Original decoded size: ${bitmap.width}x${bitmap.height}")

        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true)
        val inputBuffer = convertBitmapToByteBuffer(resizedBitmap)

        // 2. Prepare output tensor [1, NUM_CLASSES]
        val outputProbabilities = Array(1) { FloatArray(NUM_CLASSES) }

        // 3. Run inference
        Log.i(TAG, "Request sent to pre-trained vision model")
        val interp = getInterpreter(context)
        interp.run(inputBuffer, outputProbabilities)
        Log.i(TAG, "Response received from model")

        val rawScores = outputProbabilities[0]

        // 4. Calculate calibrated probabilities (Softmax if raw logits)
        val probabilities = calculateProbabilities(rawScores)

        // 5. Rank predictions
        val rankedIndices = probabilities.indices
            .sortedByDescending { probabilities[it] }

        val topKIndices = rankedIndices.take(TOP_K)

        val topPredictions = topKIndices.map { index ->
            val rawClass = PlantLabelParser.PLANT_VILLAGE_CLASSES.getOrElse(index) { "Unknown" }
            val formattedName = PlantLabelParser.formatPredictionName(rawClass)
            val score = probabilities[index]
            Prediction(name = formattedName, confidence = score)
        }

        val topIndex = rankedIndices.firstOrNull() ?: 0
        val topScore = probabilities.getOrElse(topIndex) { 0f }
        val topRawLabel = PlantLabelParser.PLANT_VILLAGE_CLASSES.getOrElse(topIndex) { "Unknown" }

        Log.i(TAG, "Prediction parsed: $topRawLabel with score $topScore")

        // 6. Parse structured plant and disease information
        val parsed = PlantLabelParser.parse(topRawLabel)

        val isLowConfidence = topScore < PlantAnalysisResult.LOW_CONFIDENCE_THRESHOLD

        PlantAnalysisResult(
            plant = parsed.plant,
            disease = parsed.disease,
            confidence = topScore,
            predictions = topPredictions,
            explanation = parsed.explanation,
            advice = parsed.advice,
            isLowConfidence = isLowConfidence,
            confidenceLabel = if (isLowConfidence) "Low confidence" else "High confidence"
        )
    }

    private fun calculateProbabilities(scores: FloatArray): FloatArray {
        var sum = 0.0
        var allPositive = true
        for (s in scores) {
            sum += s
            if (s < 0) allPositive = false
        }
        // If already normalized probabilities summing to approximately 1.0
        if (allPositive && sum in 0.95..1.05) {
            return scores
        }

        // Apply Softmax with numerical stability
        var maxVal = Float.NEGATIVE_INFINITY
        for (s in scores) {
            if (s > maxVal) maxVal = s
        }

        val expValues = FloatArray(scores.size)
        var expSum = 0f
        for (i in scores.indices) {
            val v = exp(scores[i] - maxVal)
            expValues[i] = v
            expSum += v
        }

        for (i in expValues.indices) {
            expValues[i] = if (expSum > 0f) expValues[i] / expSum else 0f
        }
        return expValues
    }

    private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(4 * INPUT_SIZE * INPUT_SIZE * 3)
        byteBuffer.order(ByteOrder.nativeOrder())

        val intValues = IntArray(INPUT_SIZE * INPUT_SIZE)
        bitmap.getPixels(intValues, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)

        var pixel = 0
        for (i in 0 until INPUT_SIZE) {
            for (j in 0 until INPUT_SIZE) {
                val value = intValues[pixel++]
                // Normalize RGB pixels to [0.0f, 1.0f]
                val r = ((value shr 16) and 0xFF) / 255.0f
                val g = ((value shr 8) and 0xFF) / 255.0f
                val b = (value and 0xFF) / 255.0f

                byteBuffer.putFloat(r)
                byteBuffer.putFloat(g)
                byteBuffer.putFloat(b)
            }
        }
        return byteBuffer
    }

    private fun decodeSampledBitmapFromUri(
        context: Context,
        uri: Uri,
        reqWidth: Int,
        reqHeight: Int
    ): Bitmap? {
        var stream: InputStream? = null
        return try {
            stream = context.contentResolver.openInputStream(uri)
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(stream, null, options)
            stream?.close()

            // Calculate inSampleSize
            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
            options.inJustDecodeBounds = false

            stream = context.contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(stream, null, options)
        } catch (e: Exception) {
            Log.e(TAG, "Error decoding image from URI", e)
            null
        } finally {
            stream?.close()
        }
    }

    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return max(1, inSampleSize)
    }
}
