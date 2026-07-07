package com.markedusduplicate.deckard.mascot

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import javax.inject.Inject
import javax.inject.Provider

/**
 * A [ViewModelProvider.Factory] backed by Hilt's **singleton** graph instead of the Activity-scoped
 * ViewModel component. `@HiltViewModel` + `hiltViewModel()` can't work in a [android.app.Service]
 * (its bindings live under `ActivityRetainedComponent`, which only exists off an `Activity`), so we
 * reproduce the pre-`@HiltViewModel` pattern: every overlay ViewModel is `@Binds @IntoMap`-bound into
 * a `Map<Class, Provider<ViewModel>>` (see `di/OverlayViewModelModule`) and looked up here by class.
 *
 * The service exposes this as its [androidx.lifecycle.HasDefaultViewModelProviderFactory], so a plain
 * `viewModel()` call inside the overlay composition resolves through it.
 */
class OverlayViewModelFactory @Inject constructor(
    private val providers: Map<Class<*>, @JvmSuppressWildcards Provider<ViewModel>>,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val provider = providers[modelClass]
            ?: error("No ViewModel provider bound for $modelClass — add an @IntoMap binding")
        @Suppress("UNCHECKED_CAST")
        return provider.get() as T
    }
}
