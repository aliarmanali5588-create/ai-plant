package com.example.services

import com.example.model.PlantAdvisory
import com.example.model.PlantAnalysisResult
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.util.concurrent.TimeUnit

interface AdvisoryApiService {
    @POST("api/advisory")
    suspend fun getAdvisory(@Body result: PlantAnalysisResult): PlantAdvisory
}

class PlantAdvisoryService {
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://plant-llm.aliarmanal5588.workers.dev/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    private val apiService = retrofit.create(AdvisoryApiService::class.java)

    suspend fun getAdvisory(result: PlantAnalysisResult): PlantAdvisory {
        return apiService.getAdvisory(result)
    }
}
