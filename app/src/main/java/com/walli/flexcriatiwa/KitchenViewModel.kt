// Conteúdo COMPLETO para o arquivo KitchenViewModel.kt

package com.walli.flexcriatiwa

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*

// 1. Modelo de dados para um pedido enviado à cozinha
data class KitchenOrder(
    val id: Long = System.currentTimeMillis(),
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

    // Fluxo principal que guarda TODOS os pedidos ativos
    private val _allActiveOrders = MutableStateFlow<List<KitchenOrder>>(emptyList())

    // --- FLUXOS PÚBLICOS PARA A UI ---

    // Filtra apenas os pedidos que estão em preparo ou prontos (visíveis na Cozinha)
    val kitchenOrders: StateFlow<List<KitchenOrder>> = _allActiveOrders.map { orders ->
        // Agora filtra APENAS os pedidos que estão em preparo
        orders.filter { it.status == OrderStatus.PREPARING }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtra apenas os pedidos que estão PRONTOS (visíveis no Balcão)
    val readyOrders: StateFlow<List<KitchenOrder>> = _allActiveOrders.map { orders ->
        orders.filter { it.status == OrderStatus.READY }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Expõe as mesas ocupadas
    val occupiedTables: StateFlow<Set<Int>> = _allActiveOrders.map { orders ->
        orders.filter { it.status != OrderStatus.DELIVERED && it.destinationType == "Local" }
            .flatMap { it.tableSelection }
            .toSet()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val ordersByTable: StateFlow<Map<Int, List<KitchenOrder>>> = _allActiveOrders.map { orders ->
        orders
            .filter { it.destinationType == "Local" && it.status != OrderStatus.DELIVERED }
            .flatMap { order -> order.tableSelection.map { tableNum -> tableNum to order } } // Cria pares (mesa, pedido)
            .groupBy(keySelector = { it.first }, valueTransform = { it.second }) // Agrupa por mesa
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // Timer para atualizar os cronômetros
    val timerFlow: Flow<Long> = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(1000)
        }
    }

    // --- FUNÇÕES PÚBLICAS PARA MANIPULAR O ESTADO ---

    fun submitNewOrder(
        items: List<OrderItem>,
        destinationType: String?,
        tableSelection: Set<Int>,
        clientName: String?,
        payments: List<SplitPayment>
    ) {
        if (items.isEmpty()) return // Regra de negócio: não envia pedido vazio

        val newOrder = KitchenOrder(
            items = items,
            destinationType = destinationType,
            tableSelection = tableSelection,
            clientName = clientName,
            payments = payments
        )
        _allActiveOrders.value += newOrder
    }

    fun addItemsToTableOrder(
        tableNumber: Int,
        newItems: List<OrderItem>
    ) {
        if (newItems.isEmpty()) return // Não faz nada se não houver itens novos

        // Encontra um pedido representativo da mesa para pegar os detalhes (nome do cliente, etc.)
        val representativeOrder = _allActiveOrders.value.find { it.tableSelection.contains(tableNumber) }

        // Cria um NOVO KitchenOrder contendo APENAS os novos itens.
        val newKitchenOrder = KitchenOrder(
            items = newItems,
            destinationType = "Local",
            tableSelection = setOf(tableNumber),
            clientName = representativeOrder?.clientName, // Reutiliza o nome do cliente se houver
            payments = emptyList() // Novos itens ainda não têm pagamento
        )
        // Adiciona este novo "pacote" de itens à lista principal de pedidos.
        _allActiveOrders.value += newKitchenOrder
    }

    fun updateOrderStatus(orderId: Long, newStatus: OrderStatus) {
        // Se o pedido foi entregue, removemos da lista. Senão, apenas atualizamos o status.
        if (newStatus == OrderStatus.DELIVERED) {
            _allActiveOrders.value = _allActiveOrders.value.filterNot { it.id == orderId }
        } else {
            _allActiveOrders.value = _allActiveOrders.value.map {
                if (it.id == orderId) it.copy(status = newStatus) else it
            }
        }
    }
}
