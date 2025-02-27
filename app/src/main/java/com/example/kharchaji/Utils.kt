package com.example.kharchaji

fun parsePrice(text: String): Double {
    val priceString = text.split(" - ").lastOrNull()?.replace("₹", "")?.trim() ?: return 0.0
    return priceString.toDoubleOrNull() ?: 0.0
}

fun parseItemText(text: String): Triple<String, String?, String> {
    val parts = text.split(" - ")
    if (parts.size < 2) return Triple(text, null, "0.0")
    
    val nameAndQuantity = parts[0]
    val price = parts[1].replace("₹", "").trim()

    val quantityMatch = Regex("""\((.*?)\)""").find(nameAndQuantity)
    val quantity = quantityMatch?.groupValues?.get(1)
    val name = nameAndQuantity.replace("""\s*\(.*?\)""".toRegex(), "").trim()

    return Triple(name, quantity, price)
} 