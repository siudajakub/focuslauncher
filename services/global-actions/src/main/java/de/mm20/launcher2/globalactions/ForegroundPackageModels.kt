package de.mm20.launcher2.globalactions

fun reduceForegroundPackage(
    currentPackage: String?,
    eventPackage: String?,
    movedToForeground: Boolean,
    movedToBackground: Boolean,
): String? {
    if (eventPackage.isNullOrBlank()) return currentPackage
    if (movedToForeground) return eventPackage
    if (movedToBackground && currentPackage == eventPackage) return null
    return currentPackage
}

data class ForegroundActivity(
    val packageName: String,
    val resumedAtMillis: Long,
    val generation: Long,
)

data class ForegroundActivityState(
    val resumedActivities: Map<String, ForegroundActivity> = emptyMap(),
    val retiredActivities: Map<String, List<ForegroundActivity>> = emptyMap(),
    val legacyForegroundPackage: String? = null,
    val nextGeneration: Long = 1L,
) {
    fun currentPackage(): String? {
        return resumedActivities.values.maxByOrNull { it.resumedAtMillis }?.packageName
            ?: legacyForegroundPackage
    }
}

fun reduceForegroundActivityState(
    state: ForegroundActivityState,
    eventPackage: String?,
    activityId: String,
    eventTimestampMillis: Long,
    activityResumed: Boolean,
    activityPaused: Boolean,
    activityStopped: Boolean,
    legacyMovedToForeground: Boolean,
    legacyMovedToBackground: Boolean,
    supportsActivityLifecycleEvents: Boolean,
): ForegroundActivityState {
    if (eventPackage.isNullOrBlank()) return state
    if (!supportsActivityLifecycleEvents) {
        return state.copy(
            legacyForegroundPackage = reduceForegroundPackage(
                currentPackage = state.legacyForegroundPackage,
                eventPackage = eventPackage,
                movedToForeground = legacyMovedToForeground,
                movedToBackground = legacyMovedToBackground,
            ),
        )
    }
    if (activityResumed) {
        val existing = state.resumedActivities[activityId]
        if (existing != null) {
            return state.copy(
                resumedActivities = state.resumedActivities + (
                    activityId to existing.copy(resumedAtMillis = eventTimestampMillis)
                ),
            )
        }
        return state.copy(
            resumedActivities = state.resumedActivities + (
                activityId to ForegroundActivity(
                    packageName = eventPackage,
                    resumedAtMillis = eventTimestampMillis,
                    generation = state.nextGeneration,
                )
            ),
            nextGeneration = state.nextGeneration + 1L,
        )
    }
    if (activityPaused) {
        val paused = state.resumedActivities[activityId] ?: return state
        return state.copy(
            resumedActivities = state.resumedActivities - activityId,
            retiredActivities = state.retiredActivities + (
                activityId to (state.retiredActivities[activityId].orEmpty() + paused)
            ),
        )
    }
    if (activityStopped) {
        val retired = state.retiredActivities[activityId].orEmpty()
        if (retired.isNotEmpty()) {
            val remaining = retired.drop(1)
            return state.copy(
                retiredActivities = if (remaining.isEmpty()) {
                    state.retiredActivities - activityId
                } else {
                    state.retiredActivities + (activityId to remaining)
                },
            )
        }
        return state.copy(resumedActivities = state.resumedActivities - activityId)
    }
    return state
}
