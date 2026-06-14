package com.markedusduplicate.deckard.di

import com.markedusduplicate.deckard.accessibility.extract.LinkedInContentExtractor
import com.markedusduplicate.deckard.accessibility.extract.ScreenContentExtractor
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

/**
 * Contributes the per-app [ScreenContentExtractor]s into the set
 * [com.markedusduplicate.deckard.accessibility.extract.ScreenContentExtractors] dispatches over. Add
 * a new app's extractor here with one `@Binds @IntoSet`. The generic fallback is injected directly,
 * not via the set.
 */
@Module
@InstallIn(SingletonComponent::class)
interface ScreenTextExtractorsModule {

    @Binds
    @IntoSet
    fun bindsLinkedInContentExtractor(impl: LinkedInContentExtractor): ScreenContentExtractor
}
