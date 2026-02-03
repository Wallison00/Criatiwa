package com.walli.flexcriatiwa

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CounterScreen(
    kitchenViewModel: KitchenViewModel,
    onOpenDrawer: () -> Unit
) {
    val readyOrders by kitchenViewModel.readyOrders.collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Balcão", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp)) {
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
}

@Composable
fun CounterOrderCard(order: KitchenOrder, onDeliver: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)), // Verde claro
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

                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = Color(0xFF2E7D32)
                )
                Text("#${order.id.toString().takeLast(4)}", color = Color.Gray)
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Text("Itens:", fontWeight = FontWeight.Bold)

            // --- LOOP DETALHADO DOS ITENS (Igual à Cozinha) ---
            order.items.forEach { item ->
                // Linha Principal (Qtd + Nome)
                Text("- ${item.quantity}x ${item.menuItem.name}", fontWeight = FontWeight.SemiBold)

                // Detalhes (Pequenos e coloridos)
                if (item.removedIngredients.isNotEmpty()) {
                    Text("   Sem: ${item.removedIngredients.joinToString(", ")}", color = Color.Red, fontSize = 12.sp)
                }

                if (item.additionalIngredients.isNotEmpty()) {
                    val addText = item.additionalIngredients.map { (name, qtd) ->
                        if(qtd > 1) "$name(x$qtd)" else name
                    }.joinToString(", ")
                    Text("   Add: $addText", color = Color(0xFF388E3C), fontSize = 12.sp) // Verde escuro
                }

                item.meatDoneness?.let {
                    Text("   Ponto: $it", color = Color.Blue, fontSize = 12.sp)
                }

                item.observations?.takeIf { it.isNotBlank() }?.let {
                    Text("   Obs: $it", fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, fontSize = 12.sp, color = Color.DarkGray)
                }

                // Espaço entre itens
                Spacer(modifier = Modifier.height(4.dp))
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