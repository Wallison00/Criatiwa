package com.walli.flexcriatiwa

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.toObject

// ViewModel agora conversa direto com o Firebase
class ManagementViewModel : ViewModel() {

    // Instância do banco de dados na nuvem
    private val db = Firebase.firestore

    // Lista de produtos que a tela vê
    var products by mutableStateOf<List<ManagedProduct>>(emptyList())
        private set

    init {
        // --- A MÁGICA ACONTECE AQUI ---
        // Ficamos "ouvindo" a coleção 'products' na nuvem.
        // Se alguém adicionar um produto em outro celular, esta lista atualiza sozinha aqui.
        db.collection("products")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("Firebase", "Erro ao ouvir dados", e)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    // Converte os documentos do Firebase para seus objetos ManagedProduct
                    products = snapshot.documents.mapNotNull { doc ->
                        // Tentamos converter automaticamente
                        val product = try {
                            // Mapeamento manual para garantir segurança nos tipos
                            ManagedProduct(
                                id = doc.id,
                                name = doc.getString("name") ?: "",
                                price = doc.getDouble("price") ?: 0.0,
                                imageUrl = doc.getString("imageUrl") ?: "",
                                isActive = doc.getBoolean("isActive") ?: true,
                                category = doc.getString("category") ?: "Geral",
                                ingredients = (doc.get("ingredients") as? List<String>)?.toSet() ?: emptySet(),
                                // Para opcionais, simplificamos convertendo do Hashmap do Firebase
                                optionals = try {
                                    val list = doc.get("optionals") as? List<Map<String, Any>>
                                    list?.map {
                                        OptionalItem(
                                            name = it["name"] as? String ?: "",
                                            price = (it["price"] as? Number)?.toDouble() ?: 0.0
                                        )
                                    }?.toSet() ?: emptySet()
                                } catch (e: Exception) { emptySet() }
                            )
                        } catch (e: Exception) {
                            null
                        }
                        product
                    }
                }
            }
    }

    // --- DADOS TEMPORÁRIOS (Categorias, etc) ---
    var categories by mutableStateOf(listOf("Lanches", "Bebidas", "Sobremesas", "Acompanhamentos"))
        private set
    var ingredients by mutableStateOf(listOf("Pão Brioche", "Carne 150g", "Queijo Cheddar", "Alface", "Tomate"))
        private set
    var optionals by mutableStateOf(listOf(OptionalItem("Ovo Extra", 2.50), OptionalItem("Bacon Crocante", 4.00), OptionalItem("Queijo Extra", 3.00)))
        private set

    // --- LÓGICA DO CARDÁPIO ---
    val productsByCategory: List<MenuCategory>
        get() {
            return products.filter { it.isActive }
                .groupBy { it.category }
                .map { (catName, items) ->
                    MenuCategory(catName, items.map { MenuItem(it.id, it.name, it.price, it.imageUrl) })
                }
        }

    // --- SALVAR NA NUVEM ---
    fun upsertProduct(product: ManagedProduct) {
        // Preparamos os dados para enviar (Firebase prefere Listas a Sets)
        val productData = hashMapOf(
            "name" to product.name,
            "price" to product.price,
            "imageUrl" to product.imageUrl,
            "isActive" to product.isActive,
            "category" to product.category,
            "ingredients" to product.ingredients.toList(),
            "optionals" to product.optionals.toList() // O Firebase serializa a classe OptionalItem auto
        )

        if (product.id.isBlank()) {
            // Criar Novo: Deixamos o Firebase gerar o ID único
            db.collection("products").add(productData)
        } else {
            // Editar Existente: Usamos o ID para atualizar
            db.collection("products").document(product.id).set(productData)
        }
    }

    // --- DELETAR DA NUVEM ---
    fun deleteProduct(product: ManagedProduct) {
        if (product.id.isNotBlank()) {
            db.collection("products").document(product.id).delete()
        }
    }

    // --- EDIÇÃO (Lógica Local) ---
    var productToEdit by mutableStateOf<ManagedProduct?>(null)
        private set

    fun loadProductForEdit(product: ManagedProduct) { productToEdit = product }
    fun clearEditState() { productToEdit = null }

    // --- FUNÇÕES AUXILIARES ---
    fun addIngredient(i: String) { if (!ingredients.contains(i)) ingredients = ingredients + i }
    fun deleteIngredient(i: String) { ingredients = ingredients - i }
    fun addCategory(c: String) { if (!categories.contains(c)) categories = categories + c }
    fun deleteCategory(c: String) { categories = categories - c }
    fun addOptional(o: OptionalItem) { if (!optionals.any { it.name.equals(o.name, true) }) optionals = optionals + o }
    fun deleteOptional(o: OptionalItem) { optionals = optionals - o }
}