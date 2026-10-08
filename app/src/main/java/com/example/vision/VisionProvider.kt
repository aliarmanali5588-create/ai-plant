package com.example.vision

import android.content.Context
import android.net.Uri
import com.example.model.PlantAnalysisResult

interface VisionProvider {
    val name: String
    suspend fun analyze(context: Context, imageUri: Uri): PlantAnalysisResult
}
