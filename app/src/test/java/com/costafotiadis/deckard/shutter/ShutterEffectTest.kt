package com.costafotiadis.deckard.shutter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

/**
 * The keys, and nothing else.
 *
 * What each effect *looks like* is judged on a screen — there is no assertion that tells you a
 * travelling highlight reads better than crop marks. What a test can hold is that the choice
 * survives: the key is what sits in the preferences file and what `scripts/deckard effect` names, so
 * renaming a constant must not silently reset everyone to the default.
 */
class ShutterEffectTest {

    @Test
    fun `every effect comes back from its own key`() {
        ShutterEffect.entries.forEach { effect ->
            assertSame(effect, ShutterEffect.fromKey(effect.key))
        }
    }

    @Test
    fun `keys are distinct`() {
        val keys = ShutterEffect.entries.map { it.key }
        assertEquals(keys.size, keys.toSet().size)
    }

    @Test
    fun `an unknown or missing key falls back to the default`() {
        assertSame(ShutterEffect.Default, ShutterEffect.fromKey(null))
        assertSame(ShutterEffect.Default, ShutterEffect.fromKey(""))
        assertSame(ShutterEffect.Default, ShutterEffect.fromKey("ripple"))
    }
}
