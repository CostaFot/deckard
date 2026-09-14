package com.costafotiadis.deckard

import android.app.Application
import android.content.Context
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.gif.AnimatedImageDecoder
import coil3.request.crossfade
import com.costafotiadis.common.coroutine.DispatcherProvider
import com.costafotiadis.deckard.core.initialize.AppInitializer
import com.costafotiadis.logging.logDebug
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject

@HiltAndroidApp
class DeckardApplication : Application(), SingletonImageLoader.Factory {

    @Inject
    lateinit var appInitializer: AppInitializer

    override fun onCreate() {
        super.onCreate()
        appInitializer.startup()
        logDebug { "onCreate application" }
    }

    override fun newImageLoader(context: Context): ImageLoader {
        return ImageLoader.Builder(context)
            .components { add(AnimatedImageDecoder.Factory()) }
            .crossfade(true)
            .build()
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ApplicationEntryPoint {
    fun appInitializer(): AppInitializer

    fun dispatcherProvider(): DispatcherProvider
}
