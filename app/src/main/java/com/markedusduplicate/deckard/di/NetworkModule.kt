package com.markedusduplicate.deckard.di

import android.content.Context
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.markedusduplicate.common.FlagProvider
import com.markedusduplicate.deckard.BuildConfig
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

/**
 * Pangram's auth header. Shared so the logging interceptor redacts the header the auth interceptor
 * actually sends: two separate literals would drift and the key would go back into logcat.
 */
private const val API_KEY_HEADER = "x-api-key"

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
            redactHeader(API_KEY_HEADER)
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
                        .addHeader(API_KEY_HEADER, BuildConfig.AI_DETECTOR_API_KEY)
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
    fun providesPangramService(
        retrofit: Retrofit,
    ): PangramService = retrofit.create(PangramService::class.java)
}
