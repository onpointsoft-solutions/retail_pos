package com.onpointinfo.transrouter

import android.content.Context
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException
import java.util.concurrent.ConcurrentHashMap

object UdpTransactionPublisher {
    private const val PORT = 45876
    private const val ACK_TIMEOUT_MS = 1_500
    private const val MAX_ATTEMPTS = 6
    private val inFlight = ConcurrentHashMap.newKeySet<String>()

    fun publish(context: Context, transaction: MpesaTransaction) {
        val host = context.getSharedPreferences("transrouter", Context.MODE_PRIVATE).getString("pos_host", "")?.trim().orEmpty()
        if (host.isBlank() || !inFlight.add(transaction.code.uppercase())) return
        Thread {
            try {
                val address = InetAddress.getByName(host)
                if (!address.isSiteLocalAddress) return@Thread
                val payload = JSONObject().apply {
                    put("code", transaction.code); put("customerName", transaction.customerName)
                    put("amount", transaction.amount); put("receivedAt", transaction.receivedAt)
                }.toString().toByteArray(Charsets.UTF_8)
                DatagramSocket().use { socket ->
                    socket.soTimeout = ACK_TIMEOUT_MS
                    repeat(MAX_ATTEMPTS) { attempt ->
                        socket.send(DatagramPacket(payload, payload.size, address, PORT))
                        try {
                            val ackBytes = ByteArray(128)
                            val ackPacket = DatagramPacket(ackBytes, ackBytes.size)
                            socket.receive(ackPacket)
                            val ack = String(
                                ackPacket.data,
                                ackPacket.offset,
                                ackPacket.length,
                                Charsets.UTF_8
                            )
                            if (ack == "ACK:${transaction.code}") {
                                TransactionStore(context).markForwarded(transaction.code)
                                return@Thread
                            }
                        } catch (_: SocketTimeoutException) {
                            if (attempt < MAX_ATTEMPTS - 1) Thread.sleep(250L * (attempt + 1))
                        }
                    }
                }
            } catch (_: Exception) {
            } finally {
                inFlight.remove(transaction.code.uppercase())
            }
        }.start()
    }

    fun retryPending(context: Context) {
        TransactionStore(context).pending().take(20).forEach { publish(context, it) }
    }
}
