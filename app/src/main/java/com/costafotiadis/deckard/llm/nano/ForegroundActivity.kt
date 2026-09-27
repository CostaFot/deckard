package com.costafotiadis.deckard.llm.nano

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * A see-through Activity with nothing on it, started by [ForegroundStage] so that Deckard is the app
 * in front while Gemini Nano reads the screen. It reports in when it is on top and finishes when its
 * turn is over; one that comes up for a turn that has already ended finishes at once.
 *
 * Its theme (`Theme.Deckard.Foreground`) is translucent with no animation and no preview window, so
 * the app underneath stays visible and nothing flashes on the way in or out.
 */
@AndroidEntryPoint
class ForegroundActivity : ComponentActivity() {

    @Inject
    lateinit var stage: ForegroundStage

    private var waiting = false

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        if (waiting) return
        val over = stage.arrived(intent.getLongExtra(EXTRA_TURN, NO_TURN))
        if (over == null) {
            finish()
            return
        }
        waiting = true
        lifecycleScope.launch {
            over.await()
            finish()
        }
    }

    companion object {
        private const val EXTRA_TURN = "com.costafotiadis.deckard.extra.TURN"
        private const val NO_TURN = -1L

        fun intent(context: Context, token: Long): Intent =
            Intent(context, ForegroundActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
                .putExtra(EXTRA_TURN, token)
    }
}
