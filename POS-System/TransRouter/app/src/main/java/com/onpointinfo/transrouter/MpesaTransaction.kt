package com.onpointinfo.transrouter

data class MpesaTransaction(
    val code: String,
    val customerName: String,
    val amount: String,
    val receivedAt: Long,
    val rawMessage: String
)
