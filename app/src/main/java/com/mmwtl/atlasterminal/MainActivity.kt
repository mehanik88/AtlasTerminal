package com.mmwtl.atlasterminal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mmwtl.atlasterminal.ui.AppTab
import com.mmwtl.atlasterminal.ui.PresetsScreen
import com.mmwtl.atlasterminal.ui.SettingsScreen
import com.mmwtl.atlasterminal.ui.TerminalScreen
import com.mmwtl.atlasterminal.ui.TerminalViewModel
import com.mmwtl.atlasterminal.ui.theme.AtlasTerminalTheme

class MainActivity : ComponentActivity() {
    private val viewModel: TerminalViewModel by viewModels {
        TerminalViewModel.Factory(application as TerminalApp)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val state by viewModel.state.collectAsState()
            val context = LocalContext.current

            AtlasTerminalTheme {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                        .padding(top = 4.dp)
                ) {
                    // Top App Header & Tabs Bar
                    TopTabBar(
                        currentTab = state.currentTab,
                        onTabSelected = viewModel::selectTab
                    )

                    // Active Tab Screen
                    when (state.currentTab) {
                        AppTab.TERMINAL -> TerminalScreen(
                            state = state,
                            onCommandChange = viewModel::setCommandText,
                            onRunCommand = viewModel::runCurrentCommand,
                            onCtrlC = viewModel::onCtrlC,
                            onClearConsole = viewModel::clearConsole,
                            onCopyAll = { viewModel.copyAllToClipboard(context) },
                            onExportLog = { viewModel.exportLogToFile(context) },
                            onTargetChange = viewModel::setExecutionTarget,
                            onPrefixModeChange = viewModel::setPrefixMode,
                            onPrefixChipTapped = viewModel::onPrefixChipTapped,
                            onHistoryUp = viewModel::navigateHistoryUp,
                            onHistoryDown = viewModel::navigateHistoryDown
                        )

                        AppTab.PRESETS -> PresetsScreen(
                            state = state,
                            onCategorySelected = viewModel::setSelectedCategory,
                            onRunPreset = viewModel::runPreset,
                            onInsertPreset = viewModel::insertPreset,
                            onSaveCustomPreset = viewModel::saveCustomPreset,
                            onDeleteCustomPreset = viewModel::deleteCustomPreset
                        )

                        AppTab.SETTINGS -> SettingsScreen(
                            state = state,
                            onAdbEnabledChange = viewModel::setAdbEnabled,
                            onAdbHostChange = viewModel::setAdbHost,
                            onAdbModeChange = viewModel::setAdbMode,
                            onAdbPortChange = viewModel::setAdbPort,
                            onConnectAdb = viewModel::connectAdb,
                            onDisconnectAdb = viewModel::disconnectAdb,
                            onAutoReconnectChange = viewModel::setAutoReconnect,
                            onCustomShellPathChange = viewModel::setCustomShellPath,
                            onFontSizeChange = viewModel::setFontSize,
                            onAutoScrollChange = viewModel::setAutoScroll,
                            onShowTimestampsChange = viewModel::setShowTimestamps,
                            onClearHistory = viewModel::clearHistory
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TopTabBar(
    currentTab: AppTab,
    onTabSelected: (AppTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.app_name),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            TabButton(
                title = stringResource(R.string.tab_terminal),
                selected = currentTab == AppTab.TERMINAL,
                onClick = { onTabSelected(AppTab.TERMINAL) }
            )
            TabButton(
                title = stringResource(R.string.tab_presets),
                selected = currentTab == AppTab.PRESETS,
                onClick = { onTabSelected(AppTab.PRESETS) }
            )
            TabButton(
                title = stringResource(R.string.tab_settings),
                selected = currentTab == AppTab.SETTINGS,
                onClick = { onTabSelected(AppTab.SETTINGS) }
            )
        }
    }
}

@Composable
private fun TabButton(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    if (selected) {
        Button(
            modifier = Modifier.height(36.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 0.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            onClick = onClick
        ) {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    } else {
        OutlinedButton(
            modifier = Modifier.height(36.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 0.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            onClick = onClick
        ) {
            Text(title, fontSize = 13.sp)
        }
    }
}
