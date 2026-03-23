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
import com.google.firebase.firestore.FieldValue
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
    private var paymentConfigListener: ListenerRegistration? = null // Novo listener

    // --- ESTADOS ---
    var isLoadingProducts by mutableStateOf(true)
        private set

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

    // Novo estado para o Mercado Pago
    var paymentConfig by mutableStateOf<PaymentConfig?>(null)
        private set

    // --- ESTOQUE ---
    var stockItems by mutableStateOf<List<StockItem>>(emptyList())
        private set
    
    var stockHistoryItems by mutableStateOf<List<StockHistory>>(emptyList())
        private set

    private var stockListener: ListenerRegistration? = null
    private var stockHistoryListener: ListenerRegistration? = null

    // --- INICIALIZAÇÃO ---
    fun updateCompanyContext(companyId: String) {
        currentCompanyId = companyId

        productsListener?.remove()
        settingsListener?.remove()
        pendingUsersListener?.remove()
        employeesListener?.remove()
        paymentConfigListener?.remove() // Limpa o anterior
        stockListener?.remove()
        stockHistoryListener?.remove()

        startListeningProducts(companyId)
        startListeningSettings(companyId)
        startListeningForPendingUsers(companyId)
        startListeningForEmployees(companyId)
        loadCompanyDetails(companyId)
        loadPaymentConfig(companyId) // Inicia a escuta do Mercado Pago
        startListeningStock(companyId)
    }

    // --- MERCADO PAGO: CARREGAR CONFIGURAÇÕES ---
    fun loadPaymentConfig(companyId: String) {
        paymentConfigListener = db.collection("companies")
            .document(companyId)
            .collection("config")
            .document("payments")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("PaymentConfig", "Erro ao carregar Mercado Pago", error)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    paymentConfig = snapshot.toObject(PaymentConfig::class.java)
                    Log.d("PaymentConfig", "Configuração MP carregada com sucesso")
                }
            }
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
                db.collection("users").document(userId).update("status", "deleted").await()
            } catch (e: Exception) {
                errorMessage = "Erro ao excluir: ${e.message}"
            }
        }
    }

    fun updateUserRole(userId: String, newRole: String) {
        viewModelScope.launch {
            try {
                db.collection("users").document(userId).update("role", newRole).await()
            } catch (e: Exception) {
                errorMessage = "Erro ao atualizar cargo: ${e.message}"
            }
        }
    }

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
                                ingredients = (doc.get("ingredients") as? List<*>)?.mapNotNull { it as? String }?.toSet() ?: emptySet(),
                                optionals = try {
                                    val list = doc.get("optionals") as? List<*>
                                    list?.mapNotNull { it as? Map<*, *> }?.map {
                                        OptionalItem(it["name"] as? String ?: "", (it["price"] as? Number)?.toDouble() ?: 0.0)
                                    }?.toSet() ?: emptySet()
                                } catch (_: Exception) { emptySet() }
                            )
                        } catch (_: Exception) { null }
                    }
                    isLoadingProducts = false
                } else {
                    isLoadingProducts = false
                }
            }
    }

    private fun startListeningSettings(companyId: String) {
        settingsListener = db.collection("companies").document(companyId)
            .collection("settings").document("menu_structure")
            .addSnapshotListener { snapshot, e ->
                if (snapshot != null && snapshot.exists()) {
                    try {
                        val rawCategories = snapshot.get("categories") as? List<*>
                        if (rawCategories != null) {
                            categoryConfigs = rawCategories.mapNotNull { it as? Map<*, *> }.map { catMap ->
                                val name = catMap["name"] as? String ?: "Sem Nome"
                                val ingredients = (catMap["defaultIngredients"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                                val rawOptionals = catMap["availableOptionals"] as? List<*>
                                val optionals = rawOptionals?.mapNotNull { it as? Map<*, *> }?.map { optMap ->
                                    OptionalItem(optMap["name"] as? String ?: "", (optMap["price"] as? Number)?.toDouble() ?: 0.0)
                                } ?: emptyList()
                                CategoryConfig(name, ingredients, optionals)
                            }
                        }
                    } catch (err: Exception) { Log.e("MngVM", "Error parsing settings", err) }
                } else {
                    createDefaultStructure()
                }
            }
    }

    private fun createDefaultStructure() {
        categoryConfigs = listOf(CategoryConfig("Geral"))
        saveCategoriesToFirebase()
    }

    fun addCategory(name: String) { if (categoryConfigs.none { it.name.equals(name, ignoreCase = true) }) { categoryConfigs = categoryConfigs + CategoryConfig(name); saveCategoriesToFirebase() } }
    fun deleteCategory(name: String) { categoryConfigs = categoryConfigs.filter { it.name != name }; saveCategoriesToFirebase() }
    fun addIngredientToCategory(cat: String, ing: String) { updateCategory(cat) { it.copy(defaultIngredients = it.defaultIngredients + ing) } }
    fun removeIngredientFromCategory(cat: String, ing: String) { updateCategory(cat) { it.copy(defaultIngredients = it.defaultIngredients - ing) } }
    fun addOptionalToCategory(cat: String, opt: OptionalItem) { updateCategory(cat) { it.copy(availableOptionals = it.availableOptionals + opt) } }
    fun removeOptionalFromCategory(cat: String, opt: OptionalItem) { updateCategory(cat) { it.copy(availableOptionals = it.availableOptionals - opt) } }
    
    fun updateIngredientsList(cat: String, list: List<String>) {
        updateCategory(cat) { it.copy(defaultIngredients = list) }
    }

    fun updateOptionalsList(cat: String, list: List<OptionalItem>) {
        updateCategory(cat) { it.copy(availableOptionals = list) }
    }
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
                val productOptionalsMap = product.optionals.map { mapOf("name" to it.name, "price" to it.price) }.toList()
                val productData = hashMapOf("code" to nextCode, "name" to product.name, "price" to product.price, "imageUrl" to finalImageUrl, "isActive" to product.isActive, "category" to product.category, "ingredients" to product.ingredients.toList(), "optionals" to productOptionalsMap)
                val ref = db.collection("companies").document(companyId).collection("products")
                if (product.id.isBlank()) ref.add(productData) else ref.document(product.id).set(productData)
                isUploading = false
                viewModelScope.launch(Dispatchers.Main) { onSuccess() }
            } catch (_: Exception) { isUploading = false }
        }
    }

    // --- GERENCIAMENTO DE ESTOQUE ---
    private fun startListeningStock(companyId: String) {
        stockListener = db.collection("companies").document(companyId).collection("stock")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    stockItems = snapshot.documents.mapNotNull { doc ->
                        try {
                            StockItem(
                                id = doc.id,
                                name = doc.getString("name") ?: "",
                                barcode = doc.getString("barcode") ?: "",
                                quantity = doc.getDouble("quantity") ?: 0.0,
                                minQuantity = doc.getDouble("minQuantity") ?: 0.0,
                                unit = doc.getString("unit") ?: "Unidade",
                                imageUrl = doc.getString("imageUrl") ?: ""
                            )
                        } catch (_: Exception) { null }
                    }
                }
            }
        
        stockHistoryListener = db.collection("companies").document(companyId).collection("stock_history")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    stockHistoryItems = snapshot.documents.mapNotNull { doc ->
                        try {
                            StockHistory(
                                id = doc.id, stockItemId = doc.getString("stockItemId") ?: "",
                                itemName = doc.getString("itemName") ?: "", type = doc.getString("type") ?: "ENTRADA",
                                changeAmount = doc.getDouble("changeAmount") ?: 0.0, finalQuantity = doc.getDouble("finalQuantity") ?: 0.0,
                                timestamp = doc.getTimestamp("timestamp") ?: com.google.firebase.Timestamp.now(),
                                handledByName = doc.getString("handledByName") ?: ""
                            )
                        } catch (_: Exception) { null }
                    }
                }
            }
    }

    private fun logStockHistory(companyId: String, stockItemId: String, itemName: String, type: String, changeAmount: Double, finalQuantity: Double, handledByName: String) {
        val historyData = hashMapOf(
            "stockItemId" to stockItemId, "itemName" to itemName, "type" to type,
            "changeAmount" to changeAmount, "finalQuantity" to finalQuantity,
            "handledByName" to handledByName, "timestamp" to FieldValue.serverTimestamp()
        )
        db.collection("companies").document(companyId).collection("stock_history").add(historyData)
    }

    fun saveStockItem(context: Context, item: StockItem, newImageUri: Uri?, handledByName: String, onSuccess: () -> Unit) {
        val companyId = currentCompanyId ?: return
        viewModelScope.launch(Dispatchers.IO) {
            isUploading = true
            try {
                // Reutiliza a função de compressão para Base64 que não salva arquivos isolados
                val finalImageUrl = if (newImageUri != null) compressUriToBase64(context, newImageUri) else item.imageUrl
                
                val itemData = hashMapOf(
                    "name" to item.name,
                    "barcode" to item.barcode,
                    "quantity" to item.quantity,
                    "minQuantity" to item.minQuantity,
                    "unit" to item.unit,
                    "imageUrl" to finalImageUrl
                )
                
                val ref = db.collection("companies").document(companyId).collection("stock")
                if (item.id.isBlank()) {
                    ref.add(itemData).addOnSuccessListener { docRef ->
                        logStockHistory(companyId, docRef.id, item.name, "CADASTRO", item.quantity, item.quantity, handledByName)
                    }.addOnFailureListener { e -> errorMessage = "Erro Firebase: ${e.message}" }
                } else {
                    ref.document(item.id).set(itemData).addOnSuccessListener {
                        logStockHistory(companyId, item.id, item.name, "EDICAO", 0.0, item.quantity, handledByName)
                    }.addOnFailureListener { e -> errorMessage = "Erro Firebase: ${e.message}" }
                }
                
                isUploading = false
                viewModelScope.launch(Dispatchers.Main) { onSuccess() }
            } catch (e: Exception) { 
                isUploading = false
                errorMessage = "Erro interno: ${e.message}"
                Log.e("Estoque", "Erro ao salvar insumo", e)
            }
        }
    }

    fun registerStockEntry(item: StockItem, additionalQuantity: Double, handledByName: String, onSuccess: () -> Unit) {
        val companyId = currentCompanyId ?: return
        if (item.id.isBlank() || additionalQuantity <= 0) return
        viewModelScope.launch(Dispatchers.IO) {
            isUploading = true
            try {
                val newQuantity = item.quantity + additionalQuantity
                val ref = db.collection("companies").document(companyId).collection("stock").document(item.id)
                ref.update("quantity", newQuantity).addOnSuccessListener {
                    logStockHistory(companyId, item.id, item.name, "ENTRADA", additionalQuantity, newQuantity, handledByName)
                    isUploading = false
                    viewModelScope.launch(Dispatchers.Main) { onSuccess() }
                }.addOnFailureListener { 
                    isUploading = false
                    errorMessage = "Falha ao registrar entrada: ${it.message}" 
                }
            } catch (e: Exception) {
                isUploading = false
            }
        }
    }

    fun deleteStockItem(item: StockItem, handledByName: String) {
        val companyId = currentCompanyId ?: return
        if (item.id.isNotBlank()) {
            db.collection("companies").document(companyId).collection("stock").document(item.id).delete().addOnSuccessListener {
                logStockHistory(companyId, item.id, item.name, "EXCLUSAO", -item.quantity, 0.0, handledByName)
            }
        }
    }


    private fun compressUriToBase64(context: Context, uri: Uri): String { return try { val inputStream = context.contentResolver.openInputStream(uri); val bitmap = BitmapFactory.decodeStream(inputStream); val outputStream = ByteArrayOutputStream(); bitmap.compress(Bitmap.CompressFormat.JPEG, 60, outputStream); "data:image/jpeg;base64," + Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP) } catch (_: Exception) { "" } }
    fun deleteProduct(product: ManagedProduct) { val companyId = currentCompanyId ?: return; if (product.id.isNotBlank()) db.collection("companies").document(companyId).collection("products").document(product.id).delete() }

    val productsByCategory: List<MenuCategory>
        get() = products.filter { it.isActive }
            .groupBy { it.category }
            .map { (cat, items) ->
                MenuCategory(
                    cat,
                    items.sortedBy { it.code }
                        .map { MenuItem(it.id, it.code, it.name, it.price, it.imageUrl) }
                )
            }

    var productToEdit by mutableStateOf<ManagedProduct?>(null); private set
    fun loadProductForEdit(product: ManagedProduct) { productToEdit = product }; fun clearEditState() { productToEdit = null }

    // Limpeza de listeners quando o ViewModel for destruído
    override fun onCleared() {
        super.onCleared()
        productsListener?.remove()
        settingsListener?.remove()
        pendingUsersListener?.remove()
        employeesListener?.remove()
        paymentConfigListener?.remove()
    }
}