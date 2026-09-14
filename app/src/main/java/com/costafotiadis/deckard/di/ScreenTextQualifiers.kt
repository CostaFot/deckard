package com.costafotiadis.deckard.di

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class OcrScreenText

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class OcrContentScreenText
