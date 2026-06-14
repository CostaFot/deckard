package com.markedusduplicate.deckard.accessibility.extract

import com.markedusduplicate.deckard.accessibility.node
import com.markedusduplicate.deckard.accessibility.tree.Bounds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GenericContentExtractorTest {

    private val extractor = GenericContentExtractor()

    @Test
    fun `with no WebView, reads the whole viewport-clipped tree`() {
        val root = node(
            children = listOf(
                node(text = "Title", bounds = Bounds(0, 0, 1080, 100)),
                node(text = "Body", bounds = Bounds(0, 100, 1080, 200)),
            ),
        )

        assertEquals("Title\nBody", extractor.extract(root))
    }

    @Test
    fun `with a WebView, reads only inside it, dropping chrome`() {
        val root = node(
            children = listOf(
                node(text = "https://example.com", bounds = Bounds(0, 0, 1080, 80)),
                node(
                    className = "android.webkit.WebView",
                    bounds = Bounds(0, 80, 1080, 1000),
                    children = listOf(node(text = "Article body", bounds = Bounds(0, 100, 1080, 300))),
                ),
            ),
        )

        assertEquals("Article body", extractor.extract(root))
    }

    @Test
    fun `returns null when there's nothing readable`() {
        assertNull(extractor.extract(node()))
    }
}
