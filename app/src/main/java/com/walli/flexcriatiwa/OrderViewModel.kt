package com.walli.flexcriatiwa

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

import java.util.UUID // <--- ADICIONE ESTA LINHA

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

    // --- INTEGRAÇÃO MERCADO PAGO POINT ---
    // --- INTEGRAÇÃO MERCADO PAGO POINT ---
    // --- INTEGRAÇÃO MERCADO PAGO POINT ---
    // --- NOVA INTEGRAÇÃO MERCADO PAGO POINT (PADRÃO ORDERS) ---
    fun enviarPagamentoParaPoint(token: String, deviceId: String, valorTotal: Double, pedidoId: String) {

        //cancelarPedidoPendente(token, "ORD01KJ096WD59MCZNY542V43HRYX")

        // 1. Definições de limpeza e autenticação
        val cleanToken = token.trim().replace("\n", "").replace("\r", "")
        val authToken = if (cleanToken.startsWith("Bearer ")) cleanToken else "Bearer $cleanToken"
        val cleanDeviceId = deviceId.trim()
        val cleanExternalRef = pedidoId.replace(Regex("[^A-Za-z0-9-_]"), "")
        val valorFormatado = "%.2f".format(valorTotal).replace(",", ".")
        val idempotencyKey = UUID.randomUUID().toString()

        Log.d("MercadoPago", "Criando Order para Terminal: $cleanDeviceId")

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val orderRequest = OrderRequest(
                    external_reference = cleanExternalRef,
                    description = "Pedido $cleanExternalRef",
                    transactions = OrderTransactions(
                        payments = listOf(OrderPayment(amount = valorFormatado))
                    ),
                    config = OrderConfig(
                        point = PointConfig(
                            terminal_id = cleanDeviceId,
                            print_on_terminal = "no_ticket"
                        )
                    )
                )

                val retrofit = Retrofit.Builder()
                    .baseUrl("https://api.mercadopago.com/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()

                val api = retrofit.create(MercadoPagoApiService::class.java)

                // Agora o authToken será reconhecido corretamente
                val response = api.createOrder(
                    token = authToken,
                    idempotencyKey = idempotencyKey,
                    request = orderRequest
                )

                if (response.isSuccessful) {
                    val orderId = response.body()?.id ?: ""
                    Log.d("MercadoPago", "SUCESSO! Order ID: $orderId")

                    if (orderId.isNotEmpty()) {
                        Log.d("MercadoPago", "Aguardando 5 segundos para simular o cliente pagando na máquina...")

                        // Aguarda 5 segundos (5000 milissegundos)
                        kotlinx.coroutines.delay(5000)

                        // SIMULAÇÃO LOCAL: Aciona o seu fluxo de sucesso direto no app!
                        clearAll()
                        Log.d("MercadoPago", "Pagamento concluído! Carrinho esvaziado com sucesso.")
                    }
                } else {
                    Log.e("MercadoPago", "ERRO API: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e("MercadoPago", "FALHA: ${e.message}")
            }
        }
    }

    // No OrderViewModel.kt

    fun testarConexaoComPoint(token: String) {
        val cleanToken = token.trim().replace("\n", "").replace("\r", "")
        val authToken = if (cleanToken.startsWith("Bearer ")) cleanToken else "Bearer $cleanToken"

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val retrofit = Retrofit.Builder()
                    .baseUrl("https://api.mercadopago.com/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()

                val api = retrofit.create(MercadoPagoApiService::class.java)

                Log.d("MercadoPago", "--- BUSCANDO O TERMINAL ID EXATO ---")
                val response = api.getTerminals(authToken)

                if (response.isSuccessful) {
                    // Isso vai imprimir no Logcat o ID exato que o MP quer que você use!
                    Log.d("MercadoPago", "LISTA DE TERMINAIS: ${response.body()}")
                } else {
                    Log.e("MercadoPago", "ERRO NA LISTA: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e("MercadoPago", "FALHA: ${e.message}")
            }
        }
    }

    // --- FUNÇÕES DE LIMPEZA (Mantenha as suas) ---
    fun clearAll() {
        currentCartItems = emptyList()
        destinationType = null
        tableSelection = emptySet()
        clientName = null
        payments = emptyList()
        itemToEdit = null
    }

    // --- FUNÇÃO PARA LIMPAR A FILA DA MÁQUINA ---
    fun cancelarPedidoPendente(token: String, orderId: String) {
        val cleanToken = token.trim().replace("\n", "").replace("\r", "")
        val authToken = if (cleanToken.startsWith("Bearer ")) cleanToken else "Bearer $cleanToken"

        // Gera uma nova chave única para a ação de cancelamento
        val idempotencyKey = UUID.randomUUID().toString()

        Log.d("MercadoPago", "Tentando cancelar a Order: $orderId")

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val retrofit = Retrofit.Builder()
                    .baseUrl("https://api.mercadopago.com/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()

                val api = retrofit.create(MercadoPagoApiService::class.java)

                val response = api.cancelOrder(
                    token = authToken,
                    idempotencyKey = idempotencyKey,
                    orderId = orderId
                )

                if (response.isSuccessful) {
                    Log.d("MercadoPago", "SUCESSO! O pedido $orderId foi cancelado e a fila está livre.")
                } else {
                    Log.e("MercadoPago", "ERRO AO CANCELAR: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e("MercadoPago", "FALHA DE REDE AO CANCELAR: ${e.message}")
            }
        }
    }

    // --- FUNÇÃO PARA SIMULAR PAGAMENTO (AMBIENTE DE TESTE) ---
    fun simularPagamentoAprovado(token: String, orderId: String) {
        val cleanToken = token.trim().replace("\n", "").replace("\r", "")
        val authToken = if (cleanToken.startsWith("Bearer ")) cleanToken else "Bearer $cleanToken"

        Log.d("MercadoPago", "Simulando aprovação de pagamento para a Order: $orderId")

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val retrofit = Retrofit.Builder()
                    .baseUrl("https://api.mercadopago.com/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()

                val api = retrofit.create(MercadoPagoApiService::class.java)

                // Dispara a simulação com os dados de cartão master padrão
                val response = api.simulateOrderPayment(
                    token = authToken,
                    orderId = orderId,
                    request = SimulatePaymentRequest()
                )

                // A documentação diz que o sucesso é o status 204 [cite: 18]
                if (response.isSuccessful || response.code() == 204) {
                    Log.d("MercadoPago", "SIMULAÇÃO 204: O pagamento foi aprovado na nuvem!")

                    // ==========================================
                    // AQUI ACONTECE A MÁGICA DO SEU APP!
                    // Como o pagamento "deu certo", limpamos o carrinho:
                    clearAll()
                    Log.d("MercadoPago", "Carrinho esvaziado. Pronto para a próxima venda.")
                    // ==========================================

                } else {
                    Log.e("MercadoPago", "ERRO NA SIMULAÇÃO: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e("MercadoPago", "FALHA DE REDE NA SIMULAÇÃO: ${e.message}")
            }
        }
    }
}