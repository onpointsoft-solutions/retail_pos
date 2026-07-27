package com.onpointinfo.transrouter

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock

class ForwardRetryReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        UdpTransactionPublisher.retryPending(context.applicationContext)
        schedule(context.applicationContext)
    }

    companion object {
        private const val RETRY_INTERVAL_MS = 5 * 60 * 1000L

        fun schedule(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, ForwardRetryReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                45876,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + RETRY_INTERVAL_MS,
                pendingIntent
            )
        }
    }
}
