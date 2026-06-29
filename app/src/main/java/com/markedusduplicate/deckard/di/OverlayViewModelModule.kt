package com.markedusduplicate.deckard.di

import androidx.lifecycle.ViewModel
import com.markedusduplicate.deckard.mascot.ExperimentViewModel
import com.markedusduplicate.deckard.poc.PocScreenAViewModel
import com.markedusduplicate.deckard.poc.PocScreenBViewModel
import com.markedusduplicate.deckard.poc.PocScreenCViewModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

/**
 * Binds overlay ViewModels into a `Map<Class, Provider<ViewModel>>` in the **singleton** graph, which
 * [com.markedusduplicate.deckard.mascot.OverlayViewModelFactory] consumes. This is how the
 * Service-hosted overlay gets DI'd ViewModels without Hilt's Activity-scoped `@HiltViewModel` support.
 * Add a ViewModel = one `@Binds @IntoMap @ClassKey(...)` line here.
 */
@Module
@InstallIn(SingletonComponent::class)
interface OverlayViewModelModule {

    @Binds
    @IntoMap
    @ClassKey(ExperimentViewModel::class)
    fun bindExperimentViewModel(viewModel: ExperimentViewModel): ViewModel

    @Binds
    @IntoMap
    @ClassKey(PocScreenAViewModel::class)
    fun bindPocScreenAViewModel(viewModel: PocScreenAViewModel): ViewModel

    @Binds
    @IntoMap
    @ClassKey(PocScreenBViewModel::class)
    fun bindPocScreenBViewModel(viewModel: PocScreenBViewModel): ViewModel

    @Binds
    @IntoMap
    @ClassKey(PocScreenCViewModel::class)
    fun bindPocScreenCViewModel(viewModel: PocScreenCViewModel): ViewModel
}
