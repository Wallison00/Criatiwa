package com.walli.flexcriatiwa

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
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

    var showCleaningDialogForTable by remember { mutableStateOf<Int?>(null) }

    if (showCleaningDialogForTable != null) {
        AlertDialog(
            onDismissRequest = { showCleaningDialogForTable = null },
            title = { Text("Liberar Mesa ${showCleaningDialogForTable}?") },
            text = { Text("Confirmar que a mesa foi limpa e está pronta para o próximo cliente.") },
            confirmButton = {
                Button(onClick = {
                    kitchenViewModel.finishAndCleanTable(showCleaningDialogForTable!!)
                    showCleaningDialogForTable = null
                }) {
                    Text("Confirmar Limpeza")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCleaningDialogForTable = null }) { Text("Cancelar") }
            }
        )
    }

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
            // Legenda
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                StatusLegend(Color(0xFFE3F2FD), "Livre")
                StatusLegend(Color(0xFFFFEBEE), "Prep")
                StatusLegend(Color(0xFFE8EAF6), "Comendo")
                StatusLegend(Color(0xFFFFF9C4), "Limpeza")
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 100.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(tables) { tableNum ->
                    val tableOrders = ordersByTable[tableNum] ?: emptyList()
                    val isOccupied = tableOrders.isNotEmpty()

                    // Define o estado visual da mesa
                    val tableStatus = when {
                        !isOccupied -> "FREE"
                        tableOrders.any { it.status == OrderStatus.NEEDS_CLEANING } -> "CLEANING"
                        tableOrders.any { it.status == OrderStatus.DELIVERED } -> "CONSUMING"
                        else -> "PREPARING" // Preparing ou Ready
                    }

                    val totalBill = tableOrders.sumOf { order ->
                        order.items.sumOf { item -> item.singleItemTotalPrice * item.quantity }
                    }

                    TableCard(
                        tableNumber = tableNum,
                        status = tableStatus,
                        totalBill = totalBill,
                        modifier = Modifier.clickable {
                            if (tableStatus == "CLEANING") {
                                showCleaningDialogForTable = tableNum
                            } else {
                                onTableClick(tableNum)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun StatusLegend(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(modifier = Modifier.size(12.dp), color = color, shape = MaterialTheme.shapes.small) {}
        Spacer(modifier = Modifier.width(4.dp))
        Text(text, fontSize = 12.sp)
    }
}

@Composable
fun TableCard(tableNumber: Int, status: String, totalBill: Double, modifier: Modifier = Modifier) {
    val bgColor = when (status) {
        "FREE" -> Color(0xFFE3F2FD)      // Azul Bebê (Livre)
        "PREPARING" -> Color(0xFFFFEBEE) // Vermelho Claro (Ocupada/Cozinha)
        "CONSUMING" -> Color(0xFFE8EAF6) // Indigo Claro (Comendo)
        "CLEANING" -> Color(0xFFFFF9C4)  // Amarelo (Limpeza)
        else -> Color.White
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = bgColor),
        modifier = modifier.height(100.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Mesa $tableNumber", fontWeight = FontWeight.Bold, fontSize = 18.sp)

            when (status) {
                "FREE" -> Text("Livre", color = Color.Gray, fontSize = 12.sp)
                "CLEANING" -> {
                    Icon(Icons.Default.CleaningServices, contentDescription = null, tint = Color(0xFFFBC02D))
                    Text("Limpeza", color = Color(0xFFFBC02D), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                else -> {
                    val statusText = if (status == "CONSUMING") "Consumindo" else "Aguardando"
                    Text(statusText, color = if(status=="CONSUMING") Color(0xFF3F51B5) else Color.Red, fontSize = 12.sp)
                    Text("R$ %.2f".format(totalBill), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}