package de.mm20.launcher2.preferences

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class LauncherSettingsDataSerializationTest {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        coerceInputValues = true
    }

    @Test
    fun `older settings use safe challenge defaults`() {
        val settings = json.decodeFromString<LauncherSettingsData>("{\"focusModeEnabled\":true}")

        assertFalse(settings.focusSystemInterceptionEnabled)
        assertEquals(FocusUnlockChallengeMethod.Steps, settings.focusUnlockChallengeMethod)
        assertEquals(30, settings.focusStepTarget)
        assertEquals(emptySet<FocusHomeSection>(), settings.focusHomeHiddenSections)
    }

    @Test
    fun `challenge settings survive serialization`() {
        val settings = LauncherSettingsData().copy(
            focusSystemInterceptionEnabled = true,
            focusUnlockChallengeMethod = FocusUnlockChallengeMethod.Tap,
            focusStepTarget = 42,
            focusHomeHiddenSections = setOf(FocusHomeSection.BrainDump, FocusHomeSection.Planning),
        )

        val restored = json.decodeFromString<LauncherSettingsData>(json.encodeToString(settings))

        assertEquals(settings, restored)
    }
}
