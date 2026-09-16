package com.appprobe.ui

import androidx.lifecycle.ViewModel
import com.appprobe.testing.TargetApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface TargetAppState {
    object None : TargetAppState
    data class Ready(val target: TargetApp) : TargetAppState
}

class TargetAppViewModel : ViewModel() {

    private val _targetState = MutableStateFlow<TargetAppState>(TargetAppState.None)
    val targetState: StateFlow<TargetAppState> = _targetState.asStateFlow()

    fun confirmTarget(target: TargetApp) {
        _targetState.value = TargetAppState.Ready(target)
    }

    fun clearTarget() {
        _targetState.value = TargetAppState.None
    }
}

