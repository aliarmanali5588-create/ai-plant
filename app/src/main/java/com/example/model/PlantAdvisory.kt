package com.example.model

import kotlinx.serialization.Serializable

@Serializable
data class PlantAdvisory(
    val what_is_it: String = "",
    val symptoms: List<String> = emptyList(),
    val what_to_do: List<String> = emptyList(),
    val prevention: List<String> = emptyList(),
    val when_to_seek_expert_help: String = "",
    val caution: String = ""
)
