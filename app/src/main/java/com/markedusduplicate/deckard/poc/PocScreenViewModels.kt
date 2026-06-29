package com.markedusduplicate.deckard.poc

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.markedusduplicate.common.coroutine.DispatcherProvider
import com.markedusduplicate.logging.logDebug
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

/**
 * POC ViewModels for the Nav3 overlay — one per screen. They exist to prove two things about hosting
 * ViewModels in the Service overlay:
 *  1. **DI works** — each takes a constructor-injected [DispatcherProvider] from the singleton graph.
 *  2. **Scoping works** — each is scoped to its `NavEntry` by `rememberViewModelStoreNavEntryDecorator()`,
 *     so it's created on first entry and [onCleared] runs when its entry is popped off the back stack.
 *
 * To make the lifecycle observable, each carries a globally-unique [instanceId] (so a *new* instance
 * is obvious in logcat / on screen) and a [clicks] counter (so state-retention across recomposition
 * vs. reset-on-recreate is visible). Construction is done by [com.markedusduplicate.deckard.mascot.OverlayViewModelFactory],
 * passed explicitly to `viewModel(factory = …)` — not `hiltViewModel()`, which can't work in a Service.
 */
abstract class PocScreenViewModel(private val label: String) : ViewModel() {

    val instanceId: Int = nextInstanceId()

    var clicks by mutableIntStateOf(0)
        private set

    init {
        logDebug { "$label ViewModel CREATED — instance #$instanceId" }
    }

    fun bump() {
        clicks++
    }

    override fun onCleared() {
        logDebug { "$label ViewModel CLEARED — instance #$instanceId (had $clicks clicks)" }
    }

    private companion object {
        val instanceCounter = AtomicInteger(0)
        fun nextInstanceId(): Int = instanceCounter.incrementAndGet()
    }
}

class PocScreenAViewModel @Inject constructor(
    dispatcherProvider: DispatcherProvider,
) : PocScreenViewModel("ScreenA(di=${dispatcherProvider::class.simpleName})")

class PocScreenBViewModel @Inject constructor(
    dispatcherProvider: DispatcherProvider,
) : PocScreenViewModel("ScreenB(di=${dispatcherProvider::class.simpleName})")

class PocScreenCViewModel @Inject constructor(
    dispatcherProvider: DispatcherProvider,
) : PocScreenViewModel("ScreenC(di=${dispatcherProvider::class.simpleName})")
