package com.mmwtl.atlasterminal.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.mmwtl.atlasterminal.core.LineType
import java.util.regex.Pattern

object TerminalColorizer {

    // Modern Terminal Palette (harmonious, readable, easy on the eyes)
    val Background = Color(0xFF0F1115)
    val HeaderBackground = Color(0xFF181B20)
    val HeaderBorder = Color(0xFF262C36)

    val CommandAccent = Color(0xFF7893A0)      // Slate cyan / primary
    val CommandPrompt = Color(0xFF88C0D0)      // Bright frost cyan
    val CommandText = Color(0xFFECEFF4)        // Crisp white

    val TextDefault = Color(0xFFD8DEE9)        // Soft white-gray
    val TextMuted = Color(0xFF7E8E9F)          // Slate gray

    val ColorSuccess = Color(0xFFA3BE8C)       // Soft pastel green
    val ColorWarning = Color(0xFFEBCB8B)       // Warm golden amber
    val ColorError = Color(0xFFD08770)         // Soft coral red
    val ColorFatal = Color(0xFFBF616A)         // Crimson red
    val ColorInfo = Color(0xFF81A1C1)          // Sky blue
    val ColorPurple = Color(0xFFB48EAD)        // Lavender / numbers / addresses
    val ColorCyan = Color(0xFF8FBCBB)          // Aqua / paths / keys

    // ANSI Escape Code Pattern: \u001b[...m
    private val ANSI_PATTERN = Pattern.compile("\u001B\\[([0-9;]*)m")

    // Android / Shell Syntax Patterns
    private val GETPROP_PATTERN = Pattern.compile("^\\[([^\\]]+)\\]:\\s*\\[([^\\]]*)\\]$")
    private val LOGCAT_PATTERN = Pattern.compile("^([VDIWEF])/([^(:\\s]+)(\\([0-9]+\\))?:\\s*(.*)$")
    private val PACKAGE_PATTERN = Pattern.compile("^(package:)(.*)$")
    private val IP_PORT_PATTERN = Pattern.compile("\\b(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}(?::\\d+)?)\\b")
    private val PATH_PATTERN = Pattern.compile("(/(?:[a-zA-Z0-9._-]+/)+[a-zA-Z0-9._-]*)")
    private val NUMBER_SIZE_PATTERN = Pattern.compile("\\b(\\d+(?:\\.\\d+)?(?:[KMGT]B?|ms|s|Hz|fps|%|kB|MB|GB)?)\\b")
    private val KEY_VALUE_PATTERN = Pattern.compile("^([a-zA-Z0-9._-]+)=([^\\s].*)$")

    fun formatLine(type: LineType, rawText: String): AnnotatedString {
        return when (type) {
            LineType.COMMAND -> formatCommandLine(rawText)
            LineType.SYSTEM -> formatSystemLine(rawText)
            LineType.STDERR -> formatStderrLine(rawText)
            LineType.EXIT_STATUS -> formatExitStatusLine(rawText)
            LineType.STDOUT -> formatStdoutLine(rawText)
        }
    }

    private fun formatCommandLine(text: String): AnnotatedString {
        return buildAnnotatedString {
            // Check for [target] prefix e.g. [adb] or [su root]
            val bracketEnd = text.indexOf(']')
            if (text.startsWith("[") && bracketEnd > 0) {
                val targetBadge = text.substring(0, bracketEnd + 1)
                val rest = text.substring(bracketEnd + 1).trimStart()

                pushStyle(SpanStyle(color = CommandAccent, fontWeight = FontWeight.Bold))
                append(targetBadge)
                pop()

                pushStyle(SpanStyle(color = CommandPrompt, fontWeight = FontWeight.Bold))
                append(" $ ")
                pop()

                pushStyle(SpanStyle(color = CommandText, fontWeight = FontWeight.SemiBold))
                append(rest)
                pop()
            } else {
                pushStyle(SpanStyle(color = CommandPrompt, fontWeight = FontWeight.Bold))
                append("$ ")
                pop()

                pushStyle(SpanStyle(color = CommandText, fontWeight = FontWeight.SemiBold))
                append(text)
                pop()
            }
        }
    }

    private fun formatSystemLine(text: String): AnnotatedString {
        return buildAnnotatedString {
            pushStyle(SpanStyle(color = TextMuted, fontStyle = FontStyle.Italic))
            append("# $text")
            pop()
        }
    }

    private fun formatStderrLine(text: String): AnnotatedString {
        return buildAnnotatedString {
            pushStyle(SpanStyle(color = ColorError, fontWeight = FontWeight.SemiBold))
            append("! ")
            pop()
            pushStyle(SpanStyle(color = ColorError))
            append(text)
            pop()
        }
    }

    private fun formatExitStatusLine(text: String): AnnotatedString {
        val isSuccess = text.contains("exit code 0") || text.contains("finished with exit code 0")
        val icon = if (isSuccess) "✔ " else "✖ "
        val color = if (isSuccess) ColorSuccess else ColorError

        return buildAnnotatedString {
            pushStyle(SpanStyle(color = color, fontWeight = FontWeight.Bold))
            append(icon)
            pop()
            pushStyle(SpanStyle(color = color))
            append(text)
            pop()
        }
    }

    private fun formatStdoutLine(text: String): AnnotatedString {
        // If line contains ANSI escape sequences, parse ANSI
        if (text.contains("\u001B[")) {
            return parseAnsiCodes(text)
        }

        // Check for getprop format: [key]: [value]
        val getpropMatcher = GETPROP_PATTERN.matcher(text)
        if (getpropMatcher.matches()) {
            return buildAnnotatedString {
                pushStyle(SpanStyle(color = TextMuted))
                append("[")
                pop()

                pushStyle(SpanStyle(color = ColorCyan, fontWeight = FontWeight.Medium))
                append(getpropMatcher.group(1).orEmpty())
                pop()

                pushStyle(SpanStyle(color = TextMuted))
                append("]: [")
                pop()

                pushStyle(SpanStyle(color = ColorSuccess))
                append(getpropMatcher.group(2).orEmpty())
                pop()

                pushStyle(SpanStyle(color = TextMuted))
                append("]")
                pop()
            }
        }

        // Check for logcat format: E/Tag(1234): message
        val logcatMatcher = LOGCAT_PATTERN.matcher(text)
        if (logcatMatcher.matches()) {
            val level = logcatMatcher.group(1).orEmpty()
            val tag = logcatMatcher.group(2).orEmpty()
            val pid = logcatMatcher.group(3).orEmpty()
            val msg = logcatMatcher.group(4).orEmpty()

            val levelColor = when (level) {
                "V" -> TextMuted
                "D" -> ColorInfo
                "I" -> ColorSuccess
                "W" -> ColorWarning
                "E" -> ColorError
                "F" -> ColorFatal
                else -> TextDefault
            }

            return buildAnnotatedString {
                pushStyle(SpanStyle(color = levelColor, fontWeight = FontWeight.Bold))
                append("$level/")
                pop()

                pushStyle(SpanStyle(color = ColorPurple, fontWeight = FontWeight.Medium))
                append(tag)
                pop()

                if (pid.isNotEmpty()) {
                    pushStyle(SpanStyle(color = TextMuted))
                    append(pid)
                    pop()
                }

                pushStyle(SpanStyle(color = TextMuted))
                append(": ")
                pop()

                pushStyle(SpanStyle(color = if (level == "E" || level == "F") ColorError else TextDefault))
                append(msg)
                pop()
            }
        }

        // Check for package:com.example.app format
        val packageMatcher = PACKAGE_PATTERN.matcher(text)
        if (packageMatcher.matches()) {
            return buildAnnotatedString {
                pushStyle(SpanStyle(color = ColorCyan, fontWeight = FontWeight.Medium))
                append(packageMatcher.group(1).orEmpty())
                pop()
                pushStyle(SpanStyle(color = TextDefault))
                append(packageMatcher.group(2).orEmpty())
                pop()
            }
        }

        // Check for key=value pairs
        val kvMatcher = KEY_VALUE_PATTERN.matcher(text)
        if (kvMatcher.matches()) {
            return buildAnnotatedString {
                pushStyle(SpanStyle(color = ColorCyan, fontWeight = FontWeight.Medium))
                append(kvMatcher.group(1).orEmpty())
                pop()
                pushStyle(SpanStyle(color = TextMuted))
                append("=")
                pop()
                pushStyle(SpanStyle(color = ColorSuccess))
                append(kvMatcher.group(2).orEmpty())
                pop()
            }
        }

        // General smart highlighting (highlights paths, IPs, numbers, success/error keywords)
        return highlightGeneralText(text)
    }

    private fun highlightGeneralText(text: String): AnnotatedString {
        return buildAnnotatedString {
            var currentIndex = 0

            // Common keyword highlighting
            val lower = text.lowercase()
            if (lower.contains("permission denied") || lower.contains("error:") || lower.contains("failed") || lower.contains("not found")) {
                pushStyle(SpanStyle(color = ColorError))
                append(text)
                pop()
                return@buildAnnotatedString
            }

            if (lower.contains("success") || lower.contains("completed") || lower.contains("connected")) {
                pushStyle(SpanStyle(color = ColorSuccess))
                append(text)
                pop()
                return@buildAnnotatedString
            }

            pushStyle(SpanStyle(color = TextDefault))
            append(text)
            pop()
        }
    }

    private fun parseAnsiCodes(rawText: String): AnnotatedString {
        return buildAnnotatedString {
            val matcher = ANSI_PATTERN.matcher(rawText)
            var lastEnd = 0
            var currentStyle = SpanStyle(color = TextDefault)

            while (matcher.find()) {
                val matchStart = matcher.start()
                if (matchStart > lastEnd) {
                    val chunk = rawText.substring(lastEnd, matchStart)
                    pushStyle(currentStyle)
                    append(chunk)
                    pop()
                }

                val codes = matcher.group(1).orEmpty()
                currentStyle = applyAnsiCodes(currentStyle, codes)
                lastEnd = matcher.end()
            }

            if (lastEnd < rawText.length) {
                val remaining = rawText.substring(lastEnd)
                pushStyle(currentStyle)
                append(remaining)
                pop()
            }
        }
    }

    private fun applyAnsiCodes(existing: SpanStyle, codeString: String): SpanStyle {
        if (codeString.isEmpty() || codeString == "0") {
            return SpanStyle(color = TextDefault)
        }

        var color = existing.color
        var weight = existing.fontWeight
        var style = existing.fontStyle

        val parts = codeString.split(';')
        for (part in parts) {
            val code = part.toIntOrNull() ?: continue
            when (code) {
                0 -> {
                    color = TextDefault
                    weight = FontWeight.Normal
                    style = FontStyle.Normal
                }
                1 -> weight = FontWeight.Bold
                2 -> color = TextMuted
                3 -> style = FontStyle.Italic
                22 -> weight = FontWeight.Normal
                23 -> style = FontStyle.Normal

                // Foreground standard
                30 -> color = Color(0xFF4C566A) // Black / Charcoal
                31 -> color = ColorError         // Red
                32 -> color = ColorSuccess       // Green
                33 -> color = ColorWarning       // Yellow
                34 -> color = ColorInfo          // Blue
                35 -> color = ColorPurple        // Magenta
                36 -> color = ColorCyan          // Cyan
                37 -> color = TextDefault        // White
                39 -> color = TextDefault        // Default

                // Bright foreground
                90 -> color = TextMuted          // Bright Black
                91 -> color = ColorFatal         // Bright Red
                92 -> color = Color(0xFFA3BE8C) // Bright Green
                93 -> color = Color(0xFFEBCB8B) // Bright Yellow
                94 -> color = Color(0xFF81A1C1) // Bright Blue
                95 -> color = Color(0xFFB48EAD) // Bright Magenta
                96 -> color = Color(0xFF88C0D0) // Bright Cyan
                97 -> color = Color(0xFFECEFF4) // Bright White
            }
        }

        return SpanStyle(color = color, fontWeight = weight, fontStyle = style)
    }
}
