package com.mmwtl.atlasterminal.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mmwtl.atlasterminal.R
import com.mmwtl.atlasterminal.core.AdbConnectionState
import com.mmwtl.atlasterminal.core.LineType
import com.mmwtl.atlasterminal.core.TerminalLine
import com.mmwtl.atlasterminal.data.ExecutionTarget
import com.mmwtl.atlasterminal.data.PrefixMode

@Composable
fun TerminalScreen(
    state: TerminalUiState,
    onCommandChange: (String) -> Unit,
    onRunCommand: () -> Unit,
    onCtrlC: () -> Unit,
    onClearConsole: () -> Unit,
    onCopyAll: () -> Unit,
    onExportLog: () -> Unit,
    onTargetChange: (ExecutionTarget) -> Unit,
    onPrefixModeChange: (PrefixMode) -> Unit,
    onPrefixChipTapped: (String) -> Unit,
    onHistoryUp: () -> Unit,
    onHistoryDown: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Status & Target Header
        StatusHeader(state)

        // Terminal Console Window
        TerminalConsole(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            lines = state.lines,
            fontSizeSp = state.fontSize.spSize,
            autoScroll = state.autoScroll,
            showTimestamps = state.showTimestamps,
            isBusy = state.isBusy,
            onClear = onClearConsole,
            onCopy = onCopyAll,
            onExport = onExportLog,
            onCtrlC = onCtrlC
        )

        // Prefix Constructor
        PrefixConstructorCard(
            selectedTarget = state.executionTarget,
            onTargetChange = onTargetChange,
            prefixMode = state.prefixMode,
            onPrefixModeChange = onPrefixModeChange,
            activePrefix = state.activePrefix,
            onPrefixChipTapped = onPrefixChipTapped
        )

        // Command Input & Controls
        CommandInputSection(
            commandText = state.commandText,
            onCommandChange = onCommandChange,
            onRunCommand = onRunCommand,
            onCtrlC = onCtrlC,
            isBusy = state.isBusy,
            historyIndex = state.historyIndex,
            historySize = state.commandHistory.size,
            onHistoryUp = onHistoryUp,
            onHistoryDown = onHistoryDown
        )
    }
}

@Composable
private fun StatusHeader(state: TerminalUiState) {
    val (statusLabel, statusColor) = when (state.executionTarget) {
        ExecutionTarget.ADB_SHELL -> {
            when (val adb = state.adbState) {
                AdbConnectionState.Connected -> "ADB: Подключён (${state.adbHost}:${state.adbPortText})" to MaterialTheme.colorScheme.primary
                AdbConnectionState.Connecting -> "ADB: Подключается…" to MaterialTheme.colorScheme.onSurfaceVariant
                AdbConnectionState.Disconnected -> "ADB: Отключён" to MaterialTheme.colorScheme.outline
                is AdbConnectionState.Error -> "ADB Ошибка: ${adb.message}" to MaterialTheme.colorScheme.error
            }
        }
        ExecutionTarget.LOCAL_SU_ROOT -> "Локальный процесс: su root" to MaterialTheme.colorScheme.primary
        ExecutionTarget.LOCAL_SU -> "Локальный процесс: su" to MaterialTheme.colorScheme.primary
        ExecutionTarget.LOCAL_SH -> "Локальный процесс: sh (/system/bin/sh)" to MaterialTheme.colorScheme.primary
        ExecutionTarget.CUSTOM -> "Кастомный процесс: ${state.customShellPath}" to MaterialTheme.colorScheme.primary
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Spacer(
            modifier = Modifier
                .width(14.dp)
                .height(10.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(statusColor)
        )
        Text(
            text = statusLabel,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.weight(1f))
        if (state.isBusy) {
            Text(
                text = "● RUNNING",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun TerminalConsole(
    modifier: Modifier,
    lines: List<TerminalLine>,
    fontSizeSp: Float,
    autoScroll: Boolean,
    showTimestamps: Boolean,
    isBusy: Boolean,
    onClear: () -> Unit,
    onCopy: () -> Unit,
    onExport: () -> Unit,
    onCtrlC: () -> Unit
) {
    val listState = rememberLazyListState()

    LaunchedEffect(lines.size, autoScroll) {
        if (autoScroll && lines.isNotEmpty()) {
            listState.animateScrollToItem(lines.size - 1)
        }
    }

    ElevatedCard(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = com.mmwtl.atlasterminal.ui.theme.TerminalColorizer.Background),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Modern Console Window Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(com.mmwtl.atlasterminal.ui.theme.TerminalColorizer.HeaderBackground)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Window Control Dots
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color(0xFFBF616A))
                    )
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color(0xFFEBCB8B))
                    )
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color(0xFFA3BE8C))
                    )
                }

                Text(
                    text = "ATLAS SHELL",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = com.mmwtl.atlasterminal.ui.theme.TerminalColorizer.CommandAccent,
                    letterSpacing = 1.sp
                )

                if (lines.isNotEmpty()) {
                    Text(
                        text = "(${lines.size})",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = com.mmwtl.atlasterminal.ui.theme.TerminalColorizer.TextMuted
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                if (isBusy) {
                    ConsoleActionButton(
                        text = "■ Ctrl+C",
                        isDestructive = true,
                        onClick = onCtrlC
                    )
                }

                ConsoleActionButton(
                    text = "Копировать",
                    onClick = onCopy
                )

                ConsoleActionButton(
                    text = "Экспорт",
                    onClick = onExport
                )

                ConsoleActionButton(
                    text = "Очистить",
                    onClick = onClear
                )
            }

            // Monospaced Lines View
            SelectionContainer(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    state = listState
                ) {
                    items(lines, key = { it.id }) { line ->
                        TerminalLineRow(line = line, fontSizeSp = fontSizeSp, showTimestamps = showTimestamps)
                    }
                }
            }
        }
    }
}

@Composable
private fun ConsoleActionButton(
    text: String,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(
                if (isDestructive) Color(0xFFBF616A) else com.mmwtl.atlasterminal.ui.theme.TerminalColorizer.HeaderBorder
            )
            .border(
                width = 1.dp,
                color = if (isDestructive) Color(0xFFBF616A) else com.mmwtl.atlasterminal.ui.theme.TerminalColorizer.HeaderBorder.copy(alpha = 0.8f),
                shape = RoundedCornerShape(4.dp)
            ),
        color = Color.Transparent,
        onClick = onClick
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            text = text,
            fontSize = 11.sp,
            fontWeight = if (isDestructive) FontWeight.Bold else FontWeight.Medium,
            color = if (isDestructive) Color.White else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun TerminalLineRow(line: TerminalLine, fontSizeSp: Float, showTimestamps: Boolean) {
    val annotatedText = com.mmwtl.atlasterminal.ui.theme.TerminalColorizer.formatLine(line.type, line.text)

    // Timestamps are only shown for commands and system events, never cluttering stdout (e.g. df -h tables)
    val showTime = showTimestamps && (line.type == LineType.COMMAND || line.type == LineType.SYSTEM)
    val timePrefix = if (showTime) "[${line.formatTimestamp()}] " else ""

    val topPadding = if (line.type == LineType.COMMAND) 10.dp else 1.dp
    val bottomPadding = if (line.type == LineType.EXIT_STATUS) 6.dp else 1.dp

    val lineBackground = when (line.type) {
        LineType.COMMAND -> Color(0x187893A0)
        LineType.STDERR -> Color(0x22BF616A)
        else -> Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = topPadding, bottom = bottomPadding)
            .clip(RoundedCornerShape(4.dp))
            .background(lineBackground)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.Top
    ) {
        if (showTime) {
            Text(
                text = timePrefix,
                fontFamily = FontFamily.Monospace,
                fontSize = (fontSizeSp - 2).coerceAtLeast(10f).sp,
                color = com.mmwtl.atlasterminal.ui.theme.TerminalColorizer.TextMuted,
                lineHeight = (fontSizeSp + 4).sp
            )
        }

        Text(
            text = annotatedText,
            fontFamily = FontFamily.Monospace,
            fontSize = fontSizeSp.sp,
            lineHeight = (fontSizeSp + 5).sp
        )
    }
}

@Composable
private fun PrefixConstructorCard(
    selectedTarget: ExecutionTarget,
    onTargetChange: (ExecutionTarget) -> Unit,
    prefixMode: PrefixMode,
    onPrefixModeChange: (PrefixMode) -> Unit,
    activePrefix: String,
    onPrefixChipTapped: (String) -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Execution Target Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TargetButton(
                    modifier = Modifier.weight(1f),
                    title = "ADB",
                    selected = selectedTarget == ExecutionTarget.ADB_SHELL,
                    onClick = { onTargetChange(ExecutionTarget.ADB_SHELL) }
                )
                TargetButton(
                    modifier = Modifier.weight(1.1f),
                    title = "su root",
                    selected = selectedTarget == ExecutionTarget.LOCAL_SU_ROOT,
                    onClick = { onTargetChange(ExecutionTarget.LOCAL_SU_ROOT) }
                )
                TargetButton(
                    modifier = Modifier.weight(0.9f),
                    title = "su",
                    selected = selectedTarget == ExecutionTarget.LOCAL_SU,
                    onClick = { onTargetChange(ExecutionTarget.LOCAL_SU) }
                )
                TargetButton(
                    modifier = Modifier.weight(0.9f),
                    title = "sh",
                    selected = selectedTarget == ExecutionTarget.LOCAL_SH,
                    onClick = { onTargetChange(ExecutionTarget.LOCAL_SH) }
                )
            }

            // Prefix Chips & Mode Switcher
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (activePrefix.isNotBlank() && prefixMode == PrefixMode.PREPEND) {
                        "Префикс: $activePrefix"
                    } else {
                        "Конструктор префикса:"
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    ModeToggleButton(
                        title = "Авто",
                        selected = prefixMode == PrefixMode.PREPEND,
                        onClick = { onPrefixModeChange(PrefixMode.PREPEND) }
                    )
                    ModeToggleButton(
                        title = "Вставка",
                        selected = prefixMode == PrefixMode.INSERT,
                        onClick = { onPrefixModeChange(PrefixMode.INSERT) }
                    )
                }
            }

            // Horizontal Chips
            val chips = listOf(
                "su root",
                "su 0",
                "sh -c",
                "nohup",
                "getprop",
                "setprop",
                "pm",
                "am",
                "dumpsys",
                "logcat",
                "df -h"
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                chips.forEach { chip ->
                    val isSelected = prefixMode == PrefixMode.PREPEND && activePrefix == chip
                    PrefixChip(
                        text = chip,
                        isSelected = isSelected,
                        onClick = { onPrefixChipTapped(chip) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TargetButton(
    modifier: Modifier,
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    if (selected) {
        Button(
            modifier = modifier.height(34.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 0.dp),
            shape = RoundedCornerShape(6.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            onClick = onClick
        ) {
            Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    } else {
        OutlinedButton(
            modifier = modifier.height(34.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 0.dp),
            shape = RoundedCornerShape(6.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            onClick = onClick
        ) {
            Text(title, fontSize = 12.sp)
        }
    }
}

@Composable
private fun ModeToggleButton(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PrefixChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
            )
            .border(
                width = 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                shape = RoundedCornerShape(6.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun CommandInputSection(
    commandText: String,
    onCommandChange: (String) -> Unit,
    onRunCommand: () -> Unit,
    onCtrlC: () -> Unit,
    isBusy: Boolean,
    historyIndex: Int,
    historySize: Int,
    onHistoryUp: () -> Unit,
    onHistoryDown: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Unified Stepper container for Up / Down History navigation
        HistoryStepper(
            historySize = historySize,
            historyIndex = historyIndex,
            onHistoryUp = onHistoryUp,
            onHistoryDown = onHistoryDown
        )

        // Main command text field
        OutlinedTextField(
            modifier = Modifier.weight(1f),
            value = commandText,
            onValueChange = onCommandChange,
            placeholder = { Text(stringResource(R.string.command_input_hint), fontSize = 13.sp) },
            singleLine = true,
            shape = RoundedCornerShape(8.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { onRunCommand() })
        )

        // Clear (X) button if text not empty
        if (commandText.isNotEmpty()) {
            OutlinedButton(
                modifier = Modifier.height(48.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                onClick = { onCommandChange("") }
            ) {
                Text("✕", fontSize = 14.sp)
            }
        }

        // Run button
        Button(
            modifier = Modifier.height(48.dp),
            enabled = !isBusy && commandText.isNotBlank(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 0.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            onClick = onRunCommand
        ) {
            Text(stringResource(R.string.action_run), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun HistoryStepper(
    historySize: Int,
    historyIndex: Int,
    onHistoryUp: () -> Unit,
    onHistoryDown: () -> Unit
) {
    val upEnabled = historySize > 0
    val downEnabled = historyIndex >= 0

    Column(
        modifier = Modifier
            .width(36.dp)
            .height(48.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                shape = RoundedCornerShape(6.dp)
            )
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clickable(enabled = upEnabled, onClick = onHistoryUp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "▲",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (upEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clickable(enabled = downEnabled, onClick = onHistoryDown),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "▼",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (downEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
            )
        }
    }
}
