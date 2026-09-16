package com.appprobe.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.appprobe.inspection.AppDiscoveryRepository
import com.appprobe.inspection.AppInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class AppFilterType {
    ALL, USER, SYSTEM
}

sealed interface AppDiscoveryUiState {
    object Loading : AppDiscoveryUiState
    data class Success(
        val allApps: List<AppInfo>,
        val filteredApps: List<AppInfo>,
        val searchQuery: String = "",
        val filterType: AppFilterType = AppFilterType.ALL
    ) : AppDiscoveryUiState
    data class Error(val message: String) : AppDiscoveryUiState
}

class AppDiscoveryViewModel(
    application: Application,
    private val repository: AppDiscoveryRepository = AppDiscoveryRepository(application)
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<AppDiscoveryUiState>(AppDiscoveryUiState.Loading)
    val uiState: StateFlow<AppDiscoveryUiState> = _uiState.asStateFlow()

    init {
        loadApplications()
    }

    fun loadApplications() {
        _uiState.value = AppDiscoveryUiState.Loading
        viewModelScope.launch {
            repository.getInstalledApplications()
                .onSuccess { apps ->
                    val currentState = _uiState.value
                    val searchQuery = (currentState as? AppDiscoveryUiState.Success)?.searchQuery ?: ""
                    val filterType = (currentState as? AppDiscoveryUiState.Success)?.filterType ?: AppFilterType.ALL
                    val filtered = applyFilter(apps, searchQuery, filterType)
                    _uiState.value = AppDiscoveryUiState.Success(
                        allApps = apps,
                        filteredApps = filtered,
                        searchQuery = searchQuery,
                        filterType = filterType
                    )
                }
                .onFailure { error ->
                    _uiState.value = AppDiscoveryUiState.Error(
                        error.localizedMessage ?: "Failed to discover installed applications."
                    )
                }
        }
    }

    fun onSearchQueryChanged(query: String) {
        val state = _uiState.value as? AppDiscoveryUiState.Success ?: return
        val filtered = applyFilter(state.allApps, query, state.filterType)
        _uiState.value = state.copy(
            searchQuery = query,
            filteredApps = filtered
        )
    }

    fun onFilterTypeChanged(filterType: AppFilterType) {
        val state = _uiState.value as? AppDiscoveryUiState.Success ?: return
        val filtered = applyFilter(state.allApps, state.searchQuery, filterType)
        _uiState.value = state.copy(
            filterType = filterType,
            filteredApps = filtered
        )
    }

    private fun applyFilter(
        apps: List<AppInfo>,
        query: String,
        filterType: AppFilterType
    ): List<AppInfo> {
        return apps.filter { app ->
            val matchesFilter = when (filterType) {
                AppFilterType.ALL -> true
                AppFilterType.USER -> !app.isSystemApp
                AppFilterType.SYSTEM -> app.isSystemApp
            }
            val matchesQuery = query.isBlank() ||
                    app.appName.contains(query, ignoreCase = true) ||
                    app.packageName.contains(query, ignoreCase = true)
            matchesFilter && matchesQuery
        }
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AppDiscoveryViewModel::class.java)) {
                return AppDiscoveryViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

