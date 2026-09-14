package com.costafotiadis.deckard.ui.activity

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.costafotiadis.common.FlagProvider
import com.costafotiadis.deckard.llm.NanoSpike
import com.costafotiadis.deckard.shutter.ShutterEffectStore
import com.costafotiadis.deckard.ui.DeckardApp
import com.costafotiadis.design.theme.AppTheme
import com.costafotiadis.logging.logDebug
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The only Activity in the app, and it does one thing: host the screens that set Deckard up and
 * configure him. Everything after that happens in the overlay, which has no Activity at all.
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var shutterEffects: ShutterEffectStore

    @Inject
    lateinit var nanoSpike: NanoSpike

    @Inject
    lateinit var flagProvider: FlagProvider

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (flagProvider.isDebugEnabled) {
            nanoSpike.register(this, setOf("activity")) { where, path, _ ->
                lifecycleScope.launch { nanoSpike.run(where, path) }
            }
        }

        setContent {
            val shutter by shutterEffects.selected.collectAsStateWithLifecycle()
            AppTheme {
                DeckardApp(
                    shutter = shutter,
                    onPickShutter = shutterEffects::select,
                )
            }
        }
    }

    override fun onDestroy() {
        logDebug { "onDestroy" }
        super.onDestroy()
    }
}
