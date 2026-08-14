package com.mmwtl.atlasterminal.core

sealed interface AdbConnectionState {
    data object Disconnected : AdbConnectionState
    data object Connecting : AdbConnectionState
    data object Connected : AdbConnectionState
    data class Error(val message: String) : AdbConnectionState
}
