package com.autoclock

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccessibilityConfigTest {

    @Test
    fun `accessibility service config listens to target package`() {
        val xml = String(Files.readAllBytes(accessibilityConfigPath()))

        assertTrue(xml.contains("android:packageNames=\"${TargetApps.CLOCK_PACKAGE}\""))
        assertFalse(xml.contains("com.example.targetapp"))
    }

    private fun accessibilityConfigPath(): Path {
        return listOf(
            Paths.get("app/src/main/res/xml/accessibility_service_config.xml"),
            Paths.get("src/main/res/xml/accessibility_service_config.xml")
        ).first { Files.exists(it) }
    }
}
