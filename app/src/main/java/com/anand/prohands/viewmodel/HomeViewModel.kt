package com.anand.prohands.viewmodel

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.anand.prohands.data.ClientProfileDto
import com.anand.prohands.data.JobResponse
import com.anand.prohands.network.JobService
import com.anand.prohands.network.ProfileApi
import com.anand.prohands.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HomeViewModel(
    private val jobService: JobService,
    private val profileApi: ProfileApi
) : ViewModel() {

    private val _jobs = MutableStateFlow<List<JobResponse>>(emptyList())
    val jobs: StateFlow<List<JobResponse>> = _jobs
    
    private val _currentUserProfile = MutableStateFlow<ClientProfileDto?>(null)
    val currentUserProfile: StateFlow<ClientProfileDto?> = _currentUserProfile

    private val _location = MutableStateFlow<Location?>(null)
    val location: StateFlow<Location?> = _location

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun setLocation(location: Location) {
        _location.value = location
        fetchJobs(location.latitude, location.longitude)
    }
    
    fun fetchUserProfile(userId: String) {
        viewModelScope.launch {
            try {
                val response = profileApi.getProfile(userId)
                if (response.isSuccessful) {
                    _currentUserProfile.value = response.body()
                }
            } catch (e: Exception) {
                // Log error
            }
        }
    }

    fun fetchJobs(latitude: Double, longitude: Double, page: Int = 0, size: Int = 20) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val response = jobService.getJobFeed(latitude, longitude, page, size)
                if (response.isSuccessful) {
                    _jobs.value = response.body() ?: emptyList()
                } else {
                    _error.value = "Failed to fetch jobs: ${response.code()}"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "An unexpected error occurred"
            } finally {
                _isLoading.value = false
            }
        }
    }
}

class HomeViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            val jobService = RetrofitClient.instance.create(JobService::class.java)
            val profileApi = RetrofitClient.instance.create(ProfileApi::class.java)
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(jobService, profileApi) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
