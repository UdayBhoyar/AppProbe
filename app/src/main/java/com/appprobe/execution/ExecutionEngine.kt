package com.appprobe.execution

import android.content.Context
import android.content.Intent
import com.appprobe.monitoring.MonitoringSummary
import com.appprobe.monitoring.PerformanceMonitor
import com.appprobe.monitoring.PerformanceSample
import com.appprobe.testing.TestAction
import com.appprobe.testing.TestActionType
import com.appprobe.testing.TestScenario
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Live execution progress emitted during scenario execution.
 */
data class ExecutionProgress(
    val status: ExecutionStatus = ExecutionStatus.IDLE,
    val scenario: TestScenario? = null,
    val currentIndex: Int = 0,
    val currentAction: TestAction? = null,
    val completedCount: Int = 0,
    val totalCount: Int = 0,
    val actionStatuses: Map<String, ActionExecutionStatus> = emptyMap(),
    val currentMessage: String = "",
    val livePerformanceSample: PerformanceSample? = null,
    val sampleCount: Int = 0,
    val result: ScenarioExecutionResult? = null
)

class ExecutionEngine(
    private val context: Context,
    private val commandExecutor: CommandExecutor = DeviceShellExecutor(),
    private val performanceMonitor: PerformanceMonitor = PerformanceMonitor(context, commandExecutor)
) {

    private val _progress = MutableStateFlow(ExecutionProgress())
    val progress: StateFlow<ExecutionProgress> = _progress.asStateFlow()

    suspend fun executeScenario(scenario: TestScenario): ScenarioExecutionResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()

        // Validation
        if (scenario.targetPackage.isBlank()) {
            val failureResult = ScenarioExecutionResult(
                targetPackage = scenario.targetPackage,
                scenarioName = scenario.name,
                startTimeMs = startTime,
                endTimeMs = System.currentTimeMillis(),
                status = ExecutionStatus.FAILED
            )
            _progress.value = ExecutionProgress(
                status = ExecutionStatus.FAILED,
                scenario = scenario,
                currentMessage = "Target package cannot be blank",
                result = failureResult
            )
            return@withContext failureResult
        }

        if (scenario.actions.isEmpty()) {
            val failureResult = ScenarioExecutionResult(
                targetPackage = scenario.targetPackage,
                scenarioName = scenario.name,
                startTimeMs = startTime,
                endTimeMs = System.currentTimeMillis(),
                status = ExecutionStatus.FAILED
            )
            _progress.value = ExecutionProgress(
                status = ExecutionStatus.FAILED,
                scenario = scenario,
                currentMessage = "Scenario must contain at least one action",
                result = failureResult
            )
            return@withContext failureResult
        }

        // Initialize progress state
        val initialStatuses = scenario.actions.associate { it.id to ActionExecutionStatus.PENDING }.toMutableMap()
        _progress.value = ExecutionProgress(
            status = ExecutionStatus.RUNNING,
            scenario = scenario,
            currentIndex = 0,
            currentAction = scenario.actions.firstOrNull(),
            completedCount = 0,
            totalCount = scenario.actions.size,
            actionStatuses = initialStatuses,
            currentMessage = "Starting scenario execution..."
        )

        val actionResults = mutableListOf<ActionResult>()
        var overallStatus = ExecutionStatus.COMPLETED
        var activeActionInfo: Pair<Int, String>? = null

        // Start performance monitoring in background
        var sampleCollectorJob: Job? = null
        performanceMonitor.startMonitoring(this, scenario.targetPackage) { activeActionInfo }
        sampleCollectorJob = launch {
            performanceMonitor.liveSample.collect { sample ->
                if (sample != null) {
                    _progress.value = _progress.value.copy(
                        livePerformanceSample = sample,
                        sampleCount = _progress.value.sampleCount + 1
                    )
                }
            }
        }

        try {
            for ((index, action) in scenario.actions.withIndex()) {
                activeActionInfo = Pair(index + 1, action.type.displayName)
                // Update current action state
                initialStatuses[action.id] = ActionExecutionStatus.RUNNING
                _progress.value = _progress.value.copy(
                    currentIndex = index + 1,
                    currentAction = action,
                    actionStatuses = HashMap(initialStatuses),
                    currentMessage = "Executing ${action.type.displayName}..."
                )

                val actionStartTime = System.currentTimeMillis()
                val stepResult = executeSingleAction(action, scenario.targetPackage)
                val actionDuration = System.currentTimeMillis() - actionStartTime

                val finalActionResult = ActionResult(
                    action = action,
                    index = index,
                    success = stepResult.success,
                    message = stepResult.message,
                    durationMs = actionDuration
                )
                actionResults.add(finalActionResult)

                if (stepResult.success) {
                    initialStatuses[action.id] = ActionExecutionStatus.PASSED
                    _progress.value = _progress.value.copy(
                        completedCount = index + 1,
                        actionStatuses = HashMap(initialStatuses),
                        currentMessage = "${action.type.displayName} completed"
                    )
                } else {
                    initialStatuses[action.id] = ActionExecutionStatus.FAILED
                    _progress.value = _progress.value.copy(
                        actionStatuses = HashMap(initialStatuses),
                        currentMessage = "Failed: ${stepResult.message}"
                    )
                    overallStatus = ExecutionStatus.FAILED
                    break
                }
            }
        } catch (e: CancellationException) {
            overallStatus = ExecutionStatus.CANCELLED
            for (action in scenario.actions) {
                if (initialStatuses[action.id] == ActionExecutionStatus.PENDING ||
                    initialStatuses[action.id] == ActionExecutionStatus.RUNNING
                ) {
                    initialStatuses[action.id] = ActionExecutionStatus.CANCELLED
                }
            }
            _progress.value = _progress.value.copy(
                status = ExecutionStatus.CANCELLED,
                actionStatuses = HashMap(initialStatuses),
                currentMessage = "Execution cancelled"
            )
            throw e
        } catch (e: Exception) {
            overallStatus = ExecutionStatus.FAILED
            _progress.value = _progress.value.copy(
                status = ExecutionStatus.FAILED,
                currentMessage = "Execution error: ${e.localizedMessage}"
            )
        } finally {
            sampleCollectorJob.cancel()
            val collectedSamples = performanceMonitor.stopMonitoring()
            val monitoringSummary = MonitoringSummary.fromSamples(collectedSamples)

            val endTime = System.currentTimeMillis()
            val finalResult = ScenarioExecutionResult(
                targetPackage = scenario.targetPackage,
                scenarioName = scenario.name,
                startTimeMs = startTime,
                endTimeMs = endTime,
                actionResults = actionResults,
                status = overallStatus,
                performanceSamples = collectedSamples,
                monitoringSummary = monitoringSummary
            )

            _progress.value = _progress.value.copy(
                status = overallStatus,
                currentMessage = if (overallStatus == ExecutionStatus.COMPLETED) "Scenario execution completed successfully" else "Scenario execution failed",
                result = finalResult
            )
        }

        return@withContext _progress.value.result ?: ScenarioExecutionResult(
            targetPackage = scenario.targetPackage,
            scenarioName = scenario.name,
            startTimeMs = startTime,
            endTimeMs = System.currentTimeMillis(),
            status = overallStatus
        )
    }


    private data class StepResult(val success: Boolean, val message: String)

    private suspend fun executeSingleAction(action: TestAction, targetPackage: String): StepResult {
        return when (action.type) {
            TestActionType.LAUNCH_APP -> {
                // 1. Try standard launcher intent
                val launchIntent = context.packageManager.getLaunchIntentForPackage(targetPackage)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                    delay(1200L)
                    return StepResult(true, "Launched $targetPackage")
                }

                // 2. Try querying main activities for the package
                val mainIntent = Intent(Intent.ACTION_MAIN).apply {
                    `package` = targetPackage
                }
                val resolveInfo = context.packageManager.queryIntentActivities(mainIntent, 0).firstOrNull()
                if (resolveInfo != null) {
                    val component = android.content.ComponentName(
                        resolveInfo.activityInfo.packageName,
                        resolveInfo.activityInfo.name
                    )
                    val explicitIntent = Intent(Intent.ACTION_MAIN).apply {
                        this.component = component
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(explicitIntent)
                    delay(1200L)
                    return StepResult(true, "Launched ${component.flattenToShortString()}")
                }

                // 3. Fallback to shell execution
                val shellResult = commandExecutor.execute("monkey -p $targetPackage 1")
                if (shellResult.success || shellResult.exitCode == 0) {
                    delay(1000L)
                    StepResult(true, "Launched $targetPackage via monkey")
                } else {
                    val amResult = commandExecutor.execute("am start -a android.intent.action.MAIN -c android.intent.category.LAUNCHER -p $targetPackage")
                    if (amResult.success || amResult.exitCode == 0) {
                        delay(1000L)
                        StepResult(true, "Launched $targetPackage via shell")
                    } else {
                        StepResult(false, "Could not launch $targetPackage (no launchable activity)")
                    }
                }
            }

            TestActionType.WAIT -> {
                val duration = parseWaitDuration(action.parameter)
                delay(duration)
                StepResult(true, "Waited ${duration}ms")
            }

            TestActionType.TAP -> {
                val (x, y) = parseCoordinates(action.parameter)
                val result = commandExecutor.execute("input tap $x $y")
                delay(300L)
                if (result.success || result.exitCode == 0) {
                    StepResult(true, "Tapped at ($x, $y)")
                } else {
                    StepResult(false, result.errorMessage ?: "Tap failed at ($x, $y)")
                }
            }

            TestActionType.SWIPE -> {
                val (x1, y1, x2, y2, duration) = parseSwipeParams(action.parameter)
                val result = commandExecutor.execute("input swipe $x1 $y1 $x2 $y2 $duration")
                delay(400L)
                if (result.success || result.exitCode == 0) {
                    StepResult(true, "Swiped from ($x1, $y1) to ($x2, $y2)")
                } else {
                    StepResult(false, result.errorMessage ?: "Swipe failed")
                }
            }

            TestActionType.PRESS_BACK -> {
                val result = commandExecutor.execute("input keyevent 4")
                delay(400L)
                if (result.success || result.exitCode == 0) {
                    StepResult(true, "Sent Back key event")
                } else {
                    StepResult(false, result.errorMessage ?: "Press Back failed")
                }
            }

            TestActionType.ROTATE -> {
                val orientation = if (action.parameter.contains("Portrait", ignoreCase = true)) 0 else 1
                commandExecutor.execute("settings put system accelerometer_rotation 0")
                val result = commandExecutor.execute("settings put system user_rotation $orientation")
                delay(1000L)
                if (result.success || result.exitCode == 0) {
                    StepResult(true, "Rotated device screen")
                } else {
                    StepResult(false, result.errorMessage ?: "Screen rotation failed")
                }
            }

            TestActionType.BACKGROUND_APP -> {
                val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(homeIntent)
                delay(1000L)
                StepResult(true, "Sent app to background")
            }

            TestActionType.RESUME_APP -> {
                val launchIntent = context.packageManager.getLaunchIntentForPackage(targetPackage)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                    context.startActivity(launchIntent)
                    delay(1000L)
                    StepResult(true, "Resumed $targetPackage")
                } else {
                    val result = commandExecutor.execute("am start -a android.intent.action.MAIN -c android.intent.category.LAUNCHER -p $targetPackage")
                    delay(1000L)
                    StepResult(result.success || result.exitCode == 0, "Resumed $targetPackage via shell")
                }
            }
        }
    }

    private fun parseWaitDuration(param: String): Long {
        val digits = param.filter { it.isDigit() }
        return digits.toLongOrNull() ?: 2000L
    }

    private fun parseCoordinates(param: String): Pair<Int, Int> {
        val numbers = Regex("\\d+").findAll(param).map { it.value.toInt() }.toList()
        return if (numbers.size >= 2) {
            Pair(numbers[0], numbers[1])
        } else {
            Pair(500, 1000)
        }
    }

    private fun parseSwipeParams(param: String): List<Int> {
        val numbers = Regex("\\d+").findAll(param).map { it.value.toInt() }.toList()
        return if (numbers.size >= 5) {
            numbers.take(5)
        } else if (numbers.size >= 4) {
            numbers.take(4) + 500
        } else if (param.contains("Down", ignoreCase = true)) {
            listOf(500, 500, 500, 1500, 500)
        } else {
            listOf(500, 1500, 500, 500, 500) // Default Swipe Up
        }
    }
}
