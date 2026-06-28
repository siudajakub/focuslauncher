package de.mm20.launcher2.services.focus

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FocusSessionRuntimeTest {

    @Test
    fun `dnd start stores previous filter only once`() {
        val first = resolveFocusDndStart(
            dndEnabled = true,
            policyAccessGranted = true,
            storedPreviousFilter = -1,
            currentFilter = 2,
        )
        val replacement = resolveFocusDndStart(
            dndEnabled = true,
            policyAccessGranted = true,
            storedPreviousFilter = 2,
            currentFilter = 3,
        )

        assertEquals(2, first.previousFilterToStore)
        assertTrue(first.shouldSetPriority)
        assertNull(replacement.previousFilterToStore)
        assertTrue(replacement.shouldSetPriority)
    }

    @Test
    fun `dnd restore only happens while launcher filter is still active`() {
        assertTrue(
            shouldRestorePreviousDndFilter(
                policyAccessGranted = true,
                storedPreviousFilter = 2,
                currentFilter = 3,
                launcherFilter = 3,
            )
        )
        assertFalse(
            shouldRestorePreviousDndFilter(
                policyAccessGranted = true,
                storedPreviousFilter = 2,
                currentFilter = 1,
                launcherFilter = 3,
            )
        )
    }

    @Test
    fun `reconciliation keeps future sessions and expires old sessions`() {
        assertEquals(
            FocusSessionReconciliation.Active(sessionId = 42L, plannedEndsAt = 2_000L),
            resolveFocusSessionReconciliation(
                activeSessionId = 42L,
                activePlannedEndsAt = 2_000L,
                now = 1_000L,
            )
        )
        assertEquals(
            FocusSessionReconciliation.Expired(sessionId = 42L, plannedEndsAt = 2_000L),
            resolveFocusSessionReconciliation(
                activeSessionId = 42L,
                activePlannedEndsAt = 2_000L,
                now = 2_000L,
            )
        )
        assertEquals(
            FocusSessionReconciliation.NoActiveSession,
            resolveFocusSessionReconciliation(
                activeSessionId = null,
                activePlannedEndsAt = null,
                now = 2_000L,
            )
        )
    }

    @Test
    fun `expected session guard rejects stale workers`() {
        assertTrue(
            isExpectedFocusSession(
                activeSessionId = 7L,
                activePlannedEndsAt = 100L,
                expectedSessionId = 7L,
                expectedPlannedEndsAt = 100L,
            )
        )
        assertFalse(
            isExpectedFocusSession(
                activeSessionId = 8L,
                activePlannedEndsAt = 100L,
                expectedSessionId = 7L,
                expectedPlannedEndsAt = 100L,
            )
        )
    }
}
