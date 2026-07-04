package de.mm20.launcher2.services.focus

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure-logic coverage for the system-wide (Settings.Secure daltonizer) grayscale decisions that
 * gate mutating a global accessibility setting during a focus session. Mirrors the DND
 * store/restore contract in [resolveFocusDndStart] / [shouldRestorePreviousDndFilter].
 */
class FocusSystemGrayscaleTest {

    @Test
    fun `no-op when the user has not opted in`() {
        val decision = resolveSystemGrayscaleStart(
            grayscaleEnabled = false,
            canWriteSecureSettings = true,
            storedPreviousEnabled = -1,
            currentEnabled = 0,
            currentMode = -1,
        )
        assertFalse(decision.shouldEnableGrayscale)
        assertNull(decision.previousEnabledToStore)
    }

    @Test
    fun `no-op without WRITE_SECURE_SETTINGS`() {
        val decision = resolveSystemGrayscaleStart(
            grayscaleEnabled = true,
            canWriteSecureSettings = false,
            storedPreviousEnabled = -1,
            currentEnabled = 0,
            currentMode = -1,
        )
        assertFalse(decision.shouldEnableGrayscale)
        assertNull(decision.previousEnabledToStore)
    }

    @Test
    fun `captures the pre-session daltonizer state exactly once`() {
        val first = resolveSystemGrayscaleStart(
            grayscaleEnabled = true,
            canWriteSecureSettings = true,
            storedPreviousEnabled = -1,
            currentEnabled = 0,
            currentMode = 13,
        )
        assertTrue(first.shouldEnableGrayscale)
        assertEquals(0, first.previousEnabledToStore)
        assertEquals(13, first.previousModeToStore)

        // Re-entrant start (state already captured): still enable, but do not overwrite the
        // stored "before" value with our own monochrome state.
        val second = resolveSystemGrayscaleStart(
            grayscaleEnabled = true,
            canWriteSecureSettings = true,
            storedPreviousEnabled = 0,
            currentEnabled = 1,
            currentMode = DALTONIZER_MONOCHROMACY,
        )
        assertTrue(second.shouldEnableGrayscale)
        assertNull(second.previousEnabledToStore)
        assertNull(second.previousModeToStore)
    }

    @Test
    fun `restores only when the launcher still owns the grayscale state`() {
        assertTrue(
            shouldRestoreSystemGrayscale(
                canWriteSecureSettings = true,
                storedPreviousEnabled = 0,
                currentEnabled = 1,
                currentMode = DALTONIZER_MONOCHROMACY,
            )
        )
        // User turned it off / switched to a colour-blind filter themselves — leave it be.
        assertFalse(
            shouldRestoreSystemGrayscale(
                canWriteSecureSettings = true,
                storedPreviousEnabled = 0,
                currentEnabled = 1,
                currentMode = 13,
            )
        )
        // Nothing captured, or permission lost.
        assertFalse(
            shouldRestoreSystemGrayscale(
                canWriteSecureSettings = true,
                storedPreviousEnabled = -1,
                currentEnabled = 1,
                currentMode = DALTONIZER_MONOCHROMACY,
            )
        )
        assertFalse(
            shouldRestoreSystemGrayscale(
                canWriteSecureSettings = false,
                storedPreviousEnabled = 0,
                currentEnabled = 1,
                currentMode = DALTONIZER_MONOCHROMACY,
            )
        )
    }
}
