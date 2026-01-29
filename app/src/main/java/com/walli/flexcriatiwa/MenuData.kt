package com.walli.flexcriatiwa

// --- MODELOS DE DADOS ---

data class MenuItem(
    val id: String = "",
    val name: String = "",
    val price: Double = 0.0,
    val imageUrl: String = ""
)

data class MenuCategory(
    val name: String,
    val items: List<MenuItem>
)

data class AdditionalIngredient(
    val name: String,
    val price: Double
)

data class OptionalItem(
    val name: String = "",
    val price: Double = 0.0
)

// Representa um item dentro do pedido (ex: 1x X-Burger sem cebola)
data class OrderItem(
    val menuItem: MenuItem = MenuItem(),
    val quantity: Int = 1,
    val removedIngredients: List<String> = emptyList(), // Mudamos Set para List (Firebase prefere List)
    val additionalIngredients: Map<String, Int> = emptyMap(),
    val meatDoneness: String? = null,
    val observations: String? = null,
    val singleItemTotalPrice: Double = 0.0
)

// --- A NOVA CLASSE DE PEDIDO (ORDER) ---
// Essa é a estrutura que vai para o Firebase
data class Order(
    val id: String = "",
    val items: List<OrderItem> = emptyList(),
    val status: String = "PENDING", // PENDING, PREPARING, READY, DELIVERED
    val destinationType: String = "Viagem", // "Mesa" ou "Viagem"
    val tableNumber: Int? = null,
    val clientName: String? = null,
    val paymentMethod: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)