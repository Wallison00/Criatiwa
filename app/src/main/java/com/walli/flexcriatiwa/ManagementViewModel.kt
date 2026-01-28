package com.walli.flexcriatiwa

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

// Nossa "fonte da verdade" para todos os dados de gestão.
class ManagementViewModel : ViewModel() {

    // --- LISTAS REAIS DE DADOS ---
    // Elas começam com os dados de exemplo, mas agora elas podem ser modificadas.
    var products by mutableStateOf(
        listOf(
            ManagedProduct(
                id = "1",
                name = "X-Burger Especial",
                price = 25.00,
                imageUrl = "",
                isActive = true,
                // --- ADICIONE OS CAMPOS FALTANTES ---
                category = "Lanches",
                ingredients = setOf("Pão", "Carne 150g", "Queijo"),
                optionals = setOf(OptionalItem("Bacon", 3.00))
            ),
            ManagedProduct(
                id = "2",
                name = "Batata Frita Grande",
                price = 15.00,
                imageUrl = "",
                isActive = false,
                // --- ADICIONE OS CAMPOS FALTANTES ---
                category = "Acompanhamentos", // Exemplo de outra categoria
                ingredients = setOf("Batata", "Sal"),
                optionals = emptySet() // Não tem opcionais
            ),
            ManagedProduct(
                id = "3",
                name = "Milkshake de Morango",
                price = 18.00,
                imageUrl = "",
                isActive = true,
                // --- ADICIONE OS CAMPOS FALTANTES ---
                category = "Bebidas",
                ingredients = setOf("Leite", "Sorvete", "Calda de Morango"),
                optionals = setOf(OptionalItem("Chantilly", 2.00))
            )
        )
    )
        private set // Só o ViewModel pode alterar a lista diretamente

    // --- ADICIONE ESTE NOVO BLOCO PARA CATEGORIAS ---
    var categories by mutableStateOf(
        listOf("Lanches", "Bebidas", "Sobremesas", "Acompanhamentos")
    )
        private set

    // --- ADICIONE ESTE BLOCO PARA INGREDIENTES ---
    var ingredients by mutableStateOf(
        listOf("Pão Brioche", "Carne 150g", "Queijo Cheddar", "Alface", "Tomate")
    )
        private set

    val productsByCategory: List<MenuCategory>
        get() {
            // 1. Filtra apenas os produtos que estão ATIVOS
            val activeProducts = products.filter { it.isActive }

            // 2. Agrupa os produtos ativos pela sua propriedade 'category'
            return activeProducts.groupBy { it.category }
                // 3. Transforma o mapa resultante em uma lista de MenuCategory
                .map { (categoryName, productList) ->
                    MenuCategory(
                        name = categoryName,
                        // 4. Transforma a lista de ManagedProduct em uma lista de MenuItem
                        items = productList.map { product ->
                            MenuItem(
                                id = product.id,
                                name = product.name,
                                price = product.price,
                                imageUrl = product.imageUrl
                            )
                        }
                    )
                }
        }

    fun addIngredient(ingredientName: String) {
        if (!ingredients.contains(ingredientName)) {
            ingredients = ingredients + ingredientName
        }
    }

    fun deleteIngredient(ingredientName: String) {
        ingredients = ingredients - ingredientName
    }

    // --- ADICIONE ESTE BLOCO PARA OPCIONAIS ---
    var optionals by mutableStateOf(
        listOf(
            OptionalItem("Ovo Extra", 2.50),
            OptionalItem("Bacon Crocante", 4.00),
            OptionalItem("Queijo Extra", 3.00)
        )
    )
        private set

    fun addOptional(optionalItem: OptionalItem) {
        if (!optionals.any { it.name.equals(optionalItem.name, ignoreCase = true) }) {
            optionals = optionals + optionalItem
        }
    }

    fun deleteOptional(optionalItem: OptionalItem) {
        optionals = optionals - optionalItem
    }
    // ------------------------------------------

    fun addCategory(categoryName: String) {
        if (!categories.contains(categoryName)) {
            categories = categories + categoryName
        }
    }

    fun deleteCategory(categoryName: String) {
        categories = categories - categoryName
    }

    // Estado para guardar o produto que está sendo editado
    var productToEdit by mutableStateOf<ManagedProduct?>(null)
        private set

    // --- FUNÇÕES PARA MANIPULAR OS DADOS ---

    // Função para adicionar ou atualizar um produto
    fun upsertProduct(product: ManagedProduct) {
        val existingProduct = products.find { it.id == product.id }

        if (existingProduct == null) {
            // Se não existe (novo produto), adiciona à lista.
            // (Em um app real, o ID seria gerado aqui)
            products = products + product
        } else {
            // Se já existe (edição), substitui o antigo pelo novo.
            products = products.map { if (it.id == product.id) product else it }
        }
    }

    fun deleteProduct(product: ManagedProduct) {
        // Filtra a lista de produtos, mantendo todos EXCETO o que tem o ID correspondente.
        products = products.filter { it.id != product.id }
    }

    // Função para carregar um produto para edição
    fun loadProductForEdit(product: ManagedProduct) {
        productToEdit = product
    }

    // Função para limpar o estado de edição quando o usuário sai da tela
    fun clearEditState() {
        productToEdit = null
    }

    // Futuramente, funções como deleteProduct, addCategory, etc., virão aqui.
}
