package com.mmwtl.atlasterminal.core

data class AdbCommandResult(
    val stdout: String = "",
    val stderr: String = "",
    val exitCode: Int = 0,
    val failure: AdbCommandFailure? = null
) {
    val isSuccess: Boolean
        get() = failure == null && exitCode == 0

    companion object {
        fun success(stdout: String): AdbCommandResult = AdbCommandResult(stdout = stdout, exitCode = 0)

        fun failure(kind: AdbCommandFailureKind, message: String): AdbCommandResult =
            AdbCommandResult(
                stderr = message,
                exitCode = -1,
                failure = AdbCommandFailure(kind, message)
            )
    }
}

data class AdbCommandFailure(
    val kind: AdbCommandFailureKind,
    val message: String
)

enum class AdbCommandFailureKind {
    DISABLED,
    INVALID_COMMAND,
    CONNECT,
    TIMEOUT,
    TRANSPORT,
    EXECUTION
}
