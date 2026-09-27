package com.costafotiadis.deckard.llm.nano

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ForegroundStageTest {

    @Test
    fun `runs the block once the activity has arrived`() = runTest {
        lateinit var stage: ForegroundStage
        stage = ForegroundStage { token -> stage.arrived(token) }

        val result = stage.inFront { "read" }

        assertEquals("read", result)
    }

    @Test
    fun `does not run the block until the activity arrives`() = runTest {
        var stepped = -1L
        val stage = ForegroundStage { token -> stepped = token }
        var ran = false

        val read = async { stage.inFront { ran = true } }
        runCurrent()

        assertFalse(ran)
        stage.arrived(stepped)
        read.await()
        assertTrue(ran)
    }

    @Test
    fun `gives up when nothing comes forward in time`() = runTest {
        val stage = ForegroundStage {}

        val read = async { stage.inFront { "read" } }
        advanceTimeBy(ForegroundStage.STEP_FORWARD_MILLIS + 1)

        assertNull(read.await())
    }

    @Test
    fun `gives up when stepping forward throws`() = runTest {
        val stage = ForegroundStage { throw IllegalStateException("refused") }

        assertNull(stage.inFront { "read" })
    }

    @Test
    fun `an activity arriving for a stale turn is sent away`() = runTest {
        val stage = ForegroundStage {}

        assertNull(stage.arrived(42L))
    }

    @Test
    fun `an activity arriving after its turn is over is sent away`() = runTest {
        var stepped = -1L
        val stage = ForegroundStage { token -> stepped = token }

        val read = async { stage.inFront { "read" } }
        runCurrent()
        read.cancel()
        runCurrent()

        assertNull(stage.arrived(stepped))
    }

    @Test
    fun `the activity is let go when the block is done`() = runTest {
        var stepped = -1L
        val stage = ForegroundStage { token -> stepped = token }

        val read = async { stage.inFront { "read" } }
        runCurrent()
        val over = stage.arrived(stepped)!!
        assertFalse(over.isCompleted)

        read.await()
        assertTrue(over.isCompleted)
    }

    @Test
    fun `a second turn is not confused with the first`() = runTest {
        val tokens = mutableListOf<Long>()
        val stage = ForegroundStage { token -> tokens += token }

        val first = async { stage.inFront { "first" } }
        runCurrent()
        first.cancel()
        runCurrent()
        val second = async { stage.inFront { "second" } }
        runCurrent()

        assertNull(stage.arrived(tokens[0]))
        val over = stage.arrived(tokens[1])
        assertEquals("second", second.await())
        assertTrue(over!!.isCompleted)
    }
}
