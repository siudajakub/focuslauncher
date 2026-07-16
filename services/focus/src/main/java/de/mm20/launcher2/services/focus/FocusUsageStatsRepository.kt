package de.mm20.launcher2.services.focus

import android.app.usage.UsageStatsManager
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId

/**
 * Today's per-package foreground time from the platform UsageStats, shared by every consumer.
 *
 * `queryAndAggregateUsageStats` over a whole day is an expensive blocking Binder call, and the
 * Focus Home and Focus Insights both want the same number, each re-deriving it whenever their
 * inputs emit. A single-flight cache with a short TTL collapses that to one platform scan per
 * cache window, no matter how many consumers or re-subscriptions there are. The cache is keyed on
 * the day, so it also drops itself at the midnight rollover rather than serving yesterday's total.
 */
class FocusUsageStatsRepository(
    private val context: Context,
) {
    private val mutex = Mutex()
    private var cachedUsage: Map<String, Long>? = null
    private var cachedDay: LocalDate? = null
    private var cachedAtMillis = 0L

    /**
     * Per-package foreground millis since the start of today. Empty when Usage Access is not
     * granted (or the service is unavailable), so callers degrade to "no data" rather than crash.
     */
    suspend fun getTodayForegroundUsage(): Map<String, Long> = mutex.withLock {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val now = System.currentTimeMillis()
        val cached = cachedUsage
        if (
            cached != null &&
            isUsageCacheFresh(
                cachedDay = cachedDay,
                cachedAtMillis = cachedAtMillis,
                today = today,
                nowMillis = now,
                ttlMillis = CACHE_TTL_MILLIS,
            )
        ) {
            return cached
        }
        val fresh = withContext(Dispatchers.IO) { query(zone, today, now) }
        cachedUsage = fresh
        cachedDay = today
        cachedAtMillis = now
        return fresh
    }

    /** Drop the cache so the next read hits the platform. For explicit user-driven refreshes. */
    suspend fun invalidate() = mutex.withLock {
        cachedUsage = null
        cachedDay = null
        cachedAtMillis = 0L
    }

    private fun query(zone: ZoneId, today: LocalDate, now: Long): Map<String, Long> {
        val usageStatsManager =
            context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
                ?: return emptyMap()
        val startOfDay = today.atStartOfDay(zone).toInstant().toEpochMilli()
        return try {
            usageStatsManager.queryAndAggregateUsageStats(startOfDay, now)
                .mapValues { it.value.totalTimeInForeground }
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private companion object {
        // Foreground time only moves in minutes, and both consumers render whole minutes, so a
        // stale-by-a-minute number is indistinguishable from a fresh one to the user.
        const val CACHE_TTL_MILLIS = 60_000L
    }
}

/**
 * Whether a cached usage aggregate may still be served. Fresh means: something is cached, it was
 * taken on [today] (so the midnight rollover always forces a re-query rather than reporting
 * yesterday's total), and it is younger than [ttlMillis]. Pure so it can be tested without the
 * platform.
 */
internal fun isUsageCacheFresh(
    cachedDay: LocalDate?,
    cachedAtMillis: Long,
    today: LocalDate,
    nowMillis: Long,
    ttlMillis: Long,
): Boolean {
    if (cachedDay == null) return false
    if (cachedDay != today) return false
    return nowMillis - cachedAtMillis < ttlMillis
}
