package com.walli.flexcriatiwa

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CounterScreen(
    onOpenDrawer: () -> Unit,
    kitchenViewModel: KitchenViewModel // Usa o mesmo ViewModel da Cozinha
) {
    // 1. Coleta a lista de pedidos PRONTOS
    // Em CounterScreen.kt
    val readyOrders by kitchenViewModel.readyOrders.collectAsState(initial = emptyList())


    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Balcão", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "Abrir Menu")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (readyOrders.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("Nenhum pedido pronto no balcão.")
            }
        } else {
            // Usamos a mesma grade da cozinha para consistência visual
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 300.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(readyOrders, key = { it.id }) { order ->
                    CounterOrderCard(
                        order = order,
                        onDeliver = {
                            // Notifica o ViewModel para marcar o pedido como ENTREGUE
                            kitchenViewModel.updateOrderStatus(order.id, OrderStatus.DELIVERED)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CounterOrderCard(
    order: KitchenOrder,
    onDeliver: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2E2E2E)) // Fundo escuro
    ) {
        Column {
            // Cabeçalho do card (sempre verde, pois o pedido está pronto)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF388E3C)) // Verde de "Pronto"
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val titleText = buildAnnotatedString {
                    val mainTitle = when (order.destinationType) {
                        "Local" -> "Mesa ${order.tableSelection.joinToString()}"
                        else -> "Viagem"
                    }
                    append(mainTitle)
                    if (!order.clientName.isNullOrBlank()) {
                        append(" (${order.clientName})")
                    }
                }
                Text(
                    text = titleText,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium
                )
            }

            // Corpo do Card
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Lista de itens (simplificada, sem detalhes)
                order.items.forEach { item ->
                    Text(
                        text = "${item.quantity} x ${item.menuItem.name}",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                // Botão de Ação para Entregar
                Button(
                    onClick = onDeliver,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Entregar Pedido")
                }
            }
        }
    }
}
