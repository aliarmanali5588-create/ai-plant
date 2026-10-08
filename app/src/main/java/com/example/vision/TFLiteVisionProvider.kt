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
                Log.d(TAG, "[PC][5] Opening model file: $MODEL_FILE")
                val fileDescriptor = context.assets.openFd(MODEL_FILE)
                val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
                val fileChannel = inputStream.channel
                val startOffset = fileDescriptor.startOffset
                val declaredLength = fileDescriptor.declaredLength
                val modelBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
                
                val options = Interpreter.Options().apply {
                    setNumThreads(4)
                }
                val interp = Interpreter(modelBuffer, options)
                
                // Verify input tensor
                val inputTensor = interp.getInputTensor(0)
                Log.d(TAG, "[PC][5] Model Input: shape=${inputTensor.shape().contentToString()}, type=${inputTensor.dataType()}")
                val outputTensor = interp.getOutputTensor(0)
                Log.d(TAG, "[PC][5] Model Output: shape=${outputTensor.shape().contentToString()}, type=${outputTensor.dataType()}")
                
                interp.also { interpreter = it }
            }
        }
    }

    override suspend fun analyze(context: Context, imageUri: Uri): PlantAnalysisResult = withContext(Dispatchers.IO) {
        Log.d(TAG, "[PC][1] IMAGE_URI: $imageUri (scheme: ${imageUri.scheme})")
        Log.d(TAG, "[PC][ENV] PackageName: ${context.packageName}")
        
        var currentStage = "INIT"
        try {
            // Check file size
            currentStage = "FILE_SIZE_CHECK"
            context.contentResolver.openAssetFileDescriptor(imageUri, "r")?.use { afd ->
                Log.d(TAG, "[PC][1] URI File Size: ${afd.length} bytes")
            } ?: Log.w(TAG, "[PC][1] Could not determine file size for URI")

            // 1. Image preprocessing and validation
            currentStage = "BITMAP_DECODE"
            Log.d(TAG, "[PC][2] BITMAP_DECODE START")
            val bitmap = decodeSampledBitmapFromUri(context, imageUri, 1024, 1024)
                ?: throw IllegalArgumentException("BitmapFactory returned null for URI: $imageUri")
            Log.d(TAG, "[PC][3] BITMAP_SIZE: ${bitmap.width}x${bitmap.height}, config=${bitmap.config}, byteCount=${bitmap.byteCount}")

            currentStage = "MODEL_LOAD"
            Log.d(TAG, "[PC][5] MODEL_LOAD START")
            val interp = getInterpreter(context)
            Log.d(TAG, "[PC][5] MODEL_LOAD COMPLETE")

            currentStage = "PREPROCESS"
            Log.d(TAG, "[PC][4] PREPROCESS START")
            val resizedBitmap = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true)
            val inputBuffer = convertBitmapToByteBuffer(resizedBitmap, interp)
            Log.d(TAG, "[PC][4] PREPROCESS COMPLETE: inputBuffer capacity=${inputBuffer.capacity()}")

            // 2. Prepare output tensor [1, NUM_CLASSES]
            val outputProbabilities = Array(1) { FloatArray(NUM_CLASSES) }

            currentStage = "INFERENCE"
            Log.d(TAG, "[PC][7] INFERENCE_START")
            synchronized(interp) {
                interp.run(inputBuffer, outputProbabilities)
            }
            Log.d(TAG, "[PC][8] INFERENCE_END")

            currentStage = "OUTPUT_PROCESSING"
            val rawScores = outputProbabilities[0]
            Log.d(TAG, "[PC][9] OUTPUT_TENSOR RECEIVED: rawScores size=${rawScores.size}")

            // 4. Calculate calibrated probabilities (Softmax if raw logits)
            val probabilities = calculateProbabilities(rawScores)

            // 5. Rank predictions
            currentStage = "RANKING"
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

            Log.d(TAG, "[PC][10] PREDICTION: $topRawLabel ($topScore)")

            // 6. Parse structured plant and disease information
            currentStage = "PARSING"
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
        } catch (e: Exception) {
            Log.e(TAG, "[PC][ERROR] Stage $currentStage failed")
            Log.e(TAG, "Exception: ${e.javaClass.name}: ${e.message}")
            Log.e(TAG, "Stack trace: ${Log.getStackTraceString(e)}")
            // Throw a wrapped exception with stage info
            throw RuntimeException("Stage $currentStage failed: ${e.message}", e)
        }
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

    private fun convertBitmapToByteBuffer(bitmap: Bitmap, interp: Interpreter): ByteBuffer {
        val inputTensor = interp.getInputTensor(0)
        val shape = inputTensor.shape() // e.g., [1, 224, 224, 3]
        val dataType = inputTensor.dataType()
        
        val batchSize = if (shape.isNotEmpty()) shape[0] else 1
        val height = if (shape.size >= 2) shape[1] else INPUT_SIZE
        val width = if (shape.size >= 3) shape[2] else INPUT_SIZE
        val channels = if (shape.size >= 4) shape[3] else 3
        
        val byteSize = when (dataType) {
            org.tensorflow.lite.DataType.FLOAT32 -> 4
            org.tensorflow.lite.DataType.INT8, org.tensorflow.lite.DataType.UINT8 -> 1
            else -> 4 // Default to 4
        }
        
        Log.d(TAG, "[PC][6] INPUT_TENSOR DATA: batch=$batchSize, h=$height, w=$width, c=$channels, type=$dataType, byteSize=$byteSize")
        
        val byteBuffer = ByteBuffer.allocateDirect(batchSize * height * width * channels * byteSize)
        byteBuffer.order(ByteOrder.nativeOrder())

        val intValues = IntArray(width * height)
        bitmap.getPixels(intValues, 0, width, 0, 0, width, height)

        var pixel = 0
        for (i in 0 until height) {
            for (j in 0 until width) {
                val value = intValues[pixel++]
                val r = ((value shr 16) and 0xFF)
                val g = ((value shr 8) and 0xFF)
                val b = (value and 0xFF)

                if (dataType == org.tensorflow.lite.DataType.FLOAT32) {
                    byteBuffer.putFloat(r / 255.0f)
                    byteBuffer.putFloat(g / 255.0f)
                    byteBuffer.putFloat(b / 255.0f)
                } else {
                    byteBuffer.put(r.toByte())
                    byteBuffer.put(g.toByte())
                    byteBuffer.put(b.toByte())
                }
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
            Log.d(TAG, "[PC][2] Opening input stream for URI: $uri")
            stream = context.contentResolver.openInputStream(uri)
            if (stream == null) {
                Log.e(TAG, "[PC][2] openInputStream returned null")
                return null
            }
            
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(stream, null, options)
            stream.close()
            
            Log.d(TAG, "[PC][2] Image bounds: ${options.outWidth}x${options.outHeight}, mimeType: ${options.outMimeType}")

            if (options.outWidth <= 0 || options.outHeight <= 0) {
                Log.e(TAG, "[PC][2] Invalid image bounds detected")
                return null
            }

            // Calculate inSampleSize
            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
            options.inJustDecodeBounds = false
            Log.d(TAG, "[PC][2] Using inSampleSize: ${options.inSampleSize}")

            stream = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(stream, null, options)
            if (bitmap == null) {
                Log.e(TAG, "[PC][2] BitmapFactory.decodeStream returned null")
            }
            bitmap
        } catch (e: Exception) {
            Log.e(TAG, "[PC][2] Error decoding image from URI", e)
            null
        } finally {
            try { stream?.close() } catch (ignored: Exception) {}
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
