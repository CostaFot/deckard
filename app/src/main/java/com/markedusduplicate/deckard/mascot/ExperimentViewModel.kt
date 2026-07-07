package com.markedusduplicate.deckard.mascot

import androidx.lifecycle.ViewModel
import com.markedusduplicate.common.coroutine.DispatcherProvider
import javax.inject.Inject

/**
 * Experiment: a constructor-injected [ViewModel] for the Service-hosted overlay. Deliberately **not**
 * `@HiltViewModel` — Hilt's `@HiltViewModel` machinery lives in the Activity-scoped
 * `ActivityRetainedComponent` → `ViewModelComponent`, which a [android.app.Service] can't reach. Instead
 * this is a plain `@Inject`-constructor class resolvable from the singleton graph, handed to the
 * overlay via [OverlayViewModelFactory] and fetched with `viewModel()` (not `hiltViewModel()`).
 */
class ExperimentViewModel @Inject constructor(
    dispatcherProvider: DispatcherProvider,
) : ViewModel() {
    val greeting = "viewModel() works — Hilt injected a ${dispatcherProvider::class.simpleName} from the singleton graph"
}
