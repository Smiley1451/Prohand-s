package com.anand.prohands.viewmodel

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.anand.prohands.data.ClientProfileDto
import com.anand.prohands.data.local.UserWageProfile
import com.anand.prohands.data.local.UserWageProfileDao
import com.anand.prohands.network.ProfileApi
import com.anand.prohands.network.RetrofitClient
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import okhttp3.MultipartBody

class EditProfileViewModel(
    private val userWageProfileDao: UserWageProfileDao?
) : ViewModel() {

    companion object {
        private const val TAG = "EditProfileViewModel"
    }

    private val profileApi: ProfileApi = RetrofitClient.instance.create(ProfileApi::class.java)

    var profile by mutableStateOf<ClientProfileDto?>(null)
        private set

    var wageProfile by mutableStateOf<UserWageProfile?>(null)
        private set

    var isLoading by mutableStateOf(false)
        private set

    var isImageUploading by mutableStateOf(false)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    var isSavingWageProfile by mutableStateOf(false)
        private set

    fun loadProfile(userId: String) {
        viewModelScope.launch {
            isLoading = true
            error = null
            try {
                val response = profileApi.getProfile(userId)
                if (response.isSuccessful) {
                    profile = response.body()
                    // Load wage profile from local database
                    loadWageProfile(userId)
                } else {
                    error = "Error loading profile: ${response.message()}"
                }
            } catch (e: Exception) {
                error = "Error loading profile: ${e.message}"
                Log.e(TAG, "Error loading profile", e)
            }
            isLoading = false
        }
    }

    private fun loadWageProfile(userId: String) {
        if (userWageProfileDao == null) {
            // Create default wage profile if no DAO
            wageProfile = UserWageProfile(userId = userId)
            return
        }

        viewModelScope.launch {
            try {
                userWageProfileDao.getWageProfile(userId)
                    .catch { e ->
                        Log.e(TAG, "Error loading wage profile", e)
                        emit(null)
                    }
                    .collectLatest { profile ->
                        wageProfile = profile ?: UserWageProfile(userId = userId)
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Error in loadWageProfile", e)
                wageProfile = UserWageProfile(userId = userId)
            }
        }
    }

    fun updateProfile(userId: String, updatedProfile: ClientProfileDto, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            isLoading = true
            error = null
            try {
                val response = profileApi.updateProfile(userId, updatedProfile)
                if (response.isSuccessful) {
                    profile = response.body()
                    onResult(true, null)
                } else {
                    error = "Error updating profile: ${response.message()}"
                    onResult(false, error)
                }
            } catch (e: Exception) {
                error = "Error updating profile: ${e.message}"
                Log.e(TAG, "Error updating profile", e)
                onResult(false, error)
            }
            isLoading = false
        }
    }

    /**
     * Save wage profile to local database
     */
    fun saveWageProfile(wageProfile: UserWageProfile, onResult: (Boolean, String?) -> Unit) {
        if (userWageProfileDao == null) {
            onResult(false, "Local storage not available")
            return
        }

        viewModelScope.launch {
            isSavingWageProfile = true
            try {
                userWageProfileDao.insertOrUpdate(wageProfile.copy(lastUpdated = System.currentTimeMillis()))
                this@EditProfileViewModel.wageProfile = wageProfile
                onResult(true, null)
            } catch (e: Exception) {
                Log.e(TAG, "Error saving wage profile", e)
                onResult(false, e.message)
            } finally {
                isSavingWageProfile = false
            }
        }
    }

    fun uploadProfilePicture(userId: String, file: MultipartBody.Part, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            isImageUploading = true
            error = null
            try {
                val response = profileApi.uploadProfilePicture(userId, file)
                if (response.isSuccessful) {
                    profile = response.body()
                    onResult(true, null)
                } else {
                    error = "Error uploading profile picture: ${response.message()}"
                    onResult(false, error)
                }
            } catch (e: Exception) {
                error = "Error uploading profile picture: ${e.message}"
                Log.e(TAG, "Error uploading profile picture", e)
                onResult(false, error)
            }
            isImageUploading = false
        }
    }
}

class EditProfileViewModelFactory(
    private val userWageProfileDao: UserWageProfileDao?
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EditProfileViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return EditProfileViewModel(userWageProfileDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
