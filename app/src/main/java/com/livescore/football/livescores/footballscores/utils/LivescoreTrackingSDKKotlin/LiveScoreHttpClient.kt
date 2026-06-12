package com.livescore.football.livescores.footballscores.utils.LivescoreTrackingSDKKotlin

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * Helper xây dựng OkHttpClient kèm Header Interceptor tự động thêm các trường nhận diện phiên bản và giải mã hóa.
 */
object LiveScoreHttpClient {

    /**
     * Tạo OkHttpClient với các Headers cần thiết cho API Live Score
     *
     * @param packageName Tên Package của ứng dụng (x-param1)
     * @param versionCode Phiên bản của ứng dụng (x-param2)
     */
    fun createClient(packageName: String, versionCode: String): OkHttpClient {
        val headerInterceptor = Interceptor { chain ->
            val originalRequest = chain.request()
            val requestWithHeaders = originalRequest.newBuilder()
                .header("x-param1", packageName)
                .header("x-param2", versionCode)
                .build()
            chain.proceed(requestWithHeaders)
        }

        return OkHttpClient.Builder()
            .addInterceptor(headerInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }
}
