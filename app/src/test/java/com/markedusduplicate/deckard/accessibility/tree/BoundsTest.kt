package com.markedusduplicate.deckard.accessibility.tree

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BoundsTest {

    @Test
    fun `overlapping rects intersect`() {
        assertTrue(Bounds(0, 0, 100, 100).intersects(Bounds(50, 50, 150, 150)))
    }

    @Test
    fun `touching edges do not intersect`() {
        assertFalse(Bounds(0, 0, 100, 100).intersects(Bounds(100, 0, 200, 100)))
    }

    @Test
    fun `intersection area is the overlap`() {
        assertEquals(2500L, Bounds(0, 0, 100, 100).intersectionArea(Bounds(50, 50, 150, 150)))
    }

    @Test
    fun `intersection area is zero when disjoint`() {
        assertEquals(0L, Bounds(0, 0, 100, 100).intersectionArea(Bounds(200, 200, 300, 300)))
    }

    @Test
    fun `zero-size rect is empty`() {
        assertTrue(Bounds(10, 10, 10, 200).isEmpty)
        assertFalse(Bounds(0, 0, 100, 100).isEmpty)
    }
}
