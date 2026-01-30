package com.walli.flexcriatiwa

// --- ITENS DO CARDÁPIO ---
data class MenuItem(
    val id: String = "",
    val code: Int = 0, // <--- NOVO CAMPO: Para exibir o #001
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