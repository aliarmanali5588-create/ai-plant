package com.example.model

data class Prediction(
    val name: String,
    val confidence: Float
)

data class PlantAnalysisResult(
    val plant: String,
    val disease: String,
    val confidence: Float,
    val predictions: List<Prediction>,
    val explanation: String = "",
    val advice: List<String> = emptyList(),
    val isLowConfidence: Boolean = false,
    val confidenceLabel: String = ""
) {
    companion object {
        const val LOW_CONFIDENCE_THRESHOLD = 0.35f
    }
}
