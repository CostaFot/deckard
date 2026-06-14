package com.markedusduplicate.deckard.accessibility.extract

import com.markedusduplicate.deckard.accessibility.node
import com.markedusduplicate.deckard.accessibility.tree.Bounds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LinkedInContentExtractorTest {

    private val extractor = LinkedInContentExtractor()

    private val textView = "android.widget.TextView"
    private val postBody = "I shipped an Android app to Google Play. Solo.\n\nThe workflow does the rest."

    @Test
    fun `handles the LinkedIn package only`() {
        assertTrue(extractor.handles("com.linkedin.android"))
        assertFalse(extractor.handles("com.android.chrome"))
    }

    /** Mirrors a real feed capture: the post body is the largest full-width TextView inside the
     *  Compose feed; author/headline are small and indented; chrome lives outside the feed. */
    @Test
    fun `returns the most-visible post body, dropping author, headline and chrome`() {
        val root = node(
            bounds = Bounds(0, 0, 1280, 2800),
            children = listOf(
                node(className = textView, text = "Search", bounds = Bounds(336, 175, 1028, 260)),
                node(
                    viewId = "sdui:lazyColumn",
                    bounds = Bounds(0, 295, 1280, 2550),
                    children = listOf(
                        node(className = textView, text = "Norbert Csibi", bounds = Bounds(238, 462, 644, 536)),
                        node(className = textView, text = "Staff Software Engineer @ Ivanti", bounds = Bounds(238, 536, 899, 593)),
                        node(className = textView, text = postBody, bounds = Bounds(0, 664, 1280, 2550)),
                    ),
                ),
                node(className = textView, text = "Home", bounds = Bounds(80, 2660, 179, 2707)),
            ),
        )

        assertEquals(postBody, extractor.extract(root))
    }

    @Test
    fun `ignores text scrolled out of the viewport`() {
        val root = node(
            bounds = Bounds(0, 0, 1280, 2800),
            children = listOf(
                node(
                    viewId = "sdui:lazyColumn",
                    bounds = Bounds(0, 295, 1280, 2550),
                    children = listOf(
                        node(className = textView, text = "On screen", bounds = Bounds(0, 664, 1280, 1200)),
                        node(className = textView, text = "Scrolled off below", bounds = Bounds(0, 3000, 1280, 4000)),
                    ),
                ),
            ),
        )

        assertEquals("On screen", extractor.extract(root))
    }
}
