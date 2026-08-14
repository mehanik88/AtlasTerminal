package com.mmwtl.atlasterminal.core

import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

enum class LocalShellType(val displayName: String) {
    LOCAL_SH("sh"),
    CUSTOM("Custom")
}

data class ShellExecutionResult(
    val stdout: String,
    val stderr: String,
    val exitCode: Int,
    val timedOut: Boolean = false,
    val error: String? = null
) {
    val isSuccess: Boolean
        get() = exitCode == 0 && !timedOut && error == null
}

class LocalShellExecutor(
    private val defaultShellType: LocalShellType = LocalShellType.LOCAL_SH,
    initialCustomShellBinary: String = "/system/bin/sh"
) {
    @Volatile
    private var customShellBinary = normalizeShellPath(initialCustomShellBinary)

    @Volatile
    private var activeProcess: Process? = null

    @Volatile
    private var interactiveProcess: Process? = null
    private var interactiveWriter: BufferedWriter? = null
    private var interactiveJob: Job? = null

    private val _interactiveOutput = MutableSharedFlow<String>(extraBufferCapacity = 100)
    val interactiveOutput: SharedFlow<String> = _interactiveOutput.asSharedFlow()

    fun setCustomShellBinary(path: String) {
        customShellBinary = normalizeShellPath(path)
    }

    suspend fun execute(
        command: String,
        shellType: LocalShellType = defaultShellType,
        timeoutMs: Long = 60_000L,
        onOutputChunk: ((String) -> Unit)? = null
    ): ShellExecutionResult = withContext(Dispatchers.IO) {
        val cmdArgs = buildProcessArgs(shellType, command)
        val processBuilder = ProcessBuilder(cmdArgs)
        processBuilder.redirectErrorStream(false)

        val stdoutBuffer = StringBuffer(256)
        val stderrBuffer = StringBuffer(256)

        var proc: Process? = null
        try {
            proc = processBuilder.start()
            activeProcess = proc

            val stdoutReader = proc.inputStream.bufferedReader(StandardCharsets.UTF_8)
            val stderrReader = proc.errorStream.bufferedReader(StandardCharsets.UTF_8)

            val stdoutJob = launch(Dispatchers.IO) {
                val buffer = CharArray(1024)
                var read = 0
                while (stdoutReader.read(buffer).also { read = it } != -1) {
                    val chunk = String(buffer, 0, read)
                    stdoutBuffer.append(chunk)
                    onOutputChunk?.invoke(chunk)
                }
            }

            val stderrJob = launch(Dispatchers.IO) {
                val buffer = CharArray(1024)
                var read = 0
                while (stderrReader.read(buffer).also { read = it } != -1) {
                    val chunk = String(buffer, 0, read)
                    stderrBuffer.append(chunk)
                    onOutputChunk?.invoke(chunk)
                }
            }

            val completed = proc.waitFor(timeoutMs, TimeUnit.MILLISECONDS)
            if (!completed) {
                proc.destroyForcibly()
                stdoutJob.cancel()
                stderrJob.cancel()
                return@withContext ShellExecutionResult(
                    stdout = stdoutBuffer.toString(),
                    stderr = stderrBuffer.toString(),
                    exitCode = -1,
                    timedOut = true,
                    error = "Process timed out after ${timeoutMs / 1000}s"
                )
            }

            stdoutJob.join()
            stderrJob.join()

            val exitCode = proc.exitValue()
            ShellExecutionResult(
                stdout = stdoutBuffer.toString(),
                stderr = stderrBuffer.toString(),
                exitCode = exitCode
            )
        } catch (t: CancellationException) {
            proc?.destroyForcibly()
            throw t
        } catch (t: Throwable) {
            Log.w(TAG, "Process execution error", t)
            ShellExecutionResult(
                stdout = stdoutBuffer.toString(),
                stderr = stderrBuffer.toString(),
                exitCode = -1,
                error = t.message ?: "Failed to execute process"
            )
        } finally {
            activeProcess = null
        }
    }

    fun interruptActive() {
        activeProcess?.destroyForcibly()
        activeProcess = null
    }

    suspend fun startInteractiveSession(
        shellType: LocalShellType = defaultShellType,
        scope: CoroutineScope
    ): Boolean = withContext(Dispatchers.IO) {
        stopInteractiveSession()
        try {
            val cmd = when (shellType) {
                LocalShellType.LOCAL_SH -> listOf("/system/bin/sh", "-i")
                LocalShellType.CUSTOM -> listOf(customShellBinary.ifBlank { "/system/bin/sh" })
            }

            val pb = ProcessBuilder(cmd)
            pb.redirectErrorStream(true)
            val proc = pb.start()
            interactiveProcess = proc
            interactiveWriter = BufferedWriter(OutputStreamWriter(proc.outputStream, StandardCharsets.UTF_8))

            interactiveJob = scope.launch(Dispatchers.IO) {
                val reader = BufferedReader(InputStreamReader(proc.inputStream, StandardCharsets.UTF_8))
                val buffer = CharArray(1024)
                var read = 0
                try {
                    while (isActive && reader.read(buffer).also { read = it } != -1) {
                        val text = String(buffer, 0, read)
                        _interactiveOutput.emit(text)
                    }
                } catch (t: Throwable) {
                    if (isActive) {
                        _interactiveOutput.emit("\n[Shell session closed: ${t.message}]\n")
                    }
                }
            }
            true
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to start interactive shell", t)
            _interactiveOutput.emit("[Failed to start shell: ${t.message}]\n")
            false
        }
    }

    suspend fun sendInteractiveInput(input: String) = withContext(Dispatchers.IO) {
        interactiveWriter?.let { writer ->
            try {
                writer.write(input)
                if (!input.endsWith("\n")) {
                    writer.write("\n")
                }
                writer.flush()
            } catch (t: Throwable) {
                Log.w(TAG, "Failed to write to shell", t)
            }
        }
    }

    fun stopInteractiveSession() {
        interactiveJob?.cancel()
        interactiveJob = null
        runCatching { interactiveWriter?.close() }
        interactiveWriter = null
        interactiveProcess?.destroyForcibly()
        interactiveProcess = null
    }

    fun isInteractiveAlive(): Boolean {
        return interactiveProcess?.isAlive == true
    }

    private fun buildProcessArgs(shellType: LocalShellType, command: String): List<String> {
        return when (shellType) {
            LocalShellType.LOCAL_SH -> listOf("/system/bin/sh", "-c", command)
            LocalShellType.CUSTOM -> listOf(customShellBinary.ifBlank { "/system/bin/sh" }, "-c", command)
        }
    }

    companion object {
        private const val TAG = "AtlasTerminalShell"

        private fun normalizeShellPath(path: String): String {
            return path.trim().ifBlank { "/system/bin/sh" }
        }
    }
}
