package com.costafotiadis.deckard.llm.nano

import android.content.Context
import com.costafotiadis.logging.logDebug
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Puts Deckard in front of the screen for as long as a block runs.
 *
 * AICore serves Gemini Nano only to the top foreground app, and an overlay service is not that: a
 * read from the service is refused as background use inside a hundred milliseconds. So a read steps
 * forward first. [inFront] starts the see-through [ForegroundActivity], waits for it to come up,
 * runs the block while it is on top, and lets it finish once the block is done. The app underneath
 * is paused for the duration — touches never reach it, with or without an untouchable window — and
 * that is the ten seconds the shutter effect is covering.
 *
 * Every turn carries a token. A second summon cancels the first mid-read, and the first's Activity
 * can still be arriving after the second has started its own; an Activity that arrives for a turn
 * that is over, or was never the current one, finishes on the spot. Get this wrong and a see-through
 * Activity is left over the user's app with nothing to do.
 */
@Singleton
class ForegroundStage internal constructor(
    private val stepForward: (token: Long) -> Unit,
) {

    @Inject
    constructor(@ApplicationContext context: Context) : this(
        stepForward = { token -> context.startActivity(ForegroundActivity.intent(context, token)) },
    )

    private var nextToken = 0L
    private var current: Turn? = null

    /** One stay in front: [arrived] completes when the Activity is on top, [over] when it may go. */
    private class Turn(val token: Long) {
        val arrived = CompletableDeferred<Unit>()
        val over = CompletableDeferred<Unit>()
    }

    /**
     * Runs [block] with Deckard in front, or returns null if he could not get there inside
     * [STEP_FORWARD_MILLIS]: a launch the system refused, or one that never came up. The system
     * refuses silently — `startActivity` returns and nothing appears — so the wait is the check.
     */
    suspend fun <T> inFront(block: suspend () -> T): T? {
        val turn = Turn(++nextToken)
        current = turn
        try {
            val stepped = runCatching { stepForward(turn.token) }
                .onFailure { logDebug { "foreground: could not step forward: $it" } }
                .isSuccess
            if (!stepped) return null
            val arrived = withTimeoutOrNull(STEP_FORWARD_MILLIS) { turn.arrived.await() } != null
            if (!arrived) {
                logDebug { "foreground: nothing came forward in ${STEP_FORWARD_MILLIS}ms" }
                return null
            }
            return block()
        } finally {
            turn.over.complete(Unit)
            if (current === turn) current = null
        }
    }

    /**
     * The Activity for [token] is on top. Returns what it waits on before finishing, or null when
     * its turn is over or was never the current one, in which case it finishes now.
     */
    internal fun arrived(token: Long): Deferred<Unit>? {
        val turn = current?.takeIf { it.token == token && !it.over.isCompleted } ?: return null
        turn.arrived.complete(Unit)
        return turn.over
    }

    internal companion object {
        /** Long enough for an Activity to come up on a busy phone, short enough to feel like an answer. */
        const val STEP_FORWARD_MILLIS = 5_000L
    }
}
