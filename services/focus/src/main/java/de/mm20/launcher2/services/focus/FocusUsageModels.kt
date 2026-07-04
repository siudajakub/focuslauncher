package de.mm20.launcher2.services.focus

/** One distracting app's real foreground time today, in whole minutes. */
data class DistractingUsageEntry(
    val label: String,
    val minutes: Int,
)

/**
 * Today's real screen time on the apps the user classified as distracting — aggregated from the
 * platform UsageStats, not the launcher's own gate log. Self-monitoring with real, goal-anchored
 * feedback is the evidence-backed complement to the friction at the gate.
 */
data class DistractingUsageSummary(
    val totalMinutes: Int = 0,
    val appCount: Int = 0,
    val topApps: List<DistractingUsageEntry> = emptyList(),
) {
    val hasData: Boolean get() = totalMinutes > 0
}

/**
 * Pure aggregation: fold per-package foreground time into a per-label summary for the distracting
 * apps only. [usageByPackage] maps a package name to its foreground millis today; [distracting] is
 * the (packageName, label) list of distracting apps (deduplicated by package here so the same
 * package under two profiles is not double-counted). Apps with zero time today are dropped.
 */
fun summarizeDistractingUsage(
    usageByPackage: Map<String, Long>,
    distracting: List<Pair<String, String>>,
    topN: Int = 4,
): DistractingUsageSummary {
    val entries = distracting
        .distinctBy { it.first }
        .mapNotNull { (packageName, label) ->
            val minutes = ((usageByPackage[packageName] ?: 0L) / 60_000L).toInt()
            if (minutes > 0) DistractingUsageEntry(label = label, minutes = minutes) else null
        }
    if (entries.isEmpty()) return DistractingUsageSummary()
    return DistractingUsageSummary(
        totalMinutes = entries.sumOf { it.minutes },
        appCount = entries.size,
        topApps = entries.sortedByDescending { it.minutes }.take(topN),
    )
}
