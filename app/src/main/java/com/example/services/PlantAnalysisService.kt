package com.example.services

import android.content.Context
import android.net.Uri
import com.example.model.PlantAnalysisResult
import com.example.vision.TFLiteVisionProvider
import com.example.vision.VisionProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout

class PlantAnalysisService(
    private val visionProvider: VisionProvider = TFLiteVisionProvider()
) {
    companion object {
        const val ANALYSIS_TIMEOUT_MS = 30_000L
    }

    suspend fun analyzePlant(
        context: Context,
        imageUriString: String,
        onStageUpdate: ((String) -> Unit)? = null
    ): PlantAnalysisResult {
        if (imageUriString.isBlank()) {
            throw IllegalArgumentException("Invalid or missing image URI.")
        }

        val uri = Uri.parse(imageUriString)

        return withTimeout(ANALYSIS_TIMEOUT_MS) {
            onStageUpdate?.invoke("Preparing image...")
            delay(500)

            onStageUpdate?.invoke("Sending image for analysis...")
            delay(500)

            onStageUpdate?.invoke("Examining leaf...")
            // Real inference execution on image
            val result = visionProvider.analyze(context, uri)

            onStageUpdate?.invoke("Identifying plant disease...")
            delay(500)

            onStageUpdate?.invoke("Preparing results...")
            delay(400)

            result
        }
    }
}
