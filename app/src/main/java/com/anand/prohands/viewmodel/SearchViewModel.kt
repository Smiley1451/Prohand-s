package com.anand.prohands.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.anand.prohands.data.SearchResultDto
import com.anand.prohands.network.ProfileApi
import com.anand.prohands.network.RetrofitClient
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SearchViewModel(private val profileApi: ProfileApi) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<SearchResultDto>>(emptyList())
    val searchResults: StateFlow<List<SearchResultDto>> = _searchResults.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var searchJob: Job? = null
    private var currentPage = 0
    private var isLastPage = false

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()
        if (query.length >= 2) { // Start searching after 2 chars
            searchJob = viewModelScope.launch {
                delay(500) // Debounce
                performSearch(query, 0)
            }
        } else {
            _searchResults.value = emptyList()
        }
    }

    private fun performSearch(query: String, page: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                // Assuming no location for now, can be added later if needed
                val response = profileApi.searchClients(q = query, page = page, size = 10)
                if (response.isSuccessful) {
                    val searchResponse = response.body()
                    if (searchResponse != null) {
                        if (page == 0) {
                            _searchResults.value = searchResponse.items
                        } else {
                            _searchResults.value = _searchResults.value + searchResponse.items
                        }
                        currentPage = searchResponse.page
                        isLastPage = searchResponse.page >= searchResponse.totalPages - 1
                    }
                } else {
                    _error.value = "Search failed: ${response.code()}"
                }
            } catch (e: Exception) {
                _error.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun loadNextPage() {
        if (!_isLoading.value && !isLastPage && _searchQuery.value.length >= 2) {
             performSearch(_searchQuery.value, currentPage + 1)
        }
    }
}

class SearchViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SearchViewModel::class.java)) {
            val profileApi = RetrofitClient.instance.create(ProfileApi::class.java)
            @Suppress("UNCHECKED_CAST")
            return SearchViewModel(profileApi) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
