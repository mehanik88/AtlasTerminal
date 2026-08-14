package com.mmwtl.atlasterminal.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mmwtl.atlasterminal.R
import com.mmwtl.atlasterminal.core.AdbEndpointMode
import com.mmwtl.atlasterminal.data.TerminalFontSize

@Composable
fun SettingsScreen(
    state: TerminalUiState,
    onAdbEnabledChange: (Boolean) -> Unit,
    onAdbHostChange: (String) -> Unit,
    onAdbModeChange: (AdbEndpointMode) -> Unit,
    onAdbPortChange: (String) -> Unit,
    onConnectAdb: () -> Unit,
    onDisconnectAdb: () -> Unit,
    onAutoReconnectChange: (Boolean) -> Unit,
    onCustomShellPathChange: (String) -> Unit,
    onFontSizeChange: (TerminalFontSize) -> Unit,
    onAutoScrollChange: (Boolean) -> Unit,
    onShowTimestampsChange: (Boolean) -> Unit,
    onClearHistory: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Section: ADB Connection
        SectionCard(title = stringResource(R.string.section_adb)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.adb_helper), fontWeight = FontWeight.Medium)
                    Text(
                        stringResource(R.string.adb_helper_description),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
                Switch(
                    checked = state.adbEnabled,
                    enabled = !state.isBusy,
                    onCheckedChange = onAdbEnabledChange
                )
            }

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = state.adbHost,
                onValueChange = onAdbHostChange,
                enabled = state.adbEnabled && !state.isBusy,
                label = { Text(stringResource(R.string.adb_host)) },
                supportingText = { Text(stringResource(R.string.adb_host_hint)) },
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
            )

            AdbModeSelector(
                selected = state.adbMode,
                enabled = state.adbEnabled && !state.isBusy,
                onSelected = onAdbModeChange
            )

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = state.adbPortText,
                onValueChange = onAdbPortChange,
                enabled = state.adbEnabled && !state.isBusy && state.adbMode != AdbEndpointMode.TELNET,
                label = { Text(stringResource(R.string.adb_port)) },
                supportingText = if (state.adbMode == AdbEndpointMode.TELNET) {
                    { Text(stringResource(R.string.adb_port_telnet)) }
                } else {
                    null
                },
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    modifier = Modifier.weight(1f),
                    enabled = state.adbEnabled && !state.isBusy,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    onClick = onConnectAdb
                ) {
                    Text(stringResource(R.string.action_connect))
                }

                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    enabled = !state.isBusy,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    onClick = onDisconnectAdb
                ) {
                    Text(stringResource(R.string.action_disconnect))
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.auto_reconnect), fontWeight = FontWeight.Medium)
                    Text(
                        stringResource(R.string.auto_reconnect_desc),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
                Switch(
                    checked = state.autoReconnect,
                    enabled = !state.isBusy,
                    onCheckedChange = onAutoReconnectChange
                )
            }
        }

        // Section: Local Shell Settings
        SectionCard(title = "Параметры локального процесса") {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = state.customShellPath,
                onValueChange = onCustomShellPathChange,
                enabled = !state.isBusy,
                label = { Text(stringResource(R.string.custom_shell_path)) },
                supportingText = { Text("По умолчанию /system/bin/sh или su") },
                singleLine = true,
                shape = RoundedCornerShape(8.dp)
            )
        }

        // Section: Terminal UI Settings
        SectionCard(title = stringResource(R.string.section_terminal_settings)) {
            Text(stringResource(R.string.font_size), fontWeight = FontWeight.Medium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FontSizeButton(
                    modifier = Modifier.weight(1f),
                    title = stringResource(R.string.font_size_small),
                    selected = state.fontSize == TerminalFontSize.SMALL,
                    onClick = { onFontSizeChange(TerminalFontSize.SMALL) }
                )
                FontSizeButton(
                    modifier = Modifier.weight(1f),
                    title = stringResource(R.string.font_size_normal),
                    selected = state.fontSize == TerminalFontSize.MEDIUM,
                    onClick = { onFontSizeChange(TerminalFontSize.MEDIUM) }
                )
                FontSizeButton(
                    modifier = Modifier.weight(1f),
                    title = stringResource(R.string.font_size_large),
                    selected = state.fontSize == TerminalFontSize.LARGE,
                    onClick = { onFontSizeChange(TerminalFontSize.LARGE) }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.terminal_autoscroll), fontWeight = FontWeight.Medium)
                Switch(
                    checked = state.autoScroll,
                    onCheckedChange = onAutoScrollChange
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.show_timestamps), fontWeight = FontWeight.Medium)
                Switch(
                    checked = state.showTimestamps,
                    onCheckedChange = onShowTimestampsChange
                )
            }

            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.error
                ),
                onClick = onClearHistory
            ) {
                Text("Очистить историю команд (${state.commandHistory.size})")
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(text = title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            content()
        }
    }
}

@Composable
private fun AdbModeSelector(
    selected: AdbEndpointMode,
    enabled: Boolean,
    onSelected: (AdbEndpointMode) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            AdbModeButton(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.adb_mode_atlas),
                selected = selected == AdbEndpointMode.ATLAS,
                enabled = enabled,
                onClick = { onSelected(AdbEndpointMode.ATLAS) }
            )
            AdbModeButton(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.adb_mode_preface),
                selected = selected == AdbEndpointMode.PREFACE,
                enabled = enabled,
                onClick = { onSelected(AdbEndpointMode.PREFACE) }
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            AdbModeButton(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.adb_mode_custom),
                selected = selected == AdbEndpointMode.CUSTOM,
                enabled = enabled,
                onClick = { onSelected(AdbEndpointMode.CUSTOM) }
            )
            AdbModeButton(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.adb_mode_telnet),
                selected = selected == AdbEndpointMode.TELNET,
                enabled = enabled,
                onClick = { onSelected(AdbEndpointMode.TELNET) }
            )
        }
    }
}

@Composable
private fun AdbModeButton(
    modifier: Modifier,
    title: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    if (selected) {
        Button(
            modifier = modifier,
            enabled = enabled,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            shape = RoundedCornerShape(8.dp),
            onClick = onClick
        ) {
            Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    } else {
        OutlinedButton(
            modifier = modifier,
            enabled = enabled,
            shape = RoundedCornerShape(8.dp),
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
private fun FontSizeButton(
    modifier: Modifier,
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    if (selected) {
        Button(
            modifier = modifier,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            shape = RoundedCornerShape(6.dp),
            onClick = onClick
        ) {
            Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    } else {
        OutlinedButton(
            modifier = modifier,
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
