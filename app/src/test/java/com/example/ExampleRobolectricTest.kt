package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.PlantAdvisory
import com.example.model.PlantAnalysisResult
import com.example.model.Prediction
import com.example.ui.PlantCareViewModel
import com.example.ui.Screen
import com.example.util.PlantLabelParser
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("PlantCare AI", appName)
  }

  @Test
  fun `verify tflite model asset exists and is readable`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val fd = context.assets.openFd("plant_disease_model.tflite")
    assertNotNull(fd)
    assertTrue(fd.length > 1_000_000)
    fd.close()
  }

  @Test
  fun `plant label parser correctly splits plant and disease`() {
    val tomatoBlight = PlantLabelParser.parse("Tomato___Early_blight")
    assertEquals("Tomato", tomatoBlight.plant)
    assertEquals("Early Blight", tomatoBlight.disease)

    val cornRust = PlantLabelParser.parse("Corn_(maize)___Common_rust_")
    assertEquals("Corn", cornRust.plant)
    assertEquals("Common Rust", cornRust.disease)

    val appleHealthy = PlantLabelParser.parse("Apple___healthy")
    assertEquals("Apple", appleHealthy.plant)
    assertEquals("Healthy", appleHealthy.disease)

    val potatoLateBlight = PlantLabelParser.parse("Potato___Late_blight")
    assertEquals("Potato", potatoLateBlight.plant)
    assertEquals("Late Blight", potatoLateBlight.disease)
  }

  @Test
  fun `low confidence threshold operates accurately`() {
    val lowConfScore = 0.28f
    val highConfScore = 0.89f

    assertTrue(lowConfScore < PlantAnalysisResult.LOW_CONFIDENCE_THRESHOLD)
    assertFalse(highConfScore < PlantAnalysisResult.LOW_CONFIDENCE_THRESHOLD)
  }

  @Test
  fun `verify Cloudflare Worker and Groq advisory JSON serialization contract`() {
    val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    // 1. Verify outbound request format: PlantAnalysisResult -> JSON
    val requestResult = PlantAnalysisResult(
        plant = "Tomato",
        disease = "Early Blight",
        confidence = 0.87f,
        predictions = listOf(
            Prediction("Early Blight", 0.87f),
            Prediction("Bacterial Spot", 0.09f),
            Prediction("Healthy", 0.04f)
        )
    )
    val requestAdapter = moshi.adapter(PlantAnalysisResult::class.java)
    val requestJson = requestAdapter.toJson(requestResult)
    assertTrue(requestJson.contains("\"plant\":\"Tomato\""))
    assertTrue(requestJson.contains("\"disease\":\"Early Blight\""))
    assertTrue(requestJson.contains("\"confidence\":0.87"))

    // 2. Verify inbound response format: Groq LLM Advisory JSON -> PlantAdvisory
    val groqResponseJson = """
    {
      "what_is_it": "Early blight is a common fungal disease of tomato plants caused by Alternaria solani.",
      "symptoms": [
        "Dark brown or black spots on older leaves",
        "Target-board concentric rings inside spots",
        "Yellowing surrounding leaf lesions"
      ],
      "what_to_do": [
        "Prune and dispose of infected lower leaves",
        "Apply organic copper fungicide spray",
        "Mulch around plant base to prevent soil splash"
      ],
      "prevention": [
        "Practice 3-year crop rotation",
        "Water soil directly, avoid wetting foliage",
        "Space plants 24 inches apart for good airflow"
      ],
      "when_to_seek_expert_help": "If blight spreads to main stem or affects more than 40% of foliage.",
      "caution": "Do not compost infected foliage as fungal spores survive winter."
    }
    """.trimIndent()

    val advisoryAdapter = moshi.adapter(PlantAdvisory::class.java)
    val advisory = advisoryAdapter.fromJson(groqResponseJson)
    assertNotNull(advisory)
    assertEquals("Early blight is a common fungal disease of tomato plants caused by Alternaria solani.", advisory?.what_is_it)
    assertEquals(3, advisory?.symptoms?.size)
    assertEquals(3, advisory?.what_to_do?.size)
    assertEquals(3, advisory?.prevention?.size)
    assertEquals("If blight spreads to main stem or affects more than 40% of foliage.", advisory?.when_to_seek_expert_help)
    assertEquals("Do not compost infected foliage as fungal spores survive winter.", advisory?.caution)
  }

  @Test
  fun `verify ViewModel navigation and state machine`() {
    val viewModel = PlantCareViewModel()

    // Initial state is Home
    assertEquals(Screen.Home, viewModel.currentScreen.value)

    // Image selected -> ImagePreview
    val testUri = "content://media/external/images/media/42"
    viewModel.handleImageSelected(testUri, isFromCamera = false)
    assertTrue(viewModel.currentScreen.value is Screen.ImagePreview)
    val previewState = viewModel.currentScreen.value as Screen.ImagePreview
    assertEquals(testUri, previewState.imageUri)
    assertFalse(previewState.isFromCamera)

    // Reset to Home
    viewModel.resetToHome()
    assertEquals(Screen.Home, viewModel.currentScreen.value)

    // Blank URI -> Error state
    viewModel.handleImageSelected("", isFromCamera = true)
    assertTrue(viewModel.currentScreen.value is Screen.Error)
  }
}


