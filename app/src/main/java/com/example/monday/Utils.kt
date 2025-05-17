package com.example.monday

fun parsePrice(text: String): Double {
    // First, remove any category metadata
    val cleanedText = text.split("|CATS:").first()
    
    // Extract price from the clean text
    val priceString = cleanedText.split(" - ₹").lastOrNull()?.trim() ?: return 0.0
    return priceString.toDoubleOrNull() ?: 0.0
}

fun parseItemText(text: String): Triple<String, String?, String> {
    // First, remove any category metadata
    val cleanedText = text.split("|CATS:").first()
    
    val parts = cleanedText.split(" - ")
    if (parts.size < 2) return Triple(cleanedText, null, "0.0")
    
    val nameAndQuantity = parts[0]
    val price = parts[1].replace("₹", "").trim()

    val quantityMatch = Regex("""\((.*?)\)""").find(nameAndQuantity)
    var extractedQuantityString = quantityMatch?.groupValues?.get(1)

    if (extractedQuantityString != null && extractedQuantityString.endsWith("items", ignoreCase = true)) {
        val numericPart = extractedQuantityString.substring(0, extractedQuantityString.length - "items".length).trim()
        if (numericPart.isNotEmpty() && numericPart.all { it.isDigit() || it == '.' }) {
            extractedQuantityString = numericPart
        }
    }

    val name = nameAndQuantity.replace("""\s*\(.*?\)""".toRegex(), "").trim()

    return Triple(name, extractedQuantityString, price)
} 