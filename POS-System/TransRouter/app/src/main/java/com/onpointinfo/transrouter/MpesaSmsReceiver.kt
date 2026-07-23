package com.onpointinfo.transrouter

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony

class MpesaSmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        
        val pendingResult = goAsync()
        Thread {
            try {
                val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
                val fullMessage = messages.joinToString(separator = "") { it.messageBody.orEmpty() }
                val sender = messages.firstOrNull()?.displayOriginatingAddress.orEmpty()

                // Parse first - if it matches the M-Pesa format, we process it regardless of sender (helps with testing)
                val transaction = MpesaParser.parse(fullMessage)
                
                if (transaction != null) {
                    // It's a valid M-Pesa message based on content
                    val store = TransactionStore(context.applicationContext)
                    if (store.add(transaction)) {
                        UdpTransactionPublisher.publish(context.applicationContext, transaction)
                    }
                } else {
                    // Optional: log or handle non-matching messages
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingResult.finish()
            }
        }.start()
    }
}
