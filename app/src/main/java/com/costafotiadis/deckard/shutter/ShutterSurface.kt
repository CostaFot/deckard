package com.costafotiadis.deckard.shutter

import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

/** How long the shutter itself takes, and how long letting go of it takes. */
private const val SNAP_MILLIS = 320L
private const val RELEASE_MILLIS = 260L

/** One turn of anything that has to keep moving while the model reads. */
private const val LOOP_MILLIS = 1600L

/**
 * The longest a run will hold before releasing itself. A vision inference takes seconds, not half a
 * minute, and a full-screen overlay that never leaves is worse than an effect that never plays.
 */
private const val MAX_WORKING_MILLIS = 30_000L

/** What the picker's miniature spends reading before it lets go and starts over. */
private const val PREVIEW_WORKING_MILLIS = 2_600L
private const val PREVIEW_PAUSE_MILLIS = 600L

/**
 * Plays [effect] over whatever this is drawn on top of: the shutter closes, the mark holds for as
 * long as [running] stays true, and then it lets go and calls [onFinished] — which is the moment the
 * window hosting this can be taken down, not a moment earlier.
 *
 * [scale] is 1 at full size; the picker's miniature passes a fraction so the same painter draws
 * proportionately into a box the size of a stamp.
 *
 * The frame is held in a state that is read **only inside [drawBehind]**, so sixty of them a second
 * invalidate the draw and recompose nothing.
 */
@Composable
fun ShutterSurface(
    effect: ShutterEffect,
    running: Boolean,
    modifier: Modifier = Modifier,
    scale: Float = 1f,
    onFinished: () -> Unit = {},
) {
    val frame = remember { mutableStateOf<ShutterFrame?>(null) }
    val finish by rememberUpdatedState(onFinished)

    // Read through a State, not the captured parameter: snapshotFlow only sees what it reads from a
    // snapshot, and a plain Boolean argument is fixed at the composition that started this effect —
    // so the run would never hear that it was over and would sit there until the safety release.
    val isRunning = rememberUpdatedState(running)

    LaunchedEffect(effect, scale) {
        var played = false
        snapshotFlow { isRunning.value }.collectLatest { running ->
            if (running) {
                played = true
                var elapsed = frame.play(ShutterPhase.Snap, SNAP_MILLIS, scale, 0L)
                elapsed = frame.play(ShutterPhase.Working, MAX_WORKING_MILLIS, scale, elapsed)
                frame.play(ShutterPhase.Release, RELEASE_MILLIS, scale, elapsed)
                frame.value = null
                finish()
            } else if (played) {
                val elapsed = frame.value?.loop?.times(LOOP_MILLIS)?.toLong() ?: 0L
                frame.play(ShutterPhase.Release, RELEASE_MILLIS, scale, elapsed)
                frame.value = null
                finish()
            }
        }
    }

    ShutterCanvas(effect = effect, frame = frame, modifier = modifier)
}

/**
 * The same painter on a loop, for the picker: snap, read a moment, release, again. Nothing about it
 * waits on a model, so choosing between the effects costs neither an inference nor a Pangram call.
 */
@Composable
fun ShutterLoop(
    effect: ShutterEffect,
    modifier: Modifier = Modifier,
    scale: Float = 1f,
) {
    val frame = remember { mutableStateOf<ShutterFrame?>(null) }

    LaunchedEffect(effect, scale) {
        while (true) {
            var elapsed = frame.play(ShutterPhase.Snap, SNAP_MILLIS, scale, 0L)
            elapsed = frame.play(ShutterPhase.Working, PREVIEW_WORKING_MILLIS, scale, elapsed)
            frame.play(ShutterPhase.Release, RELEASE_MILLIS, scale, elapsed)
            frame.value = null
            delay(PREVIEW_PAUSE_MILLIS)
        }
    }

    ShutterCanvas(effect = effect, frame = frame, modifier = modifier)
}

@Composable
private fun ShutterCanvas(
    effect: ShutterEffect,
    frame: MutableState<ShutterFrame?>,
    modifier: Modifier,
) {
    val ink = MaterialTheme.colorScheme.onSurface
    Spacer(
        modifier = modifier.drawBehind {
            val current = frame.value ?: return@drawBehind
            with(effect.painter) { paint(current, ink) }
        },
    )
}

/**
 * Write one phase's worth of frames, a frame at a time, and hand back the total elapsed time so the
 * next phase carries the loop on from where this one left it — otherwise the travelling pieces jump
 * at every phase boundary.
 */
private suspend fun MutableState<ShutterFrame?>.play(
    phase: ShutterPhase,
    durationMillis: Long,
    scale: Float,
    startedAtMillis: Long,
): Long {
    var total = startedAtMillis
    var spent = 0L
    var last = withFrameMillis { it }
    while (spent < durationMillis) {
        withFrameMillis { now ->
            val delta = (now - last).coerceIn(0L, 64L)
            last = now
            spent += delta
            total += delta
            value = ShutterFrame(
                phase = phase,
                progress = (spent.toFloat() / durationMillis).coerceIn(0f, 1f),
                loop = (total % LOOP_MILLIS).toFloat() / LOOP_MILLIS,
                scale = scale,
            )
        }
    }
    return total
}
