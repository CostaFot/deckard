package com.markedusduplicate.deckard.accessibility.tree

import com.markedusduplicate.deckard.accessibility.node
import org.junit.Assert.assertEquals
import org.junit.Test

class ScreenNodeDebugTest {

    @Test
    fun `renders class, id, text and bounds, indented by depth`() {
        val root = node(
            className = "android.widget.FrameLayout",
            bounds = Bounds(0, 0, 1080, 2000),
            children = listOf(
                node(
                    className = "android.widget.TextView",
                    viewId = "com.linkedin.android:id/feed_text",
                    text = "Hello",
                    bounds = Bounds(0, 0, 1080, 100),
                ),
            ),
        )

        val expected = """
            FrameLayout [0,0-1080,2000]
              TextView #feed_text "Hello" [0,0-1080,100]

        """.trimIndent()

        assertEquals(expected, root.toDebugString())
    }
}
