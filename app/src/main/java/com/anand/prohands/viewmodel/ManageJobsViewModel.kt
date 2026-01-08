package com.anand.prohands.viewmodel

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.anand.prohands.data.JobResponse
import com.anand.prohands.data.ReviewRequest
import com.anand.prohands.network.JobService
import com.anand.prohands.network.ReviewApi
import com.anand.prohands.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ManageJobsViewModel(
    private val jobService: JobService,
    private val reviewApi: ReviewApi
) : ViewModel() {

    private val _jobs = MutableStateFlow<List<JobResponse>>(emptyList())
    val jobs: StateFlow<List<JobResponse>> = _jobs

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    
    private val _actionError = MutableStateFlow<String?>(null)
    val actionError: StateFlow<String?> = _actionError

    private val _actionSuccess = MutableStateFlow<String?>(null)
    val actionSuccess: StateFlow<String?> = _actionSuccess

    fun fetchJobs(providerId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = jobService.getJobsByProvider(providerId)
                if (response.isSuccessful) {
                    _jobs.value = response.body() ?: emptyList()
                } else {
                    _actionError.value = "Failed to fetch jobs: ${response.message()}"
                }
            } catch (e: Exception) {
                _actionError.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateJobStatus(jobId: String, newStatus: String, providerId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = jobService.updateStatus(jobId, newStatus)
                if (response.isSuccessful) {
                    _actionSuccess.value = "Job status updated to $newStatus"
                    fetchJobs(providerId) // Refresh list
                } else {
                    _actionError.value = "Failed to update status"
                }
            } catch (e: Exception) {
                _actionError.value = "Error updating status: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun submitReview(reviewRequest: ReviewRequest) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = reviewApi.submitReview(reviewRequest)
                if (response.isSuccessful) {
                    _actionSuccess.value = "Review submitted successfully"
                } else {
                    _actionError.value = "Failed to submit review"
                }
            } catch (e: Exception) {
                _actionError.value = "Error submitting review: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun clearMessages() {
        _actionError.value = null
        _actionSuccess.value = null
    }
}

class ManageJobsViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ManageJobsViewModel::class.java)) {
            val jobService = RetrofitClient.instance.create(JobService::class.java)
            val reviewApi = RetrofitClient.instance.create(ReviewApi::class.java)
            @Suppress("UNCHECKED_CAST")
            return ManageJobsViewModel(jobService, reviewApi) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
