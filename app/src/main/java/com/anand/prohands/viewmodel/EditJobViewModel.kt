package com.anand.prohands.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.anand.prohands.data.JobRequest
import com.anand.prohands.data.JobResponse
import com.anand.prohands.network.JobService
import com.anand.prohands.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class EditJobViewModel(private val jobService: JobService) : ViewModel() {

    private val _job = MutableStateFlow<JobResponse?>(null)
    val job: StateFlow<JobResponse?> = _job

    private val _updateResult = MutableStateFlow<Boolean?>(null)
    val updateResult: StateFlow<Boolean?> = _updateResult

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun fetchJob(jobId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = jobService.getJob(jobId)
                if (response.isSuccessful) {
                    _job.value = response.body()
                } else {
                    _error.value = "Failed to fetch job details"
                }
            } catch (e: Exception) {
                _error.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateJob(jobId: String, jobRequest: JobRequest) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val response = jobService.updateJob(jobId, jobRequest)
                if (response.isSuccessful) {
                    _updateResult.value = true
                } else {
                    _error.value = "Failed to update job: ${response.code()}"
                    _updateResult.value = false
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "An unexpected error occurred"
                _updateResult.value = false
            } finally {
                _isLoading.value = false
            }
        }
    }
}

class EditJobViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EditJobViewModel::class.java)) {
            val jobService = RetrofitClient.instance.create(JobService::class.java)
            @Suppress("UNCHECKED_CAST")
            return EditJobViewModel(jobService) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
