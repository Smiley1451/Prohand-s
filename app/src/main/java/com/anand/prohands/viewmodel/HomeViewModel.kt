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

    private val _isMoreLoading = MutableStateFlow(false)
    val isMoreLoading: StateFlow<Boolean> = _isMoreLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private var currentPage = 0
    private var isLastPage = false
    private val pageSize = 20

    fun setLocation(location: Location) {
        val oldLocation = _location.value
        _location.value = location
        
        // If location changed significantly or first load, refresh jobs
        if (oldLocation == null || calculateDistance(oldLocation, location) > 500) { // 500 meters
            refreshJobs()
        }
    }

    private fun calculateDistance(loc1: Location, loc2: Location): Float {
        val results = FloatArray(1)
        Location.distanceBetween(loc1.latitude, loc1.longitude, loc2.latitude, loc2.longitude, results)
        return results[0]
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

    fun refreshJobs() {
        val loc = _location.value ?: return
        currentPage = 0
        isLastPage = false
        fetchJobs(loc.latitude, loc.longitude, isRefresh = true)
    }

    fun loadMoreJobs() {
        val loc = _location.value ?: return
        if (_isMoreLoading.value || isLastPage) return
        
        currentPage++
        fetchJobs(loc.latitude, loc.longitude, isRefresh = false)
    }

    private fun fetchJobs(latitude: Double, longitude: Double, isRefresh: Boolean) {
        viewModelScope.launch {
            if (isRefresh) {
                _isLoading.value = true
            } else {
                _isMoreLoading.value = true
            }
            
            _error.value = null
            try {
                val response = jobService.getJobFeed(latitude, longitude, currentPage, pageSize)
                if (response.isSuccessful) {
                    val newJobs = response.body() ?: emptyList()
                    if (isRefresh) {
                        _jobs.value = newJobs
                    } else {
                        _jobs.value = _jobs.value + newJobs
                    }
                    
                    if (newJobs.size < pageSize) {
                        isLastPage = true
                    }
                } else {
                    _error.value = "Failed to fetch jobs: ${response.code()}"
                    if (!isRefresh) currentPage-- // Rollback page on error
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "An unexpected error occurred"
                if (!isRefresh) currentPage-- // Rollback page on error
            } finally {
                if (isRefresh) {
                    _isLoading.value = false
                } else {
                    _isMoreLoading.value = false
                }
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
