package com.costafotiadis.deckard.di

import com.costafotiadis.deckard.slop.AccessibilityScreenTextReader
import com.costafotiadis.deckard.slop.OcrContentScreenTextReader
import com.costafotiadis.deckard.slop.OcrScreenTextReader
import com.costafotiadis.deckard.slop.ScreenTextReader
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
    @OcrContentScreenText
    fun bindsOcrContentScreenTextReader(impl: OcrContentScreenTextReader): ScreenTextReader

    @Binds
    @AccessibilityScreenText
    fun bindsAccessibilityScreenTextReader(impl: AccessibilityScreenTextReader): ScreenTextReader
}
