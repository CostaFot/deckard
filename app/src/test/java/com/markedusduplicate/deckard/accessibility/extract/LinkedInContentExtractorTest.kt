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
    private val button = "android.widget.Button"
    private val postBody = "I shipped an Android app to Google Play. Solo.\n\nThe workflow does the rest."

    @Test
    fun `handles the LinkedIn package only`() {
        assertTrue(extractor.handles("com.linkedin.android"))
        assertFalse(extractor.handles("com.android.chrome"))
    }

    /** Mirrors a real feed capture: the post body is the largest full-width node inside the Compose
     *  feed; author/headline are small and indented; chrome lives outside the feed. */
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

    /** A real capture: the body is a `Button` (not a `TextView`), truncated with "… more", with the
     *  full post kept in contentDescription. The largest `TextView` is the "Suggested" chrome label. */
    @Test
    fun `reads a Button body and prefers the full content description over truncated text`() {
        val fullPost = "Are Zombie Coroutines lurking in your Android app? " +
                "Cancellation is cooperative and CPU-bound work can keep running. " +
                "Here is how to handle it correctly with ensureActive and NonCancellable."
        val root = node(
            bounds = Bounds(0, 0, 1280, 2800),
            children = listOf(
                node(
                    viewId = "sdui:lazyColumn",
                    bounds = Bounds(0, 295, 1280, 2550),
                    children = listOf(
                        node(className = textView, text = "Suggested", bounds = Bounds(42, 2011, 1014, 2068)),
                        node(
                            className = button,
                            text = "Are Zombie Coroutines lurking in your Android app?… more",
                            contentDescription = fullPost,
                            bounds = Bounds(0, 600, 1280, 2000),
                        ),
                    ),
                ),
            ),
        )

        assertEquals(fullPost, extractor.extract(root))
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
                        node(className = button, text = "On screen", bounds = Bounds(0, 664, 1280, 2000)),
                        node(className = button, text = "Scrolled off below", bounds = Bounds(0, 3000, 1280, 4000)),
                    ),
                ),
            ),
        )

        assertEquals("On screen", extractor.extract(root))
    }

    /** Selection follows what the user centres: the post under the screen centre wins even when a
     *  neighbouring post covers more area. */
    @Test
    fun `prefers the post under the screen centre over a larger neighbour`() {
        val centered = "The post the user has centred on screen, long enough to be the body."
        val root = node(
            bounds = Bounds(0, 0, 1280, 2800),
            children = listOf(
                node(
                    viewId = "sdui:lazyColumn",
                    bounds = Bounds(0, 295, 1280, 2550),
                    children = listOf(
                        node(className = button, text = centered, bounds = Bounds(0, 900, 1280, 1500)),
                        node(
                            className = button,
                            text = "A much taller neighbouring post that covers more of the screen area",
                            bounds = Bounds(0, 1600, 1280, 2550),
                        ),
                    ),
                ),
            ),
        )

        assertEquals(centered, extractor.extract(root))
    }
}
