package com.livescore.football.livescores.footballscores.data.remote

import android.content.Context
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.livescore.football.livescores.footballscores.data.remote.model.RegisterDeviceRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceRegistrationManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val apiService: ApiService
) {
    private val prefs = context.getSharedPreferences("device_registration_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val TAG = "DeviceRegistration"
        private const val KEY_LAST_TOKEN = "last_push_token"
        private const val KEY_IS_REGISTERED_WITHOUT_TOKEN = "is_registered_without_token"
    }

    private fun getDeviceId(): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown_device"
    }

    private fun getAppVersionName(): String {
        return try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
    }

    private fun getModelName(): String {
        val manufacturer = Build.MANUFACTURER
        val model = Build.MODEL
        return if (model.startsWith(manufacturer)) {
            model
        } else {
            "$manufacturer $model"
        }
    }

    private fun getLanguageCode(): String {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                context.resources.configuration.locales[0].language
            } else {
                @Suppress("DEPRECATION")
                context.resources.configuration.locale.language
            }
        } catch (e: Exception) {
            Locale.getDefault().language ?: "en"
        }
    }

    suspend fun registerDevice(pushToken: String? = null) = withContext(Dispatchers.IO) {
        val deviceId = getDeviceId()
        val osVersion = Build.VERSION.RELEASE ?: "unknown"
        val appVersion = getAppVersionName()
        val modelName = getModelName()
        val langCode = getLanguageCode()

        val token = pushToken?.takeIf { it.trim().isNotEmpty() }
        val lastToken = prefs.getString(KEY_LAST_TOKEN, null)
        val isRegisteredWithoutToken = prefs.getBoolean(KEY_IS_REGISTERED_WITHOUT_TOKEN, false)

        if (token == null && isRegisteredWithoutToken) {
            Log.d(TAG, "Already registered basic device info without token. Skipping.")
            return@withContext
        }
        if (token != null && token == lastToken) {
            Log.d(TAG, "Already registered device with this push token. Skipping.")
            return@withContext
        }

        val request = RegisterDeviceRequest(
            device_id = deviceId,
            push_token = token,
            os_type = "android",
            os_version = osVersion,
            app_version = appVersion,
            model_name = modelName,
            language_code = langCode
        )

        try {
            Log.d(TAG, "Registering device. Request: $request")
            val response = apiService.registerDevice(request)
            Log.d(TAG, "Device registration response: code=${response.code}, message=${response.message}")
            if (response.code == 200) {
                if (token != null) {
                    prefs.edit()
                        .putString(KEY_LAST_TOKEN, token)
                        .putBoolean(KEY_IS_REGISTERED_WITHOUT_TOKEN, true)
                        .apply()
                } else {
                    prefs.edit()
                        .putBoolean(KEY_IS_REGISTERED_WITHOUT_TOKEN, true)
                        .apply()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register device: ${e.message}", e)
        }
    }
}
