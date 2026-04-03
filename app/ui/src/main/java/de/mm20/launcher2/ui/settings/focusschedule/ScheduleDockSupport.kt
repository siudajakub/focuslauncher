package de.mm20.launcher2.ui.settings.focusschedule

import de.mm20.launcher2.preferences.ScheduleDockMapping

fun findScheduleDockMapping(
    eventName: String,
    mappings: List<ScheduleDockMapping>,
): ScheduleDockMapping? {
    return mappings.firstOrNull { it.eventName == eventName }
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
