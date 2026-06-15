package de.mm20.launcher2.ui.settings.focusschedule

import de.mm20.launcher2.ktx.normalize
import de.mm20.launcher2.preferences.ScheduleDockMapping

fun findScheduleDockMapping(
    eventName: String,
    mappings: List<ScheduleDockMapping>,
): ScheduleDockMapping? {
    val normalizedEventName = eventName.normalizeScheduleDockEventName()
    return mappings.firstOrNull {
        it.eventName.normalizeScheduleDockEventName() == normalizedEventName
    }
}

private fun String.normalizeScheduleDockEventName(): String {
    return normalize().trim().replace(Regex("\\s+"), " ")
}

fun formatScheduleDockAppsSummary(appLabels: List<String>): String {
    if (appLabels.isEmpty()) {
        return ""
    }
    val shown = appLabels.take(3)
    return if (appLabels.size <= 3) {
        shown.joinToString(", ")
    } else {
        "${shown.joinToString(", ")} +${appLabels.size - 3}"
    }
}
