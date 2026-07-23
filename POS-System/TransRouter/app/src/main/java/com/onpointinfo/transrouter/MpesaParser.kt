package com.onpointinfo.transrouter

object MpesaParser {
    /**
     * Updated Regex to handle:
     * - Missing spaces (e.g., "Confirmed.You")
     * - Different wordings ("You have received Ksh" vs "Ksh received")
     * - Names followed by masked phone numbers (0702***952)
     */
    private val payment = Regex(
        "(?i)([A-Z0-9]{10})\\s+Confirmed\\.?\\s*" +
        "(?:you\\s+have\\s+received\\s+)?" +
        "Ksh\\s?([0-9,]+(?:\\.[0-9]{2})?)\\s+" +
        "(?:received\\s+from\\s+|from\\s+)" +
        "(.+?)" +
        "(?:\\s+[0-9*]{7,15}|\\s+on\\s+|$)"
    )

    fun parse(message: String): MpesaTransaction? {
        // Clean up the message: replace multiple spaces/newlines with a single space
        val cleanMessage = message.replace("\\s+".toRegex(), " ").trim()
        val match = payment.find(cleanMessage) ?: return null
        
        val code = match.groupValues[1].uppercase()
        val amount = match.groupValues[2].replace(",", "")
        val name = match.groupValues[3].trim()
        
        return MpesaTransaction(
            code = code,
            customerName = name,
            amount = amount,
            receivedAt = System.currentTimeMillis(),
            rawMessage = message
        )
    }
}
