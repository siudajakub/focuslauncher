package de.mm20.launcher2.ui.launcher.focus

internal fun shouldShowFocusQuickStart(
    focusModeEnabled: Boolean,
    essentialCount: Int,
    distractingCount: Int,
    dailyScheduleEnabled: Boolean,
    habitsEnabled: Boolean,
): Boolean {
    if (essentialCount == 0 && distractingCount == 0) return true
    if (!focusModeEnabled) return false
    return !dailyScheduleEnabled && !habitsEnabled
}
