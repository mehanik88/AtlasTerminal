package com.mmwtl.atlasterminal

import com.mmwtl.atlasterminal.core.TerminalOutputNormalizer
import org.junit.Assert.assertEquals
import org.junit.Test

class TerminalOutputNormalizerTest {

    @Test
    fun removesCursorAndTerminalProbeSequences() {
        val raw = "\u001B[s\u001B[999C\u001B[999B\u001B[6n\u001B[u" +
            "\u001B[?25l\u001B[H\u001B[JTasks: 170 total\n"

        assertEquals("Tasks: 170 total\n", TerminalOutputNormalizer.normalize(raw))
    }

    @Test
    fun keepsSgrColorSequencesForColorizer() {
        val raw = "\u001B[32mSuccess\u001B[0m"

        assertEquals(raw, TerminalOutputNormalizer.normalize(raw))
    }

    @Test
    fun handlesControlSequenceSplitAcrossChunks() {
        val normalizer = TerminalOutputNormalizer()

        val first = normalizer.feed("\u001B[?25")
        val second = normalizer.feed("lTasks")
        val last = normalizer.finish()

        assertEquals("", first)
        assertEquals("Tasks", second)
        assertEquals("", last)
    }

    @Test
    fun removesShortEscapeSequences() {
        val raw = "before\u001B(Bafter\u001B7done"

        assertEquals("beforeafterdone", TerminalOutputNormalizer.normalize(raw))
    }

    @Test
    fun normalizesCarriageReturnsAndDropsOtherControlCharacters() {
        val raw = "one\r\ntwo\rthree\u0007"

        assertEquals("one\ntwo\nthree", TerminalOutputNormalizer.normalize(raw))
    }
}
