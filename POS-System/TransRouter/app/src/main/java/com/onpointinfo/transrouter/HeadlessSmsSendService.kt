package com.onpointinfo.transrouter

import android.app.Service
import android.content.Intent
import android.os.IBinder

/**
 * Required to be a default SMS app. Handles quick response from notifications.
 */
class HeadlessSmsSendService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Handle sending response
        stopSelf()
        return START_NOT_STICKY
    }
}