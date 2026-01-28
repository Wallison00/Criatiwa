// No arquivo: OrderViewModel.kt
package com.walli.flexcriatiwa

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class OrderViewModel : ViewModel() {

    // O carrinho de compras atual.
    var currentCartItems by mutableStateOf<List<OrderItem>>(emptyList())
        private set

    // Detalhes do resumo (destino, pagamento) para o pedido ATUAL.
    var destinationType by mutableStateOf<String?>(null)
    var tableSelection by mutableStateOf<Set<Int>>(emptySet())
    var clientName by mutableStateOf<String?>(null)
    var payments by mutableStateOf<List<SplitPayment>>(emptyList())

    // Item em modo de edição (para a tela de detalhes)
    var itemToEdit by mutableStateOf<OrderItem?>(null)
        private set

    val isOrderEmpty: Boolean
        get() = currentCartItems.isEmpty()

    // --- FUNÇÕES PARA ATUALIZAR OS ESTADOS DO RESUMO ---
    fun updateDestination(newDestinationType: String?, newTables: Set<Int>, newClientName: String?) {
        destinationType = newDestinationType
        tableSelection = newTables
        clientName = newClientName
    }

    fun updatePayments(newPayments: List<SplitPayment>) {
        payments = newPayments
    }

    fun addPayment(newPayment: SplitPayment) {
        payments = payments + newPayment
    }

    fun clearPayments() {
        payments = emptyList()
    }
    // ------------------------------------------------

    // --- FUNÇÕES DE MANIPULAÇÃO DO CARRINHO ---
    fun upsertItem(item: OrderItem, originalItem: OrderItem? = null) {
        val existingItem = originalItem ?: item
        if (currentCartItems.contains(existingItem)) {
            currentCartItems = currentCartItems.map { if (it == existingItem) item else it }
        } else {
            currentCartItems = currentCartItems + item
        }
    }

    fun removeItem(item: OrderItem) {
        currentCartItems = currentCartItems - item
    }

    fun loadItemForEdit(item: OrderItem) {
        itemToEdit = item
    }

    fun clearEdit() {
        itemToEdit = null
    }

    /**
     * Limpa completamente o estado do OrderViewModel.
     * Usado ao cancelar um pedido ou após enviá-lo para a cozinha.
     */
    fun clearAll() {
        currentCartItems = emptyList()
        destinationType = null
        tableSelection = emptySet()
        clientName = null
        payments = emptyList()
        itemToEdit = null
    }
}
