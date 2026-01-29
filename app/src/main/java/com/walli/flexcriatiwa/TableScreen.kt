package com.walli.flexcriatiwa

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TableScreen(
    kitchenViewModel: KitchenViewModel,
    onTableClick: (Int) -> Unit = {} // <--- Adicione este parâmetro se não tiver
) {
    // Mapa de Mesa -> Lista de Pedidos
    val ordersByTable by kitchenViewModel.ordersByTable.collectAsState()

    // Lista de mesas (simulação de 1 a 20)
    val tables = (1..20).toList()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Mapa de Mesas", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 100.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(tables) { tableNum ->
                val tableOrders = ordersByTable[tableNum] ?: emptyList()
                val isOccupied = tableOrders.isNotEmpty()

                // Calcula total da conta da mesa
                val totalBill = tableOrders.sumOf { order ->
                    order.items.sumOf { item -> item.singleItemTotalPrice * item.quantity }
                }

                TableCard(
                    tableNumber = tableNum,
                    isOccupied = isOccupied,
                    totalBill = totalBill
                )
            }
        }
    }
}

@Composable
fun TableCard(tableNumber: Int, isOccupied: Boolean, totalBill: Double) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isOccupied) Color(0xFFFFEBEE) else Color(0xFFE3F2FD) // Vermelho claro (ocupado) ou Azul claro (livre)
        ),
        modifier = Modifier.height(100.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
        ) {
            Text("Mesa $tableNumber", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            if (isOccupied) {
                Text("Ocupada", color = Color.Red, fontSize = 12.sp)
                Text("R$ %.2f".format(totalBill), fontWeight = FontWeight.Bold)
            } else {
                Text("Livre", color = Color.Gray, fontSize = 12.sp)
            }
        }
    }
}