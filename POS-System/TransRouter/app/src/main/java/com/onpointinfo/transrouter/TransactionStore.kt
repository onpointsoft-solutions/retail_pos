package com.onpointinfo.transrouter

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

private val transactionStoreLock = Any()

class TransactionStore(context: Context) {
    private val preferences = context.getSharedPreferences("transrouter", Context.MODE_PRIVATE)

    fun add(transaction: MpesaTransaction): Boolean {
        synchronized(transactionStoreLock) {
            val rows = JSONArray(preferences.getString("transactions", "[]"))
            for (index in 0 until rows.length()) {
                if (rows.getJSONObject(index).getString("code") == transaction.code) return false
            }

            val updated = JSONArray().put(JSONObject().apply {
                put("code", transaction.code)
                put("customerName", transaction.customerName)
                put("amount", transaction.amount)
                put("receivedAt", transaction.receivedAt)
                put("rawMessage", transaction.rawMessage)
                put("forwarded", false)
            })
            for (index in 0 until minOf(rows.length(), 49)) updated.put(rows.getJSONObject(index))
            return preferences.edit().putString("transactions", updated.toString()).commit()
        }
    }

    fun recent(): List<MpesaTransaction> {
        synchronized(transactionStoreLock) {
            val rows = JSONArray(preferences.getString("transactions", "[]"))
            return (0 until rows.length()).map { index -> rows.getJSONObject(index) }.map {
                MpesaTransaction(
                    it.getString("code"),
                    it.optString("customerName"),
                    it.optString("amount"),
                    it.optLong("receivedAt"),
                    it.optString("rawMessage"),
                    it.optBoolean("forwarded", false)
                )
            }
        }
    }

    fun pending(): List<MpesaTransaction> = recent().filterNot { it.forwarded }

    fun markForwarded(code: String) {
        synchronized(transactionStoreLock) {
            val rows = JSONArray(preferences.getString("transactions", "[]"))
            var changed = false
            for (index in 0 until rows.length()) {
                val row = rows.getJSONObject(index)
                if (row.optString("code").equals(code, ignoreCase = true)) {
                    row.put("forwarded", true)
                    changed = true
                }
            }
            if (changed) {
                preferences.edit().putString("transactions", rows.toString()).commit()
            }
        }
    }
}
