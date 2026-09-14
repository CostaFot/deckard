package com.costafotiadis.deckard.shutter

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.AbstractComposeView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.costafotiadis.deckard.R
import com.costafotiadis.design.theme.AppTheme
import kotlinx.coroutines.flow.StateFlow

/**
 * The full-screen overlay window the effect is drawn into.
 *
 * It exists only for the length of a run: the service adds it the moment the screenshot is taken —
 * never before, or the effect is in the picture the model is asked to read the post out of — and
 * takes it down when [onFinished] says the release has finished playing.
 *
 * Untouchable and unreadable by design. The window carries `FLAG_NOT_TOUCHABLE` so it cannot take a
 * gesture from the app underneath, and the view is hidden from accessibility so it can never be the
 * thing Deckard ends up reading.
 */
@SuppressLint("ViewConstructor")
class DeckardShutterView(
    context: Context,
    val effect: ShutterEffect,
    private val running: StateFlow<Boolean>,
    private val onFinished: () -> Unit,
) : AbstractComposeView(context) {

    init {
        id = R.id.deckardShutterView
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
    }

    @Composable
    override fun Content() {
        val isRunning by running.collectAsStateWithLifecycle()
        AppTheme {
            ShutterSurface(
                effect = effect,
                running = isRunning,
                modifier = Modifier.fillMaxSize(),
                onFinished = onFinished,
            )
        }
    }
}
