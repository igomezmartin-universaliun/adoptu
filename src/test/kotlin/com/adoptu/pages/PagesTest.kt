package com.adoptu.pages

import kotlinx.html.HTML
import org.junit.jupiter.api.Test
import kotlin.test.*

class PagesTest {

    @Test
    fun `pages package exists`() {
        assertEquals("com.adoptu.pages", this::class.java.packageName)
    }

    @Test
    fun `HTML class exists in kotlinx html`() {
        val htmlClass: Class<HTML> = HTML::class.java
        assertNotNull(htmlClass)
    }
}
