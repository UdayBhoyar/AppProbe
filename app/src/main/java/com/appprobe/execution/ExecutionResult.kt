package com.appprobe.execution

import com.appprobe.testing.TestAction

/**
 * Status of an individual action during scenario execution.
 */
enum class ActionExecutionStatus {
    PENDING,
    RUNNING,
    PASSED,
    FAILED,
    CANCELLED
}

/**
 * Execution outcome for a single action in a scenario.
 */
data class ActionResult(
    val action: TestAction,
    val index: Int,
    val success: Boolean,
    val message: String = "",
    val durationMs: Long = 0L
)

/**
 * Overall status of a scenario execution run.
 */
enum class ExecutionStatus {
    IDLE,
    RUNNING,
    COMPLETED,
    FAILED,
    CANCELLED
}

/**
 * Complete result of a scenario execution.
 */
data class ScenarioExecutionResult(
    val targetPackage: String,
    val scenarioName: String,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val actionResults: List<ActionResult> = emptyList(),
    val status: ExecutionStatus
) {
    val totalDurationMs: Long
        get() = if (endTimeMs > startTimeMs) endTimeMs - startTimeMs else 0L

    val passedCount: Int
        get() = actionResults.count { it.success }

    val failedCount: Int
        get() = actionResults.count { !it.success }
}
