package com.appprobe.monitoring

import android.app.ActivityManager
import android.content.Context
import android.os.Debug
import com.appprobe.execution.CommandExecutor
import com.appprobe.execution.DeviceShellExecutor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Performance and resource monitoring component for target Android application processes.
 * Runs asynchronously off the UI thread and collects timestamped memory, CPU, and thread metrics.
 */
class PerformanceMonitor(
    private val context: Context,
    private val commandExecutor: CommandExecutor = DeviceShellExecutor(),
    private val samplingIntervalMs: Long = 1000L
) {
    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager

    private val _liveSample = MutableStateFlow<PerformanceSample?>(null)
    val liveSample: StateFlow<PerformanceSample?> = _liveSample.asStateFlow()

    private val collectedSamples = mutableListOf<PerformanceSample>()
    private var monitoringJob: Job? = null

    // State for CPU delta calculation
    private var lastCpuTicks: Long? = null
    private var lastCpuTimestamp: Long? = null

    /**
     * Starts background periodic sampling of the target package.
     */
    fun startMonitoring(
        scope: CoroutineScope,
        targetPackage: String,
        activeActionProvider: () -> Pair<Int, String>?
    ) {
        stopMonitoring()
        collectedSamples.clear()
        _liveSample.value = null
        lastCpuTicks = null
        lastCpuTimestamp = null

        monitoringJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                try {
                    val activeAction = activeActionProvider()
                    val sample = captureSample(targetPackage, activeAction?.first, activeAction?.second)
                    if (sample != null) {
                        collectedSamples.add(sample)
                        _liveSample.value = sample
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    // Non-fatal: Ignore sample failure and continue monitoring
                }
                delay(samplingIntervalMs)
            }
        }
    }

    /**
     * Stops the active monitoring job and returns the immutable list of collected samples.
     */
    fun stopMonitoring(): List<PerformanceSample> {
        monitoringJob?.cancel()
        monitoringJob = null
        return collectedSamples.toList()
    }

    /**
     * Captures a single resource measurement for the target package.
     */
    suspend fun captureSample(
        targetPackage: String,
        actionIndex: Int? = null,
        actionType: String? = null
    ): PerformanceSample? = withContext(Dispatchers.IO) {
        runCatching {
            val pid = findTargetPid(targetPackage)
            if (pid <= 0) {
                // Target process is not yet running or PID not accessible
                return@withContext PerformanceSample(
                    timestamp = System.currentTimeMillis(),
                    targetPackage = targetPackage,
                    pid = -1,
                    actionIndex = actionIndex,
                    actionType = actionType,
                    memory = ProcessMemoryMetrics(),
                    cpuUsagePercent = null,
                    threadCount = null
                )
            }

            val memory = collectMemoryMetrics(pid, targetPackage)
            val cpu = collectCpuUsage(pid)
            val threadCount = collectThreadCount(pid)

            PerformanceSample(
                timestamp = System.currentTimeMillis(),
                targetPackage = targetPackage,
                pid = pid,
                actionIndex = actionIndex,
                actionType = actionType,
                memory = memory,
                cpuUsagePercent = cpu,
                threadCount = threadCount
            )
        }.getOrNull()
    }

    /**
     * Resolves the PID of the target application package.
     */
    private suspend fun findTargetPid(targetPackage: String): Int {
        if (targetPackage == context.packageName) {
            return android.os.Process.myPid()
        }

        // 1. Try ActivityManager running processes
        activityManager?.runningAppProcesses?.firstOrNull { it.processName == targetPackage }?.pid?.let {
            if (it > 0) return it
        }

        // 2. Try shell pidof
        val pidofResult = commandExecutor.execute("pidof -s $targetPackage")
        if (pidofResult.success && pidofResult.stdout.isNotBlank()) {
            pidofResult.stdout.trim().toIntOrNull()?.let { return it }
        }

        // 3. Try shell ps lookup
        val psResult = commandExecutor.execute("ps -A -o PID,NAME")
        if (psResult.success && psResult.stdout.isNotBlank()) {
            for (line in psResult.stdout.lines()) {
                if (line.contains(targetPackage)) {
                    val tokens = line.trim().split("\\s+".toRegex())
                    if (tokens.isNotEmpty()) {
                        tokens[0].toIntOrNull()?.let { return it }
                    }
                }
            }
        }

        return -1
    }

    /**
     * Collects explicit memory metrics (Total PSS, Dalvik PSS, Native PSS) in KB.
     */
    private suspend fun collectMemoryMetrics(pid: Int, targetPackage: String): ProcessMemoryMetrics {
        // Own process direct inspection
        if (pid == android.os.Process.myPid()) {
            val memInfo = Debug.MemoryInfo()
            Debug.getMemoryInfo(memInfo)
            val runtime = Runtime.getRuntime()
            val dalvikUsedKb = (runtime.totalMemory() - runtime.freeMemory()) / 1024L
            val nativeAllocKb = Debug.getNativeHeapAllocatedSize() / 1024L
            val totalPssKb = memInfo.totalPss.toLong().let { if (it > 0) it else dalvikUsedKb + nativeAllocKb }

            return ProcessMemoryMetrics(
                totalPssKb = totalPssKb,
                dalvikPssKb = dalvikUsedKb,
                nativePssKb = nativeAllocKb,
                otherPssKb = memInfo.otherPss.toLong()
            )
        }

        // 1. Try ActivityManager.getProcessMemoryInfo
        val am = activityManager
        if (am != null) {
            runCatching {
                val memInfoArray = am.getProcessMemoryInfo(intArrayOf(pid))
                if (memInfoArray.isNotEmpty()) {
                    val info = memInfoArray[0]
                    val totalPss = info.totalPss.toLong()
                    val dalvikPss = info.dalvikPss.toLong()
                    val nativePss = info.nativePss.toLong()
                    val otherPss = info.otherPss.toLong()

                    if (totalPss > 0 || dalvikPss > 0 || nativePss > 0) {
                        return ProcessMemoryMetrics(
                            totalPssKb = totalPss,
                            dalvikPssKb = dalvikPss,
                            nativePssKb = nativePss,
                            otherPssKb = otherPss
                        )
                    }
                }
            }
        }

        // 2. Try parsing /proc/<pid>/status for VmRSS / VmSize
        runCatching {
            val statusFile = File("/proc/$pid/status")
            if (statusFile.exists() && statusFile.canRead()) {
                var vmRssKb: Long? = null
                for (line in statusFile.readLines()) {
                    if (line.startsWith("VmRSS:", ignoreCase = true)) {
                        vmRssKb = Regex("\\d+").find(line)?.value?.toLongOrNull()
                        break
                    }
                }
                if (vmRssKb != null && vmRssKb > 0) {
                    return ProcessMemoryMetrics(
                        totalPssKb = vmRssKb,
                        dalvikPssKb = null,
                        nativePssKb = null,
                        otherPssKb = null
                    )
                }
            }
        }

        // 3. Shell fallback: dumpsys meminfo
        runCatching {
            val dumpsys = commandExecutor.execute("dumpsys meminfo $pid")
            if (dumpsys.success && dumpsys.stdout.isNotBlank()) {
                var totalPssKb: Long? = null
                var javaHeapKb: Long? = null
                var nativeHeapKb: Long? = null

                for (line in dumpsys.stdout.lines()) {
                    val trimmed = line.trim()
                    if (trimmed.startsWith("TOTAL PSS:", ignoreCase = true) || trimmed.startsWith("TOTAL:", ignoreCase = true)) {
                        totalPssKb = Regex("\\d+").find(trimmed)?.value?.toLongOrNull()
                    } else if (trimmed.startsWith("Java Heap:", ignoreCase = true)) {
                        javaHeapKb = Regex("\\d+").find(trimmed)?.value?.toLongOrNull()
                    } else if (trimmed.startsWith("Native Heap:", ignoreCase = true)) {
                        nativeHeapKb = Regex("\\d+").find(trimmed)?.value?.toLongOrNull()
                    }
                }

                if (totalPssKb != null) {
                    return ProcessMemoryMetrics(
                        totalPssKb = totalPssKb,
                        dalvikPssKb = javaHeapKb,
                        nativePssKb = nativeHeapKb,
                        otherPssKb = null
                    )
                }
            }
        }

        return ProcessMemoryMetrics()
    }

    /**
     * Calculates target process CPU usage percentage across sampling intervals.
     * Returns null (Unavailable) if calculation cannot be computed reliably.
     */
    private fun collectCpuUsage(pid: Int): Double? {
        return runCatching {
            val statFile = File("/proc/$pid/stat")
            if (!statFile.exists() || !statFile.canRead()) return null

            val statContent = statFile.readText().trim()
            val tokens = statContent.split("\\s+".toRegex())
            if (tokens.size < 15) return null

            val utime = tokens[13].toLongOrNull() ?: return null
            val stime = tokens[14].toLongOrNull() ?: return null
            val totalTicks = utime + stime
            val currentTimestamp = System.currentTimeMillis()

            val lastTicks = lastCpuTicks
            val lastTime = lastCpuTimestamp

            lastCpuTicks = totalTicks
            lastCpuTimestamp = currentTimestamp

            if (lastTicks != null && lastTime != null && currentTimestamp > lastTime) {
                val deltaTicks = totalTicks - lastTicks
                val deltaSeconds = (currentTimestamp - lastTime) / 1000.0
                if (deltaSeconds > 0.1 && deltaTicks >= 0) {
                    // Clock ticks per second is typically 100Hz on Android Linux kernel
                    val cpuPercentage = (deltaTicks / 100.0) / deltaSeconds * 100.0
                    return (cpuPercentage * 10.0).toInt() / 10.0 // Round to 1 decimal place
                }
            }
            null
        }.getOrNull()
    }

    /**
     * Collects the active thread count of the target process.
     */
    private fun collectThreadCount(pid: Int): Int? {
        return runCatching {
            val statusFile = File("/proc/$pid/status")
            if (statusFile.exists() && statusFile.canRead()) {
                for (line in statusFile.readLines()) {
                    if (line.startsWith("Threads:", ignoreCase = true)) {
                        val count = Regex("\\d+").find(line)?.value?.toIntOrNull()
                        if (count != null) return@runCatching count
                    }
                }
            }

            val taskDir = File("/proc/$pid/task")
            if (taskDir.exists() && taskDir.isDirectory) {
                val list = taskDir.list()
                if (list != null) return@runCatching list.size
            }
            null
        }.getOrNull()
    }
}

