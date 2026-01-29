package com.walli.flexcriatiwa

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*

// 1. Modelo de dados para um pedido enviado à cozinha
data class KitchenOrder(
    val id: Long = 0, // Usaremos o timestamp como ID numérico para compatibilidade
    val firebaseId: String = "", // ID do documento no Firebase (String)
    val items: List<OrderItem>,
    val timestamp: Long = System.currentTimeMillis(),
    val status: OrderStatus = OrderStatus.PREPARING,
    val destinationType: String?,
    val tableSelection: Set<Int>,
    val clientName: String?,
    val payments: List<SplitPayment>
)

// 2. Enum para os status do pedido
enum class OrderStatus {
    PREPARING,
    READY,
    DELIVERED
}

// 3. O ViewModel que gerencia a lógica da cozinha
class KitchenViewModel : ViewModel() {

    private val db = Firebase.firestore
    private val _allActiveOrders = MutableStateFlow<List<KitchenOrder>>(emptyList())

    // --- FLUXOS PÚBLICOS PARA A UI ---

    val kitchenOrders: StateFlow<List<KitchenOrder>> = _allActiveOrders.map { orders ->
        orders.filter { it.status == OrderStatus.PREPARING }
            .sortedBy { it.timestamp } // Mais antigos primeiro
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val readyOrders: StateFlow<List<KitchenOrder>> = _allActiveOrders.map { orders ->
        orders.filter { it.status == OrderStatus.READY }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val occupiedTables: StateFlow<Set<Int>> = _allActiveOrders.map { orders ->
        orders.filter { it.status != OrderStatus.DELIVERED && it.destinationType == "Local" }
            .flatMap { it.tableSelection }
            .toSet()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val ordersByTable: StateFlow<Map<Int, List<KitchenOrder>>> = _allActiveOrders.map { orders ->
        orders
            .filter { it.destinationType == "Local" && it.status != OrderStatus.DELIVERED }
            .flatMap { order -> order.tableSelection.map { tableNum -> tableNum to order } }
            .groupBy(keySelector = { it.first }, valueTransform = { it.second })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val timerFlow: Flow<Long> = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(1000)
        }
    }

    init {
        // --- ESCUTA EM TEMPO REAL DO FIREBASE ---
        db.collection("orders")
            .whereNotEqualTo("status", "DELIVERED") // Otimização: não baixa pedidos entregues
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("KitchenVM", "Erro ao ouvir pedidos", e)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val ordersList = snapshot.documents.mapNotNull { doc ->
                        try {
                            // Conversão manual segura
                            val itemsList = (doc.get("items") as? List<Map<String, Any>>)?.map { mapToOrderItem(it) } ?: emptyList()
                            val tablesList = (doc.get("tableSelection") as? List<Number>)?.map { it.toInt() }?.toSet() ?: emptySet()
                            val paymentsList = (doc.get("payments") as? List<Map<String, Any>>)?.map {
                                SplitPayment(
                                    amount = (it["amount"] as? Number)?.toDouble() ?: 0.0,
                                    method = it["method"] as? String ?: ""
                                )
                            } ?: emptyList()

                            KitchenOrder(
                                id = (doc.get("timestamp") as? Number)?.toLong() ?: 0L,
                                firebaseId = doc.id,
                                items = itemsList,
                                timestamp = (doc.get("timestamp") as? Number)?.toLong() ?: 0L,
                                status = OrderStatus.valueOf(doc.getString("status") ?: "PREPARING"),
                                destinationType = doc.getString("destinationType"),
                                tableSelection = tablesList,
                                clientName = doc.getString("clientName"),
                                payments = paymentsList
                            )
                        } catch (e: Exception) {
                            Log.e("KitchenVM", "Erro ao converter pedido: ${doc.id}", e)
                            null
                        }
                    }
                    _allActiveOrders.value = ordersList
                }
            }
    }

    // --- FUNÇÕES DE AÇÃO (ENVIA PARA O FIREBASE) ---

    fun submitNewOrder(
        items: List<OrderItem>,
        destinationType: String?,
        tableSelection: Set<Int>,
        clientName: String?,
        payments: List<SplitPayment>
    ) {
        if (items.isEmpty()) return

        val timestamp = System.currentTimeMillis()

        val orderMap = hashMapOf(
            "timestamp" to timestamp,
            "status" to OrderStatus.PREPARING.name,
            "destinationType" to destinationType,
            "tableSelection" to tableSelection.toList(), // Firebase prefere List a Set
            "clientName" to clientName,
            "items" to items.map { orderItemToMap(it) },
            "payments" to payments.map { mapOf("amount" to it.amount, "method" to it.method) }
        )

        db.collection("orders").add(orderMap)
            .addOnFailureListener { e -> Log.e("KitchenVM", "Erro ao salvar pedido", e) }
    }

    fun addItemsToTableOrder(
        tableNumber: Int,
        newItems: List<OrderItem>
    ) {
        if (newItems.isEmpty()) return

        // Procura se já tem um pedido aberto dessa mesa no Firebase
        val existingOrder = _allActiveOrders.value.find {
            it.tableSelection.contains(tableNumber) && it.status != OrderStatus.DELIVERED
        }

        if (existingOrder != null) {
            // Se já existe, adiciona os novos itens ao pedido existente
            val currentItemsMaps = existingOrder.items.map { orderItemToMap(it) }
            val newItemsMaps = newItems.map { orderItemToMap(it) }
            val allItems = currentItemsMaps + newItemsMaps

            db.collection("orders").document(existingOrder.firebaseId)
                .update(
                    mapOf(
                        "items" to allItems,
                        "status" to OrderStatus.PREPARING.name // Volta para preparando se adicionar item
                    )
                )
        } else {
            // Se não achar (erro de sincronia?), cria novo
            submitNewOrder(newItems, "Local", setOf(tableNumber), "Mesa $tableNumber", emptyList())
        }
    }

    fun updateOrderStatus(numericId: Long, newStatus: OrderStatus) {
        // Encontra o ID real do documento (String) usando o ID numérico (timestamp)
        val order = _allActiveOrders.value.find { it.id == numericId } ?: return

        db.collection("orders").document(order.firebaseId)
            .update("status", newStatus.name)
    }

    // --- HELPER FUNCTIONS (MAPERS) ---
    // Convertem seus objetos Kotlin para Mapas que o Firebase entende bem

    private fun orderItemToMap(item: OrderItem): Map<String, Any?> {
        return mapOf(
            "menuItemId" to item.menuItem.id,
            "menuItemName" to item.menuItem.name,
            "menuItemPrice" to item.menuItem.price,
            "menuItemImage" to item.menuItem.imageUrl,
            "quantity" to item.quantity,
            "removedIngredients" to item.removedIngredients,
            "additionalIngredients" to item.additionalIngredients, // Map<String, Int> salva direto
            "meatDoneness" to item.meatDoneness,
            "observations" to item.observations,
            "singleItemTotalPrice" to item.singleItemTotalPrice
        )
    }

    private fun mapToOrderItem(data: Map<String, Any>): OrderItem {
        return OrderItem(
            menuItem = MenuItem(
                id = data["menuItemId"] as? String ?: "",
                name = data["menuItemName"] as? String ?: "",
                price = (data["menuItemPrice"] as? Number)?.toDouble() ?: 0.0,
                imageUrl = data["menuItemImage"] as? String ?: ""
            ),
            quantity = (data["quantity"] as? Number)?.toInt() ?: 1,
            removedIngredients = (data["removedIngredients"] as? List<String>) ?: emptyList(),
            additionalIngredients = (data["additionalIngredients"] as? Map<String, Int>) ?: emptyMap(),
            meatDoneness = data["meatDoneness"] as? String,
            observations = data["observations"] as? String,
            singleItemTotalPrice = (data["singleItemTotalPrice"] as? Number)?.toDouble() ?: 0.0
        )
    }
}