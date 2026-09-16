package com.appprobe.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.appprobe.execution.ExecutionEngine
import com.appprobe.execution.ExecutionProgress
import com.appprobe.execution.ExecutionStatus
import com.appprobe.testing.TargetApp
import com.appprobe.testing.TestScenario
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ScenarioExecutionViewModel(
    application: Application,
    val target: TargetApp,
    val scenario: TestScenario,
    private val executionEngine: ExecutionEngine = ExecutionEngine(application)
) : AndroidViewModel(application) {

    val executionProgress: StateFlow<ExecutionProgress> = executionEngine.progress
    private var executionJob: Job? = null

    init {
        // Auto-start scenario execution upon entering the screen
        startExecution()
    }

    fun startExecution() {
        if (executionProgress.value.status == ExecutionStatus.RUNNING) return
        executionJob?.cancel()
        executionJob = viewModelScope.launch {
            executionEngine.executeScenario(scenario)
        }
    }

    fun cancelExecution() {
        executionJob?.cancel()
    }

    class Factory(
        private val application: Application,
        private val target: TargetApp,
        private val scenario: TestScenario
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ScenarioExecutionViewModel::class.java)) {
                return ScenarioExecutionViewModel(application, target, scenario) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
