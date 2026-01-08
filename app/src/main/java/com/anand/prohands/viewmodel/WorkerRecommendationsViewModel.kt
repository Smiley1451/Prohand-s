package com.anand.prohands.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.anand.prohands.data.WorkerRecommendationDto
import com.anand.prohands.network.ProfileApi
import com.anand.prohands.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class WorkerRecommendationsViewModel(private val profileApi: ProfileApi) : ViewModel() {

    private val _recommendations = MutableStateFlow<List<WorkerRecommendationDto>>(emptyList())
    val recommendations: StateFlow<List<WorkerRecommendationDto>> = _recommendations

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun fetchRecommendations(
        jobTitle: String,
        jobDescription: String,
        latitude: Double,
        longitude: Double,
        page: Int,
        size: Int
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val response = profileApi.getWorkerRecommendations(
                    jobTitle = jobTitle,
                    jobDescription = jobDescription,
                    latitude = latitude,
                    longitude = longitude,
                    page = page,
                    size = size
                )
                if (response.isSuccessful) {
                    _recommendations.value = response.body() ?: emptyList()
                } else {
                    _error.value = "Failed to fetch recommendations: ${response.code()}"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "An unexpected error occurred"
            } finally {
                _isLoading.value = false
            }
        }
    }
}

class WorkerRecommendationsViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WorkerRecommendationsViewModel::class.java)) {
            val profileApi = RetrofitClient.instance.create(ProfileApi::class.java)
            @Suppress("UNCHECKED_CAST")
            return WorkerRecommendationsViewModel(profileApi) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}