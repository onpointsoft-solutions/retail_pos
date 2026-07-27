package com.onpointinfo.transrouter

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Required to be a default SMS app.
 */
class MmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // MMS processing logic if needed
    }
}