package com.walli.flexcriatiwa

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class KitchenViewModel : ViewModel() {

    private val db = Firebase.firestore

    private var currentCompanyId: String? = null
    private var ordersListener: ListenerRegistration? = null

    private val _allActiveOrders = MutableStateFlow<List<KitchenOrder>>(emptyList())

    // Filtra pedidos para a tela da cozinha (Apenas PREPARING)
    val kitchenOrders: StateFlow<List<KitchenOrder>> = _allActiveOrders.map { orders ->
        orders.filter { it.status == OrderStatus.PREPARING }
            .sortedBy { it.timestamp } // Ordena por chegada (FIFO)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtra pedidos prontos
    val readyOrders: StateFlow<List<KitchenOrder>> = _allActiveOrders.map { orders ->
        orders.filter { it.status == OrderStatus.READY }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Mesas ocupadas (qualquer pedido não finalizado)
    val occupiedTables: StateFlow<Set<Int>> = _allActiveOrders.map { orders ->
        orders.filter { it.status != OrderStatus.FINISHED && it.destinationType == "Local" }
            .flatMap { it.tableSelection }
            .toSet()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    // Agrupa pedidos por mesa para a tela do garçom
    val ordersByTable: StateFlow<Map<Int, List<KitchenOrder>>> = _allActiveOrders.map { orders ->
        orders.filter { it.destinationType == "Local" && it.status != OrderStatus.FINISHED }
            .flatMap { order -> order.tableSelection.map { tableNum -> tableNum to order } }
            .groupBy(keySelector = { it.first }, valueTransform = { it.second })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun updateCompanyContext(companyId: String) {
        if (currentCompanyId == companyId) return
        currentCompanyId = companyId
        ordersListener?.remove()

        ordersListener = db.collection("companies").document(companyId).collection("orders")
            .whereNotEqualTo("status", "FINISHED")
            .addSnapshotListener { snapshot, e ->
                if (snapshot != null) {
                    _allActiveOrders.value = snapshot.documents.mapNotNull { doc ->
                        try {
                            val items = (doc.get("items") as? List<Map<String, Any>>)?.map { mapToOrderItem(it) } ?: emptyList()
                            val tables = (doc.get("tableSelection") as? List<Number>)?.map { it.toInt() }?.toSet() ?: emptySet()
                            val payments = (doc.get("payments") as? List<Map<String, Any>>)?.map { SplitPayment((it["amount"] as? Number)?.toDouble() ?: 0.0, it["method"] as? String ?: "") } ?: emptyList()
                            val closingNote = doc.getString("closingNote")

                            KitchenOrder(
                                id = (doc.get("timestamp") as? Number)?.toLong() ?: 0L,
                                firebaseId = doc.id, items = items,
                                timestamp = (doc.get("timestamp") as? Number)?.toLong() ?: 0L,
                                status = OrderStatus.valueOf(doc.getString("status") ?: "PREPARING"),
                                destinationType = doc.getString("destinationType"),
                                tableSelection = tables, clientName = doc.getString("clientName"),
                                payments = payments,
                                closingNote = closingNote
                            )
                        } catch (e: Exception) { null }
                    }
                }
            }
    }

    fun submitNewOrder(items: List<OrderItem>, destinationType: String?, tableSelection: Set<Int>, clientName: String?, payments: List<SplitPayment>) {
        val companyId = currentCompanyId ?: return
        if (items.isEmpty()) return
        val data = hashMapOf(
            "timestamp" to System.currentTimeMillis(), "status" to OrderStatus.PREPARING.name,
            "destinationType" to destinationType, "tableSelection" to tableSelection.toList(), "clientName" to clientName,
            "items" to items.map { orderItemToMap(it) },
            "payments" to payments.map { mapOf("amount" to it.amount, "method" to it.method) },
            "closingNote" to null
        )
        db.collection("companies").document(companyId).collection("orders").add(data)
    }

    // --- CORREÇÃO PRINCIPAL AQUI ---
    fun addItemsToTableOrder(tableNumber: Int, newItems: List<OrderItem>) {
        // Removemos a lógica de buscar 'existingOrder' para não furar fila na cozinha.
        // Cada envio da mesa gera um ticket novo.
        submitNewOrder(
            items = newItems,
            destinationType = "Local",
            tableSelection = setOf(tableNumber),
            clientName = "Mesa $tableNumber",
            payments = emptyList()
        )
    }

    fun updateOrderStatus(numericId: Long, newStatus: OrderStatus) {
        val companyId = currentCompanyId ?: return
        val order = _allActiveOrders.value.find { it.id == numericId } ?: return
        db.collection("companies").document(companyId).collection("orders").document(order.firebaseId).update("status", newStatus.name)
    }

    fun closeBillWithDetails(orders: List<KitchenOrder>, newPayments: List<SplitPayment>, note: String?) {
        val companyId = currentCompanyId ?: return
        viewModelScope.launch {
            orders.forEach { order ->
                if (order.status != OrderStatus.FINISHED) {
                    val updateData = mutableMapOf<String, Any>("status" to OrderStatus.NEEDS_CLEANING.name)
                    if (!note.isNullOrBlank()) updateData["closingNote"] = note

                    if (newPayments.isNotEmpty()) {
                        val currentPaymentsMaps = order.payments.map { mapOf("amount" to it.amount, "method" to it.method) }
                        val newPaymentsMaps = newPayments.map { mapOf("amount" to it.amount, "method" to it.method) }
                        updateData["payments"] = currentPaymentsMaps + newPaymentsMaps
                    }
                    db.collection("companies").document(companyId).collection("orders").document(order.firebaseId).update(updateData)
                }
            }
        }
    }

    fun finishAndCleanTable(tableNumber: Int) {
        val companyId = currentCompanyId ?: return
        val orders = _allActiveOrders.value.filter { it.tableSelection.contains(tableNumber) && it.status == OrderStatus.NEEDS_CLEANING }
        orders.forEach { db.collection("companies").document(companyId).collection("orders").document(it.firebaseId).update("status", OrderStatus.FINISHED.name) }
    }

    private fun orderItemToMap(item: OrderItem): Map<String, Any?> = mapOf(
        "menuItemId" to item.menuItem.id, "menuItemName" to item.menuItem.name, "menuItemPrice" to item.menuItem.price,
        "menuItemImage" to item.menuItem.imageUrl, "quantity" to item.quantity, "removedIngredients" to item.removedIngredients,
        "additionalIngredients" to item.additionalIngredients, "meatDoneness" to item.meatDoneness, "observations" to item.observations,
        "singleItemTotalPrice" to item.singleItemTotalPrice
    )

    private fun mapToOrderItem(data: Map<String, Any>): OrderItem {
        @Suppress("UNCHECKED_CAST") val removed = (data["removedIngredients"] as? List<String>) ?: emptyList()
        @Suppress("UNCHECKED_CAST") val additional = (data["additionalIngredients"] as? Map<String, Int>) ?: emptyMap()
        return OrderItem(
            menuItem = MenuItem(data["menuItemId"] as? String ?: "", name = data["menuItemName"] as? String ?: "", price = (data["menuItemPrice"] as? Number)?.toDouble() ?: 0.0, imageUrl = data["menuItemImage"] as? String ?: ""),
            quantity = (data["quantity"] as? Number)?.toInt() ?: 1, removedIngredients = removed, additionalIngredients = additional,
            meatDoneness = data["meatDoneness"] as? String, observations = data["observations"] as? String, singleItemTotalPrice = (data["singleItemTotalPrice"] as? Number)?.toDouble() ?: 0.0
        )
    }
}