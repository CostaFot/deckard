package com.markedusduplicate.deckard.di

import com.markedusduplicate.deckard.slop.AccessibilityScreenTextReader
import com.markedusduplicate.deckard.slop.OcrScreenTextReader
import com.markedusduplicate.deckard.slop.ScreenTextReader
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface ScreenTextModule {

    @Binds
    @OcrScreenText
    fun bindsOcrScreenTextReader(impl: OcrScreenTextReader): ScreenTextReader

    @Binds
    @AccessibilityScreenText
    fun bindsAccessibilityScreenTextReader(impl: AccessibilityScreenTextReader): ScreenTextReader
}
