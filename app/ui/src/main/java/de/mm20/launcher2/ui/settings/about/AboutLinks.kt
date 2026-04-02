package de.mm20.launcher2.ui.settings.about

internal data class AboutLink(
    val title: String,
    val summary: String,
    val iconRes: Int,
    val url: String? = null,
)

internal fun aboutLinks(): List<AboutLink> {
    return listOf(
        AboutLink(
            title = "GitHub",
            summary = "github.com/MM2-0/Kvaesitso",
            iconRes = de.mm20.launcher2.ui.R.drawable.github,
        )
    )
}
