package com.costafotiadis.deckard.di

import androidx.lifecycle.ViewModel
import com.costafotiadis.deckard.mascot.ExperimentViewModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

/**
 * Binds overlay ViewModels into a `Map<Class, Provider<ViewModel>>` in the **singleton** graph, which
 * [com.costafotiadis.deckard.mascot.OverlayViewModelFactory] consumes. This is how the
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
}
