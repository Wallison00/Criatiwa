package com.walli.flexcriatiwa

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CounterScreen(kitchenViewModel: KitchenViewModel) {
    val readyOrders by kitchenViewModel.readyOrders.collectAsState(initial = emptyList())

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Balcão de Entrega", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("Pedidos Prontos aguardando retirada", fontSize = 14.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(16.dp))

        if (readyOrders.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Nenhum pedido pronto para entrega.")
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(readyOrders) { order ->
                    CounterOrderCard(
                        order = order,
                        onDeliver = {
                            kitchenViewModel.updateOrderStatus(order.id, OrderStatus.DELIVERED)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun CounterOrderCard(order: KitchenOrder, onDeliver: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val title = if (order.destinationType == "Local")
                    "MESA ${order.tableSelection.sorted().joinToString(", ")}"
                else
                    "SENHA: ${order.clientName ?: "Anon"}"

                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color(0xFF2E7D32))
                Text("#${order.id.toString().takeLast(4)}", color = Color.Gray)
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Text("Itens:", fontWeight = FontWeight.Bold)
            order.items.forEach { item ->
                Text("- ${item.quantity}x ${item.menuItem.name}")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onDeliver,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("ENTREGAR PEDIDO")
            }
        }
    }
}