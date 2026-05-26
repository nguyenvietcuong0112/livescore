package com.livescore.football.livescores.footballscores.data.remote

import android.content.Context
import android.util.Log
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@Singleton
class RemoteConfigManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // Lazily initialize Firebase Remote Config instance to prevent crashes if Firebase is not yet initialized
    private val remoteConfig: FirebaseRemoteConfig by lazy {
        FirebaseRemoteConfig.getInstance()
    }

    companion object {
        private const val KEY_API_KEY = "apisports_key"
        private const val DEFAULT_API_KEY = "eeb82da4384bf7352f346c9371fe3dad"
    }

    init {
        try {
            // Set up Firebase Remote Config settings
            val configSettings = FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(0) // 1 hour fetch interval for optimal caching
                .build()
            remoteConfig.setConfigSettingsAsync(configSettings)

            // Define default local fallback values in case of network issues
            val defaults = mapOf(KEY_API_KEY to DEFAULT_API_KEY)
            remoteConfig.setDefaultsAsync(defaults)
        } catch (e: Exception) {
            Log.e("RemoteConfigManager", "Failed to initialize Firebase Remote Config defaults: ${e.message}")
        }
    }

    /**
     * Retrieve the active API Key from Firebase Remote Config.
     * Falls back to the hardcoded local default key on any retrieval error.
     */
    fun getApiKey(): String {
        return try {
            val key = remoteConfig.getString(KEY_API_KEY)
            if (key.isNullOrEmpty()) DEFAULT_API_KEY else key
        } catch (e: Exception) {
            Log.e("RemoteConfigManager", "Error reading key from Remote Config, using fallback: ${e.message}")
            DEFAULT_API_KEY
        }
    }

    /**
     * Fetch the latest config variables from Firebase servers and apply them
     */
    suspend fun fetchAndActivate(): Boolean = suspendCoroutine { continuation ->
        try {
            remoteConfig.fetchAndActivate()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val updated = task.result
                        Log.d("RemoteConfigManager", "Firebase Config fetched and activated successfully. Updated: $updated")
                        continuation.resume(true)
                    } else {
                        Log.w("RemoteConfigManager", "Firebase Config fetch failed, utilizing cached/default keys")
                        continuation.resume(false)
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("RemoteConfigManager", "Firebase Remote Config fetch failed: ${e.message}")
                    continuation.resume(false)
                }
        } catch (e: Exception) {
            Log.e("RemoteConfigManager", "Firebase Remote Config SDK is not initialized, using local fallback key: ${e.message}")
            continuation.resume(false)
        }
    }
}
