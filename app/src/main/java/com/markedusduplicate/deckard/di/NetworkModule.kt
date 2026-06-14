package com.markedusduplicate.deckard.di

import android.content.Context
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.markedusduplicate.common.FlagProvider
import com.markedusduplicate.deckard.BuildConfig
import com.markedusduplicate.deckard.net.JsonPlaceHolderService
import com.markedusduplicate.deckard.net.PangramService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.Cache
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun providesCache(@ApplicationContext context: Context): Cache {
        return Cache(context.cacheDir, 10 * 1024 * 1024)
    }

    @Provides
    @Singleton
    fun providesHttpLoggingInterceptor(
        flagProvider: FlagProvider
    ): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = when {
                flagProvider.isDebugEnabled -> HttpLoggingInterceptor.Level.BODY
                else -> HttpLoggingInterceptor.Level.NONE
            }
        }
    }

    @Provides
    @Singleton
    fun providesOkHttpClient(
        cache: Cache,
        httpLoggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient {
        return OkHttpClient().newBuilder()
            .apply {
                addInterceptor { chain ->
                    val request = chain.request().newBuilder()
                        .addHeader("x-api-key", BuildConfig.AI_DETECTOR_API_KEY)
                        .build()
                    chain.proceed(request)
                }
                addInterceptor(httpLoggingInterceptor)
                cache(cache)
            }.build()
    }

    @Provides
    @Singleton
    fun providesJson(): Json = Json {
        ignoreUnknownKeys = true
    }

    @Provides
    fun providesRetrofit(
        okHttpClient: OkHttpClient,
        json: Json
    ): Retrofit {
        return Retrofit.Builder()
            .apply {
                baseUrl("https://text.external-api.pangram.com/")
                client(okHttpClient)
                addConverterFactory(
                    json.asConverterFactory("application/json".toMediaType())
                )
            }.build()
    }

    @Provides
    fun providesJsonPlaceHolderService(
        retrofit: Retrofit
    ): JsonPlaceHolderService = retrofit.create(JsonPlaceHolderService::class.java)

    @Provides
    fun providesPangramService(
        retrofit: Retrofit,
    ): PangramService = retrofit.create(PangramService::class.java)
}
