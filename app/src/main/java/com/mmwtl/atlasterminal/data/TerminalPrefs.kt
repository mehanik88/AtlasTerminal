package com.mmwtl.atlasterminal.data

import android.content.Context
import android.content.SharedPreferences
import com.mmwtl.atlasterminal.core.AdbEndpoint
import com.mmwtl.atlasterminal.core.AdbEndpointMode
import org.json.JSONArray
import org.json.JSONObject

enum class ExecutionTarget(val title: String) {
    ADB_SHELL("ADB Shell"),
    LOCAL_SU_ROOT("su root"),
    LOCAL_SU("su"),
    LOCAL_SH("sh"),
    CUSTOM("Custom")
}

enum class PrefixMode {
    PREPEND,
    INSERT
}

enum class TerminalFontSize(val titleResName: String, val spSize: Float) {
    SMALL("font_size_small", 12f),
    MEDIUM("font_size_normal", 14f),
    LARGE("font_size_large", 18f)
}

data class CustomPreset(
    val id: String,
    val title: String,
    val command: String,
    val category: String = "Custom",
    val description: String = ""
)

class TerminalPrefs(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var adbEnabled: Boolean
        get() = prefs.getBoolean(KEY_ADB_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_ADB_ENABLED, value).apply()

    var adbHost: String
        get() = prefs.getString(KEY_ADB_HOST, DEFAULT_HOST).orEmpty().ifBlank { DEFAULT_HOST }
        set(value) = prefs.edit().putString(KEY_ADB_HOST, value.trim().ifBlank { DEFAULT_HOST }).apply()

    var adbPort: Int
        get() = prefs.getInt(KEY_ADB_PORT, AdbEndpoint.ATLAS_PORT)
        set(value) = prefs.edit().putInt(KEY_ADB_PORT, value).apply()

    var autoReconnect: Boolean
        get() = prefs.getBoolean(KEY_AUTO_RECONNECT, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_RECONNECT, value).apply()

    var executionTarget: ExecutionTarget
        get() {
            val name = prefs.getString(KEY_EXECUTION_TARGET, ExecutionTarget.ADB_SHELL.name)
            return runCatching { ExecutionTarget.valueOf(name!!) }.getOrDefault(ExecutionTarget.ADB_SHELL)
        }
        set(value) = prefs.edit().putString(KEY_EXECUTION_TARGET, value.name).apply()

    var customShellPath: String
        get() = prefs.getString(KEY_CUSTOM_SHELL_PATH, "/system/bin/sh").orEmpty()
        set(value) = prefs.edit().putString(KEY_CUSTOM_SHELL_PATH, value).apply()

    var prefixMode: PrefixMode
        get() {
            val name = prefs.getString(KEY_PREFIX_MODE, PrefixMode.PREPEND.name)
            return runCatching { PrefixMode.valueOf(name!!) }.getOrDefault(PrefixMode.PREPEND)
        }
        set(value) = prefs.edit().putString(KEY_PREFIX_MODE, value.name).apply()

    var activePrefix: String
        get() = prefs.getString(KEY_ACTIVE_PREFIX, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_ACTIVE_PREFIX, value).apply()

    var fontSize: TerminalFontSize
        get() {
            val name = prefs.getString(KEY_FONT_SIZE, TerminalFontSize.MEDIUM.name)
            return runCatching { TerminalFontSize.valueOf(name!!) }.getOrDefault(TerminalFontSize.MEDIUM)
        }
        set(value) = prefs.edit().putString(KEY_FONT_SIZE, value.name).apply()

    var maxBufferLines: Int
        get() = prefs.getInt(KEY_MAX_BUFFER_LINES, 2000)
        set(value) = prefs.edit().putInt(KEY_MAX_BUFFER_LINES, value).apply()

    var autoScroll: Boolean
        get() = prefs.getBoolean(KEY_AUTO_SCROLL, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_SCROLL, value).apply()

    var showTimestamps: Boolean
        get() = prefs.getBoolean(KEY_SHOW_TIMESTAMPS, false)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_TIMESTAMPS, value).apply()

    fun getCommandHistory(): List<String> {
        val raw = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            List(array.length()) { array.getString(it) }
        }.getOrDefault(emptyList())
    }

    fun addCommandToHistory(command: String) {
        val trimmed = command.trim()
        if (trimmed.isBlank()) return
        val current = getCommandHistory().toMutableList()
        current.remove(trimmed)
        current.add(0, trimmed)
        val limited = current.take(50)
        val array = JSONArray()
        limited.forEach { array.put(it) }
        prefs.edit().putString(KEY_HISTORY, array.toString()).apply()
    }

    fun clearCommandHistory() {
        prefs.edit().remove(KEY_HISTORY).apply()
    }

    fun getCustomPresets(): List<CustomPreset> {
        val raw = prefs.getString(KEY_CUSTOM_PRESETS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            List(array.length()) { i ->
                val obj = array.getJSONObject(i)
                CustomPreset(
                    id = obj.optString("id", System.currentTimeMillis().toString()),
                    title = obj.optString("title", ""),
                    command = obj.optString("command", ""),
                    category = obj.optString("category", "Custom"),
                    description = obj.optString("description", "")
                )
            }
        }.getOrDefault(emptyList())
    }

    fun saveCustomPreset(preset: CustomPreset) {
        val current = getCustomPresets().toMutableList()
        val index = current.indexOfFirst { it.id == preset.id }
        if (index >= 0) {
            current[index] = preset
        } else {
            current.add(preset)
        }
        persistPresets(current)
    }

    fun deleteCustomPreset(id: String) {
        val current = getCustomPresets().toMutableList()
        current.removeAll { it.id == id }
        persistPresets(current)
    }

    private fun persistPresets(presets: List<CustomPreset>) {
        val array = JSONArray()
        presets.forEach { preset ->
            val obj = JSONObject()
            obj.put("id", preset.id)
            obj.put("title", preset.title)
            obj.put("command", preset.command)
            obj.put("category", preset.category)
            obj.put("description", preset.description)
            array.put(obj)
        }
        prefs.edit().putString(KEY_CUSTOM_PRESETS, array.toString()).apply()
    }

    companion object {
        private const val PREFS_NAME = "atlas_terminal_prefs"
        private const val DEFAULT_HOST = "localhost"

        private const val KEY_ADB_ENABLED = "adb_enabled"
        private const val KEY_ADB_HOST = "adb_host"
        private const val KEY_ADB_PORT = "adb_port"
        private const val KEY_AUTO_RECONNECT = "auto_reconnect"
        private const val KEY_EXECUTION_TARGET = "execution_target"
        private const val KEY_CUSTOM_SHELL_PATH = "custom_shell_path"
        private const val KEY_PREFIX_MODE = "prefix_mode"
        private const val KEY_ACTIVE_PREFIX = "active_prefix"
        private const val KEY_FONT_SIZE = "font_size"
        private const val KEY_MAX_BUFFER_LINES = "max_buffer_lines"
        private const val KEY_AUTO_SCROLL = "auto_scroll"
        private const val KEY_SHOW_TIMESTAMPS = "show_timestamps"
        private const val KEY_HISTORY = "command_history"
        private const val KEY_CUSTOM_PRESETS = "custom_presets"
    }
}
