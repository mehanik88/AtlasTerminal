package com.mmwtl.atlasterminal

import com.mmwtl.atlasterminal.core.AdbClient
import com.mmwtl.atlasterminal.core.AdbEndpoint
import com.mmwtl.atlasterminal.core.AdbEndpointMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdbEndpointTest {

    @Test
    fun testModeForPort() {
        assertEquals(AdbEndpointMode.ATLAS, AdbEndpoint.modeForPort(5555))
        assertEquals(AdbEndpointMode.PREFACE, AdbEndpoint.modeForPort(7777))
        assertEquals(AdbEndpointMode.TELNET, AdbEndpoint.modeForPort(-667))
        assertEquals(AdbEndpointMode.CUSTOM, AdbEndpoint.modeForPort(8888))
    }

    @Test
    fun testValidPort() {
        assertTrue(AdbEndpoint.isValidPort(5555))
        assertTrue(AdbEndpoint.isValidPort(7777))
        assertTrue(AdbEndpoint.isValidPort(-667))
        assertTrue(AdbEndpoint.isValidPort(1))
        assertTrue(AdbEndpoint.isValidPort(65535))
        assertFalse(AdbEndpoint.isValidPort(0))
        assertFalse(AdbEndpoint.isValidPort(-1))
        assertFalse(AdbEndpoint.isValidPort(70000))
    }

    @Test
    fun testAppendMarker() {
        val cmd = "getprop ro.build.version.release"
        val marker = "__DONE__:"
        val result = AdbClient.appendMarker(cmd, marker)
        assertEquals("getprop ro.build.version.release; echo __DONE__:\$?", result)

        val cmdWithSemicolon = "echo 1;"
        val result2 = AdbClient.appendMarker(cmdWithSemicolon, marker)
        assertEquals("echo 1; echo __DONE__:\$?", result2)
    }

    @Test
    fun testParseLeadingInt() {
        assertEquals(0, AdbClient.parseLeadingInt("0"))
        assertEquals(0, AdbClient.parseLeadingInt("  0\n"))
        assertEquals(127, AdbClient.parseLeadingInt("127\noutput"))
        assertEquals(null, AdbClient.parseLeadingInt("abc"))
    }
}
