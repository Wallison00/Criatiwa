package com.walli.flexcriatiwa

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material.icons.filled.CenterFocusWeak
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

data class Point(val x: Int, val y: Int)

fun getTableWidthSquares(shape: String, seats: Int): Int {
    return when(shape) { 
        "rectangle" -> {
            val needed = (seats + 1) / 2
            if (needed % 2 != 0) needed + 1 else needed
        }
        "counter" -> kotlin.math.max(2, seats)
        else -> 2 
    }
}

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
    
    val allTableIds = remember<List<String>>(tableCount, tablePositions) {
        val numericIds = (1..tableCount).map { it.toString() }
        val nonNumericIds = tablePositions.keys.filter { it.toIntOrNull() == null }
        (numericIds + nonNumericIds).distinct()
    }

    val highlightTableId by kitchenViewModel.highlightTableId.collectAsState()
    var zoomLevel by remember { mutableStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    var isMapView by remember { mutableStateOf(true) }

    var isInitialized by remember { mutableStateOf(false) }
    LaunchedEffect(highlightTableId) {
        if (highlightTableId != null) isMapView = true
    }

    var showCleaningDialogForTable by remember { mutableStateOf<Int?>(null) }

    if (showCleaningDialogForTable != null) {
        AlertDialog(
            onDismissRequest = { showCleaningDialogForTable = null },
            title = { Text("Liberar Mesa $showCleaningDialogForTable?") },
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
                        Icon(Icons.Default.Menu, "Menu")
                    }
                },
                actions = {
                    IconButton(onClick = { isMapView = !isMapView }) {
                        Icon(
                            if (isMapView) Icons.AutoMirrored.Filled.List else Icons.Default.Map,
                            if (isMapView) "List View" else "Map View"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            val displayScale = 1.35f
            val snapSizePx = 140f * displayScale

            if (isMapView) {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val density = LocalDensity.current
                    val boxWidth = constraints.maxWidth.toFloat()
                    val activeAreaHeight = constraints.maxHeight.toFloat()
                    val baseSnapSize = 140f

                    val centerView = {
                        if (tablePositions.isNotEmpty() && boxWidth > 0f) {
                            var minX = Float.MAX_VALUE; var minY = Float.MAX_VALUE
                            var maxX = Float.MIN_VALUE; var maxY = Float.MIN_VALUE

                            for (pos in tablePositions.values) {
                                val isR = (pos.rotation % 180f) != 0f
                                val w = if (isR) 2f * baseSnapSize else getTableWidthSquares(pos.shape, pos.seats).toFloat() * baseSnapSize
                                val h = if (isR) getTableWidthSquares(pos.shape, pos.seats).toFloat() * baseSnapSize else 2f * baseSnapSize
                                
                                minX = minOf(minX, pos.x)
                                minY = minOf(minY, pos.y)
                                maxX = maxOf(maxX, pos.x + w)
                                maxY = maxOf(maxY, pos.y + h)
                            }

                            val padding = 120f
                            val contentW = (maxX - minX).coerceAtLeast(100f)
                            val contentH = (maxY - minY).coerceAtLeast(100f)

                            val zoomX = (boxWidth - padding) / contentW
                            val zoomY = (activeAreaHeight - padding) / contentH
                            val fitZoom = minOf(zoomX, zoomY).coerceIn(0.4f, 1.5f)

                            zoomLevel = fitZoom
                            panOffset = Offset(
                                boxWidth / 2f - (minX + maxX) / 2f * fitZoom,
                                activeAreaHeight / 2f - (minY + maxY) / 2f * fitZoom
                            )
                        } else {
                            panOffset = Offset.Zero
                            zoomLevel = 1.0f
                        }
                    }

                    LaunchedEffect(tablePositions, boxWidth, activeAreaHeight) {
                        if (!isInitialized && boxWidth > 0) {
                            centerView()
                            isInitialized = true
                        }
                    }

                    val currentSnapPx = baseSnapSize * zoomLevel

                    Box(modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTransformGestures { centroid, pan, zoom, _ ->
                                val oldZoom = zoomLevel
                                val maxZoom = 2.5f
                                zoomLevel = (zoomLevel * zoom).coerceIn(0.4f, maxZoom)
                                val zoomFactor = zoomLevel / oldZoom
                                panOffset = (panOffset * zoomFactor) + pan - (centroid * (zoomFactor - 1f))
                            }
                        }
                        .offset { IntOffset(panOffset.x.roundToInt(), panOffset.y.roundToInt()) }
                    ) {


                        allTableIds.forEach { tableId ->
                            val tableNum = tableId.toIntOrNull() ?: 0
                            val tableOrders = ordersByTable[tableNum] ?: emptyList()
                            val tStatus = when {
                                tableOrders.isEmpty() -> "FREE"
                                tableOrders.any { it.status == OrderStatus.NEEDS_CLEANING } -> "CLEANING"
                                tableOrders.any { it.status == OrderStatus.DELIVERED } -> "CONSUMING"
                                else -> "PREPARING"
                            }
                            val totalBill = tableOrders.sumOf { o -> o.items.sumOf { i -> i.singleItemTotalPrice * i.quantity } }
                            val pos = tablePositions[tableId]
                            
                            val isHighlighted = tableId == highlightTableId
                            val counterPosEntry = tablePositions.entries.find { it.value.shape == "counter" }
                            
                            val fX = (pos?.x ?: (if (tableNum > 0) ((tableNum - 1) % 4 * 270f) else 0f)) * zoomLevel
                            val fY = (pos?.y ?: (if (tableNum > 0) ((tableNum - 1) / 4 * 270f) else 0f)) * zoomLevel

                            Box(
                                modifier = Modifier
                                    .offset { IntOffset(fX.roundToInt(), fY.roundToInt()) }
                                    .alpha(if (highlightTableId != null && !isHighlighted && tableId != counterPosEntry?.key) 0.5f else 1f)
                            ) {
                                TableCard(
                                    tableNumber = tableNum,
                                    status = tStatus,
                                    totalBill = totalBill,
                                    shape = pos?.shape ?: "square",
                                    seats = pos?.seats ?: 4,
                                    rotationAngle = pos?.rotation ?: 0f,
                                    snapSizePx = currentSnapPx,
                                    modifier = Modifier
                                        .clickable { 
                                            if (tableNum > 0) { 
                                                if (tStatus == "CLEANING") showCleaningDialogForTable = tableNum 
                                                else onTableClick(tableNum) 
                                            } 
                                        }
                                )

                                if (isHighlighted) {
                                    val transition = rememberInfiniteTransition()
                                    val pulseScale by transition.animateFloat(
                                        initialValue = 1f,
                                        targetValue = 1.4f,
                                        animationSpec = infiniteRepeatable(
                                            animation = tween(600),
                                            repeatMode = RepeatMode.Reverse
                                        ),
                                        label = "Pulse"
                                    )
                                    val pinSize = 44.dp * zoomLevel
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = Color(0xFFE53935), // Red to highlight prominently
                                        modifier = Modifier
                                            .align(Alignment.TopCenter)
                                            .offset(y = (-16).dp * zoomLevel)
                                            .size(pinSize)
                                            .scale(pulseScale)
                                    )
                                }
                            }
                        }
                    }

                    // Toolbox Zoom/Centering
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 16.dp)
                            .shadow(8.dp, RoundedCornerShape(24.dp))
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color.White.copy(alpha = 0.95f))
                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(24.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { zoomLevel = (zoomLevel / 1.15f).coerceAtLeast(0.4f) }) {
                            Icon(Icons.Default.Remove, "Zoom Out", tint = Color(0xFF5E35B1))
                        }
                        Text(
                            "${(zoomLevel * 100).roundToInt()}%", 
                            fontSize = 13.sp, 
                            fontWeight = FontWeight.Bold,
                            color = Color.DarkGray,
                            modifier = Modifier.width(42.dp).clickable { zoomLevel = 1.0f },
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        IconButton(onClick = { zoomLevel = (zoomLevel * 1.15f).coerceAtMost(2.5f) }) {
                            Icon(Icons.Default.Add, "Zoom In", tint = Color(0xFF5E35B1))
                        }

                        VerticalDivider(modifier = Modifier.height(24.dp), thickness = 1.dp, color = Color(0xFFE2E8F0))
                        IconButton(onClick = centerView) {
                            Icon(Icons.Default.CenterFocusWeak, "Enquadrar Tudo", tint = Color(0xFF5E35B1))
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(if (LocalConfiguration.current.screenWidthDp > 600) 4 else 3),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    val listIds = allTableIds.filter { tablePositions[it]?.shape != "counter" }
                    items(listIds) { tableId ->
                        val tableNum = tableId.toIntOrNull() ?: 0
                        val tableOrders = ordersByTable[tableNum] ?: emptyList()
                        val tStatus = when {
                            tableOrders.isEmpty() -> "FREE"
                            tableOrders.any { it.status == OrderStatus.NEEDS_CLEANING } -> "CLEANING"
                            tableOrders.any { it.status == OrderStatus.DELIVERED } -> "CONSUMING"
                            else -> "PREPARING"
                        }
                        val totalBill = tableOrders.sumOf { o -> o.items.sumOf { i -> i.singleItemTotalPrice * i.quantity } }
                        val pos = tablePositions[tableId]

                        TableCard(
                            tableNumber = tableNum,
                            status = tStatus,
                            totalBill = totalBill,
                            shape = pos?.shape ?: "square",
                            seats = pos?.seats ?: 4,
                            rotationAngle = pos?.rotation ?: 0f,
                            snapSizePx = 180f,
                            isListView = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1.2f)
                                .clickable { 
                                    if (tableNum > 0) {
                                        if (tStatus == "CLEANING") showCleaningDialogForTable = tableNum
                                        else onTableClick(tableNum)
                                    }
                                }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TableCard(
    tableNumber: Int,
    status: String,
    totalBill: Double,
    shape: String,
    seats: Int,
    rotationAngle: Float,
    modifier: Modifier = Modifier,
    snapSizePx: Float = 135f,
    isListView: Boolean = false
) {
    val bgColor = when (status) {
        "FREE" -> Color.White 
        "CLEANING" -> Color(0xFFFFF9C4)
        "CONSUMING" -> Color(0xFFE8F5E9)
        else -> Color(0xFFE3F2FD)
    }

    val tableBorderColor = if (status == "FREE") Color(0xFF94A3B8).copy(alpha = 0.6f) else Color.Transparent

    if (isListView) {
        androidx.compose.material3.Card(
            modifier = modifier,
            colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = bgColor),
            border = if (status == "FREE") androidx.compose.foundation.BorderStroke(1.dp, tableBorderColor) else null,
            elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (shape == "counter") {
                    Text("Balcão", fontWeight = FontWeight.Bold, fontSize = 24.sp, color = Color.DarkGray)
                } else {
                    Text("$tableNumber", fontWeight = FontWeight.ExtraBold, fontSize = 32.sp, color = Color.DarkGray)
                    if (status != "FREE") {
                        if (status == "CLEANING") {
                            Icon(Icons.Default.CleaningServices, null, tint = Color(0xFFFBC02D), modifier = Modifier.size(24.dp))
                        } else {
                            Text("R$ %.0f".format(totalBill), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.DarkGray)
                        }
                    }
                }
            }
        }
        return
    }

    val squaresWidth = getTableWidthSquares(shape, seats)
    val displayScale = snapSizePx / 140f
    val density = LocalDensity.current

    val baseWidthPx = squaresWidth.toFloat() * snapSizePx
    val baseHeightPx = 2f * snapSizePx
    
    val isRotated = (rotationAngle % 180f) != 0f
    val layoutWidth = if (isRotated) baseHeightPx else baseWidthPx
    val layoutHeight = if (isRotated) baseWidthPx else baseHeightPx

    Box(
        modifier = modifier.size(with(density) { layoutWidth.toDp() }, with(density) { layoutHeight.toDp() }),
        contentAlignment = Alignment.Center
    ) {
        // We use a Square box to avoid Clipping when rotating inside Canvas
        val canvasSize = maxOf(baseWidthPx, baseHeightPx)
        
        Canvas(modifier = Modifier.size(with(density){canvasSize.toDp()})) {
            val dCenter = Offset(size.width / 2f, size.height / 2f)
            
            withTransform({
                rotate(rotationAngle, pivot = dCenter)
            }) {
                val drawW = baseWidthPx
                val drawH = baseHeightPx
                val left = dCenter.x - drawW / 2f
                val top = dCenter.y - drawH / 2f

                // --- COLORS & GRADIENTS (PREMIUM WOOD STYLE) ---
                val woodLight = Color(0xFFD2B48C) 
                val woodDark = Color(0xFF8B4513)  
                val woodGold = Color(0xFFCD853F)  
                
                val chairSeatColor = Color(0xFF263238) 
                val woodBrush = Brush.verticalGradient(listOf(woodGold, woodDark))
                val chairBackBrush = Brush.verticalGradient(listOf(woodLight, woodGold))

                fun drawChairInternal(cx: Float, cy: Float, rot: Float) {
                    val cW = snapSizePx * 0.32f
                    val cD = snapSizePx * 0.26f
                    val bT = snapSizePx * 0.08f
                    withTransform({
                        translate(left = cx, top = cy)
                        rotate(rot, pivot = Offset.Zero)
                    }) {
                        // Conectores madeira (suporte encosto)
                        drawLine(
                            color = woodDark.copy(alpha = 0.5f),
                            start = Offset(-cW * 0.2f, -cD * 0.4f),
                            end = Offset(-cW * 0.25f, -cD * 0.5f - bT),
                            strokeWidth = 2f * displayScale
                        )
                        drawLine(
                            color = woodDark.copy(alpha = 0.5f),
                            start = Offset(cW * 0.2f, -cD * 0.4f),
                            end = Offset(cW * 0.25f, -cD * 0.5f - bT),
                            strokeWidth = 2f * displayScale
                        )

                        // Encosto Curvo Madeira (Referência Realista)
                        drawArc(
                            brush = chairBackBrush,
                            startAngle = 180f,
                            sweepAngle = 180f,
                            useCenter = true,
                            topLeft = Offset(-cW * 0.45f, -cD / 2f - bT * 1.6f),
                            size = Size(cW * 0.9f, bT * 2.5f)
                        )
                        
                        // Assento Couro Preto
                        drawRoundRect(
                            color = chairSeatColor,
                            topLeft = Offset(-cW / 2f, -cD / 2f),
                            size = Size(cW, cD),
                            cornerRadius = CornerRadius(snapSizePx * 0.08f)
                        )
                        // Borda sutil de brilho no couro
                        drawRoundRect(
                            color = Color.White.copy(alpha = 0.1f),
                            topLeft = Offset(-cW / 2f, -cD / 2f),
                            size = Size(cW, cD),
                            cornerRadius = CornerRadius(snapSizePx * 0.08f),
                            style = Stroke(width = 0.8f * displayScale)
                        )
                    }
                }

                if (seats > 0 && shape != "counter") {
                    val tBodyW_Local = if (shape == "rectangle") drawW * 0.82f else if(shape == "round") snapSizePx * 0.9f else snapSizePx * 0.95f
                    val tBodyH_Local = if (shape == "round") snapSizePx * 0.9f else snapSizePx * 0.95f
                    
                    if (shape == "rectangle") {
                        val sideOffset = (tBodyH_Local / 2f) + (snapSizePx * 0.04f)
                        val endOffset = (tBodyW_Local / 2f) + (snapSizePx * 0.04f)
                        
                        if (seats >= 4) {
                            val sideSeats = (seats - 2) / 2
                            val topSeats = if(seats % 2 == 0) sideSeats else sideSeats + 1
                            val bottomSeats = sideSeats
                            
                            val spaceXTop = tBodyW_Local / (topSeats.toFloat() + 1f)
                            val spaceXBottom = tBodyW_Local / (bottomSeats.toFloat() + 1f)
                            
                            // Cadeiras Laterais (Cima)
                            for (i in 1..topSeats) drawChairInternal(dCenter.x - (tBodyW_Local/2f) + (i.toFloat() * spaceXTop), dCenter.y - sideOffset, 0f)
                            // Cadeiras Laterais (Baixo)
                            for (i in 1..bottomSeats) drawChairInternal(dCenter.x - (tBodyW_Local/2f) + (i.toFloat() * spaceXBottom), dCenter.y + sideOffset, 180f)
                            
                            // Cadeiras das Pontas
                            drawChairInternal(dCenter.x - endOffset, dCenter.y, 270f)
                            drawChairInternal(dCenter.x + endOffset, dCenter.y, 90f)
                        } else {
                            val topS = (seats + 1) / 2
                            val botS = seats / 2
                            val spaceX = tBodyW_Local / (topS + 1)
                            for (i in 1..topS) drawChairInternal(dCenter.x - (tBodyW_Local/2f) + (i * spaceX), dCenter.y - sideOffset, 0f)
                            for (i in 1..botS) drawChairInternal(dCenter.x - (tBodyW_Local/2f) + (i * spaceX), dCenter.y + sideOffset, 180f)
                        }
                    } else {
                        val angleStep = 360f / seats.toFloat()
                        val dist = (tBodyH_Local / 2f) + (snapSizePx * 0.04f)
                        for (i in 0 until seats) {
                            val angleDeg = (i.toFloat() * angleStep) - 90f
                            val angleRad = (angleDeg * (kotlin.math.PI / 180.0)).toFloat()
                            val cx = dCenter.x + dist * kotlin.math.cos(angleRad)
                            val cy = dCenter.y + dist * kotlin.math.sin(angleRad)
                            drawChairInternal(cx, cy, angleDeg + 90f)
                        }
                    }
                }
                
                // --- TABLE BODY (WOODEN TEXTURE) ---
                val tBodyW = if (shape == "rectangle" || shape == "counter") drawW * 0.82f else if(shape == "round") snapSizePx * 0.9f else snapSizePx * 0.95f
                val tBodyH_Final = if (shape == "round") snapSizePx * 0.9f else snapSizePx * 0.95f
                
                val tLeft = dCenter.x - tBodyW / 2f
                val tTop = dCenter.y - tBodyH_Final / 2f
                
                // Wooden Base with Gradient
                drawRoundRect(
                    brush = woodBrush,
                    topLeft = Offset(tLeft, tTop),
                    size = Size(tBodyW, tBodyH_Final),
                    cornerRadius = CornerRadius(if(shape=="round") tBodyW/2f else 10f * displayScale),
                    style = androidx.compose.ui.graphics.drawscope.Fill
                )
                // Subtle Wooden Border
                drawRoundRect(
                    color = woodDark.copy(alpha = 0.8f),
                    topLeft = Offset(tLeft, tTop),
                    size = Size(tBodyW, tBodyH_Final),
                    cornerRadius = CornerRadius(if(shape=="round") tBodyW/2f else 10f * displayScale),
                    style = Stroke(width = 1.5f * displayScale)
                )
            }
        }

        // Font Scaling Balance: Simplified for only Table Number
        val labelScale = (1f + (1f / displayScale.coerceAtLeast(0.5f) - 1f) * 0.4f).coerceIn(1f, 1.8f)
        val numberFontSize = (19.sp.value * displayScale * labelScale).sp
        val priceFontSize = (12.sp.value * displayScale * labelScale).sp

        Column(
            modifier = Modifier.rotate(0f),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (shape == "counter") {
                val counterFontSize = (16.sp.value * displayScale * labelScale).sp
                if ((rotationAngle % 180f) != 0f) {
                    val vertText = "B\nA\nL\nC\nÃ\nO"
                    Text(vertText, fontWeight = FontWeight.Bold, fontSize = counterFontSize, color = Color.White.copy(alpha = 0.9f), lineHeight = counterFontSize)
                } else {
                    Text("Balcão", fontWeight = FontWeight.Bold, fontSize = counterFontSize, color = Color.White.copy(alpha = 0.9f))
                }
            } else {
                Text("$tableNumber", fontWeight = FontWeight.ExtraBold, fontSize = numberFontSize, color = Color.White)
            }
        }
    }
}

fun calculateObstacleFreePath(
    startX: Float, startY: Float, endX: Float, endY: Float,
    tablePositions: Map<String, TablePosition>,
    cellSize: Float = 30f,
    baseSnapSize: Float
): List<Offset> {
    val startC = Pair((startX / cellSize).roundToInt(), (startY / cellSize).roundToInt())
    val endC = Pair((endX / cellSize).roundToInt(), (endY / cellSize).roundToInt())
    
    val obstacles = mutableSetOf<Pair<Int, Int>>()
    for ((_, pos) in tablePositions) {
        val isRot = (pos.rotation % 180f) != 0f
        val w = if (isRot) 2f * baseSnapSize else getTableWidthSquares(pos.shape, pos.seats) * baseSnapSize
        val h = if (isRot) getTableWidthSquares(pos.shape, pos.seats) * baseSnapSize else 2f * baseSnapSize
        
        val margin = 35f 
        val cxMin = ((pos.x - margin) / cellSize).toInt()
        val cxMax = ((pos.x + w + margin) / cellSize).toInt()
        val cyMin = ((pos.y - margin) / cellSize).toInt()
        val cyMax = ((pos.y + h + margin) / cellSize).toInt()
        
        for (cx in cxMin..cxMax) {
            for (cy in cyMin..cyMax) {
                obstacles.add(Pair(cx, cy))
            }
        }
    }
    
    for (dx in -2..2) {
        for (dy in -2..2) {
            obstacles.remove(Pair(startC.first + dx, startC.second + dy))
            obstacles.remove(Pair(endC.first + dx, endC.second + dy))
        }
    }
    
    data class Node(val c: Pair<Int, Int>, val dir: Int)
    
    val queue = java.util.PriorityQueue<Pair<Node, Int>>(compareBy { it.second })
    val costs = mutableMapOf<Node, Int>()
    val cameFrom = mutableMapOf<Node, Node>()
    
    val startNode = Node(startC, -1)
    queue.add(Pair(startNode, 0))
    costs[startNode] = 0
    
    var iters = 0
    var endNode: Node? = null
    
    val dirs = listOf(
        Pair(0, -1) to 0,
        Pair(1, 0) to 1,
        Pair(0, 1) to 2,
        Pair(-1, 0) to 3
    )
    
    while(queue.isNotEmpty() && iters < 25000) {
        iters++
        val (curr, _) = queue.poll()
        
        if (curr.c == endC) {
            endNode = curr
            break
        }
        
        val currCost = costs[curr] ?: 0
        
        for ((d, dirIdx) in dirs) {
            val nx = curr.c.first + d.first
            val ny = curr.c.second + d.second
            val nCell = Pair(nx, ny)
            
            if (nCell in obstacles) continue
            
            val turnPenalty = if (curr.dir != -1 && curr.dir != dirIdx) 20 else 0
            val newCost = currCost + 10 + turnPenalty
            
            val nNode = Node(nCell, dirIdx)
            if (newCost < (costs[nNode] ?: Int.MAX_VALUE)) {
                costs[nNode] = newCost
                val hCost = (kotlin.math.abs(nx - endC.first) + kotlin.math.abs(ny - endC.second)) * 10
                queue.add(Pair(nNode, newCost + hCost))
                cameFrom[nNode] = curr
            }
        }
    }
    
    if (endNode == null) {
        return listOf(Offset(startX, startY), Offset(startX, endY), Offset(endX, endY))
    }
    
    val path = mutableListOf<Pair<Int, Int>>()
    var step = endNode
    while (step != null && step.c != startC) {
        path.add(step.c)
        step = cameFrom[step]
    }
    path.add(startC)
    path.reverse()
    
    val simplified = mutableListOf<Offset>()
    simplified.add(Offset(startX, startY))
    
    if (path.size > 2) {
        for (i in 1 until path.size - 1) {
            val prev = path[i-1]
            val curr = path[i]
            val next = path[i+1]
            if (prev.first != next.first && prev.second != next.second) {
                simplified.add(Offset(curr.first * cellSize, curr.second * cellSize))
            }
        }
    }
    simplified.add(Offset(endX, endY))
    
    return simplified
}