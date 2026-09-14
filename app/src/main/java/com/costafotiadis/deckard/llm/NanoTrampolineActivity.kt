package com.costafotiadis.deckard.llm

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.costafotiadis.logging.logDebug
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * COS-259 spike, step 3: a see-through Activity the overlay service starts after the screenshot, so
 * Deckard is the top app while Gemini Nano reads it. Runs one read from `onResume` and finishes.
 */
@AndroidEntryPoint
class NanoTrampolineActivity : ComponentActivity() {

    @Inject
    lateinit var spike: NanoSpike

    private var started = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val untouchable = intent.getBooleanExtra(NanoSpike.EXTRA_UNTOUCHABLE, false)
        if (untouchable) window.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
        logDebug { "nano[trampoline]: created, untouchable=$untouchable" }
    }

    override fun onResume() {
        super.onResume()
        if (started) return
        started = true
        logDebug { "nano[trampoline]: resumed, reading" }
        lifecycleScope.launch {
            spike.run("trampoline", intent.getStringExtra(NanoSpike.EXTRA_FILE))
            logDebug { "nano[trampoline]: finishing" }
            finish()
        }
    }
}
