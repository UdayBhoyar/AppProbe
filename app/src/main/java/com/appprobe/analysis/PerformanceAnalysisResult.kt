package com.appprobe.analysis

/**
 * Suspicion classification based on performance evidence analysis.
 * Explicitly avoids claiming definitive memory leak proof.
 */
enum class AnalysisSuspicionStatus(val displayName: String) {
    INSUFFICIENT_DATA("Insufficient Data"),
    STABLE("Stable Memory Profile"),
    MEMORY_GROWTH_DETECTED("Memory Growth Detected"),
    POSSIBLE_MEMORY_RETENTION("Possible Memory Retention")
}

/**
 * General trajectory of memory measurements over the scenario run.
 */
enum class MemoryTrend(val displayName: String) {
    STABLE("Stable"),
    INCREASING("Increasing"),
    DECREASING("Decreasing"),
    FLUCTUATING("Fluctuating"),
    UNKNOWN("Unknown")
}

/**
 * Result of performance and memory-trend analysis on a completed scenario run.
 */
data class PerformanceAnalysisResult(
    val status: AnalysisSuspicionStatus,
    val summaryReason: String,
    val validSampleCount: Int = 0,
    val initialTotalPssKb: Long? = null,
    val finalTotalPssKb: Long? = null,
    val minTotalPssKb: Long? = null,
    val maxTotalPssKb: Long? = null,
    val avgTotalPssKb: Long? = null,
    val growthTotalPssKb: Long? = null,
    val growthPercentage: Double? = null,
    val trend: MemoryTrend = MemoryTrend.UNKNOWN
) {
    val initialTotalPssMb: Double?
        get() = initialTotalPssKb?.let { it / 1024.0 }

    val finalTotalPssMb: Double?
        get() = finalTotalPssKb?.let { it / 1024.0 }

    val minTotalPssMb: Double?
        get() = minTotalPssKb?.let { it / 1024.0 }

    val maxTotalPssMb: Double?
        get() = maxTotalPssKb?.let { it / 1024.0 }

    val avgTotalPssMb: Double?
        get() = avgTotalPssKb?.let { it / 1024.0 }

    val growthTotalPssMb: Double?
        get() = growthTotalPssKb?.let { it / 1024.0 }

    val isAnalyzable: Boolean
        get() = status != AnalysisSuspicionStatus.INSUFFICIENT_DATA
}

