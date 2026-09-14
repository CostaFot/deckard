package com.costafotiadis.deckard.di

import com.costafotiadis.deckard.llm.LlmEngine
import com.costafotiadis.deckard.llm.VisionModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Which model reads the screen. The OCR readers ask for a [VisionModel] and nothing more, so this
 * binding is the one place that decides it is the LiteRT-LM [LlmEngine]; a chooser between it and
 * a system-provided model would replace this binding and touch nothing in `slop/`.
 */
@Module
@InstallIn(SingletonComponent::class)
interface VisionModelModule {

    @Binds
    fun bindsVisionModel(impl: LlmEngine): VisionModel
}
