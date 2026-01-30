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

    init {
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
    }

    fun saveProductWithImage(
        context: Context,
        product: ManagedProduct,
        newImageUri: Uri?,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            isUploading = true

            try {
                val finalImageUrl = if (newImageUri != null) {
                    compressUriToBase64(context, newImageUri)
                } else {
                    product.imageUrl
                }

                val nextCode = if (product.id.isBlank()) {
                    (products.maxOfOrNull { it.code } ?: 0) + 1
                } else {
                    product.code
                }

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

                if (product.id.isBlank()) {
                    db.collection("products").add(productData)
                } else {
                    db.collection("products").document(product.id).set(productData)
                }

                isUploading = false
                viewModelScope.launch(Dispatchers.Main) { onSuccess() }

            } catch (e: Exception) {
                Log.e("SaveProduct", "Erro ao salvar", e)
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

            val scaledBitmap = if (originalBitmap.width > maxWidth) {
                Bitmap.createScaledBitmap(originalBitmap, maxWidth, newHeight, true)
            } else {
                originalBitmap
            }

            val outputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 60, outputStream)
            val byteArray = outputStream.toByteArray()

            "data:image/jpeg;base64," + Base64.encodeToString(byteArray, Base64.NO_WRAP)
        } catch (e: Exception) { "" }
    }

    fun deleteProduct(product: ManagedProduct) {
        if (product.id.isNotBlank()) {
            db.collection("products").document(product.id).delete()
        }
    }

    var categories by mutableStateOf(listOf("Lanches", "Bebidas", "Sobremesas", "Acompanhamentos"))
    var ingredients by mutableStateOf(listOf("Pão Brioche", "Carne 150g", "Queijo Cheddar", "Alface", "Tomate"))
    var optionals by mutableStateOf(listOf(OptionalItem("Ovo Extra", 2.50), OptionalItem("Bacon Crocante", 4.00), OptionalItem("Queijo Extra", 3.00)))

    // --- AQUI ESTÁ A ATUALIZAÇÃO IMPORTANTE ---
    val productsByCategory: List<MenuCategory>
        get() {
            return products.filter { it.isActive }
                .groupBy { it.category }
                .map { (catName, items) ->
                    MenuCategory(catName, items.map {
                        // Mapeia o ManagedProduct (com code) para MenuItem (com code)
                        MenuItem(
                            id = it.id,
                            code = it.code, // <--- Passando o código aqui
                            name = it.name,
                            price = it.price,
                            imageUrl = it.imageUrl
                        )
                    })
                }
        }

    var productToEdit by mutableStateOf<ManagedProduct?>(null)
        private set

    fun loadProductForEdit(product: ManagedProduct) { productToEdit = product }
    fun clearEditState() { productToEdit = null }

    fun addIngredient(i: String) { if (!ingredients.contains(i)) ingredients = ingredients + i }
    fun deleteIngredient(i: String) { ingredients = ingredients - i }
    fun addCategory(c: String) { if (!categories.contains(c)) categories = categories + c }
    fun deleteCategory(c: String) { categories = categories - c }
    fun addOptional(o: OptionalItem) { if (!optionals.any { it.name.equals(o.name, true) }) optionals = optionals + o }
    fun deleteOptional(o: OptionalItem) { optionals = optionals - o }
}