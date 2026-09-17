package com.appprobe.analysis

import com.appprobe.execution.ExecutionStatus
import com.appprobe.monitoring.PerformanceSample
import com.appprobe.monitoring.ProcessMemoryMetrics
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PerformanceAnalyzerTest {

    private fun createSample(totalPssKb: Long?, index: Int = 1): PerformanceSample {
        return PerformanceSample(
            timestamp = System.currentTimeMillis() + (index * 1000L),
            targetPackage = "com.test.app",
            pid = 1234,
            actionIndex = index,
            actionType = "Wait",
            memory = ProcessMemoryMetrics(
                totalPssKb = totalPssKb,
                dalvikPssKb = totalPssKb?.let { it / 2 },
                nativePssKb = totalPssKb?.let { it / 4 }
            )
        )
    }

    @Test
    fun testInsufficientDataWhenTooFewSamples() {
        val samples = listOf(
            createSample(100_000L, 1),
            createSample(105_000L, 2)
        )
        val result = PerformanceAnalyzer.analyze(samples)
        assertEquals(AnalysisSuspicionStatus.INSUFFICIENT_DATA, result.status)
        assertEquals(2, result.validSampleCount)
    }

    @Test
    fun testInsufficientDataWhenCancelled() {
        val samples = listOf(
            createSample(100_000L, 1),
            createSample(110_000L, 2),
            createSample(120_000L, 3),
            createSample(130_000L, 4)
        )
        val result = PerformanceAnalyzer.analyze(samples, executionStatus = ExecutionStatus.CANCELLED)
        assertEquals(AnalysisSuspicionStatus.INSUFFICIENT_DATA, result.status)
    }

    @Test
    fun testStableMemoryProfile() {
        val samples = listOf(
            createSample(100_000L, 1),
            createSample(101_000L, 2),
            createSample(99_500L, 3),
            createSample(100_500L, 4),
            createSample(100_000L, 5)
        )
        val result = PerformanceAnalyzer.analyze(samples)
        assertEquals(AnalysisSuspicionStatus.STABLE, result.status)
        assertEquals(MemoryTrend.STABLE, result.trend)
        assertEquals(100_000L, result.initialTotalPssKb)
        assertEquals(100_000L, result.finalTotalPssKb)
        assertEquals(0L, result.growthTotalPssKb)
        assertEquals(0.0, result.growthPercentage ?: 0.0, 0.1)
    }

    @Test
    fun testMemoryGrowthDetected() {
        val samples = listOf(
            createSample(100_000L, 1),
            createSample(103_000L, 2),
            createSample(106_000L, 3),
            createSample(109_000L, 4),
            createSample(110_000L, 5) // +10% growth (< 15% retention threshold, but > 5% growth threshold)
        )
        val result = PerformanceAnalyzer.analyze(samples)
        assertEquals(AnalysisSuspicionStatus.MEMORY_GROWTH_DETECTED, result.status)
        assertEquals(MemoryTrend.INCREASING, result.trend)
        assertEquals(10.0, result.growthPercentage ?: 0.0, 0.1)
        assertEquals(10_000L, result.growthTotalPssKb)
    }

    @Test
    fun testPossibleMemoryRetentionDetected() {
        val samples = listOf(
            createSample(100_000L, 1),
            createSample(115_000L, 2),
            createSample(125_000L, 3),
            createSample(130_000L, 4),
            createSample(128_000L, 5) // +28% growth and tail remains near peak 130MB
        )
        val result = PerformanceAnalyzer.analyze(samples)
        assertEquals(AnalysisSuspicionStatus.POSSIBLE_MEMORY_RETENTION, result.status)
        assertNotNull(result.initialTotalPssMb)
        assertNotNull(result.finalTotalPssMb)
        assertTrue(result.growthPercentage!! >= 15.0)
    }
}

