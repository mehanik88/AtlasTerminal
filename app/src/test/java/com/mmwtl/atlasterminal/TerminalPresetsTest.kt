package com.mmwtl.atlasterminal

import com.mmwtl.atlasterminal.data.CustomPreset
import com.mmwtl.atlasterminal.data.TerminalPresets
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TerminalPresetsTest {

    @Test
    fun testBuiltInCategoriesNotEmpty() {
        assertTrue(TerminalPresets.BUILT_IN_CATEGORIES.isNotEmpty())
        val allItems = TerminalPresets.BUILT_IN_CATEGORIES.flatMap { it.items }
        assertTrue(allItems.size >= 20)
        assertTrue(allItems.none { it.command.contains("remount") })
        assertTrue(allItems.none { it.command.matches(Regex(".*\\bmount\\s+-o\\s+remount\\b.*")) })
    }

    @Test
    fun testGetAllPresetsWithCustom() {
        val custom = listOf(
            CustomPreset(
                id = "custom_1",
                title = "Test Macro",
                command = "echo hello",
                category = "Custom Category"
            )
        )
        val all = TerminalPresets.getAllPresets(custom)
        val foundCustom = all.firstOrNull { it.id == "custom_1" }
        assertTrue(foundCustom != null)
        assertEquals("Test Macro", foundCustom?.title)
        assertEquals("echo hello", foundCustom?.command)
        assertTrue(foundCustom?.isCustom == true)
        assertEquals("custom_1", all.first().id)
    }

    @Test
    fun topPresetUsesBatchModeForReadableOutput() {
        val top = TerminalPresets.BUILT_IN_CATEGORIES
            .flatMap { it.items }
            .first { it.id == "sys_top" }

        assertEquals("top -b -n 1 -m 10", top.command)
    }
}
