package de.mm20.launcher2.ui.settings.about

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AboutLinksTest {
    @Test
    fun `about links only exposes inert github entry`() {
        val links = aboutLinks()
        val github = links.single()

        assertEquals(1, links.size)
        assertEquals("GitHub", github.title)
        assertEquals("github.com/MM2-0/Kvaesitso", github.summary)
        assertNull(github.url)
    }
}
