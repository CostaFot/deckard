package com.markedusduplicate.deckard.accessibility.extract

import com.markedusduplicate.deckard.accessibility.node
import com.markedusduplicate.deckard.accessibility.tree.Bounds
import com.markedusduplicate.deckard.accessibility.tree.ScreenNode
import org.junit.Assert.assertEquals
import org.junit.Test

class ScreenContentExtractorsTest {

    private val tree = node(children = listOf(node(text = "hello", bounds = Bounds(0, 0, 1080, 100))))

    private val sentinel = object : ScreenContentExtractor {
        override fun handles(packageName: String) = packageName == "com.example.app"
        override fun extract(root: ScreenNode) = "SENTINEL"
    }

    private val registry = ScreenContentExtractors(setOf(sentinel), GenericContentExtractor())

    @Test
    fun `dispatches to the extractor that claims the package`() {
        assertEquals("SENTINEL", registry.extract("com.example.app", tree))
    }

    @Test
    fun `falls back to the generic extractor for an unclaimed package`() {
        assertEquals("hello", registry.extract("com.unknown.app", tree))
    }
}
