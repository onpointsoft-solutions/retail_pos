package com.onpointinfo.transrouter

import android.content.Context
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

object UdpTransactionPublisher {
    private const val PORT = 45876

    fun publish(context: Context, transaction: MpesaTransaction) {
        val host = context.getSharedPreferences("transrouter", Context.MODE_PRIVATE).getString("pos_host", "")?.trim().orEmpty()
        if (host.isBlank()) return
        Thread {
            try {
                val address = InetAddress.getByName(host)
                if (!address.isSiteLocalAddress) return@Thread
                val payload = JSONObject().apply {
                    put("code", transaction.code); put("customerName", transaction.customerName)
                    put("amount", transaction.amount); put("receivedAt", transaction.receivedAt)
                }.toString().toByteArray(Charsets.UTF_8)
                DatagramSocket().use { socket ->
                    repeat(3) { attempt ->
                        socket.send(DatagramPacket(payload, payload.size, address, PORT))
                        if (attempt < 2) Thread.sleep(150)
                    }
                }
            } catch (_: Exception) { }
        }.start()
    }
}
