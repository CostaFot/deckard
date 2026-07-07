package com.markedusduplicate.deckard.accessibility.extract

import com.markedusduplicate.deckard.accessibility.node
import com.markedusduplicate.deckard.accessibility.tree.Bounds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NodeTextTest {

    private val viewport = Bounds(0, 0, 1080, 2000)

    @Test
    fun `collects visible text in reading order, de-duplicated`() {
        val root = node(
            children = listOf(
                node(text = "First", bounds = Bounds(0, 0, 1080, 100)),
                node(text = "Second", bounds = Bounds(0, 100, 1080, 200)),
                node(text = "First", bounds = Bounds(0, 200, 1080, 300)),
            ),
        )

        assertEquals("First\nSecond", collectVisibleText(listOf(root), viewport))
    }

    @Test
    fun `excludes off-screen, hidden, and password nodes`() {
        val root = node(
            children = listOf(
                node(text = "On screen", bounds = Bounds(0, 0, 1080, 100)),
                node(text = "Scrolled off", bounds = Bounds(0, 2500, 1080, 2600)),
                node(text = "Marked hidden", bounds = Bounds(0, 100, 1080, 200), isVisibleToUser = false),
                node(text = "secret", bounds = Bounds(0, 200, 1080, 300), isPassword = true),
            ),
        )

        assertEquals("On screen", collectVisibleText(listOf(root), viewport))
    }

    @Test
    fun `findTopmostByClassName does not descend into a match`() {
        val inner = node(className = "android.webkit.WebView", text = "inner")
        val outer = node(className = "android.webkit.WebView", children = listOf(inner))
        val root = node(children = listOf(outer))

        val found = root.findTopmostByClassName("android.webkit.WebView")

        assertEquals(1, found.size)
        assertEquals(outer, found.single())
    }

    @Test
    fun `findByViewIdSuffix matches the id segment`() {
        val match = node(viewId = "com.linkedin.android:id/feed_update", text = "post")
        val root = node(
            children = listOf(match, node(viewId = "com.linkedin.android:id/toolbar")),
        )

        assertEquals(listOf(match), root.findByViewIdSuffix("feed_update"))
    }

    @Test
    fun `mostVisible picks the largest viewport overlap`() {
        val small = node(text = "small", bounds = Bounds(0, 0, 100, 100))
        val large = node(text = "large", bounds = Bounds(0, 0, 1080, 1000))

        assertEquals(large, mostVisible(listOf(small, large), viewport))
    }

    @Test
    fun `mostVisible returns null when nothing overlaps`() {
        val offscreen = node(bounds = Bounds(0, 3000, 100, 3100))

        assertNull(mostVisible(listOf(offscreen), viewport))
    }
}
