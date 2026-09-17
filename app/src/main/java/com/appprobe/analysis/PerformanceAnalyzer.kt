package com.appprobe.analysis

import com.appprobe.execution.ExecutionStatus
import com.appprobe.monitoring.PerformanceSample

/**
 * Deterministic analyzer for evaluating performance evidence collected during test scenarios.
 * Identifies suspicious memory-growth and retention patterns without claiming definitive memory leaks.
 */
object PerformanceAnalyzer {

    // Centralized Analysis Thresholds
    const val MIN_SAMPLES_FOR_ANALYSIS = 3
    const val GROWTH_THRESHOLD_PERCENT = 5.0
    const val RETENTION_THRESHOLD_PERCENT = 15.0
    const val RETENTION_TAIL_FRACTION = 0.3
    const val TAIL_ELEVATION_RATIO = 0.85

    /**
     * Analyzes collected performance samples and produces a conservative suspicion result.
     */
    fun analyze(
        samples: List<PerformanceSample>,
        executionStatus: ExecutionStatus = ExecutionStatus.COMPLETED
    ): PerformanceAnalysisResult {
        // 1. Filter valid samples with non-null Total PSS
        val validSamples = samples.filter {
            it.memory.totalPssKb != null && it.memory.totalPssKb > 0
        }

        // 2. Handle cancelled execution or insufficient data
        if (executionStatus == ExecutionStatus.CANCELLED) {
            return PerformanceAnalysisResult(
                status = AnalysisSuspicionStatus.INSUFFICIENT_DATA,
                summaryReason = "Scenario execution was cancelled before completing a full analysis cycle.",
                validSampleCount = validSamples.size
            )
        }

        if (validSamples.size < MIN_SAMPLES_FOR_ANALYSIS) {
            return PerformanceAnalysisResult(
                status = AnalysisSuspicionStatus.INSUFFICIENT_DATA,
                summaryReason = if (samples.isEmpty()) {
                    "No performance samples were collected during execution."
                } else if (validSamples.isEmpty()) {
                    "Memory metrics were unavailable from the target process."
                } else {
                    "Insufficient valid memory samples collected (${validSamples.size} of $MIN_SAMPLES_FOR_ANALYSIS required)."
                },
                validSampleCount = validSamples.size
            )
        }

        // 3. Compute baseline metrics
        val pssValues = validSamples.mapNotNull { it.memory.totalPssKb }
        val initialPss = pssValues.first()
        val finalPss = pssValues.last()
        val minPss = pssValues.min()
        val maxPss = pssValues.max()
        val avgPss = pssValues.average().toLong()

        val growthKb = finalPss - initialPss
        val growthPercent = if (initialPss > 0) {
            ((finalPss - initialPss).toDouble() / initialPss.toDouble()) * 100.0
        } else {
            0.0
        }
        val roundedGrowthPercent = (growthPercent * 10.0).toInt() / 10.0

        // 4. Evaluate Trend
        val trend = evaluateTrend(pssValues, avgPss)

        // 5. Evaluate Tail Retention
        val tailCount = maxOf(1, (pssValues.size * RETENTION_TAIL_FRACTION).toInt())
        val tailAvg = pssValues.takeLast(tailCount).average()
        val isTailElevated = maxPss > 0 && (tailAvg >= maxPss * TAIL_ELEVATION_RATIO)

        // 6. Conservative Classification
        return when {
            roundedGrowthPercent >= RETENTION_THRESHOLD_PERCENT && isTailElevated -> {
                PerformanceAnalysisResult(
                    status = AnalysisSuspicionStatus.POSSIBLE_MEMORY_RETENTION,
                    summaryReason = "Memory remains elevated near peak through the end of the test scenario (+${String.format("%.1f", roundedGrowthPercent)}% growth).",
                    validSampleCount = validSamples.size,
                    initialTotalPssKb = initialPss,
                    finalTotalPssKb = finalPss,
                    minTotalPssKb = minPss,
                    maxTotalPssKb = maxPss,
                    avgTotalPssKb = avgPss,
                    growthTotalPssKb = growthKb,
                    growthPercentage = roundedGrowthPercent,
                    trend = trend
                )
            }

            roundedGrowthPercent >= GROWTH_THRESHOLD_PERCENT && trend == MemoryTrend.INCREASING -> {
                PerformanceAnalysisResult(
                    status = AnalysisSuspicionStatus.MEMORY_GROWTH_DETECTED,
                    summaryReason = "Overall memory trend shows sustained upward growth of +${String.format("%.1f", roundedGrowthPercent)}% across execution.",
                    validSampleCount = validSamples.size,
                    initialTotalPssKb = initialPss,
                    finalTotalPssKb = finalPss,
                    minTotalPssKb = minPss,
                    maxTotalPssKb = maxPss,
                    avgTotalPssKb = avgPss,
                    growthTotalPssKb = growthKb,
                    growthPercentage = roundedGrowthPercent,
                    trend = trend
                )
            }

            else -> {
                PerformanceAnalysisResult(
                    status = AnalysisSuspicionStatus.STABLE,
                    summaryReason = "Memory usage remained within expected operating variance with no sustained retention pattern.",
                    validSampleCount = validSamples.size,
                    initialTotalPssKb = initialPss,
                    finalTotalPssKb = finalPss,
                    minTotalPssKb = minPss,
                    maxTotalPssKb = maxPss,
                    avgTotalPssKb = avgPss,
                    growthTotalPssKb = growthKb,
                    growthPercentage = roundedGrowthPercent,
                    trend = trend
                )
            }
        }
    }

    private fun evaluateTrend(values: List<Long>, avgPss: Long): MemoryTrend {
        if (values.size < 2) return MemoryTrend.UNKNOWN

        var positiveSteps = 0
        var negativeSteps = 0
        for (i in 1 until values.size) {
            val delta = values[i] - values[i - 1]
            if (delta > 0) positiveSteps++
            else if (delta < 0) negativeSteps++
        }

        val totalSteps = values.size - 1
        val minPss = values.min()
        val maxPss = values.max()
        val varianceFraction = if (avgPss > 0) (maxPss - minPss).toDouble() / avgPss.toDouble() else 0.0

        return when {
            varianceFraction <= 0.05 -> MemoryTrend.STABLE
            positiveSteps.toDouble() / totalSteps >= 0.65 -> MemoryTrend.INCREASING
            negativeSteps.toDouble() / totalSteps >= 0.65 -> MemoryTrend.DECREASING
            else -> MemoryTrend.FLUCTUATING
        }
    }
}

