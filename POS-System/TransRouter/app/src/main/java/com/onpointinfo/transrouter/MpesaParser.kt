package com.onpointinfo.transrouter

object MpesaParser {
    fun parse(message: String): MpesaTransaction? {
        // Clean up the message: replace multiple spaces/newlines with a single space
        val cleanMessage = message.replace("\\s+".toRegex(), " ").trim()
        
        // 1. Find Transaction Code (10 alphanumeric chars at the start)
        val codeMatch = Regex("^([A-Z0-9]{10})", RegexOption.IGNORE_CASE).find(cleanMessage) ?: return null
        val code = codeMatch.groupValues[1].uppercase()

        // 2. Find Amount (the first occurrence of Ksh followed by numbers)
        val amountMatch = Regex("Ksh\\s?([0-9,]+\\.[0-9]{2})", RegexOption.IGNORE_CASE).find(cleanMessage) ?: return null
        val amount = amountMatch.groupValues[1].replace(",", "")

        // 3. Find Sender/Customer Name
        // Usually follows "received from" or "from" and ends before "New Account", "on", a phone number, or end of string
        val senderMatch = Regex("(?:received\\s+from|from)\\s+(.+?)(?:\\.\\s+New|\\s+on|\\s+[0-9*]{10,15}|$)", RegexOption.IGNORE_CASE).find(cleanMessage)
        val name = senderMatch?.groupValues?.get(1)?.trim() ?: "Unknown"

        return MpesaTransaction(
            code = code,
            customerName = name,
            amount = amount,
            receivedAt = System.currentTimeMillis(),
            rawMessage = message
        )
    }
}
