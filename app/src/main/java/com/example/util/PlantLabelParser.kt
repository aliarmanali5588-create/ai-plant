package com.example.util

data class ParsedDiagnosis(
    val plant: String,
    val disease: String,
    val explanation: String,
    val advice: List<String>
)

object PlantLabelParser {

    // 38 PlantVillage classes mapped to index
    val PLANT_VILLAGE_CLASSES = listOf(
        "Apple___Apple_scab",
        "Apple___Black_rot",
        "Apple___Cedar_apple_rust",
        "Apple___healthy",
        "Blueberry___healthy",
        "Cherry_(including_sour)___Powdery_mildew",
        "Cherry_(including_sour)___healthy",
        "Corn_(maize)___Cercospora_leaf_spot Gray_leaf_spot",
        "Corn_(maize)___Common_rust_",
        "Corn_(maize)___Northern_Leaf_Blight",
        "Corn_(maize)___healthy",
        "Grape___Black_rot",
        "Grape___Esca_(Black_Measles)",
        "Grape___Leaf_blight_(Isariopsis_Leaf_Spot)",
        "Grape___healthy",
        "Orange___Haunglongbing_(Citrus_greening)",
        "Peach___Bacterial_spot",
        "Peach___healthy",
        "Pepper,_bell___Bacterial_spot",
        "Pepper,_bell___healthy",
        "Potato___Early_blight",
        "Potato___Late_blight",
        "Potato___healthy",
        "Raspberry___healthy",
        "Soybean___healthy",
        "Squash___Powdery_mildew",
        "Strawberry___Leaf_scorch",
        "Strawberry___healthy",
        "Tomato___Bacterial_spot",
        "Tomato___Early_blight",
        "Tomato___Late_blight",
        "Tomato___Leaf_Mold",
        "Tomato___Septoria_leaf_spot",
        "Tomato___Spider_mites Two-spotted_spider_mite",
        "Tomato___Target_Spot",
        "Tomato___Tomato_Yellow_Leaf_Curl_Virus",
        "Tomato___Tomato_mosaic_virus",
        "Tomato___healthy"
    )

    fun parse(rawLabel: String): ParsedDiagnosis {
        val (plantPart, diseasePart) = if (rawLabel.contains("___")) {
            val parts = rawLabel.split("___", limit = 2)
            parts[0] to parts[1]
        } else if (rawLabel.contains("__")) {
            val parts = rawLabel.split("__", limit = 2)
            parts[0] to parts[1]
        } else {
            rawLabel to "AI could not determine a specific disease."
        }

        val cleanPlant = formatPlantName(plantPart)
        val cleanDisease = formatDiseaseName(diseasePart)

        val (explanation, advice) = getDiseaseInfo(cleanPlant, cleanDisease)

        return ParsedDiagnosis(
            plant = cleanPlant,
            disease = cleanDisease,
            explanation = explanation,
            advice = advice
        )
    }

    fun formatPredictionName(rawLabel: String): String {
        return if (rawLabel.contains("___")) {
            val parts = rawLabel.split("___", limit = 2)
            formatDiseaseName(parts[1])
        } else {
            rawLabel.replace('_', ' ').trim()
        }
    }

    private fun formatPlantName(raw: String): String {
        var p = raw.replace("_(including_sour)", "")
            .replace("_(maize)", "")
            .replace(",_bell", " Bell")
            .replace('_', ' ')
            .trim()

        if (p.isEmpty()) return "Plant detected"
        return p.split(" ")
            .filter { it.isNotBlank() }
            .joinToString(" ") { word -> word.lowercase().replaceFirstChar { it.uppercase() } }
    }

    private fun formatDiseaseName(raw: String): String {
        if (raw.equals("healthy", ignoreCase = true)) return "Healthy"
        var d = raw.replace('_', ' ')
            .replace(Regex("\\(.*?\\)"), "")
            .replace(Regex("\\s+"), " ")
            .trim()

        if (d.contains("healthy", ignoreCase = true)) return "Healthy"
        if (d.isEmpty()) return "AI could not determine a specific disease."

        return d.split(" ")
            .filter { it.isNotBlank() }
            .joinToString(" ") { word -> word.lowercase().replaceFirstChar { it.uppercase() } }
    }

    private fun getDiseaseInfo(plant: String, disease: String): Pair<String, List<String>> {
        val lower = disease.lowercase()
        return when {
            lower.contains("healthy") -> {
                "The foliage exhibits strong, healthy characteristics with no evident lesions, chlorosis, or fungal distress." to listOf(
                    "Maintain current watering schedule according to plant species needs.",
                    "Ensure adequate sunlight and proper soil drainage.",
                    "Inspect leaves periodically for early signs of pests or nutrient deficiencies."
                )
            }
            lower.contains("early blight") -> {
                "Early Blight is a common fungal condition affecting $plant foliage. It typically appears as dark, concentric ring lesions on older leaves." to listOf(
                    "Remove severely affected lower leaves to halt spore propagation.",
                    "Ensure good airflow and spacing between plants.",
                    "Water strictly at the soil line; keep leaves dry.",
                    "Apply an organic copper fungicide or neem oil if spread continues."
                )
            }
            lower.contains("late blight") -> {
                "Late Blight is a severe water-mold pathogen causing pale green to brown water-soaked lesions that rapidly darken on $plant leaves." to listOf(
                    "Promptly isolate or discard infected foliage to protect nearby plants.",
                    "Reduce humidity and overhead watering immediately.",
                    "Ensure full morning sunlight to dry remaining dew.",
                    "Consult local agricultural extension for approved protective treatments."
                )
            }
            lower.contains("bacterial spot") -> {
                "Bacterial Spot causes small, water-soaked, dark angular lesions often surrounded by a yellow halo on $plant leaves." to listOf(
                    "Avoid handling foliage when wet to avoid spreading bacteria.",
                    "Sterilize pruning shears between each cut with 70% alcohol.",
                    "Drip irrigate at the base to prevent water splashing.",
                    "Apply fixed copper sprays during early outbreak stages."
                )
            }
            lower.contains("powdery mildew") -> {
                "Powdery Mildew is characterized by white, powdery fungal patches across the upper surface of $plant leaves and stems." to listOf(
                    "Prune overcrowded stems to maximize air circulation and sunlight.",
                    "Avoid overhead irrigation in late afternoon or evening.",
                    "Apply a potassium bicarbonate or horticultural oil spray.",
                    "Clean up and discard fallen leaves around the root base."
                )
            }
            lower.contains("rust") -> {
                "Rust is a fungal disease identified by orange, reddish-brown, or yellow pustules forming on the undersides of leaves." to listOf(
                    "Prune heavily infected leaves and destroy them away from compost.",
                    "Keep foliage completely dry during irrigation.",
                    "Ensure optimal sun exposure to reduce localized humidity.",
                    "Apply sulfur or organic bio-fungicides if infection recurs."
                )
            }
            lower.contains("scab") -> {
                "Apple scab is a fungal infection producing velvety olive-green to dark brown spots on leaves and fruit." to listOf(
                    "Rake and compost or destroy all fallen leaves in autumn.",
                    "Prune canopy branches to improve air circulation and sunlight penetration.",
                    "Apply preventative organic fungicides in early spring as buds break."
                )
            }
            lower.contains("leaf mold") -> {
                "Leaf Mold thrives in high humidity environments, producing pale yellow spots on upper leaves and olive-green mold underneath." to listOf(
                    "Lower relative greenhouse/room humidity below 85%.",
                    "Increase ventilation and use oscillating fans.",
                    "Prune lower canopy leaves to increase airflow."
                )
            }
            lower.contains("spider mite") -> {
                "Spider mite damage presents as fine stippling, yellowing, and delicate webbing on the undersides of $plant leaves." to listOf(
                    "Rinse leaf undersides with a gentle stream of water to dislodge mites.",
                    "Apply insecticidal soap or neem oil thoroughly covering undersides.",
                    "Maintain moderate ambient humidity to discourage mite breeding."
                )
            }
            else -> {
                "Symptoms detected on $plant correspond to $disease. Visual patterns indicate localized stress or leaf pathology." to listOf(
                    "Isolate the plant to prevent potential spread to adjacent vegetation.",
                    "Prune noticeably damaged foliage using sanitized tools.",
                    "Adjust moisture levels and verify that the potting mix drains well.",
                    "Monitor new growth over the next 5-7 days for symptom progression."
                )
            }
        }
    }
}
