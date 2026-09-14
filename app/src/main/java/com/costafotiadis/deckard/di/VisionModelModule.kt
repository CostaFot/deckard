package com.costafotiadis.deckard.di

import com.costafotiadis.deckard.llm.NanoOrLocalVisionModel
import com.costafotiadis.deckard.llm.VisionModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Which model reads the screen. The OCR readers ask for a [VisionModel] and nothing more, so this
 * binding is the one place that decides: [NanoOrLocalVisionModel], the phone's Gemini Nano where
 * AICore has it and the LiteRT-LM engine otherwise. Nothing in `slop/` knows there are two.
 */
@Module
@InstallIn(SingletonComponent::class)
interface VisionModelModule {

    @Binds
    fun bindsVisionModel(impl: NanoOrLocalVisionModel): VisionModel
}
