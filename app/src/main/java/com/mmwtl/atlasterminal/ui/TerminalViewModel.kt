package com.mmwtl.atlasterminal.ui

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Environment
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mmwtl.atlasterminal.TerminalApp
import com.mmwtl.atlasterminal.core.AdbConnectionState
import com.mmwtl.atlasterminal.core.AdbEndpoint
import com.mmwtl.atlasterminal.core.AdbEndpointMode
import com.mmwtl.atlasterminal.core.TerminalLine
import com.mmwtl.atlasterminal.data.CustomPreset
import com.mmwtl.atlasterminal.data.ExecutionTarget
import com.mmwtl.atlasterminal.data.PrefixMode
import com.mmwtl.atlasterminal.data.PresetItem
import com.mmwtl.atlasterminal.data.TerminalFontSize
import com.mmwtl.atlasterminal.data.TerminalPresets
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class AppTab {
    TERMINAL,
    PRESETS,
    SETTINGS
}

data class TerminalUiState(
    val currentTab: AppTab = AppTab.TERMINAL,
    val lines: List<TerminalLine> = emptyList(),
    val isBusy: Boolean = false,
    val adbState: AdbConnectionState = AdbConnectionState.Disconnected,
    val adbEnabled: Boolean = true,
    val adbHost: String = "localhost",
    val adbPortText: String = "5555",
    val adbMode: AdbEndpointMode = AdbEndpointMode.ATLAS,
    val autoReconnect: Boolean = true,
    val executionTarget: ExecutionTarget = ExecutionTarget.ADB_SHELL,
    val prefixMode: PrefixMode = PrefixMode.PREPEND,
    val activePrefix: String = "",
    val commandText: String = "",
    val commandHistory: List<String> = emptyList(),
    val historyIndex: Int = -1,
    val presets: List<PresetItem> = emptyList(),
    val selectedCategory: String = "Все",
    val fontSize: TerminalFontSize = TerminalFontSize.MEDIUM,
    val autoScroll: Boolean = true,
    val showTimestamps: Boolean = false,
    val customShellPath: String = "/system/bin/sh"
)

class TerminalViewModel(
    application: Application
) : AndroidViewModel(application) {
    private val app = application as TerminalApp
    private val prefs = app.prefs
    private val adbClient = app.adbClient
    private val sessionManager = app.sessionManager

    private val _uiState = MutableStateFlow(
        TerminalUiState(
            adbEnabled = prefs.adbEnabled,
            adbHost = prefs.adbHost,
            adbPortText = prefs.adbPort.toString(),
            adbMode = AdbEndpoint.modeForPort(prefs.adbPort),
            autoReconnect = prefs.autoReconnect,
            executionTarget = prefs.executionTarget,
            prefixMode = prefs.prefixMode,
            activePrefix = prefs.activePrefix,
            commandHistory = prefs.getCommandHistory(),
            presets = TerminalPresets.getAllPresets(prefs.getCustomPresets()),
            fontSize = prefs.fontSize,
            autoScroll = prefs.autoScroll,
            showTimestamps = prefs.showTimestamps,
            customShellPath = prefs.customShellPath
        )
    )

    val state: StateFlow<TerminalUiState> = combine(
        _uiState,
        sessionManager.lines,
        sessionManager.isBusy,
        adbClient.connectionState
    ) { baseState, lines, isBusy, adbState ->
        baseState.copy(
            lines = lines,
            isBusy = isBusy,
            adbState = adbState
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = _uiState.value
    )

    init {
        if (prefs.adbEnabled) {
            viewModelScope.launch {
                adbClient.connect()
            }
        }
    }

    fun selectTab(tab: AppTab) {
        _uiState.update { it.copy(currentTab = tab) }
    }

    fun setCommandText(text: String) {
        _uiState.update { it.copy(commandText = text, historyIndex = -1) }
    }

    fun setExecutionTarget(target: ExecutionTarget) {
        prefs.executionTarget = target
        _uiState.update { it.copy(executionTarget = target) }
    }

    fun setPrefixMode(mode: PrefixMode) {
        prefs.prefixMode = mode
        _uiState.update { it.copy(prefixMode = mode) }
    }

    fun setActivePrefix(prefix: String) {
        val newPrefix = if (_uiState.value.activePrefix == prefix) "" else prefix
        prefs.activePrefix = newPrefix
        _uiState.update { it.copy(activePrefix = newPrefix) }
    }

    fun onPrefixChipTapped(chip: String) {
        val current = _uiState.value
        if (current.prefixMode == PrefixMode.PREPEND) {
            setActivePrefix(chip)
        } else {
            // Insert mode: append / insert chip text into command
            val updated = if (current.commandText.isBlank()) {
                chip
            } else if (current.commandText.endsWith(" ")) {
                "${current.commandText}$chip"
            } else {
                "${current.commandText} $chip"
            }
            setCommandText(updated)
        }
    }

    fun runCurrentCommand() {
        val current = _uiState.value
        val raw = current.commandText.trim()
        if (raw.isBlank() || current.isBusy) return

        val effectivePrefix = if (current.prefixMode == PrefixMode.PREPEND) current.activePrefix else ""
        viewModelScope.launch {
            val started = sessionManager.executeCommand(
                rawCommand = raw,
                target = current.executionTarget,
                prefix = effectivePrefix
            )
            if (started) {
                _uiState.update {
                    it.copy(
                        commandText = "",
                        historyIndex = -1,
                        commandHistory = prefs.getCommandHistory()
                    )
                }
            }
        }
    }

    fun runPreset(preset: PresetItem) {
        val current = _uiState.value
        if (current.isBusy) return
        val effectivePrefix = if (current.prefixMode == PrefixMode.PREPEND) current.activePrefix else ""
        viewModelScope.launch {
            val started = sessionManager.executeCommand(
                rawCommand = preset.command,
                target = current.executionTarget,
                prefix = effectivePrefix
            )
            if (started) {
                _uiState.update {
                    it.copy(
                        currentTab = AppTab.TERMINAL,
                        commandHistory = prefs.getCommandHistory()
                    )
                }
            }
        }
    }

    fun insertPreset(preset: PresetItem) {
        setCommandText(preset.command)
        selectTab(AppTab.TERMINAL)
    }

    fun onCtrlC() {
        sessionManager.interruptActive()
    }

    fun clearConsole() {
        sessionManager.clearBuffer()
    }

    fun copyAllToClipboard(context: Context) {
        val text = sessionManager.exportPlainText()
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("AtlasTerminal Log", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Лог скопирован в буфер", Toast.LENGTH_SHORT).show()
    }

    fun exportLogToFile(context: Context) {
        viewModelScope.launch {
            val text = sessionManager.exportPlainText(includeTimestamps = true)
            val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val targetDir = File(downloadDir, "AtlasTerminal").apply { mkdirs() }
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val file = File(targetDir, "terminal_log_$timeStamp.txt")
            runCatching {
                file.writeText(text)
                Toast.makeText(context, "Сохранено: ${file.absolutePath}", Toast.LENGTH_LONG).show()
            }.onFailure {
                Toast.makeText(context, "Ошибка сохранения: ${it.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun navigateHistoryUp() {
        val history = _uiState.value.commandHistory
        if (history.isEmpty()) return
        val currentIndex = _uiState.value.historyIndex
        val newIndex = if (currentIndex < 0) 0 else (currentIndex + 1).coerceAtMost(history.size - 1)
        _uiState.update {
            it.copy(
                historyIndex = newIndex,
                commandText = history[newIndex]
            )
        }
    }

    fun navigateHistoryDown() {
        val history = _uiState.value.commandHistory
        val currentIndex = _uiState.value.historyIndex
        if (currentIndex <= 0) {
            _uiState.update { it.copy(historyIndex = -1, commandText = "") }
        } else {
            val newIndex = currentIndex - 1
            _uiState.update {
                it.copy(
                    historyIndex = newIndex,
                    commandText = history[newIndex]
                )
            }
        }
    }

    fun clearHistory() {
        prefs.clearCommandHistory()
        _uiState.update { it.copy(commandHistory = emptyList(), historyIndex = -1) }
    }

    // ADB Controls
    fun setAdbEnabled(enabled: Boolean) {
        prefs.adbEnabled = enabled
        _uiState.update { it.copy(adbEnabled = enabled) }
        viewModelScope.launch {
            if (enabled) adbClient.connect() else adbClient.disconnect()
        }
    }

    fun setAdbHost(host: String) {
        prefs.adbHost = host
        _uiState.update { it.copy(adbHost = host) }
    }

    fun setAdbMode(mode: AdbEndpointMode) {
        val port = when (mode) {
            AdbEndpointMode.ATLAS -> AdbEndpoint.ATLAS_PORT
            AdbEndpointMode.PREFACE -> AdbEndpoint.PREFACE_PORT
            AdbEndpointMode.TELNET -> AdbEndpoint.TELNET_PORT
            AdbEndpointMode.CUSTOM -> _uiState.value.adbPortText.toIntOrNull() ?: AdbEndpoint.ATLAS_PORT
        }
        prefs.adbPort = port
        _uiState.update {
            it.copy(
                adbMode = mode,
                adbPortText = if (mode == AdbEndpointMode.TELNET) "-667" else port.toString()
            )
        }
        viewModelScope.launch {
            adbClient.reconnect()
        }
    }

    fun setAdbPort(portText: String) {
        val port = portText.toIntOrNull() ?: 5555
        prefs.adbPort = port
        _uiState.update {
            it.copy(
                adbPortText = portText,
                adbMode = AdbEndpoint.modeForPort(port)
            )
        }
    }

    fun connectAdb() {
        viewModelScope.launch {
            adbClient.connect()
        }
    }

    fun disconnectAdb() {
        viewModelScope.launch {
            adbClient.disconnect()
        }
    }

    fun setAutoReconnect(enabled: Boolean) {
        prefs.autoReconnect = enabled
        _uiState.update { it.copy(autoReconnect = enabled) }
    }

    // Custom Presets
    fun setSelectedCategory(category: String) {
        _uiState.update { it.copy(selectedCategory = category) }
    }

    fun saveCustomPreset(title: String, command: String, category: String, description: String) {
        val preset = CustomPreset(
            id = "custom_${System.currentTimeMillis()}",
            title = title.trim(),
            command = command.trim(),
            category = category.trim().ifBlank { "Мои команды" },
            description = description.trim()
        )
        prefs.saveCustomPreset(preset)
        _uiState.update {
            it.copy(presets = TerminalPresets.getAllPresets(prefs.getCustomPresets()))
        }
    }

    fun deleteCustomPreset(id: String) {
        prefs.deleteCustomPreset(id)
        _uiState.update {
            it.copy(presets = TerminalPresets.getAllPresets(prefs.getCustomPresets()))
        }
    }

    // Settings
    fun setFontSize(size: TerminalFontSize) {
        prefs.fontSize = size
        _uiState.update { it.copy(fontSize = size) }
    }

    fun setAutoScroll(enabled: Boolean) {
        prefs.autoScroll = enabled
        _uiState.update { it.copy(autoScroll = enabled) }
    }

    fun setShowTimestamps(enabled: Boolean) {
        prefs.showTimestamps = enabled
        _uiState.update { it.copy(showTimestamps = enabled) }
    }

    fun setCustomShellPath(path: String) {
        prefs.customShellPath = path
        app.localShell.setCustomShellBinary(path)
        _uiState.update { it.copy(customShellPath = path) }
    }

    class Factory(private val app: TerminalApp) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TerminalViewModel(app) as T
        }
    }
}
