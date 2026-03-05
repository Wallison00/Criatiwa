package com.walli.flexcriatiwa

import retrofit2.Response
import retrofit2.http.*

data class OrderRequest(
    val type: String = "point",
    val external_reference: String,
    val description: String,
    // Removido o amount daqui [cite: 36]
    val transactions: OrderTransactions,
    val config: OrderConfig
)

data class OrderTransactions(
    val payments: List<OrderPayment>
)

data class OrderPayment(
    val amount: String // Mudado para String
    // Removido o description daqui [cite: 36]
)

data class OrderConfig(
    val point: PointConfig
)

data class PointConfig(
    val terminal_id: String,
    val print_on_terminal: String = "no_ticket" // Mudado para String
)

// Classe para receber a resposta do Mercado Pago
data class OrderResponse(
    val id: String,
    val status: String,
    val external_reference: String
)

// Modelo para simular o pagamento aprovado
data class SimulatePaymentRequest(
    val status: String = "processed", // Força o status de "pago com sucesso"
    val payment_method_type: String = "credit_card",
    val installments: Int = 1,
    val payment_method_id: String = "master",
    val status_detail: String = "accredited"
)

interface MercadoPagoApiService {
    @POST("v1/orders")
    suspend fun createOrder(
        @Header("Authorization") token: String,
        @Header("X-Idempotency-Key") idempotencyKey: String,
        @Body request: OrderRequest
    ): Response<OrderResponse>

    // O Endpoint correto que lista suas máquinas cadastradas
    @GET("terminals/v1/list")
    suspend fun getTerminals(
        @Header("Authorization") token: String
    ): Response<Any>

    // NOVA ROTA: Cancelar um pedido pendente na máquina
    @POST("v1/orders/{orderId}/cancel")
    suspend fun cancelOrder(
        @Header("Authorization") token: String,
        @Header("X-Idempotency-Key") idempotencyKey: String,
        @Path("orderId") orderId: String
    ): Response<Any> // Usando Any ou um modelo de resposta simples

    // NOVA ROTA: Simular o processamento do pagamento
    @POST("v1/orders/{order_id}/events")
    suspend fun simulateOrderPayment(
        @Header("Authorization") token: String,
        @Path("order_id") orderId: String,
    @Body request: SimulatePaymentRequest
    ): Response<Void> // Void porque a API retorna 204 sem corpo [cite: 18, 29]
}
