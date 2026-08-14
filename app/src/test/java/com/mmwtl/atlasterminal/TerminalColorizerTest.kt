package com.mmwtl.atlasterminal

import com.mmwtl.atlasterminal.core.LineType
import com.mmwtl.atlasterminal.ui.theme.TerminalColorizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TerminalColorizerTest {

    @Test
    fun testCommandLineFormatting() {
        val line = TerminalColorizer.formatLine(LineType.COMMAND, "[adb] getprop ro.product.model")
        assertEquals("[adb] $ getprop ro.product.model", line.text)
    }

    @Test
    fun testExitStatusFormatting() {
        val successLine = TerminalColorizer.formatLine(LineType.EXIT_STATUS, "Process finished with exit code 0")
        assertTrue(successLine.text.startsWith("✔ "))

        val failLine = TerminalColorizer.formatLine(LineType.EXIT_STATUS, "Process failed with exit code 1")
        assertTrue(failLine.text.startsWith("✖ "))
    }

    @Test
    fun testAnsiCodeParsing() {
        val ansiText = "\u001B[32mSuccess\u001B[0m \u001B[31mFailure\u001B[0m"
        val line = TerminalColorizer.formatLine(LineType.STDOUT, ansiText)
        assertEquals("Success Failure", line.text)
    }

    @Test
    fun testGetpropFormatting() {
        val propText = "[ro.build.version.release]: [10]"
        val line = TerminalColorizer.formatLine(LineType.STDOUT, propText)
        assertEquals("[ro.build.version.release]: [10]", line.text)
    }
}
