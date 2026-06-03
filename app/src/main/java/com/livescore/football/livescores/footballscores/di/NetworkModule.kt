package com.livescore.football.livescores.footballscores.di

import android.content.Context
import com.livescore.football.livescores.footballscores.data.remote.ApiService
import com.livescore.football.livescores.footballscores.data.remote.RemoteConfigManager
import com.livescore.football.livescores.footballscores.data.remote.DecryptionInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val BASE_URL = "https://api.1teps.com/livescore/"

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        @ApplicationContext context: Context,
        loggingInterceptor: HttpLoggingInterceptor,
        requestLimitInterceptor: RequestLimitInterceptor,
        remoteConfigManager: RemoteConfigManager
    ): OkHttpClient {
        val packageName = context.packageName
        val versionCode = try {
            val pInfo = context.packageManager.getPackageInfo(packageName, 0)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                pInfo.longVersionCode.toString()
            } else {
                @Suppress("DEPRECATION")
                pInfo.versionCode.toString()
            }
        } catch (e: Exception) {
            "3" // Fallback to current versionCode
        }

        return OkHttpClient.Builder()
            .addInterceptor(requestLimitInterceptor)
            .addInterceptor { chain ->
                val originalRequest = chain.request()
                val newUrl = originalRequest.url.newBuilder()
                    .addQueryParameter("param1", packageName)
                    .addQueryParameter("param2", versionCode)
                    .build()

                val request = originalRequest.newBuilder()
                    .url(newUrl)
                    .addHeader("x-param1", packageName)
                    .addHeader("x-param2", versionCode)
                    .addHeader("x-apisports-key", remoteConfigManager.getApiKey())
                    .build()
                chain.proceed(request)
            }
            .addInterceptor(loggingInterceptor)
            .addInterceptor(DecryptionInterceptor(packageName, versionCode))
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService {
        return retrofit.create(ApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideGsmAuthInterceptor(): com.livescore.football.livescores.footballscores.data.remote.gsm.GsmAuthInterceptor {
        return com.livescore.football.livescores.footballscores.data.remote.gsm.GsmAuthInterceptor()
    }

    @Provides
    @Singleton
    @GsmNetwork
    fun provideGsmOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor,
        gsmAuthInterceptor: com.livescore.football.livescores.footballscores.data.remote.gsm.GsmAuthInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(gsmAuthInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    @GsmNetwork
    fun provideGsmRetrofit(@GsmNetwork okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(com.livescore.football.livescores.footballscores.data.remote.gsm.GsmConfig.CURRENT_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideGsmApiService(@GsmNetwork retrofit: Retrofit): com.livescore.football.livescores.footballscores.data.remote.gsm.GsmApiService {
        return retrofit.create(com.livescore.football.livescores.footballscores.data.remote.gsm.GsmApiService::class.java)
    }
}

@javax.inject.Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class GsmNetwork

