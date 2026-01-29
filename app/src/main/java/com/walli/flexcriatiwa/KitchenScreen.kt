package com.walli.flexcriatiwa

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun KitchenScreen(kitchenViewModel: KitchenViewModel) {
    // CORREÇÃO: Observa 'kitchenOrders' (que é List<KitchenOrder>)
    val orders by kitchenViewModel.kitchenOrders.collectAsState(initial = emptyList())

    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while(true) {
            delay(1000)
            currentTime = System.currentTimeMillis()
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Fila de Preparo", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        if (orders.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Cozinha Livre! Sem pedidos pendentes.")
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(orders) { order ->
                    KitchenOrderCard(
                        order = order,
                        currentTime = currentTime,
                        onAdvanceStatus = {
                            // CORREÇÃO: Usa a função updateOrderStatus com o Enum correto
                            kitchenViewModel.updateOrderStatus(order.id, OrderStatus.READY)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun KitchenOrderCard(
    order: KitchenOrder, // CORREÇÃO: Recebe KitchenOrder
    currentTime: Long,
    onAdvanceStatus: () -> Unit
) {
    val statusColor = when (order.status) {
        OrderStatus.PREPARING -> Color(0xFF2196F3) // Azul
        else -> Color.Gray
    }

    val elapsedTimeMillis = currentTime - order.timestamp
    val elapsedSeconds = elapsedTimeMillis / 1000
    val minutes = elapsedSeconds / 60
    val seconds = elapsedSeconds % 60
    val timeDisplay = "%02d:%02d".format(minutes, seconds)

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    val title = if (order.destinationType == "Local")
                        "Mesa ${order.tableSelection.joinToString(", ")}"
                    else
                        "Viagem: ${order.clientName ?: "Cliente"}"

                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text("ID: #${order.id.toString().takeLast(4)}", fontSize = 12.sp, color = Color.Gray)
                }

                Surface(
                    color = statusColor,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = timeDisplay,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            order.items.forEach { item ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${item.quantity}x ${item.menuItem.name}", fontWeight = FontWeight.Bold)
                }
                if (item.removedIngredients.isNotEmpty()) {
                    Text("   Sem: ${item.removedIngredients.joinToString(", ")}", color = Color.Red, fontSize = 12.sp)
                }
                if (item.additionalIngredients.isNotEmpty()) {
                    val addText = item.additionalIngredients.map { (name, qtd) ->
                        if(qtd > 1) "$name(x$qtd)" else name
                    }.joinToString(", ")
                    Text("   Add: $addText", color = Color(0xFF4CAF50), fontSize = 12.sp)
                }
                item.meatDoneness?.let {
                    Text("   Ponto: $it", color = Color.Blue, fontSize = 12.sp)
                }
                item.observations?.let {
                    if(it.isNotBlank()) Text("   Obs: $it", fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onAdvanceStatus,
                colors = ButtonDefaults.buttonColors(containerColor = statusColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("MARCAR COMO PRONTO")
            }
        }
    }
}