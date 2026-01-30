package com.walli.flexcriatiwa

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
fun TableScreen(
    kitchenViewModel: KitchenViewModel,
    onOpenDrawer: () -> Unit,
    onTableClick: (Int) -> Unit = {}
) {
    val ordersByTable by kitchenViewModel.ordersByTable.collectAsState(initial = emptyMap())
    val tables = (1..20).toList()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Mesas", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp)) {
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

                    val totalBill = tableOrders.sumOf { order ->
                        order.items.sumOf { item -> item.singleItemTotalPrice * item.quantity }
                    }

                    TableCard(
                        tableNumber = tableNum,
                        isOccupied = isOccupied,
                        totalBill = totalBill,
                        modifier = Modifier.clickable { onTableClick(tableNum) }
                    )
                }
            }
        }
    }
}

@Composable
fun TableCard(tableNumber: Int, isOccupied: Boolean, totalBill: Double, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isOccupied) Color(0xFFFFEBEE) else Color(0xFFE3F2FD)
        ),
        modifier = modifier.height(100.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
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