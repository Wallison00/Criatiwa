package com.walli.flexcriatiwa


import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TableScreen(
    onOpenDrawer: () -> Unit,
    kitchenViewModel: KitchenViewModel, // Reutilizamos o mesmo ViewModel
    navController: NavController
) {
    // 1. Coleta a nova lista de pedidos agrupados por mesa
    val ordersByTable by kitchenViewModel.ordersByTable.collectAsState(initial = emptyMap())
    val currentTime by kitchenViewModel.timerFlow.collectAsState(initial = System.currentTimeMillis())

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Mesas", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "Abrir Menu")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (ordersByTable.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("Nenhuma mesa com pedidos ativos.")
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2), // Duas colunas para os cards das mesas
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Itera sobre o mapa de mesas e seus pedidos
                items(ordersByTable.entries.toList(), key = { it.key }) { (tableNumber, tableOrders) ->
                    TableOrderCard(
                        tableNumber = tableNumber,
                        orders = tableOrders,
                        currentTime = currentTime,
                        onClick = {
                            navController.navigate("order_summary/$tableNumber")
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TableOrderCard(
    tableNumber: Int,
    orders: List<KitchenOrder>,
    currentTime: Long,
    onClick: () -> Unit
) {
    // Calcula o valor total e o tempo do primeiro pedido da mesa
    val totalValue = orders.sumOf { order -> order.items.sumOf { it.singleItemTotalPrice * it.quantity } }
    val firstOrderTimestamp = orders.minOfOrNull { it.timestamp } ?: currentTime
    val elapsedSeconds = (currentTime - firstOrderTimestamp) / 1000
    val minutes = elapsedSeconds / 60
    val seconds = elapsedSeconds % 60
    val timeString = "%02d:%02d".format(minutes, seconds)

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Mesa $tableNumber",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "R$ ${"%.2f".format(totalValue)}",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Aberta há $timeString",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
