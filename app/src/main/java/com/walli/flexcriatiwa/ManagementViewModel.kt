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
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream

class ManagementViewModel : ViewModel() {

    private val db = Firebase.firestore

    private var currentCompanyId: String? = null
    private var productsListener: ListenerRegistration? = null
    private var settingsListener: ListenerRegistration? = null
    private var pendingUsersListener: ListenerRegistration? = null
    private var employeesListener: ListenerRegistration? = null

    // --- ESTADOS ---
    var products by mutableStateOf<List<ManagedProduct>>(emptyList())
        private set

    var isUploading by mutableStateOf(false)
        private set

    var categoryConfigs by mutableStateOf<List<CategoryConfig>>(emptyList())

    var currentCompany by mutableStateOf<Company?>(null)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    var pendingUsers by mutableStateOf<List<UserProfile>>(emptyList())
        private set

    var employees by mutableStateOf<List<UserProfile>>(emptyList())
        private set

    // --- INICIALIZAÇÃO ---
    fun updateCompanyContext(companyId: String) {
        currentCompanyId = companyId

        productsListener?.remove()
        settingsListener?.remove()
        pendingUsersListener?.remove()
        employeesListener?.remove()

        startListeningProducts(companyId)
        startListeningSettings(companyId)
        startListeningForPendingUsers(companyId)
        startListeningForEmployees(companyId)
        loadCompanyDetails(companyId)
    }

    // --- GERENCIAMENTO DE USUÁRIOS ---

    private fun startListeningForPendingUsers(companyId: String) {
        pendingUsersListener = db.collection("users")
            .whereEqualTo("companyId", companyId)
            .whereEqualTo("status", "pending_approval")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    pendingUsers = snapshot.toObjects(UserProfile::class.java)
                }
            }
    }

    private fun startListeningForEmployees(companyId: String) {
        employeesListener = db.collection("users")
            .whereEqualTo("companyId", companyId)
            .whereIn("status", listOf("active", "blocked"))
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    employees = snapshot.toObjects(UserProfile::class.java)
                        .sortedBy { it.name }
                }
            }
    }

    fun toggleUserStatus(user: UserProfile) {
        val newStatus = if (user.status == "blocked") "active" else "blocked"
        viewModelScope.launch {
            try {
                db.collection("users").document(user.uid).update("status", newStatus).await()
            } catch (e: Exception) {
                errorMessage = "Erro ao alterar status: ${e.message}"
            }
        }
    }

    fun deleteUser(userId: String) {
        viewModelScope.launch {
            try {
                // Exclusão Lógica (Mantém histórico, mas remove acesso)
                db.collection("users").document(userId).update("status", "deleted").await()
            } catch (e: Exception) {
                errorMessage = "Erro ao excluir: ${e.message}"
            }
        }
    }

    // --- NOVA FUNÇÃO: ATUALIZAR CARGO ---
    fun updateUserRole(userId: String, newRole: String) {
        viewModelScope.launch {
            try {
                db.collection("users").document(userId).update("role", newRole).await()
            } catch (e: Exception) {
                errorMessage = "Erro ao atualizar cargo: ${e.message}"
            }
        }
    }

    // --- DADOS DA EMPRESA (Mantido) ---
    private fun loadCompanyDetails(companyId: String) {
        viewModelScope.launch {
            errorMessage = null
            try {
                val doc = db.collection("companies").document(companyId).get().await()
                if (doc.exists()) {
                    var company = doc.toObject(Company::class.java)
                    if (company != null) {
                        if (company.shareCode.isBlank()) {
                            val newCode = (1..6).map { ('A'..'Z').random() }.joinToString("")
                            db.collection("companies").document(companyId).update("shareCode", newCode).await()
                            company = company.copy(shareCode = newCode)
                        }
                        currentCompany = company
                    }
                }
            } catch (e: Exception) { errorMessage = "Erro: ${e.message}" }
        }
    }

    // --- PRODUTOS E CONFIGURAÇÕES (Mantido igual) ---
    private fun startListeningProducts(companyId: String) {
        productsListener = db.collection("companies").document(companyId).collection("products")
            .addSnapshotListener { snapshot, e ->
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
                                        OptionalItem(it["name"] as? String ?: "", (it["price"] as? Number)?.toDouble() ?: 0.0)
                                    }?.toSet() ?: emptySet()
                                } catch (e: Exception) { emptySet() }
                            )
                        } catch (e: Exception) { null }
                    }
                }
            }
    }

    private fun startListeningSettings(companyId: String) {
        settingsListener = db.collection("companies").document(companyId)
            .collection("settings").document("menu_structure")
            .addSnapshotListener { snapshot, e ->
                if (snapshot != null && snapshot.exists()) {
                    try {
                        @Suppress("UNCHECKED_CAST")
                        val rawCategories = snapshot.get("categories") as? List<Map<String, Any>>
                        if (rawCategories != null) {
                            categoryConfigs = rawCategories.map { catMap ->
                                val name = catMap["name"] as? String ?: "Sem Nome"
                                val ingredients = (catMap["defaultIngredients"] as? List<String>) ?: emptyList()
                                val rawOptionals = catMap["availableOptionals"] as? List<Map<String, Any>>
                                val optionals = rawOptionals?.map { optMap ->
                                    OptionalItem(optMap["name"] as? String ?: "", (optMap["price"] as? Number)?.toDouble() ?: 0.0)
                                } ?: emptyList()
                                CategoryConfig(name, ingredients, optionals)
                            }
                        }
                    } catch (err: Exception) { Log.e("MngVM", "Error parsing settings", err) }
                } else {
                    createDefaultStructure(companyId)
                }
            }
    }

    private fun createDefaultStructure(companyId: String) {
        categoryConfigs = listOf(CategoryConfig("Geral"))
        saveCategoriesToFirebase()
    }

    fun addCategory(name: String) { if (categoryConfigs.none { it.name.equals(name, ignoreCase = true) }) { categoryConfigs = categoryConfigs + CategoryConfig(name); saveCategoriesToFirebase() } }
    fun deleteCategory(name: String) { categoryConfigs = categoryConfigs.filter { it.name != name }; saveCategoriesToFirebase() }
    fun addIngredientToCategory(cat: String, ing: String) { updateCategory(cat) { it.copy(defaultIngredients = it.defaultIngredients + ing) } }
    fun removeIngredientFromCategory(cat: String, ing: String) { updateCategory(cat) { it.copy(defaultIngredients = it.defaultIngredients - ing) } }
    fun addOptionalToCategory(cat: String, opt: OptionalItem) { updateCategory(cat) { it.copy(availableOptionals = it.availableOptionals + opt) } }
    fun removeOptionalFromCategory(cat: String, opt: OptionalItem) { updateCategory(cat) { it.copy(availableOptionals = it.availableOptionals - opt) } }
    private fun updateCategory(name: String, update: (CategoryConfig) -> CategoryConfig) { categoryConfigs = categoryConfigs.map { if (it.name == name) update(it) else it }; saveCategoriesToFirebase() }

    private fun saveCategoriesToFirebase() {
        val companyId = currentCompanyId ?: return
        val dataToSave = mapOf("categories" to categoryConfigs.map { cat ->
            mapOf("name" to cat.name, "defaultIngredients" to cat.defaultIngredients, "availableOptionals" to cat.availableOptionals.map { mapOf("name" to it.name, "price" to it.price) })
        })
        db.collection("companies").document(companyId).collection("settings").document("menu_structure").set(dataToSave)
    }

    fun saveProductWithImage(context: Context, product: ManagedProduct, newImageUri: Uri?, onSuccess: () -> Unit) {
        val companyId = currentCompanyId ?: return
        viewModelScope.launch(Dispatchers.IO) {
            isUploading = true
            try {
                val finalImageUrl = if (newImageUri != null) compressUriToBase64(context, newImageUri) else product.imageUrl
                val nextCode = if (product.id.isBlank()) (products.maxOfOrNull { it.code } ?: 0) + 1 else product.code
                val productData = hashMapOf("code" to nextCode, "name" to product.name, "price" to product.price, "imageUrl" to finalImageUrl, "isActive" to product.isActive, "category" to product.category, "ingredients" to product.ingredients.toList(), "optionals" to product.optionals.toList())
                val ref = db.collection("companies").document(companyId).collection("products")
                if (product.id.isBlank()) ref.add(productData) else ref.document(product.id).set(productData)
                isUploading = false
                viewModelScope.launch(Dispatchers.Main) { onSuccess() }
            } catch (e: Exception) { isUploading = false }
        }
    }

    private fun compressUriToBase64(context: Context, uri: Uri): String { return try { val inputStream = context.contentResolver.openInputStream(uri); val bitmap = BitmapFactory.decodeStream(inputStream); val outputStream = ByteArrayOutputStream(); bitmap.compress(Bitmap.CompressFormat.JPEG, 60, outputStream); "data:image/jpeg;base64," + Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP) } catch (e: Exception) { "" } }
    fun deleteProduct(product: ManagedProduct) { val companyId = currentCompanyId ?: return; if (product.id.isNotBlank()) db.collection("companies").document(companyId).collection("products").document(product.id).delete() }

    // --- CORREÇÃO AQUI: Ordenação por Código ---
    val productsByCategory: List<MenuCategory>
        get() = products.filter { it.isActive }
            .groupBy { it.category }
            .map { (cat, items) ->
                MenuCategory(
                    cat,
                    items.sortedBy { it.code } // Ordena itens pelo código (menor -> maior)
                        .map { MenuItem(it.id, it.code, it.name, it.price, it.imageUrl) }
                )
            }

    var productToEdit by mutableStateOf<ManagedProduct?>(null); private set
    fun loadProductForEdit(product: ManagedProduct) { productToEdit = product }; fun clearEditState() { productToEdit = null }
}