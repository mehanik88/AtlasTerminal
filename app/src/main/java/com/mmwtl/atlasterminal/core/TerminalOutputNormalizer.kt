package com.mmwtl.atlasterminal.core

/**
 * Removes terminal control sequences that a line-oriented console cannot render.
 * SGR color sequences are kept for [com.mmwtl.atlasterminal.ui.theme.TerminalColorizer].
 */
internal class TerminalOutputNormalizer {
    private enum class State {
        NORMAL,
        ESC,
        ESC_INTERMEDIATE,
        CSI,
        OSC,
        OSC_ESC
    }

    private var state = State.NORMAL
    private var sequence = StringBuilder()
    private var pendingCarriageReturn = false

    @Synchronized
    fun feed(chunk: String): String = buildString(chunk.length) {
        chunk.forEach { consume(it, this) }
    }

    @Synchronized
    fun finish(): String = buildString {
        if (pendingCarriageReturn) append('\n')
        reset()
    }

    private fun consume(character: Char, output: StringBuilder) {
        if (state == State.NORMAL && pendingCarriageReturn) {
            if (character == '\n') {
                output.append('\n')
                pendingCarriageReturn = false
                return
            }
            output.append('\n')
            pendingCarriageReturn = false
        }

        when (state) {
            State.NORMAL -> consumeNormal(character, output)
            State.ESC -> consumeAfterEscape(character, output)
            State.ESC_INTERMEDIATE -> consumeEscapeIntermediate(character, output)
            State.CSI -> consumeCsi(character, output)
            State.OSC -> consumeOsc(character)
            State.OSC_ESC -> consumeOscAfterEscape(character)
        }
    }

    private fun consumeNormal(character: Char, output: StringBuilder) {
        when {
            character == '\u001B' -> {
                state = State.ESC
                sequence = StringBuilder().append(character)
            }
            character == '\r' -> pendingCarriageReturn = true
            character == '\n' || character == '\t' -> output.append(character)
            character.code < 0x20 || character.code == 0x7F || character.isISOControl() -> Unit
            else -> output.append(character)
        }
    }

    private fun consumeAfterEscape(character: Char, output: StringBuilder) {
        sequence.append(character)
        when {
            character == '[' -> state = State.CSI
            character == ']' -> state = State.OSC
            character.code in ESC_INTERMEDIATE_RANGE -> state = State.ESC_INTERMEDIATE
            character == '\n' || character == '\r' -> {
                reset()
                consume(character, output)
            }
            else -> reset()
        }
    }

    private fun consumeEscapeIntermediate(character: Char, output: StringBuilder) {
        when {
            character.code in ESC_INTERMEDIATE_RANGE -> Unit
            character.code in ESC_FINAL_RANGE -> reset()
            character == '\n' || character == '\r' -> {
                reset()
                consume(character, output)
            }
            else -> reset()
        }
    }

    private fun consumeCsi(character: Char, output: StringBuilder) {
        if (character == '\n' || character == '\r') {
            reset()
            consume(character, output)
            return
        }

        sequence.append(character)
        if (character.code in CSI_FINAL_RANGE) {
            if (sequence.toString().matches(SGR_PATTERN)) {
                output.append(sequence)
            }
            reset()
        }
    }

    private fun consumeOsc(character: Char) {
        sequence.append(character)
        when {
            character == '\u0007' -> reset()
            character == '\u001B' -> state = State.OSC_ESC
        }
    }

    private fun consumeOscAfterEscape(character: Char) {
        sequence.append(character)
        state = if (character == '\\') State.NORMAL else State.OSC
        if (state == State.NORMAL) sequence.setLength(0)
    }

    private fun reset() {
        state = State.NORMAL
        sequence.setLength(0)
        pendingCarriageReturn = false
    }

    companion object {
        private val SGR_PATTERN = Regex("\\u001B\\[[0-9;]*m")
        private val ESC_INTERMEDIATE_RANGE = 0x20..0x2F
        private val ESC_FINAL_RANGE = 0x30..0x7E
        private val CSI_FINAL_RANGE = 0x40..0x7E

        fun normalize(text: String): String {
            val normalizer = TerminalOutputNormalizer()
            return normalizer.feed(text) + normalizer.finish()
        }
    }
}
