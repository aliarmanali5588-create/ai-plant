package com.example.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.PlantAnalysisResult
import com.example.model.PlantAdvisory
import com.example.services.PlantAnalysisService
import com.example.services.PlantAdvisoryService
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.UnknownHostException

sealed interface Screen {
    object Home : Screen
    data class ImagePreview(val imageUri: String, val isFromCamera: Boolean) : Screen
    data class Analyzing(val imageUri: String) : Screen
    data class Result(
        val result: PlantAnalysisResult,
        val imageUri: String,
        val advisory: PlantAdvisory? = null,
        val isAdvisoryLoading: Boolean = false,
        val advisoryError: String? = null
    ) : Screen
    data class Error(val message: String, val retryAction: () -> Unit) : Screen
}

class PlantCareViewModel : ViewModel() {

    private val analysisService = PlantAnalysisService()
    private val advisoryService = PlantAdvisoryService()

    private val _currentScreen = MutableStateFlow<Screen>(Screen.Home)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val _loadingProgressText = MutableStateFlow("Preparing image...")
    val loadingProgressText: StateFlow<String> = _loadingProgressText.asStateFlow()

    private val navigationHistory = mutableListOf<Screen>()

    fun fetchAdvisory(result: PlantAnalysisResult, imageUri: String) {
        val current = _currentScreen.value
        if (current is Screen.Result) {
            _currentScreen.value = current.copy(isAdvisoryLoading = true, advisoryError = null)
        }

        viewModelScope.launch {
            try {
                val advisory = advisoryService.getAdvisory(result)
                val updated = _currentScreen.value
                if (updated is Screen.Result) {
                    _currentScreen.value = updated.copy(advisory = advisory, isAdvisoryLoading = false)
                }
            } catch (e: Exception) {
                val updated = _currentScreen.value
                if (updated is Screen.Result) {
                    _currentScreen.value = updated.copy(isAdvisoryLoading = false, advisoryError = "Advisory temporarily unavailable.")
                }
            }
        }
    }

    fun navigateTo(screen: Screen) {
        navigationHistory.add(_currentScreen.value)
        _currentScreen.value = screen
    }

    fun navigateBack(): Boolean {
        if (navigationHistory.isNotEmpty()) {
            _currentScreen.value = navigationHistory.removeAt(navigationHistory.size - 1)
            return true
        }
        return false
    }

    fun resetToHome() {
        navigationHistory.clear()
        _currentScreen.value = Screen.Home
    }

    fun handleImageSelected(imageUri: String, isFromCamera: Boolean) {
        if (imageUri.isBlank()) {
            navigateTo(Screen.Error("We couldn't use this image. Please choose another photo.") {
                resetToHome()
            })
            return
        }
        navigateTo(Screen.ImagePreview(imageUri, isFromCamera))
    }

    fun startAnalysis(context: Context, imageUri: String) {
        _currentScreen.value = Screen.Analyzing(imageUri)
        _loadingProgressText.value = "Preparing image..."

        viewModelScope.launch {
            try {
                val result = analysisService.analyzePlant(
                    context = context,
                    imageUriString = imageUri,
                    onStageUpdate = { stage ->
                        _loadingProgressText.value = stage
                    }
                )
                _currentScreen.value = Screen.Result(result, imageUri)
            } catch (e: TimeoutCancellationException) {
                _currentScreen.value = Screen.Error("The analysis is taking too long. Please try again.") {
                    navigateTo(Screen.ImagePreview(imageUri, false))
                }
            } catch (e: UnknownHostException) {
                _currentScreen.value = Screen.Error("🌐 No internet connection\n\nPlantCare AI needs an internet connection to analyze this image. Please check your connection and try again.") {
                    navigateTo(Screen.ImagePreview(imageUri, false))
                }
            } catch (e: IllegalArgumentException) {
                _currentScreen.value = Screen.Error("We couldn't analyze this image. Try taking a clearer photo of the leaf.") {
                    navigateTo(Screen.ImagePreview(imageUri, false))
                }
            } catch (e: Exception) {
                _currentScreen.value = Screen.Error("Something went wrong. Our AI analysis service is temporarily unavailable. Please try again.") {
                    navigateTo(Screen.ImagePreview(imageUri, false))
                }
            }
        }
    }
}
