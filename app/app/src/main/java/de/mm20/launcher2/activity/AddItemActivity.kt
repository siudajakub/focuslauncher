package de.mm20.launcher2.activity

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import de.mm20.launcher2.appshortcuts.AppShortcut
import de.mm20.launcher2.data.customattrs.CustomAttributesRepository
import de.mm20.launcher2.services.favorites.FavoritesService
import de.mm20.launcher2.ui.R
import org.koin.android.ext.android.inject

class AddItemActivity : Activity() {

    private val favoritesService: FavoritesService by inject()
    private val customAttributesRepository: CustomAttributesRepository by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val shortcut = AppShortcut(this, intent)
        if (shortcut != null) {
            favoritesService.pinItem(shortcut)
            tagWebShortcut(shortcut)
            Toast.makeText(
                this,
                getString(R.string.shortcut_pinned, shortcut.label),
                Toast.LENGTH_SHORT,
            ).show()
        } else {
            Log.w("MM20", "Shortcut could not be added")
            Toast.makeText(this, R.string.shortcut_pin_failed, Toast.LENGTH_SHORT).show()
        }
        finish()
    }

    /**
     * Chrome (and other browsers) pin "Add to home screen" web links as launcher shortcuts that
     * are otherwise indistinguishable from regular app shortcuts. Give those a persistent
     * "(web)" label so the user can tell PWAs apart from app shortcuts everywhere they surface.
     */
    private fun tagWebShortcut(shortcut: de.mm20.launcher2.search.AppShortcut) {
        val packageName = shortcut.packageName ?: return
        if (!isWebShortcut(packageName)) return
        val label = shortcut.label
        if (label.isBlank() || label.trimEnd().endsWith("(web)")) return
        customAttributesRepository.setCustomLabel(shortcut, "$label (web)")
    }

    private fun isWebShortcut(packageName: String): Boolean {
        return packageName.startsWith("org.chromium.webapk.") || packageName in WebBrowserPackages
    }

    companion object {
        private val WebBrowserPackages = setOf(
            "com.android.chrome",
            "com.chrome.beta",
            "com.chrome.dev",
            "com.chrome.canary",
            "org.chromium.chrome",
            "org.mozilla.firefox",
            "org.mozilla.firefox_beta",
            "org.mozilla.fenix",
            "org.mozilla.focus",
            "com.brave.browser",
            "com.microsoft.emmx",
            "com.opera.browser",
            "com.opera.gx",
            "com.opera.mini.native",
            "com.duckduckgo.mobile.android",
            "com.kiwibrowser.browser",
            "com.sec.android.app.sbrowser",
            "com.vivaldi.browser",
            "com.UCMobile.intl",
        )
    }
}
