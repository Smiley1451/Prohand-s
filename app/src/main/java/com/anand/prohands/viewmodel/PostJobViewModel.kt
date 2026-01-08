package com.anand.prohands.viewmodel

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.anand.prohands.data.JobRequest
import com.anand.prohands.network.JobService
import com.anand.prohands.network.RetrofitClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class PostJobViewModel(private val jobService: JobService, context: Context) : ViewModel() {

    private val _postResult = MutableStateFlow<Boolean?>(null)
    val postResult: StateFlow<Boolean?> = _postResult

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _currentLocation = MutableStateFlow<Location?>(null)
    val currentLocation: StateFlow<Location?> = _currentLocation

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    fun fetchCurrentLocation() {
        viewModelScope.launch {
            try {
                // Using a cancellation token is a good practice for location requests
                val location = fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token).await()
                _currentLocation.value = location
            } catch (e: Exception) {
                _error.value = "Could not get current location."
            }
        }
    }

    fun postJob(jobRequest: JobRequest) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val response = jobService.createJob(jobRequest)
                if (response.isSuccessful) {
                    _postResult.value = true
                } else {
                    _error.value = "Failed to post job: ${response.code()}"
                    _postResult.value = false
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "An unexpected error occurred"
                _postResult.value = false
            } finally {
                _isLoading.value = false
            }
        }
    }
}

class PostJobViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PostJobViewModel::class.java)) {
            val jobService = RetrofitClient.instance.create(JobService::class.java)
            @Suppress("UNCHECKED_CAST")
            return PostJobViewModel(jobService, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
