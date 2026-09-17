package com.appprobe.monitoring

/**
 * Detailed memory metrics for the target process.
 * Explicitly separates Total PSS, Dalvik/Java Heap PSS, Native Heap PSS, and other allocations.
 */
data class ProcessMemoryMetrics(
    val totalPssKb: Long? = null,
    val dalvikPssKb: Long? = null,
    val nativePssKb: Long? = null,
    val otherPssKb: Long? = null
) {
    val totalPssMb: Double?
        get() = totalPssKb?.let { it / 1024.0 }

    val dalvikPssMb: Double?
        get() = dalvikPssKb?.let { it / 1024.0 }

    val nativePssMb: Double?
        get() = nativePssKb?.let { it / 1024.0 }

    val isAvailable: Boolean
        get() = totalPssKb != null || dalvikPssKb != null || nativePssKb != null
}

/**
 * A single timestamped resource measurement of the target process.
 */
data class PerformanceSample(
    val timestamp: Long = System.currentTimeMillis(),
    val targetPackage: String,
    val pid: Int,
    val actionIndex: Int? = null,
    val actionType: String? = null,
    val memory: ProcessMemoryMetrics = ProcessMemoryMetrics(),
    val cpuUsagePercent: Double? = null,
    val threadCount: Int? = null
)

/**
 * Compact statistical summary of performance samples collected over a test scenario run.
 */
data class MonitoringSummary(
    val sampleCount: Int = 0,
    val peakTotalPssKb: Long? = null,
    val minTotalPssKb: Long? = null,
    val avgTotalPssKb: Long? = null,
    val peakDalvikPssKb: Long? = null,
    val peakNativePssKb: Long? = null,
    val peakCpuPercent: Double? = null,
    val peakThreadCount: Int? = null
) {
    val peakTotalPssMb: Double?
        get() = peakTotalPssKb?.let { it / 1024.0 }

    val minTotalPssMb: Double?
        get() = minTotalPssKb?.let { it / 1024.0 }

    val avgTotalPssMb: Double?
        get() = avgTotalPssKb?.let { it / 1024.0 }

    val peakDalvikPssMb: Double?
        get() = peakDalvikPssKb?.let { it / 1024.0 }

    val peakNativePssMb: Double?
        get() = peakNativePssKb?.let { it / 1024.0 }

    companion object {
        fun fromSamples(samples: List<PerformanceSample>): MonitoringSummary {
            if (samples.isEmpty()) return MonitoringSummary()

            val validTotalPss = samples.mapNotNull { it.memory.totalPssKb }
            val validDalvikPss = samples.mapNotNull { it.memory.dalvikPssKb }
            val validNativePss = samples.mapNotNull { it.memory.nativePssKb }
            val validCpu = samples.mapNotNull { it.cpuUsagePercent }
            val validThreads = samples.mapNotNull { it.threadCount }

            return MonitoringSummary(
                sampleCount = samples.size,
                peakTotalPssKb = validTotalPss.maxOrNull(),
                minTotalPssKb = validTotalPss.minOrNull(),
                avgTotalPssKb = if (validTotalPss.isNotEmpty()) validTotalPss.average().toLong() else null,
                peakDalvikPssKb = validDalvikPss.maxOrNull(),
                peakNativePssKb = validNativePss.maxOrNull(),
                peakCpuPercent = validCpu.maxOrNull(),
                peakThreadCount = validThreads.maxOrNull()
            )
        }
    }
}
