package com.markedusduplicate.deckard.suggestion.llm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OcrPromptTest {

    @Test
    fun `transcribe prompt asks to read the screenshot text`() {
        val prompt = OcrPrompt.transcribe()
        assertTrue(prompt.contains("screenshot"))
        assertTrue(prompt.contains("Transcribe"))
    }

    @Test
    fun `extractMainContent prompt isolates one post and forbids rewriting`() {
        val prompt = OcrPrompt.extractMainContent()
        assertTrue(prompt.contains("word for word"))
        assertTrue(prompt.contains("Do not summarize"))
        assertTrue(prompt.contains("one post"))
    }

    @Test
    fun `clean trims whitespace and surrounding quotes`() {
        assertEquals("Hello world", OcrPrompt.clean("  \"Hello world\" "))
    }

    @Test
    fun `clean leaves plain text untouched`() {
        assertEquals("Hello world", OcrPrompt.clean("Hello world"))
    }
}
