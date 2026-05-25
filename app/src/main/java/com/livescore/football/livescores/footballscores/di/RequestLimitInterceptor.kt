package com.livescore.football.livescores.footballscores.di

import com.livescore.football.livescores.footballscores.data.local.RequestLimitManager
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RequestLimitInterceptor @Inject constructor(
    private val limitManager: RequestLimitManager
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val url = request.url.toString()

        // Apply to the specific API Sports endpoints
        if (url.contains("fixtures") || url.contains("statistics") || url.contains("events") || url.contains("lineups")) {
            if (limitManager.isLimitExceeded()) {
                limitManager.triggerLimitExceeded()

                val jsonResponse = "{\"message\": \"Bạn đã hết 30 lượt truy cập miễn phí hôm nay! Vui lòng nâng cấp Premium để tiếp tục.\"}"
                val mediaType = "application/json".toMediaTypeOrNull()
                val responseBody = jsonResponse.toResponseBody(mediaType)

                return Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(429)
                    .message("Too Many Requests")
                    .body(responseBody)
                    .build()
            }

            val response = chain.proceed(request)
            if (response.isSuccessful) {
                limitManager.incrementCount()
            }
            return response
        }

        return chain.proceed(request)
    }
}
