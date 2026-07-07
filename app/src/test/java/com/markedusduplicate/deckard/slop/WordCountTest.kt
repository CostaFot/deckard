package com.markedusduplicate.deckard.slop

import org.junit.Assert.assertEquals
import org.junit.Test

class WordCountTest {

    @Test
    fun `counts whitespace-separated words`() {
        assertEquals(2, wordCount("hello world"))
    }

    @Test
    fun `collapses runs of whitespace, including newlines and tabs`() {
        assertEquals(3, wordCount("  one\n\ntwo\tthree  "))
    }

    @Test
    fun `blank text is zero words`() {
        assertEquals(0, wordCount(""))
        assertEquals(0, wordCount("   \n  "))
    }

    @Test
    fun `the detection threshold is fifty words`() {
        assertEquals(50, MIN_WORDS_TO_DETECT)
    }
}
