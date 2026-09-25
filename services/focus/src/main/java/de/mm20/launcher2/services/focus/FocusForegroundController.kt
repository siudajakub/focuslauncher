package de.mm20.launcher2.services.focus

interface FocusForegroundController {
    fun isAccessibilityConnected(): Boolean
    fun hasUsageAccess(): Boolean
    fun currentForegroundPackage(): String?
    fun goHome(): Boolean
    fun isProtectedPackage(packageName: String): Boolean
}
