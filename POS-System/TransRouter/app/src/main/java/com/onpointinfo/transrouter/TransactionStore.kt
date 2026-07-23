package com.onpointinfo.transrouter

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class TransactionStore(context: Context) {
    private val preferences = context.getSharedPreferences("transrouter", Context.MODE_PRIVATE)

    fun add(transaction: MpesaTransaction): Boolean {
        val rows = JSONArray(preferences.getString("transactions", "[]"))
        
        // Check for duplicates by code
        for (i in 0 until rows.length()) {
            if (rows.getJSONObject(i).getString("code") == transaction.code) return false
        }

        val updated = JSONArray().put(JSONObject().apply {
            put("code", transaction.code); put("customerName", transaction.customerName)
            put("amount", transaction.amount); put("receivedAt", transaction.receivedAt); put("rawMessage", transaction.rawMessage)
        })
        for (index in 0 until minOf(rows.length(), 49)) updated.put(rows.getJSONObject(index))
        preferences.edit().putString("transactions", updated.toString()).apply()
        return true
    }

    fun recent(): List<MpesaTransaction> {
        val rows = JSONArray(preferences.getString("transactions", "[]"))
        return (0 until rows.length()).map { index -> rows.getJSONObject(index) }.map {
            MpesaTransaction(it.getString("code"), it.getString("customerName"), it.getString("amount"), it.getLong("receivedAt"), it.getString("rawMessage"))
        }
    }
}
