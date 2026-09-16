package com.appprobe.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.appprobe.testing.TargetApp
import com.appprobe.testing.TestAction
import com.appprobe.testing.TestActionType
import com.appprobe.testing.TestScenario
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ScenarioBuilderUiState(
    val target: TargetApp,
    val scenarioName: String,
    val actions: List<TestAction> = emptyList()
)

class ScenarioBuilderViewModel(
    val target: TargetApp
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ScenarioBuilderUiState(
            target = target,
            scenarioName = "${target.appName} Test Scenario",
            actions = listOf(
                TestAction(type = TestActionType.LAUNCH_APP),
                TestAction(type = TestActionType.WAIT, parameter = "2000 ms")
            )
        )
    )
    val uiState: StateFlow<ScenarioBuilderUiState> = _uiState.asStateFlow()

    fun updateScenarioName(name: String) {
        _uiState.value = _uiState.value.copy(scenarioName = name)
    }

    fun addAction(type: TestActionType, parameter: String = "") {
        val currentList = _uiState.value.actions.toMutableList()
        val defaultParam = when (type) {
            TestActionType.WAIT -> if (parameter.isBlank()) "2000 ms" else parameter
            TestActionType.SWIPE -> if (parameter.isBlank()) "Swipe Up" else parameter
            TestActionType.ROTATE -> if (parameter.isBlank()) "Landscape" else parameter
            TestActionType.BACKGROUND_APP -> if (parameter.isBlank()) "3000 ms" else parameter
            TestActionType.TAP -> if (parameter.isBlank()) "Center (500, 1000)" else parameter
            else -> parameter
        }
        currentList.add(TestAction(type = type, parameter = defaultParam))
        _uiState.value = _uiState.value.copy(actions = currentList)
    }

    fun removeAction(actionId: String) {
        val updated = _uiState.value.actions.filterNot { it.id == actionId }
        _uiState.value = _uiState.value.copy(actions = updated)
    }

    fun moveActionUp(index: Int) {
        if (index <= 0 || index >= _uiState.value.actions.size) return
        val currentList = _uiState.value.actions.toMutableList()
        val item = currentList.removeAt(index)
        currentList.add(index - 1, item)
        _uiState.value = _uiState.value.copy(actions = currentList)
    }

    fun moveActionDown(index: Int) {
        if (index < 0 || index >= _uiState.value.actions.size - 1) return
        val currentList = _uiState.value.actions.toMutableList()
        val item = currentList.removeAt(index)
        currentList.add(index + 1, item)
        _uiState.value = _uiState.value.copy(actions = currentList)
    }

    fun clearScenario() {
        _uiState.value = _uiState.value.copy(actions = emptyList())
    }

    fun toTestScenario(): TestScenario {
        val state = _uiState.value
        return TestScenario(
            name = state.scenarioName,
            targetPackage = state.target.packageName,
            actions = state.actions
        )
    }

    class Factory(private val target: TargetApp) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ScenarioBuilderViewModel::class.java)) {
                return ScenarioBuilderViewModel(target) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

