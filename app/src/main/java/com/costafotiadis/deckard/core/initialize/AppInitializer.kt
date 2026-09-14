package com.costafotiadis.deckard.core.initialize

import com.costafotiadis.common.FlagProvider
import com.costafotiadis.logging.logDebug
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class AppInitializer @Inject constructor(
    private val featureFlagProvider: FlagProvider
) {

    private val isInitialized = AtomicBoolean(false)

    fun startup() {
        check(!isInitialized.get()) { "Attempted to initialize app more than once" }
        initLogger()
        isInitialized.set(true)
    }

    private fun initLogger() {
        if (featureFlagProvider.isDebugEnabled) {
            Timber.plant(Timber.DebugTree())
            logDebug { "Logger initialised" }
        }
    }

}
