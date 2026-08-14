package com.mmwtl.atlasterminal

import android.app.Application
import com.mmwtl.atlasterminal.core.AdbClient
import com.mmwtl.atlasterminal.core.LocalShellExecutor
import com.mmwtl.atlasterminal.core.TerminalSessionManager
import com.mmwtl.atlasterminal.data.TerminalPrefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class TerminalApp : Application() {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    lateinit var prefs: TerminalPrefs
        private set

    lateinit var adbClient: AdbClient
        private set

    lateinit var localShell: LocalShellExecutor
        private set

    lateinit var sessionManager: TerminalSessionManager
        private set

    override fun onCreate() {
        super.onCreate()
        prefs = TerminalPrefs(this)
        adbClient = AdbClient(this, prefs)
        localShell = LocalShellExecutor(
            initialCustomShellBinary = prefs.customShellPath
        )
        sessionManager = TerminalSessionManager(
            adbClient = adbClient,
            localShell = localShell,
            prefs = prefs,
            scope = appScope
        )
    }
}
