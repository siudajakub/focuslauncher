package de.mm20.launcher2.services.focus

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class FocusUsageCacheTest {

    private val today: LocalDate = LocalDate.of(2026, 7, 16)
    private val ttl = 60_000L

    @Test
    fun `nothing cached is never fresh`() {
        assertFalse(
            isUsageCacheFresh(
                cachedDay = null,
                cachedAtMillis = 0L,
                today = today,
                nowMillis = 1_000L,
                ttlMillis = ttl,
            )
        )
    }

    @Test
    fun `a cache taken moments ago is served`() {
        assertTrue(
            isUsageCacheFresh(
                cachedDay = today,
                cachedAtMillis = 10_000L,
                today = today,
                nowMillis = 10_500L,
                ttlMillis = ttl,
            )
        )
    }

    @Test
    fun `a cache older than the ttl is re-queried`() {
        assertFalse(
            isUsageCacheFresh(
                cachedDay = today,
                cachedAtMillis = 10_000L,
                today = today,
                nowMillis = 10_000L + ttl,
                ttlMillis = ttl,
            )
        )
    }

    @Test
    fun `yesterday's aggregate is never served after the midnight rollover`() {
        // Even well inside the TTL: the day changed, so the total covers the wrong window.
        assertFalse(
            isUsageCacheFresh(
                cachedDay = today.minusDays(1),
                cachedAtMillis = 10_000L,
                today = today,
                nowMillis = 10_100L,
                ttlMillis = ttl,
            )
        )
    }
}
