package com.mmwtl.atlasterminal.core

import com.mmwtl.atlasterminal.data.ExecutionTarget
import com.mmwtl.atlasterminal.data.TerminalPrefs
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong

enum class LineType {
    COMMAND,
    STDOUT,
    STDERR,
    SYSTEM,
    EXIT_STATUS
}

data class TerminalLine(
    val id: Long,
    val timestamp: Long,
    val type: LineType,
    val text: String
) {
    fun formatTimestamp(): String {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}

class TerminalSessionManager(
    private val adbClient: AdbClient,
    private val localShell: LocalShellExecutor,
    private val prefs: TerminalPrefs,
    private val scope: CoroutineScope
) {
    private val lineIdGen = AtomicLong(0)
    private val executionMutex = Mutex()

    private val _lines = MutableStateFlow<List<TerminalLine>>(emptyList())
    val lines: StateFlow<List<TerminalLine>> = _lines.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    @Volatile
    private var activeExecutionJob: Job? = null

    init {
        appendLine(LineType.SYSTEM, "AtlasTerminal initialized. Ready for automotive shell operations.")
    }

    suspend fun executeCommand(
        rawCommand: String,
        target: ExecutionTarget = prefs.executionTarget,
        prefix: String = prefs.activePrefix
    ): Boolean = withContext(Dispatchers.IO) {
        val trimmed = rawCommand.trim()
        if (trimmed.isBlank()) return@withContext false

        if (!executionMutex.tryLock()) {
            appendLine(LineType.SYSTEM, "Another command is already running. Press Ctrl+C first.")
            return@withContext false
        }

        // Build effective command with prefix if configured
        val effectiveCommand = if (prefix.isNotBlank() && !trimmed.startsWith(prefix.trim())) {
            "$prefix $trimmed"
        } else {
            trimmed
        }

        prefs.addCommandToHistory(trimmed)

        val targetLabel = when (target) {
            ExecutionTarget.ADB_SHELL -> "adb"
            ExecutionTarget.LOCAL_SH -> "sh"
            ExecutionTarget.CUSTOM -> "custom"
        }

        appendLine(LineType.COMMAND, "[$targetLabel] $effectiveCommand")
        _isBusy.value = true

        val outputNormalizer = TerminalOutputNormalizer()
        var outputFlushed = false
        val onOutputChunk: (String) -> Unit = { chunk ->
            appendChunkLines(LineType.STDOUT, outputNormalizer.feed(chunk))
        }
        fun flushOutput() {
            if (!outputFlushed) {
                appendChunkLines(LineType.STDOUT, outputNormalizer.finish())
                outputFlushed = true
            }
        }

        activeExecutionJob = scope.launch(Dispatchers.IO) {
            try {
                when (target) {
                    ExecutionTarget.ADB_SHELL -> {
                        val result = adbClient.execute(
                            command = effectiveCommand,
                            onOutputChunk = onOutputChunk
                        )
                        if (result.stderr.isNotBlank() && result.failure != null) {
                            appendChunkLines(
                                LineType.STDERR,
                                TerminalOutputNormalizer.normalize(result.stderr)
                            )
                        }
                        flushOutput()
                        val exitText = if (result.isSuccess) {
                            "Process finished with exit code 0"
                        } else {
                            "Process failed with exit code ${result.exitCode}" +
                                (result.failure?.let { " (${it.message})" } ?: "")
                        }
                        appendLine(LineType.EXIT_STATUS, exitText)
                    }

                    ExecutionTarget.LOCAL_SH,
                    ExecutionTarget.CUSTOM -> {
                        val shellType = when (target) {
                            ExecutionTarget.LOCAL_SH -> LocalShellType.LOCAL_SH
                            ExecutionTarget.CUSTOM -> LocalShellType.CUSTOM
                            ExecutionTarget.ADB_SHELL -> LocalShellType.LOCAL_SH
                        }
                        val result = localShell.execute(
                            command = effectiveCommand,
                            shellType = shellType,
                            onOutputChunk = onOutputChunk
                        )
                        if (result.stderr.isNotBlank() && result.stdout.isEmpty()) {
                            appendChunkLines(
                                LineType.STDERR,
                                TerminalOutputNormalizer.normalize(result.stderr)
                            )
                        }
                        flushOutput()
                        val exitText = if (result.isSuccess) {
                            "Process finished with exit code 0"
                        } else if (result.timedOut) {
                            "Process timed out"
                        } else {
                            "Process finished with exit code ${result.exitCode}" +
                                (result.error?.let { " ($it)" } ?: "")
                        }
                        appendLine(LineType.EXIT_STATUS, exitText)
                    }
                }
            } catch (t: CancellationException) {
                appendLine(LineType.SYSTEM, "Process cancelled / interrupted (SIGINT)")
            } catch (t: Throwable) {
                appendLine(LineType.STDERR, "Execution error: ${t.message ?: "Unknown error"}")
            } finally {
                flushOutput()
                _isBusy.value = false
                activeExecutionJob = null
                executionMutex.unlock()
            }
        }
        true
    }

    fun interruptActive() {
        activeExecutionJob?.cancel()
        activeExecutionJob = null
        localShell.interruptActive()
        _isBusy.value = false
        appendLine(LineType.SYSTEM, "Sent interrupt signal (Ctrl+C)")
    }

    fun clearBuffer() {
        _lines.value = emptyList()
        appendLine(LineType.SYSTEM, "Console cleared.")
    }

    fun appendLine(type: LineType, text: String) {
        val line = TerminalLine(
            id = lineIdGen.incrementAndGet(),
            timestamp = System.currentTimeMillis(),
            type = type,
            text = text
        )
        val max = prefs.maxBufferLines
        _lines.update { current ->
            val updated = current + line
            if (updated.size > max) updated.takeLast(max) else updated
        }
    }

    private fun appendChunkLines(type: LineType, chunk: String) {
        if (chunk.isEmpty()) return
        val split = chunk.split("\n")
        split.forEachIndexed { index, lineStr ->
            if (lineStr.isNotEmpty() || index < split.size - 1) {
                appendLine(type, lineStr)
            }
        }
    }

    fun exportPlainText(includeTimestamps: Boolean = prefs.showTimestamps): String {
        return _lines.value.joinToString(separator = "\n") { line ->
            val showTime = includeTimestamps && (line.type == LineType.COMMAND || line.type == LineType.SYSTEM)
            val prefix = if (showTime) "[${line.formatTimestamp()}] " else ""
            val typeIndicator = when (line.type) {
                LineType.COMMAND -> "$ "
                LineType.SYSTEM -> "# "
                LineType.STDERR -> "! "
                LineType.EXIT_STATUS -> "-> "
                LineType.STDOUT -> ""
            }
            "$prefix$typeIndicator${line.text}"
        }
    }
}
