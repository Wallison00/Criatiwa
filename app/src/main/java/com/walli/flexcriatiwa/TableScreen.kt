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
import androidx.compose.foundation.background
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.drawscope.withTransform
import kotlin.math.roundToInt
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.rotate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TableScreen(
    kitchenViewModel: KitchenViewModel,
    onOpenDrawer: () -> Unit,
    onTableClick: (Int) -> Unit = {}
) {
    val ordersByTable by kitchenViewModel.ordersByTable.collectAsState(initial = emptyMap())
    val tableCount by kitchenViewModel.tableCount.collectAsState(initial = 20)
    val tablePositions by kitchenViewModel.tablePositions.collectAsState(initial = emptyMap())
    val tables = (1..tableCount).toList()

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

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF5F5F5))
            ) {
                val density = androidx.compose.ui.platform.LocalDensity.current
                
                tables.forEach { tableNum ->
                    val tableOrders = ordersByTable[tableNum] ?: emptyList()
                    val isOccupied = tableOrders.isNotEmpty()

                    val tableStatus = when {
                        !isOccupied -> "FREE"
                        tableOrders.any { it.status == OrderStatus.NEEDS_CLEANING } -> "CLEANING"
                        tableOrders.any { it.status == OrderStatus.DELIVERED } -> "CONSUMING"
                        else -> "PREPARING"
                    }

                    val totalBill = tableOrders.sumOf { order ->
                        order.items.sumOf { item -> item.singleItemTotalPrice * item.quantity }
                    }

                    val initialPos = tablePositions[tableNum.toString()]
                    
                    val defaultX = with(density) { ((tableNum - 1) % 4 * 90).dp.toPx() }
                    val defaultY = with(density) { ((tableNum - 1) / 4 * 90).dp.toPx() }
                    
                    val finalX = initialPos?.x ?: defaultX
                    val finalY = initialPos?.y ?: defaultY

                    val shape = initialPos?.shape ?: "square"
                    val seats = initialPos?.seats ?: 4
                    val rotation = initialPos?.rotation ?: 0f

                    TableCard(
                        tableNumber = tableNum,
                        status = tableStatus,
                        totalBill = totalBill,
                        shape = shape,
                        seats = seats,
                        rotationAngle = rotation,
                        modifier = Modifier
                            .offset { androidx.compose.ui.unit.IntOffset(finalX.roundToInt(), finalY.roundToInt()) }
                            .clickable {
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
fun TableCard(
    tableNumber: Int, 
    status: String, 
    totalBill: Double, 
    shape: String = "square", 
    seats: Int = 4, 
    rotationAngle: Float = 0f, 
    modifier: Modifier = Modifier
) {
    val bgColor = when (status) {
        "FREE" -> Color(0xFFE3F2FD)      
        "PREPARING" -> Color(0xFFFFEBEE) 
        "CONSUMING" -> Color(0xFFE8EAF6) 
        "CLEANING" -> Color(0xFFFFF9C4)  
        else -> Color.White
    }

    // 1 "Quadrado" agora equivale ao novo mini-grid de 45dp. Uma mesa quadrada usa 2x2.
    // Retangular: se tem 8 cadeiras, ela deve ocupar 8 quadrados no total (altura=2, largura=4).
    val squaresWidth = if (shape == "rectangle") kotlin.math.max(2, (seats + 1) / 2) else 2
    val boxWidth = (squaresWidth * 45 - 10).dp
    val boxHeight = 80.dp

    Box(
        modifier = modifier
            .size(boxWidth, boxHeight)
            .rotate(rotationAngle),
        contentAlignment = Alignment.Center
    ) {
        // Desenhando cadeiras em volta (Canvas)
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val chairWidth = 14.dp.toPx()
            val chairDepth = 10.dp.toPx()
            val backrestThickness = 4.dp.toPx()

            fun drawChair(cx: Float, cy: Float, rotationDegrees: Float) {
                withTransform({
                    translate(left = cx, top = cy)
                    rotate(rotationDegrees, pivot = androidx.compose.ui.geometry.Offset.Zero)
                }) {
                    // Assento
                    drawRoundRect(
                        color = Color(0xFFE0E0E0),
                        topLeft = androidx.compose.ui.geometry.Offset(-chairWidth/2, -chairDepth/2),
                        size = androidx.compose.ui.geometry.Size(chairWidth, chairDepth),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx())
                    )
                    // Encosto da cadeira (parte superior de onde a cadeira 'encara')
                    drawRoundRect(
                        color = Color.DarkGray,
                        topLeft = androidx.compose.ui.geometry.Offset(-chairWidth/2, -chairDepth/2 - backrestThickness/2),
                        size = androidx.compose.ui.geometry.Size(chairWidth, backrestThickness),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.dp.toPx())
                    )
                }
            }
            
            if (seats > 0) {
                if (shape == "rectangle") {
                    if (seats >= 4) {
                        val topSeats = (seats - 2 + 1) / 2
                        val bottomSeats = (seats - 2) / 2
                        
                        val spaceXTop = size.width / (topSeats + 1)
                        val spaceXBottom = size.width / (bottomSeats + 1)

                        // Topo e Baixo
                        for (i in 1..topSeats) drawChair(i * spaceXTop, 6.dp.toPx(), 0f)
                        for (i in 1..bottomSeats) drawChair(i * spaceXBottom, size.height - 6.dp.toPx(), 180f)
                        
                        // Esquerda e Direita
                        drawChair(6.dp.toPx(), size.height / 2f, 270f) // Virada pra direita (para dentro da mesa)
                        drawChair(size.width - 6.dp.toPx(), size.height / 2f, 90f) // Virada pra esquerda (para dentro)
                    } else {
                        val topSeats = (seats + 1) / 2
                        val bottomSeats = seats / 2
                        
                        val spaceXTop = size.width / (topSeats + 1)
                        val spaceXBottom = size.width / (bottomSeats + 1)

                        for (i in 1..topSeats) drawChair(i * spaceXTop, 6.dp.toPx(), 0f)
                        for (i in 1..bottomSeats) drawChair(i * spaceXBottom, size.height - 6.dp.toPx(), 180f)
                    }
                } else {
                    val angleStep = 360f / seats
                    val dist = (size.height / 2f) - 6.dp.toPx()
                    val centerOffset = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)
                    for (i in 0 until seats) {
                        val angleDeg = (i * angleStep) - 90f
                        val angleRad = Math.toRadians(angleDeg.toDouble())
                        val cx = centerOffset.x + dist * kotlin.math.cos(angleRad).toFloat()
                        val cy = centerOffset.y + dist * kotlin.math.sin(angleRad).toFloat()
                        drawChair(cx, cy, angleDeg + 90f)
                    }
                }
            }
        }

        val tableShape = when (shape) {
            "round" -> androidx.compose.foundation.shape.CircleShape
            "rectangle" -> androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
            else -> androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
        }

        val tableModifier = when (shape) {
            "rectangle" -> Modifier.size(boxWidth - 16.dp, 56.dp)
            else -> Modifier.size(56.dp)
        }

        // A própria mesa
        Card(
            shape = tableShape,
            colors = CardDefaults.cardColors(containerColor = bgColor),
            modifier = tableModifier,
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(4.dp).rotate(-rotationAngle),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("$tableNumber", fontWeight = FontWeight.Bold, fontSize = 16.sp)

                when (status) {
                    "FREE" -> Text("Livre", color = Color.Gray, fontSize = 10.sp)
                    "CLEANING" -> {
                        Icon(Icons.Default.CleaningServices, null, tint = Color(0xFFFBC02D), modifier = Modifier.size(16.dp))
                    }
                    else -> {
                        Text("R$ %.0f".format(totalBill), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}