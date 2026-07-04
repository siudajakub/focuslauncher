package de.mm20.launcher2.ui.launcher.focus

import android.app.usage.UsageStatsManager
import android.content.Context
import java.time.LocalDate
import java.time.ZoneId

/**
 * Today's per-package foreground time (millis) from the platform UsageStats. Returns an empty map
 * when Usage Access is not granted (or the service is unavailable), so callers degrade to "no data"
 * rather than crashing. Blocking Binder call — invoke off the main thread.
 */
internal fun queryTodayForegroundUsage(context: Context): Map<String, Long> {
    val usageStatsManager =
        context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return emptyMap()
    val zone = ZoneId.systemDefault()
    val startOfDay = LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli()
    val now = System.currentTimeMillis()
    return try {
        usageStatsManager.queryAndAggregateUsageStats(startOfDay, now)
            .mapValues { it.value.totalTimeInForeground }
    } catch (e: Exception) {
        emptyMap()
    }
}
