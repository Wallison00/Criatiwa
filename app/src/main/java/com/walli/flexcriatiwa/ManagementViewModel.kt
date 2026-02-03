package com.walli.flexcriatiwa

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

class ManagementViewModel : ViewModel() {

    private val db = Firebase.firestore

    var products by mutableStateOf<List<ManagedProduct>>(emptyList())
        private set

    var isUploading by mutableStateOf(false)
        private set

    // A lista começa vazia ou com um padrão, mas será sobrescrita pelo Firebase
    var categoryConfigs by mutableStateOf<List<CategoryConfig>>(emptyList())

    init {
        // 1. ESCUTA DE PRODUTOS (Já existia)
        db.collection("products")
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                if (snapshot != null) {
                    products = snapshot.documents.mapNotNull { doc ->
                        try {
                            ManagedProduct(
                                id = doc.id,
                                code = (doc.get("code") as? Number)?.toInt() ?: 0,
                                name = doc.getString("name") ?: "",
                                price = doc.getDouble("price") ?: 0.0,
                                imageUrl = doc.getString("imageUrl") ?: "",
                                isActive = doc.getBoolean("isActive") ?: true,
                                category = doc.getString("category") ?: "Geral",
                                ingredients = (doc.get("ingredients") as? List<String>)?.toSet() ?: emptySet(),
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
                        } catch (e: Exception) { null }
                    }
                }
            }

        // 2. NOVA ESCUTA: ESTRUTURA DO CARDÁPIO (Categorias, Ingredientes, Opcionais)
        db.collection("settings").document("menu_structure")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("ManagementVM", "Erro ao carregar estrutura", e)
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    try {
                        @Suppress("UNCHECKED_CAST")
                        val rawCategories = snapshot.get("categories") as? List<Map<String, Any>>

                        if (rawCategories != null) {
                            val parsedCategories = rawCategories.map { catMap ->
                                val name = catMap["name"] as? String ?: "Sem Nome"
                                val ingredients = (catMap["defaultIngredients"] as? List<String>) ?: emptyList()

                                val rawOptionals = catMap["availableOptionals"] as? List<Map<String, Any>>
                                val optionals = rawOptionals?.map { optMap ->
                                    OptionalItem(
                                        name = optMap["name"] as? String ?: "",
                                        price = (optMap["price"] as? Number)?.toDouble() ?: 0.0
                                    )
                                } ?: emptyList()

                                CategoryConfig(name, ingredients, optionals)
                            }
                            categoryConfigs = parsedCategories
                        }
                    } catch (err: Exception) {
                        Log.e("ManagementVM", "Erro ao parsear estrutura", err)
                    }
                } else {
                    // Se não existir no banco, cria o padrão inicial
                    if (categoryConfigs.isEmpty()) {
                        createDefaultStructure()
                    }
                }
            }
    }

    // Cria estrutura padrão no banco se estiver vazio
    private fun createDefaultStructure() {
        categoryConfigs = listOf(
            CategoryConfig(
                name = "Lanches",
                defaultIngredients = listOf("Pão", "Carne", "Queijo", "Alface", "Tomate"),
                availableOptionals = listOf(OptionalItem("Bacon", 4.0), OptionalItem("Ovo", 2.0))
            ),
            CategoryConfig(
                name = "Bebidas",
                defaultIngredients = listOf("Gelo", "Limão", "Açúcar"),
                availableOptionals = listOf(OptionalItem("Leite Condensado", 2.5))
            )
        )
        saveCategoriesToFirebase()
    }

    // --- GESTÃO DE CATEGORIAS E SEUS ITENS ---

    fun addCategory(name: String) {
        if (categoryConfigs.none { it.name.equals(name, ignoreCase = true) }) {
            categoryConfigs = categoryConfigs + CategoryConfig(name)
            saveCategoriesToFirebase()
        }
    }

    fun deleteCategory(name: String) {
        categoryConfigs = categoryConfigs.filter { it.name != name }
        saveCategoriesToFirebase()
    }

    fun addIngredientToCategory(categoryName: String, ingredient: String) {
        updateCategory(categoryName) { it.copy(defaultIngredients = it.defaultIngredients + ingredient) }
    }

    fun removeIngredientFromCategory(categoryName: String, ingredient: String) {
        updateCategory(categoryName) { it.copy(defaultIngredients = it.defaultIngredients - ingredient) }
    }

    fun addOptionalToCategory(categoryName: String, optional: OptionalItem) {
        updateCategory(categoryName) { it.copy(availableOptionals = it.availableOptionals + optional) }
    }

    fun removeOptionalFromCategory(categoryName: String, optional: OptionalItem) {
        updateCategory(categoryName) { it.copy(availableOptionals = it.availableOptionals - optional) }
    }

    private fun updateCategory(name: String, update: (CategoryConfig) -> CategoryConfig) {
        categoryConfigs = categoryConfigs.map {
            if (it.name == name) update(it) else it
        }
        saveCategoriesToFirebase()
    }

    // --- PERSISTÊNCIA NO FIREBASE ---
    private fun saveCategoriesToFirebase() {
        val dataToSave = mapOf(
            "categories" to categoryConfigs.map { cat ->
                mapOf(
                    "name" to cat.name,
                    "defaultIngredients" to cat.defaultIngredients,
                    "availableOptionals" to cat.availableOptionals.map { opt ->
                        mapOf("name" to opt.name, "price" to opt.price)
                    }
                )
            }
        )

        db.collection("settings").document("menu_structure")
            .set(dataToSave)
            .addOnFailureListener { Log.e("ManagementVM", "Erro ao salvar estrutura", it) }
    }

    // --- PRODUTOS E IMAGEM ---

    fun saveProductWithImage(context: Context, product: ManagedProduct, newImageUri: Uri?, onSuccess: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            isUploading = true
            try {
                val finalImageUrl = if (newImageUri != null) compressUriToBase64(context, newImageUri) else product.imageUrl
                val nextCode = if (product.id.isBlank()) (products.maxOfOrNull { it.code } ?: 0) + 1 else product.code

                val productData = hashMapOf(
                    "code" to nextCode,
                    "name" to product.name,
                    "price" to product.price,
                    "imageUrl" to finalImageUrl,
                    "isActive" to product.isActive,
                    "category" to product.category,
                    "ingredients" to product.ingredients.toList(),
                    "optionals" to product.optionals.toList()
                )

                if (product.id.isBlank()) db.collection("products").add(productData)
                else db.collection("products").document(product.id).set(productData)

                isUploading = false
                viewModelScope.launch(Dispatchers.Main) { onSuccess() }
            } catch (e: Exception) {
                Log.e("SaveProduct", "Erro", e)
                isUploading = false
            }
        }
    }

    private fun compressUriToBase64(context: Context, uri: Uri): String {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            val maxWidth = 600
            val ratio = maxWidth.toDouble() / originalBitmap.width.toDouble()
            val newHeight = (originalBitmap.height * ratio).toInt()
            val scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, maxWidth, newHeight, true)
            val outputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 60, outputStream)
            "data:image/jpeg;base64," + Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
        } catch (e: Exception) { "" }
    }

    fun deleteProduct(product: ManagedProduct) {
        if (product.id.isNotBlank()) db.collection("products").document(product.id).delete()
    }

    // Tradutor para o Cardápio (Menu)
    val productsByCategory: List<MenuCategory>
        get() {
            return products.filter { it.isActive }
                .groupBy { it.category }
                .map { (catName, items) ->
                    MenuCategory(catName, items.map {
                        MenuItem(it.id, it.code, it.name, it.price, it.imageUrl)
                    })
                }
        }

    var productToEdit by mutableStateOf<ManagedProduct?>(null)
        private set
    fun loadProductForEdit(product: ManagedProduct) { productToEdit = product }
    fun clearEditState() { productToEdit = null }
}