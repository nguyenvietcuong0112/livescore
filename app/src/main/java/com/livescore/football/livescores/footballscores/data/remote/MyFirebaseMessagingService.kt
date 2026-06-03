package com.livescore.football.livescores.footballscores.data.remote

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MyFirebaseMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var deviceRegistrationManager: DeviceRegistrationManager

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCMService", "Refreshed token: $token")
        
        // Gửi token mới lên server thông qua DeviceRegistrationManager
        CoroutineScope(Dispatchers.IO).launch {
            deviceRegistrationManager.registerDevice(token)
        }
    }
}
