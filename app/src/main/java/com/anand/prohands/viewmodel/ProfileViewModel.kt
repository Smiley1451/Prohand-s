package com.anand.prohands.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.anand.prohands.data.ClientProfileDto
import com.anand.prohands.data.JobResponse
import com.anand.prohands.data.local.UserWageProfile
import com.anand.prohands.data.local.UserWageProfileDao
import com.anand.prohands.network.JobService
import com.anand.prohands.network.ProfileApi
import com.anand.prohands.network.RetrofitClient
import com.anand.prohands.network.WagePredictionClient
import com.anand.prohands.network.WagePredictionRequest
import com.anand.prohands.network.WagePredictionService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class ProfileViewModel(
    private val profileApi: ProfileApi,
    private val jobService: JobService,
    private val wagePredictionService: WagePredictionService,
    private val userWageProfileDao: UserWageProfileDao?
) : ViewModel() {

    companion object {
        private const val TAG = "ProfileViewModel"
    }

    private val _profile = MutableStateFlow<ClientProfileDto?>(null)
    val profile: StateFlow<ClientProfileDto?> = _profile

    private val _nearbyJobs = MutableStateFlow<List<JobResponse>>(emptyList())
    val nearbyJobs: StateFlow<List<JobResponse>> = _nearbyJobs

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    // AI Wage Prediction
    private val _aiRecommendedWage = MutableStateFlow<Double?>(null)
    val aiRecommendedWage: StateFlow<Double?> = _aiRecommendedWage

    private val _aiMonthlyEstimate = MutableStateFlow<Double?>(null)
    val aiMonthlyEstimate: StateFlow<Double?> = _aiMonthlyEstimate

    private val _isLoadingWage = MutableStateFlow(false)
    val isLoadingWage: StateFlow<Boolean> = _isLoadingWage

    private val _wageError = MutableStateFlow<String?>(null)
    val wageError: StateFlow<String?> = _wageError

    // Local wage profile
    private val _localWageProfile = MutableStateFlow<UserWageProfile?>(null)
    val localWageProfile: StateFlow<UserWageProfile?> = _localWageProfile

    private var currentUserId: String? = null

    fun fetchProfile(userId: String) {
        currentUserId = userId
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val response = profileApi.getProfile(userId)
                if (response.isSuccessful) {
                    _profile.value = response.body()
                    val userProfile = response.body()

                    // Fetch nearby jobs safely
                    if (userProfile?.latitude != null && userProfile.longitude != null) {
                        fetchNearbyJobsSafe(userProfile.latitude, userProfile.longitude)
                    }

                    // Load local wage profile and then fetch AI wage
                    loadLocalWageProfile(userId)
                } else {
                    _error.value = "Error loading profile: ${response.code()}"
                }
            } catch (e: UnknownHostException) {
                _error.value = "No internet connection"
                Log.e(TAG, "Network error", e)
            } catch (e: SocketTimeoutException) {
                _error.value = "Connection timed out"
                Log.e(TAG, "Timeout error", e)
            } catch (e: Exception) {
                _error.value = e.message ?: "An unexpected error occurred"
                Log.e(TAG, "Error fetching profile", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun loadLocalWageProfile(userId: String) {
        if (userWageProfileDao == null) {
            // If no DAO available, use defaults based on backend profile
            _profile.value?.let { fetchAiRecommendedWage(it, null) }
            return
        }

        viewModelScope.launch {
            try {
                userWageProfileDao.getWageProfile(userId)
                    .catch { e ->
                        Log.e(TAG, "Error loading local wage profile", e)
                        emit(null)
                    }
                    .collectLatest { wageProfile ->
                        _localWageProfile.value = wageProfile
                        // Fetch AI wage with local profile data
                        _profile.value?.let { fetchAiRecommendedWage(it, wageProfile) }
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Error in loadLocalWageProfile", e)
                _profile.value?.let { fetchAiRecommendedWage(it, null) }
            }
        }
    }

    private fun fetchNearbyJobsSafe(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            try {
                val response = jobService.getJobFeed(latitude, longitude, 0, 50)
                if (response.isSuccessful) {
                    _nearbyJobs.value = response.body() ?: emptyList()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching nearby jobs", e)
                // Silently fail - nearby jobs are not critical
            }
        }
    }

    fun fetchNearbyJobs(latitude: Double, longitude: Double) {
        fetchNearbyJobsSafe(latitude, longitude)
    }

    /**
     * Fetch AI recommended wage based on user profile.
     * Uses local wage profile if available, otherwise builds from backend profile.
     */
    private fun fetchAiRecommendedWage(profile: ClientProfileDto, localWageProfile: UserWageProfile?) {
        viewModelScope.launch {
            _isLoadingWage.value = true
            _wageError.value = null

            try {
                val request = if (localWageProfile != null) {
                    // Use local profile data
                    buildRequestFromLocalProfile(localWageProfile)
                } else {
                    // Build from backend profile
                    buildRequestFromBackendProfile(profile)
                }

                Log.d(TAG, "Requesting wage prediction: ${request.sector}, data=${request.data}")

                val response = wagePredictionService.predictWage(request)

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.success == true && body.predicted_wage != null) {
                        _aiRecommendedWage.value = body.predicted_wage
                        _aiMonthlyEstimate.value = body.monthly_estimate ?: (body.predicted_wage * 26)
                        Log.d(TAG, "AI Wage prediction: ${body.predicted_wage}")
                    } else {
                        handleWagePredictionError(body?.error, profile)
                    }
                } else {
                    handleWagePredictionError("Server error: ${response.code()}", profile)
                }
            } catch (e: UnknownHostException) {
                handleWagePredictionError("No internet connection", profile)
            } catch (e: SocketTimeoutException) {
                handleWagePredictionError("Connection timed out", profile)
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching wage prediction", e)
                handleWagePredictionError(e.message, profile)
            } finally {
                _isLoadingWage.value = false
            }
        }
    }

    private fun handleWagePredictionError(error: String?, profile: ClientProfileDto) {
        Log.e(TAG, "Wage prediction failed: $error")
        _wageError.value = error
        // Fall back to backend's recommendedWagePerHour if available
        _aiRecommendedWage.value = profile.recommendedWagePerHour
        if (profile.recommendedWagePerHour != null) {
            _aiMonthlyEstimate.value = profile.recommendedWagePerHour * 26
        }
    }

    private fun buildRequestFromLocalProfile(wageProfile: UserWageProfile): WagePredictionRequest {
        val data = if (wageProfile.sector == "agriculture") {
            mapOf(
                "age" to wageProfile.age,
                "experience_years" to wageProfile.experienceYears,
                "education_level" to wageProfile.educationLevel,
                "occupation" to wageProfile.occupation,
                "skill_level" to wageProfile.skillLevel,
                "state" to wageProfile.state,
                "working_hours" to wageProfile.workingHours,
                "employment_type" to mapEmploymentType(wageProfile.employmentType, "agriculture")
            )
        } else {
            mapOf(
                "age" to wageProfile.age,
                "experience_years" to wageProfile.experienceYears,
                "education_level" to wageProfile.educationLevel,
                "job_role" to wageProfile.jobRole,
                "skill_level" to wageProfile.skillLevel,
                "city_tier" to wageProfile.cityTier,
                "working_hours" to wageProfile.workingHours,
                "employment_type" to mapEmploymentType(wageProfile.employmentType, "construction"),
                "project_type" to wageProfile.projectType
            )
        }
        return WagePredictionRequest(sector = wageProfile.sector, data = data)
    }

    private fun mapEmploymentType(type: String, sector: String): String {
        // Map employment type to valid API values per sector
        return when (sector) {
            "agriculture" -> when (type.lowercase()) {
                "casual" -> "casual"
                "seasonal" -> "seasonal"
                "permanent" -> "permanent"
                else -> "casual"
            }
            "construction" -> when (type.lowercase()) {
                "casual" -> "casual"
                "contract" -> "contract"
                "permanent" -> "permanent"
                else -> "casual"
            }
            else -> "casual"
        }
    }

    private fun buildRequestFromBackendProfile(profile: ClientProfileDto): WagePredictionRequest {
        val sector = determineSector(profile.skills)
        val data = buildPredictionData(profile, sector)
        return WagePredictionRequest(sector = sector, data = data)
    }


    private fun determineSector(skills: List<String>?): String {
        if (skills.isNullOrEmpty()) return "construction"

        val skillsLower = skills.map { it.lowercase() }

        val agricultureKeywords = listOf(
            "farm", "harvest", "plough", "irrigation", "pesticide", "tractor",
            "agriculture", "farming", "crop", "field", "agricultural"
        )

        val constructionKeywords = listOf(
            "mason", "carpenter", "electrician", "plumber", "welder", "painter",
            "construction", "building", "steel", "helper", "laborer", "labour"
        )

        val agricultureScore = skillsLower.count { skill ->
            agricultureKeywords.any { keyword -> skill.contains(keyword) }
        }

        val constructionScore = skillsLower.count { skill ->
            constructionKeywords.any { keyword -> skill.contains(keyword) }
        }

        return if (agricultureScore > constructionScore) "agriculture" else "construction"
    }


    private fun buildPredictionData(profile: ClientProfileDto, sector: String): Map<String, Any> {
        val experienceYears = when (profile.experienceLevel?.lowercase()) {
            "expert" -> 10
            "intermediate" -> 5
            "beginner" -> 1
            else -> 2
        }

        val skillLevel = when {
            (profile.profileStrengthScore ?: 0) >= 80 -> 5
            (profile.profileStrengthScore ?: 0) >= 60 -> 4
            (profile.profileStrengthScore ?: 0) >= 40 -> 3
            (profile.profileStrengthScore ?: 0) >= 20 -> 2
            else -> 1
        }

        return if (sector == "agriculture") {
            mapOf(
                "age" to 30,
                "experience_years" to experienceYears,
                "education_level" to "secondary",
                "occupation" to mapSkillToAgricultureOccupation(profile.skills),
                "skill_level" to skillLevel,
                "state" to "MH",
                "working_hours" to 8,
                "employment_type" to "casual"
            )
        } else {
            mapOf(
                "age" to 30,
                "experience_years" to experienceYears,
                "education_level" to "secondary",
                "job_role" to mapSkillToConstructionRole(profile.skills),
                "skill_level" to skillLevel,
                "city_tier" to "Tier-2",
                "working_hours" to 8,
                "employment_type" to "casual",
                "project_type" to "residential"
            )
        }
    }

    private fun mapSkillToAgricultureOccupation(skills: List<String>?): String {
        if (skills.isNullOrEmpty()) return "farm labourer"
        val skillsLower = skills.map { it.lowercase() }
        return when {
            skillsLower.any { it.contains("tractor") } -> "tractor operator"
            skillsLower.any { it.contains("harvest") } -> "harvester"
            skillsLower.any { it.contains("plough") } -> "ploughman"
            skillsLower.any { it.contains("irrigation") } -> "irrigation assistant"
            skillsLower.any { it.contains("pesticide") || it.contains("spray") } -> "pesticide sprayer"
            else -> "farm labourer"
        }
    }

    private fun mapSkillToConstructionRole(skills: List<String>?): String {
        if (skills.isNullOrEmpty()) return "helper"
        val skillsLower = skills.map { it.lowercase() }
        return when {
            skillsLower.any { it.contains("electrician") || it.contains("electric") } -> "electrician"
            skillsLower.any { it.contains("plumber") || it.contains("plumbing") } -> "plumber"
            skillsLower.any { it.contains("carpenter") || it.contains("wood") } -> "carpenter"
            skillsLower.any { it.contains("mason") || it.contains("masonry") } -> "mason"
            skillsLower.any { it.contains("welder") || it.contains("welding") } -> "welder"
            skillsLower.any { it.contains("painter") || it.contains("paint") } -> "painter"
            skillsLower.any { it.contains("steel") } -> "steel fixer"
            skillsLower.any { it.contains("labor") || it.contains("labour") } -> "laborer"
            else -> "helper"
        }
    }

    /**
     * Refresh wage prediction - can be called from UI
     */
    fun refreshWagePrediction() {
        val profile = _profile.value ?: return
        val localProfile = _localWageProfile.value
        fetchAiRecommendedWage(profile, localProfile)
    }
}

class ProfileViewModelFactory(
    private val userWageProfileDao: UserWageProfileDao? = null
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            val profileApi = RetrofitClient.instance.create(ProfileApi::class.java)
            val jobService = RetrofitClient.instance.create(JobService::class.java)
            val wagePredictionService = try {
                WagePredictionClient.instance
            } catch (e: Exception) {
                Log.e("ProfileViewModelFactory", "Error creating wage prediction service", e)
                null
            }
            @Suppress("UNCHECKED_CAST")
            return ProfileViewModel(
                profileApi,
                jobService,
                wagePredictionService ?: createDummyWageService(),
                userWageProfileDao
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }

    // Fallback dummy service that returns null
    private fun createDummyWageService(): WagePredictionService {
        return object : WagePredictionService {
            override suspend fun healthCheck() = throw NotImplementedError()
            override suspend fun getConfig() = throw NotImplementedError()
            override suspend fun getSectors() = throw NotImplementedError()
            override suspend fun predictWage(request: WagePredictionRequest) = throw NotImplementedError()
            override suspend fun testPrediction(sector: String) = throw NotImplementedError()
        }
    }
}
