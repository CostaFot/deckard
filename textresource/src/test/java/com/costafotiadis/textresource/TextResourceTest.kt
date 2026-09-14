package com.costafotiadis.textresource

import android.content.res.Resources
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TextResourceTest {

    private val resources = mockk<Resources>()

    @Test
    fun `raw says its text without touching resources`() {
        assertEquals("Slop, my son.", TextResource.raw("Slop, my son.").asString(resources))
    }

    @Test
    fun `simple resolves its resource with the args it was given`() {
        every { resources.getString(any(), *anyVararg()) } returns "Bring me 50 words."

        val result = TextResource.simple(TOO_THIN, 50).asString(resources)

        assertEquals("Bring me 50 words.", result)
        verify { resources.getString(TOO_THIN, 50) }
    }

    @Test
    fun `simple with no args skips the formatting overload`() {
        every { resources.getString(any()) } returns "Slop, my son."

        assertEquals("Slop, my son.", TextResource.simple(CATCHPHRASE).asString(resources))

        verify { resources.getString(CATCHPHRASE) }
        verify(exactly = 0) { resources.getString(any(), *anyVararg()) }
    }

    @Test
    fun `factory instances with the same value are equal`() {
        assertEquals(TextResource.raw("hi"), TextResource.raw("hi"))
        assertEquals(TextResource.raw("hi").hashCode(), TextResource.raw("hi").hashCode())

        assertEquals(TextResource.simple(TOO_THIN, 50), TextResource.simple(TOO_THIN, 50))
        assertEquals(
            TextResource.simple(TOO_THIN, 50).hashCode(),
            TextResource.simple(TOO_THIN, 50).hashCode(),
        )
    }

    @Test
    fun `factory instances differing in resource or args are not equal`() {
        assertNotEquals(TextResource.raw("hi"), TextResource.raw("bye"))
        assertNotEquals(TextResource.simple(TOO_THIN, 50), TextResource.simple(CATCHPHRASE, 50))
        assertNotEquals(TextResource.simple(TOO_THIN, 50), TextResource.simple(TOO_THIN, 40))
        assertNotEquals(
            TextResource.simple(TOO_THIN, "a", "b"),
            TextResource.simple(TOO_THIN, "b", "a"),
        )
    }

    @Test
    fun `mutating the caller's array afterwards does not change what simple says`() {
        val args = arrayOf<Any>("first")
        val text = TextResource.simple(TOO_THIN, *args)

        args[0] = "second"

        assertEquals(TextResource.simple(TOO_THIN, "first"), text)
    }

    @Test
    fun `a SAM instance has reference equality, unlike a factory one`() {
        val sam = TextResource { "Slop, my son." }
        val other = TextResource { "Slop, my son." }

        assertNotEquals(sam, other)
        assertTrue(listOf(sam).contains(sam))
        assertEquals(listOf(TextResource.raw("hi")).contains(TextResource.raw("hi")), true)
    }

    private companion object {
        const val TOO_THIN = 0x7f0a0001
        const val CATCHPHRASE = 0x7f0a0002
    }
}
