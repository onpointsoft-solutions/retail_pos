package com.onpointinfo.transrouter

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

/**
 * Optional fallback for devices where the M-Pesa confirmation is exposed only
 * as a notification. Android requires the phone owner to enable this service
 * in Accessibility settings; apps are not allowed to enable it themselves.
 */
class MpesaAccessibilityService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) return
        val text = event.text?.joinToString(" ").orEmpty()
        val transaction = MpesaParser.parse(text) ?: return
        val context = applicationContext
        if (TransactionStore(context).add(transaction)) {
            UdpTransactionPublisher.publish(context, transaction)
            ForwardRetryReceiver.schedule(context)
        }
    }

    override fun onInterrupt() = Unit
}
