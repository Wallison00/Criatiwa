package com.walli.flexcriatiwa

// --- ESTRUTURAS DE DADOS (MODELOS) ---
data class MenuItem(
    val id: String,
    val name: String,
    val price: Double,
    val imageUrl: String
)

data class MenuCategory(
    val name: String,
    val items: List<MenuItem>
)

// --- DADOS DE EXEMPLO ---
val sampleCategories = listOf(
    MenuCategory(
        name = "Amburguers",
        items = listOf(
            MenuItem("001", "X-Bacon Supremo", 25.50, "https://i.imgur.com/8rv75t6.jpeg" ),
            MenuItem("002", "Duplo Cheddar", 28.00, "https://i.imgur.com/sC2x2pL.jpeg" ),
            MenuItem("003", "Frango Crocante", 23.90, "https://i.imgur.com/d5Zt2Lz.jpeg" ),
            MenuItem("004", "Vegetariano", 22.00, "https://i.imgur.com/VbC2dYg.jpeg" ),
        )
    ),
    MenuCategory(
        name = "Refeições",
        items = listOf(
            MenuItem("005", "PF da Casa", 18.00, "https://i.imgur.com/5wYvbc3.jpeg" ),
            MenuItem("006", "Feijoada", 35.00, "https://i.imgur.com/hADpm5n.jpeg" ),
            MenuItem("007", "Parmegiana", 32.50, "https://i.imgur.com/e5YyXf0.jpeg" ),
        )
    )
)
data class AdditionalIngredient(
    val name: String,
    val price: Double
)

val sampleAdditionalIngredients = listOf(
    AdditionalIngredient("Queijo", 2.50),
    AdditionalIngredient("Ovo", 1.00),
    AdditionalIngredient("Bacon", 5.00),
    AdditionalIngredient("Blend 100g", 7.80)
)

data class OrderItem(
    val menuItem: MenuItem,
    val quantity: Int,
    val removedIngredients: Set<String>,
    val additionalIngredients: Map<String, Int>, // Mapeia nome do ingrediente para quantidade
    val meatDoneness: String?, // Pode ser nulo se não for um item de carne
    val observations: String?, // Pode ser nulo
    val singleItemTotalPrice: Double // Preço total para este item (preço base + adicionais)
)

data class OptionalItem(
    val name: String,
    val price: Double
)