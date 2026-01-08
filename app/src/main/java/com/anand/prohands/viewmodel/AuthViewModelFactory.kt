package com.anand.prohands.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.anand.prohands.network.AuthApi
import com.anand.prohands.network.ProfileApi
import com.anand.prohands.network.RetrofitClient
import com.anand.prohands.utils.SessionManager

class AuthViewModelFactory(private val sessionManager: SessionManager) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            val authApi = RetrofitClient.instance.create(AuthApi::class.java)
            val profileApi = RetrofitClient.instance.create(ProfileApi::class.java)
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(authApi, profileApi, sessionManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
