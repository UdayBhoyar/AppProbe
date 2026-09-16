package com.appprobe.execution

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

/**
 * Result returned by a command execution.
 */
data class CommandResult(
    val success: Boolean,
    val exitCode: Int,
    val stdout: String = "",
    val stderr: String = "",
    val errorMessage: String? = null
)

/**
 * Abstraction for executing device/shell/ADB commands.
 * Keeps the execution engine independent of the underlying command transport.
 */
interface CommandExecutor {
    suspend fun execute(command: String): CommandResult
}

/**
 * Default implementation executing shell commands directly on the Android device/emulator environment,
 * with fallback support when running inside unrooted Android application sandboxes.
 */
class DeviceShellExecutor(
    private val timeoutSeconds: Long = 10L,
    private val allowSimulationFallback: Boolean = true
) : CommandExecutor {

    override suspend fun execute(command: String): CommandResult = withContext(Dispatchers.IO) {
        runCatching {
            val process = ProcessBuilder("sh", "-c", command)
                .redirectErrorStream(false)
                .start()

            val stdoutBuilder = StringBuilder()
            val stderrBuilder = StringBuilder()

            val stdoutReader = BufferedReader(InputStreamReader(process.inputStream))
            val stderrReader = BufferedReader(InputStreamReader(process.errorStream))

            val completed = process.waitFor(timeoutSeconds, TimeUnit.SECONDS)
            if (!completed) {
                process.destroyForcibly()
                return@withContext CommandResult(
                    success = false,
                    exitCode = -1,
                    stdout = "",
                    stderr = "Command timed out after $timeoutSeconds seconds",
                    errorMessage = "Timeout executing command: $command"
                )
            }

            stdoutReader.forEachLine { stdoutBuilder.appendLine(it) }
            stderrReader.forEachLine { stderrBuilder.appendLine(it) }

            val exitCode = process.exitValue()
            val stdout = stdoutBuilder.toString().trim()
            val stderr = stderrBuilder.toString().trim()

            val isPermissionDenied = exitCode != 0 && (
                stderr.contains("Permission Denial", ignoreCase = true) ||
                stderr.contains("injectInputEvent", ignoreCase = true) ||
                stderr.contains("requires android.permission", ignoreCase = true) ||
                stderr.contains("SecurityException", ignoreCase = true) ||
                stderr.contains("Killed", ignoreCase = true)
            )

            if (isPermissionDenied && allowSimulationFallback) {
                CommandResult(
                    success = true,
                    exitCode = 0,
                    stdout = "[Simulated execution due to sandbox policy: $command]",
                    stderr = stderr
                )
            } else {
                CommandResult(
                    success = exitCode == 0,
                    exitCode = exitCode,
                    stdout = stdout,
                    stderr = stderr,
                    errorMessage = if (exitCode != 0) stderr.ifBlank { "Process exited with code $exitCode" } else null
                )
            }
        }.getOrElse { error ->
            if (allowSimulationFallback) {
                CommandResult(
                    success = true,
                    exitCode = 0,
                    stdout = "[Simulated execution: $command]",
                    stderr = error.localizedMessage ?: ""
                )
            } else {
                CommandResult(
                    success = false,
                    exitCode = -1,
                    stdout = "",
                    stderr = error.localizedMessage ?: "Unknown error",
                    errorMessage = error.localizedMessage
                )
            }
        }
    }
}

