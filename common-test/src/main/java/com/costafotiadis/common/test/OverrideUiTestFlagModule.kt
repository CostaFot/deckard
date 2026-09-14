package com.costafotiadis.common.test

import com.costafotiadis.common.di.RunningUiTestFlag
import com.costafotiadis.common.di.UiTestFlagModule
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [UiTestFlagModule::class],
)
object OverrideUiTestFlagModule {
    @Singleton
    @Provides
    @RunningUiTestFlag
    fun providesRunningUiTest(): Boolean = true
}
