package com.livescore.football.livescores.footballscores.utils.LivescoreTrackingSDKKotlin

import android.content.Context
import android.provider.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object AdTrackingHelper {
    fun logAdRequest(
        apiService: LiveScoreApiService,
        context: Context,
        adType: String,
        adUnitId: String,
        screenName: String
    ) {
        val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown_device"
        val sessionId = ScreenTracker.getSessionId()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                apiService.logAdEvent(
                    AdEventRequest(
                        device_id = deviceId,
                        session_id = sessionId,
                        event_type = "request",
                        ad_type = adType,
                        ad_unit_id = adUnitId,
                        screen_name = screenName
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun logAdLoadSuccess(
        apiService: LiveScoreApiService,
        context: Context,
        adType: String,
        adUnitId: String,
        screenName: String
    ) {
        val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown_device"
        val sessionId = ScreenTracker.getSessionId()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                apiService.logAdEvent(
                    AdEventRequest(
                        device_id = deviceId,
                        session_id = sessionId,
                        event_type = "load_success",
                        ad_type = adType,
                        ad_unit_id = adUnitId,
                        screen_name = screenName
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun logAdLoadFailed(
        apiService: LiveScoreApiService,
        context: Context,
        adType: String,
        adUnitId: String,
        screenName: String,
        errorCode: Int?
    ) {
        val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown_device"
        val sessionId = ScreenTracker.getSessionId()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                apiService.logAdEvent(
                    AdEventRequest(
                        device_id = deviceId,
                        session_id = sessionId,
                        event_type = "load_failed",
                        ad_type = adType,
                        ad_unit_id = adUnitId,
                        screen_name = screenName,
                        error_code = errorCode
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun logAdShow(
        apiService: LiveScoreApiService,
        context: Context,
        adType: String,
        adUnitId: String,
        screenName: String,
        ecpm: Double? = null
    ) {
        val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown_device"
        val sessionId = ScreenTracker.getSessionId()
        val impressionIndex = AdSessionTracker.incrementAndGet(adType)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                apiService.logAdEvent(
                    AdEventRequest(
                        device_id = deviceId,
                        session_id = sessionId,
                        event_type = "show",
                        ad_type = adType,
                        ad_unit_id = adUnitId,
                        screen_name = screenName,
                        impression_index = impressionIndex,
                        ecpm = ecpm
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
