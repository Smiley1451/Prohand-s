package com.anand.prohands.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path


interface WagePredictionService {

    @GET("health")
    suspend fun healthCheck(): Response<HealthResponse>

    @GET("api/config")
    suspend fun getConfig(): Response<WageConfigResponse>

    @GET("api/sectors")
    suspend fun getSectors(): Response<SectorsResponse>

    @POST("api/predict")
    suspend fun predictWage(@Body request: WagePredictionRequest): Response<WagePredictionResponse>

    @GET("api/test/{sector}")
    suspend fun testPrediction(@Path("sector") sector: String): Response<WagePredictionResponse>
}

// --- Data Classes ---

data class HealthResponse(
    val status: String,
    val models_loaded: Map<String, Boolean>?
)

data class SectorsResponse(
    val sectors: List<String>
)

data class WageConfigResponse(
    val agriculture: SectorConfig?,
    val construction: SectorConfig?
)

data class SectorConfig(
    val name: String,
    val icon: String?,
    val metadata: SectorMetadata?,
    val categorical_fields: List<String>?,
    val numerical_fields: List<String>?,
    val valid_values: Map<String, List<String>>?
)

data class SectorMetadata(
    val model_type: String?,
    val test_mae: Double?,
    val test_rmse: Double?,
    val test_r2: Double?
)

data class WagePredictionRequest(
    val sector: String,
    val data: Map<String, Any>
)

data class WagePredictionResponse(
    val success: Boolean,
    val predicted_wage: Double?,
    val sector: String?,
    val monthly_estimate: Double?,
    val input_data: Map<String, Any>?,
    val error: String?
)

