package com.walli.flexcriatiwa

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*

class KitchenViewModel : ViewModel() {

    private val db = Firebase.firestore
    private val _allActiveOrders = MutableStateFlow<List<KitchenOrder>>(emptyList())

    // --- FLUXOS PÚBLICOS ---

    // Cozinha vê apenas PREPARING
    val kitchenOrders: StateFlow<List<KitchenOrder>> = _allActiveOrders.map { orders ->
        orders.filter { it.status == OrderStatus.PREPARING }
            .sortedBy { it.timestamp }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Balcão vê apenas READY
    val readyOrders: StateFlow<List<KitchenOrder>> = _allActiveOrders.map { orders ->
        orders.filter { it.status == OrderStatus.READY }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Mesas ocupadas são todas que NÃO estão FINALIZADAS (inclui Delivered e Needs Cleaning)
    val occupiedTables: StateFlow<Set<Int>> = _allActiveOrders.map { orders ->
        orders.filter { it.status != OrderStatus.FINISHED && it.destinationType == "Local" }
            .flatMap { it.tableSelection }
            .toSet()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    // Mapa de mesas para desenhar a tela de Mesas
    val ordersByTable: StateFlow<Map<Int, List<KitchenOrder>>> = _allActiveOrders.map { orders ->
        orders
            .filter { it.destinationType == "Local" && it.status != OrderStatus.FINISHED }
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
        // Busca tudo que não está "FINISHED" (Livre)
        db.collection("orders")
            .whereNotEqualTo("status", "FINISHED")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("KitchenVM", "Erro ao ouvir pedidos", e)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val ordersList = snapshot.documents.mapNotNull { doc ->
                        try {
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

    // --- AÇÕES ---

    fun submitNewOrder(items: List<OrderItem>, destinationType: String?, tableSelection: Set<Int>, clientName: String?, payments: List<SplitPayment>) {
        if (items.isEmpty()) return
        val timestamp = System.currentTimeMillis()
        val orderMap = hashMapOf(
            "timestamp" to timestamp,
            "status" to OrderStatus.PREPARING.name,
            "destinationType" to destinationType,
            "tableSelection" to tableSelection.toList(),
            "clientName" to clientName,
            "items" to items.map { orderItemToMap(it) },
            "payments" to payments.map { mapOf("amount" to it.amount, "method" to it.method) }
        )
        db.collection("orders").add(orderMap)
    }

    fun addItemsToTableOrder(tableNumber: Int, newItems: List<OrderItem>) {
        if (newItems.isEmpty()) return
        val existingOrder = _allActiveOrders.value.find {
            it.tableSelection.contains(tableNumber) && it.status != OrderStatus.FINISHED
        }

        if (existingOrder != null) {
            val currentItemsMaps = existingOrder.items.map { orderItemToMap(it) }
            val newItemsMaps = newItems.map { orderItemToMap(it) }
            val allItems = currentItemsMaps + newItemsMaps

            // Se a mesa estava consumindo (DELIVERED) e pede mais, volta para PREPARING?
            // Geralmente sim, pois a cozinha precisa fazer.
            // Se estava NEEDS_CLEANING, assumimos que o cliente sentou de novo? Vamos forçar PREPARING.
            db.collection("orders").document(existingOrder.firebaseId)
                .update(mapOf("items" to allItems, "status" to OrderStatus.PREPARING.name))
        } else {
            submitNewOrder(newItems, "Local", setOf(tableNumber), "Mesa $tableNumber", emptyList())
        }
    }

    // Atualiza status (ex: DELIVERED -> NEEDS_CLEANING)
    fun updateOrderStatus(numericId: Long, newStatus: OrderStatus) {
        val order = _allActiveOrders.value.find { it.id == numericId } ?: return
        db.collection("orders").document(order.firebaseId).update("status", newStatus.name)
    }

    // Novo: Limpar mesa (Libera para novos clientes)
    fun finishAndCleanTable(tableNumber: Int) {
        val ordersOnTable = _allActiveOrders.value.filter {
            it.tableSelection.contains(tableNumber) && it.status == OrderStatus.NEEDS_CLEANING
        }
        ordersOnTable.forEach { order ->
            db.collection("orders").document(order.firebaseId).update("status", OrderStatus.FINISHED.name)
        }
    }

    // --- MAPPERS ---
    private fun orderItemToMap(item: OrderItem): Map<String, Any?> {
        return mapOf(
            "menuItemId" to item.menuItem.id,
            "menuItemName" to item.menuItem.name,
            "menuItemPrice" to item.menuItem.price,
            "menuItemImage" to item.menuItem.imageUrl,
            "quantity" to item.quantity,
            "removedIngredients" to item.removedIngredients,
            "additionalIngredients" to item.additionalIngredients,
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