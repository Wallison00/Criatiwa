package com.walli.flexcriatiwa

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
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
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersListScreen(
    kitchenViewModel: KitchenViewModel,
    onOpenDrawer: () -> Unit,
    onOrderClick: (Int?) -> Unit // O 'Int?' refere-se opcionalmente à mesa do pedido se quisermos abri-lo
) {
    val allActiveOrders by kitchenViewModel.allActiveOrders.collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Todos os Pedidos", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp)) {
            Text("Gestão Geral de Pedidos", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("Listagem de todos os pedidos ativos (Mesa, Viagem e Balcão)", fontSize = 14.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(16.dp))

            if (allActiveOrders.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Nenhum pedido ativo no momento.")
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(allActiveOrders.sortedByDescending { it.timestamp }) { order ->
                        GeneralOrderCard(
                            order = order,
                            onClick = {
                                // Se for local e tem mesa, a gente manda o tableSelection pra abrir a mesa certa
                                // Se for viagem mandamos null ou algo para abrir o resumo viagem.
                                val tableId = order.tableSelection.firstOrNull()
                                onOrderClick(tableId)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GeneralOrderCard(order: KitchenOrder, onClick: () -> Unit) {
    val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    val timeStr = formatter.format(Date(order.timestamp))

    val statusColor = when (order.status) {
        OrderStatus.PREPARING -> Color(0xFFFFA500) // Laranja
        OrderStatus.READY -> Color(0xFF4CAF50) // Verde
        OrderStatus.DELIVERED -> Color.Gray
        OrderStatus.NEEDS_CLEANING -> Color.Red
        OrderStatus.FINISHED -> Color.Black
    }
    
    val statusText = when (order.status) {
        OrderStatus.PREPARING -> "Preparando"
        OrderStatus.READY -> "Pronto!"
        OrderStatus.DELIVERED -> "Entregue"
        OrderStatus.NEEDS_CLEANING -> "Limpeza necess."
        OrderStatus.FINISHED -> "Finalizado"
    }

    val nomeDestino = order.tableSelection.firstOrNull()?.let { "Mesa $it" }
        ?: order.clientName?.takeIf { it.isNotBlank() }
        ?: order.destinationType 
        ?: "Balcão"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = nomeDestino,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
                Text(timeStr, color = Color.Gray, fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.height(8.dp))
            
            Text("Itens: ${order.items.size}", style = MaterialTheme.typography.bodyMedium)
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Surface(
                color = statusColor.copy(alpha = 0.15f),
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = statusText,
                    color = statusColor,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    fontSize = 12.sp
                )
            }
        }
    }
}
