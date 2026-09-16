package com.appprobe.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.appprobe.inspection.AppInspectionInfo
import com.appprobe.inspection.AppInspectionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface AppInspectionUiState {
    object Loading : AppInspectionUiState
    data class Success(val info: AppInspectionInfo) : AppInspectionUiState
    data class Error(val message: String) : AppInspectionUiState
}

class AppInspectionViewModel(
    application: Application,
    private val packageName: String,
    private val repository: AppInspectionRepository = AppInspectionRepository(application)
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<AppInspectionUiState>(AppInspectionUiState.Loading)
    val uiState: StateFlow<AppInspectionUiState> = _uiState.asStateFlow()

    init {
        loadInspectionInfo()
    }

    fun loadInspectionInfo() {
        _uiState.value = AppInspectionUiState.Loading
        viewModelScope.launch {
            repository.getAppInspectionInfo(packageName)
                .onSuccess { info ->
                    _uiState.value = AppInspectionUiState.Success(info)
                }
                .onFailure { error ->
                    _uiState.value = AppInspectionUiState.Error(
                        error.localizedMessage ?: "Failed to inspect application."
                    )
                }
        }
    }

    class Factory(
        private val application: Application,
        private val packageName: String
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AppInspectionViewModel::class.java)) {
                return AppInspectionViewModel(application, packageName) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

