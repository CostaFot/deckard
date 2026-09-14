package com.costafotiadis.common.di

import com.costafotiadis.common.coroutine.DefaultApplicationCoroutineScope
import com.costafotiadis.common.coroutine.DefaultDispatcherProvider
import com.costafotiadis.common.coroutine.DispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DispatcherModule {

    @Singleton
    @Provides
    @ApplicationCoroutineScope
    fun providesApplicationScope(): CoroutineScope = DefaultApplicationCoroutineScope

    @Singleton
    @Provides
    fun providesDispatcherProvider(): DispatcherProvider = DefaultDispatcherProvider
}
