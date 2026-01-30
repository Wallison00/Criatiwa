package com.walli.flexcriatiwa

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

// --- ITENS DO CARDÁPIO ---
data class MenuItem(
    val id: String = "",
    val code: Int = 0,
    val name: String = "",
    val price: Double = 0.0,
    val imageUrl: String = ""
)

data class MenuCategory(
    val name: String,
    val items: List<MenuItem>
)

data class OptionalItem(
    val name: String = "",
    val price: Double = 0.0
)

// --- ITENS DO PEDIDO (CARRINHO) ---
data class OrderItem(
    val menuItem: MenuItem = MenuItem(),
    val quantity: Int = 1,
    val removedIngredients: List<String> = emptyList(),
    val additionalIngredients: Map<String, Int> = emptyMap(),
    val meatDoneness: String? = null,
    val observations: String? = null,
    val singleItemTotalPrice: Double = 0.0
)

// --- PEDIDOS DA COZINHA E STATUS ---
data class KitchenOrder(
    val id: Long = 0,
    val firebaseId: String = "",
    val items: List<OrderItem>,
    val timestamp: Long = System.currentTimeMillis(),
    val status: OrderStatus = OrderStatus.PREPARING,
    val destinationType: String?,
    val tableSelection: Set<Int>,
    val clientName: String?,
    val payments: List<SplitPayment>
)

enum class OrderStatus {
    PREPARING,
    READY,
    DELIVERED
}

// --- PAGAMENTOS ---
data class SplitPayment(
    val amount: Double,
    val method: String
)

// --- UTILITÁRIOS (FORMATAÇÃO DE MOEDA) ---
// Esta classe agora está visível para todo o projeto
class CurrencyVisualTransformation(
    private val currencySymbol: String = "R$ ",
    private val thousandsSeparator: Char = '.'
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val digitsOnly = text.text.filter { it.isDigit() }
        val intValue = digitsOnly.toLongOrNull() ?: 0L
        val formattedNumber = intValue.toString().padStart(3, '0')
        val integerPart = formattedNumber.dropLast(2)
        val decimalPart = formattedNumber.takeLast(2)

        // Formata milhar: 1000 -> 1.000
        val formattedIntegerPart = integerPart.reversed()
            .chunked(3)
            .joinToString(separator = thousandsSeparator.toString())
            .reversed()

        val maskedText = currencySymbol + formattedIntegerPart + ',' + decimalPart

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int) = maskedText.length
            override fun transformedToOriginal(offset: Int) = digitsOnly.length
        }

        return TransformedText(AnnotatedString(maskedText), offsetMapping)
    }
}