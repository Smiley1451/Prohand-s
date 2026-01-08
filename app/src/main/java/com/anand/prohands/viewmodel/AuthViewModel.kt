package com.anand.prohands.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anand.prohands.data.*
import com.anand.prohands.network.AuthApi
import com.anand.prohands.network.ProfileApi
import com.anand.prohands.utils.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.net.SocketTimeoutException
import java.io.IOException

// UI State data class for the original UI pattern
data class AuthUiState(
    val isLoading: Boolean = false,
    val loginSuccess: Boolean = false,
    val signupSuccess: Boolean = false,
    val verifySuccess: Boolean = false,
    val mfaRequired: Boolean = false,
    val passwordResetRequestSuccess: Boolean = false,
    val passwordResetSuccess: Boolean = false,
    val error: String? = null,
    val emailForVerification: String? = null,
    val userIdForMfa: String? = null
)

class AuthViewModel(
    private val authApi: AuthApi,
    private val profileApi: ProfileApi,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state

    private val _currentUser = MutableStateFlow<ClientProfileDto?>(null)
    val currentUser: StateFlow<ClientProfileDto?> = _currentUser

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val response = authApi.login(LoginRequest(email, password))
                if (response.isSuccessful) {
                    val loginResponse = response.body()
                    if (loginResponse != null) {
                        if (loginResponse.mfaRequired) {
                            _state.value = _state.value.copy(
                                isLoading = false,
                                mfaRequired = true,
                                userIdForMfa = loginResponse.userId,
                                emailForVerification = email
                            )
                        } else {
                            sessionManager.saveAuthToken(loginResponse.token)
                            sessionManager.saveUserId(loginResponse.userId)
                            fetchProfile()
                            _state.value = _state.value.copy(isLoading = false, loginSuccess = true)
                        }
                    } else {
                        _state.value = _state.value.copy(isLoading = false, error = "Empty response body")
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    _state.value = _state.value.copy(isLoading = false, error = "Login failed: ${response.code()} - $errorBody")
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = handleException(e))
            }
        }
    }

    fun verifyMfa(otp: String) {
        val email = _state.value.emailForVerification ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val response = authApi.verifyMfa(MfaRequest(email, otp))
                if (response.isSuccessful) {
                    val mfaResponse = response.body()
                    if (mfaResponse != null) {
                        sessionManager.saveAuthToken(mfaResponse.token)
                        sessionManager.saveUserId(mfaResponse.userId)
                        fetchProfile()
                        _state.value = _state.value.copy(
                            isLoading = false,
                            loginSuccess = true,
                            mfaRequired = false
                        )
                    } else {
                        _state.value = _state.value.copy(isLoading = false, error = "Empty MFA response")
                    }
                } else {
                    _state.value = _state.value.copy(isLoading = false, error = "MFA failed: ${response.code()}")
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = handleException(e))
            }
        }
    }

    fun signup(username: String, email: String, password: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val request = SignupRequest(email, username, password)
                val response = authApi.signup(request)
                if (response.isSuccessful) {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        signupSuccess = true,
                        emailForVerification = email
                    )
                } else {
                    val errorBody = response.errorBody()?.string()
                    _state.value = _state.value.copy(isLoading = false, error = "Registration failed: ${response.code()} - $errorBody")
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = handleException(e))
            }
        }
    }

    fun verifyAccount(otp: String) {
        val email = _state.value.emailForVerification ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val response = authApi.verifyAccount(VerifyAccountRequest(email, otp))
                if (response.isSuccessful) {
                    _state.value = _state.value.copy(isLoading = false, verifySuccess = true)
                } else {
                    _state.value = _state.value.copy(isLoading = false, error = "Verification failed: ${response.code()}")
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = handleException(e))
            }
        }
    }

    fun requestPasswordReset(email: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null, emailForVerification = email)
            try {
                val response = authApi.requestPasswordReset(email)
                if (response.isSuccessful) {
                    _state.value = _state.value.copy(isLoading = false, passwordResetRequestSuccess = true)
                } else {
                    _state.value = _state.value.copy(isLoading = false, error = "Failed to send reset OTP: ${response.code()}")
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = handleException(e))
            }
        }
    }

    fun resetPassword(otp: String, newPassword: String) {
        val email = _state.value.emailForVerification ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val response = authApi.resetPassword(ResetPasswordRequest(email, otp, newPassword))
                if (response.isSuccessful) {
                    _state.value = _state.value.copy(isLoading = false, passwordResetSuccess = true)
                } else {
                    _state.value = _state.value.copy(isLoading = false, error = "Password reset failed: ${response.code()}")
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = handleException(e))
            }
        }
    }

    fun fetchProfile() {
        val userId = sessionManager.getUserId() ?: return
        viewModelScope.launch {
            try {
                val response = profileApi.getProfile(userId)
                if (response.isSuccessful) {
                    _currentUser.value = response.body()
                }
            } catch (e: Exception) {
                // Log error
            }
        }
    }

    fun logout() {
        sessionManager.clearSession()
        _state.value = AuthUiState()
        _currentUser.value = null
    }
    
    fun checkSession() {
        if (sessionManager.getAuthToken() != null) {
            _state.value = _state.value.copy(loginSuccess = true)
            fetchProfile()
        }
    }

    fun resetState() {
        _state.value = AuthUiState()
    }

    fun clearStatusFlags() {
        _state.value = _state.value.copy(
            error = null,
            loginSuccess = false,
            signupSuccess = false,
            verifySuccess = false,
            mfaRequired = false,
            passwordResetRequestSuccess = false,
            passwordResetSuccess = false
        )
    }

    private fun handleException(e: Exception): String {
        return when (e) {
            is SocketTimeoutException -> "Connection timed out. Please check your internet."
            is IOException -> "Network error. Please check your connection."
            is HttpException -> "Server error: ${e.code()}"
            else -> e.message ?: "An unknown error occurred"
        }
    }
}
