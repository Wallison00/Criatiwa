package com.walli.flexcriatiwa

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Add
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TableLayoutConfigScreen(
    companyId: String,
    onNavigateBack: () -> Unit
) {
    var tableCount by remember { mutableStateOf(20) }
    var tablePositions by remember { mutableStateOf<Map<String, TablePosition>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(companyId) {
        Firebase.firestore.collection("companies").document(companyId).get()
            .addOnSuccessListener { snapshot ->
                tableCount = snapshot.getLong("tableCount")?.toInt() ?: 20
                
                val mapPos = snapshot.get("tablePositions") as? Map<*, *>
                if (mapPos != null) {
                    try {
                        val parsedMap = mapPos.mapNotNull {
                            val key = it.key as? String ?: return@mapNotNull null
                            val posMap = it.value as? Map<*, *> ?: return@mapNotNull null
                            val x = (posMap["x"] as? Number)?.toFloat() ?: 0f
                            val y = (posMap["y"] as? Number)?.toFloat() ?: 0f
                            key to TablePosition(x, y)
                        }.toMap()
                        tablePositions = parsedMap
                    } catch (_: Exception) { }
                }
                isLoading = false
            }
            .addOnFailureListener {
                isLoading = false
            }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Layout do Salão", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar") }
                }
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                SmallFloatingActionButton(
                    onClick = {
                        tableCount += 1
                    },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Icon(Icons.Default.Add, "Adicionar Mesa")
                }
                Spacer(Modifier.height(16.dp))
                SmallFloatingActionButton(
                    onClick = {
                        scope.launch {
                            try {
                                Firebase.firestore.collection("companies").document(companyId)
                                    .update("tablePositions", emptyMap<String, Any>())
                                    .addOnSuccessListener {
                                        tablePositions = emptyMap()
                                        scope.launch { snackbarHostState.showSnackbar("Layout resetado com sucesso!") }
                                    }
                            } catch (_: Exception) {}
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                ) {
                    Icon(Icons.Default.Refresh, "Resetar")
                }
                Spacer(Modifier.height(16.dp))
                ExtendedFloatingActionButton(
                    onClick = {
                        scope.launch {
                            try {
                                val updateMap = tablePositions.mapValues { 
                                    mapOf("x" to it.value.x, "y" to it.value.y)
                                }
                                val updates = mapOf(
                                    "tablePositions" to updateMap,
                                    "tableCount" to tableCount
                                )
                                Firebase.firestore.collection("companies").document(companyId)
                                    .update(updates)
                                    .addOnSuccessListener {
                                        scope.launch { snackbarHostState.showSnackbar("Layout e mesas salvos com sucesso!") }
                                    }
                            } catch (e: Exception) {}
                        }
                    },
                    icon = { Icon(Icons.Default.Save, null) },
                    text = { Text("Salvar") }
                )
            }
        }
    ) { innerPadding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(Color(0xFFF5F5F5))
            ) {
                val boxMaxWidth = constraints.maxWidth.toFloat()
                val boxMaxHeight = constraints.maxHeight.toFloat()
                
                val cardWidth = 80.dp
                val cardHeight = 80.dp
                val density = LocalDensity.current
                val snapSizePx = with(density) { 45.dp.toPx() } // Novo quadrante reduzido pela metade
                val baseObjectSizePx = with(density) { 90.dp.toPx() } // Espaço matriz original (90dp)
                val cardSizePx = with(density) { 80.dp.toPx() }

                // Area de captura no rodapé (Doca)
                val dockHeightPx = with(density) { 140.dp.toPx() }
                val gridMaxHeight = boxMaxHeight - dockHeightPx

                // Desenha a "Folha Quadriculada" e a Doca ao fundo
                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                    val cols = (boxMaxWidth / snapSizePx).toInt() + 1
                    val rows = (gridMaxHeight / snapSizePx).toInt() + 1
                    
                    val gridColor = Color.LightGray.copy(alpha = 0.5f)
                    val strokeWidth = 1.dp.toPx()
                    
                    // Linhas Verticais
                    for (i in 0..cols) {
                        val x = i * snapSizePx
                        drawLine(
                            color = gridColor,
                            start = androidx.compose.ui.geometry.Offset(x, 0f),
                            end = androidx.compose.ui.geometry.Offset(x, gridMaxHeight),
                            strokeWidth = strokeWidth
                        )
                    }
                    // Linhas Horizontais
                    for (i in 0..rows) {
                        val y = i * snapSizePx
                        drawLine(
                            color = gridColor,
                            start = androidx.compose.ui.geometry.Offset(0f, y),
                            end = androidx.compose.ui.geometry.Offset(boxMaxWidth, y),
                            strokeWidth = strokeWidth
                        )
                    }
                    
                    // Fundo da Doca (Rodapé)
                    drawRect(
                        color = Color.DarkGray.copy(alpha = 0.15f),
                        topLeft = androidx.compose.ui.geometry.Offset(0f, gridMaxHeight),
                        size = androidx.compose.ui.geometry.Size(boxMaxWidth, dockHeightPx)
                    )
                }

                // Dica escrita na doca
                Text(
                    text = "Área de Mesas Novas (Arraste para cima)",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    modifier = Modifier.offset { androidx.compose.ui.unit.IntOffset(16, (gridMaxHeight + with(density) { 4.dp.toPx() }).toInt()) }
                )

                val tables = (1..tableCount).toList()
                
                // Mapeamento absoluto da tela (mistura mesas salvas com posições padrão na Doca)
                val visualPositions = remember(tableCount, tablePositions, boxMaxWidth, gridMaxHeight) {
                    val map = mutableMapOf<Int, TablePosition>()
                    var unplacedCount = 0
                    for (i in 1..tableCount) {
                        val saved = tablePositions[i.toString()]
                        if (saved != null) {
                            map[i] = saved
                        } else {
                            val colsInDock = (boxMaxWidth / snapSizePx).toInt().coerceAtLeast(1)
                            val rowOffset = unplacedCount / colsInDock
                            val colOffset = unplacedCount % colsInDock
                            
                            val dx = (colOffset * baseObjectSizePx) + with(density) { 5.dp.toPx() }
                            // Coloca na doca, mais pra baixo
                            val dy = gridMaxHeight + with(density) { 24.dp.toPx() } + (rowOffset * baseObjectSizePx)
                            
                            map[i] = TablePosition(dx, dy)
                            unplacedCount++
                        }
                    }
                    map
                }

                tables.forEach { tableNum ->
                    val pos = visualPositions[tableNum] ?: TablePosition(0f, 0f)

                    var offsetX by remember(pos.x, pos.y) { mutableStateOf(pos.x) }
                    var offsetY by remember(pos.x, pos.y) { mutableStateOf(pos.y) }
                    var currentRotation by remember(pos.rotation) { mutableStateOf(pos.rotation) }
                    var showConfigDialog by remember { mutableStateOf(false) }

                    if (showConfigDialog) {
                        var tempShape by remember { mutableStateOf(pos.shape) }
                        var tempSeats by remember { mutableStateOf(pos.seats.toString()) }
                        var tempRotation by remember { mutableStateOf(pos.rotation) }
                        
                        AlertDialog(
                            onDismissRequest = { showConfigDialog = false },
                            title = { Text("Configurar Mesa $tableNum", fontWeight = FontWeight.Bold) },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Formato", fontWeight = FontWeight.SemiBold)
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        listOf("square" to "Quad", "round" to "Redon", "rectangle" to "Retan").forEach { (shapeKey, shapeLabel) ->
                                            OutlinedButton(
                                                onClick = { tempShape = shapeKey },
                                                contentPadding = PaddingValues(4.dp),
                                                colors = ButtonDefaults.outlinedButtonColors(
                                                    containerColor = if (tempShape == shapeKey) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                                                )
                                            ) { Text(shapeLabel, fontSize = 11.sp) }
                                        }
                                    }
                                    OutlinedTextField(
                                        value = tempSeats,
                                        onValueChange = { tempSeats = it.filter { char -> char.isDigit() } },
                                        label = { Text("Cadeiras") },
                                        singleLine = true
                                    )
                                    Text("Rotação: ${tempRotation.toInt()}°", fontWeight = FontWeight.SemiBold)
                                    Slider(
                                        value = tempRotation,
                                        onValueChange = { tempRotation = it },
                                        valueRange = 0f..270f,
                                        steps = 2
                                    )
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = {
                                    val newPositions = tablePositions.toMutableMap()
                                    newPositions[tableNum.toString()] = TablePosition(offsetX, offsetY, tempShape, tempSeats.toIntOrNull() ?: 4, tempRotation)
                                    tablePositions = newPositions
                                    showConfigDialog = false
                                }) { Text("Salvar") }
                            },
                            dismissButton = {
                                TextButton(onClick = { showConfigDialog = false }) { Text("Cancelar") }
                            }
                        )
                    }

                    Box(
                        modifier = Modifier
                            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                            .pointerInput(tableNum, pos) {
                                var currentDragX = offsetX
                                var currentDragY = offsetY
                                var startDragX = offsetX
                                var startDragY = offsetY
                                detectDragGestures(
                                    onDragStart = {
                                        startDragX = offsetX
                                        startDragY = offsetY
                                        currentDragX = offsetX
                                        currentDragY = offsetY
                                    },
                                    onDragEnd = {
                                        // Verificar se o usuário soltou a mesa na Doca
                                        if (currentDragY + (cardSizePx / 2) >= gridMaxHeight) {
                                            // Se soltou na doca, reseta essa mesa (volta pra unplaced status)
                                            val newPositions = tablePositions.toMutableMap()
                                            newPositions.remove(tableNum.toString())
                                            tablePositions = newPositions
                                        } else {
                                            // Arredondar para a célula do Grid mais próxima (matriz 90x90 dp)
                                            var snappedX = kotlin.math.round(currentDragX / snapSizePx) * snapSizePx
                                            var snappedY = kotlin.math.round(currentDragY / snapSizePx) * snapSizePx
                                            
                                            val squaresWidth = if (pos.shape == "rectangle") kotlin.math.max(2, (pos.seats + 1) / 2) else 2
                                            
                                            val w = snapSizePx * squaresWidth
                                            val h = snapSizePx * 2
                                            val isVirtuallyRotated = (pos.rotation % 180f) > 45f && (pos.rotation % 180f) < 135f
                                            
                                            val visualW = if (isVirtuallyRotated) h else w
                                            val visualH = if (isVirtuallyRotated) w else h
                                            
                                            val shiftX = if (isVirtuallyRotated) (w - h) / 2f else 0f
                                            val shiftY = if (isVirtuallyRotated) (h - w) / 2f else 0f

                                            val minX = -shiftX
                                            val maxX = boxMaxWidth - visualW - shiftX
                                            val minY = -shiftY
                                            val maxY = gridMaxHeight - visualH - shiftY

                                            // Garantir que fique totalmente dentro da grade desenhada!
                                            snappedX = snappedX.coerceIn(minX, maxX.coerceAtLeast(minX))
                                            snappedY = snappedY.coerceIn(minY, maxY.coerceAtLeast(minY))
                                            
                                            snappedX = kotlin.math.round(snappedX / snapSizePx) * snapSizePx
                                            snappedY = kotlin.math.round(snappedY / snapSizePx) * snapSizePx

                                            val offsetCellX = snappedX + with(density) { 5.dp.toPx() }
                                            val offsetCellY = snappedY + with(density) { 5.dp.toPx() }

                                            // Checagem de Colisão (Bounding Box) abrangendo múltiplas células - Corrigido pelo Eixo de Rotação Visual
                                            var isCellOccupied = false
                                            val dynamicMap = tablePositions.toMutableMap()
                                            
                                            val marginPx = with(density) { 10.dp.toPx() }
                                            val rectLeft = offsetCellX + shiftX
                                            val rectRight = rectLeft + visualW - marginPx
                                            val rectTop = offsetCellY + shiftY
                                            val rectBottom = rectTop + visualH - marginPx

                                            for ((otherKey, otherPos) in visualPositions) {
                                                if (otherKey != tableNum) {
                                                    val realOtherPos = dynamicMap[otherKey.toString()] ?: otherPos
                                                    
                                                    // Verifica apenas outras mesas que ESTÃO na grade (saíram da doca)
                                                    if (realOtherPos.y < gridMaxHeight) {
                                                        val otherSquares = if (realOtherPos.shape == "rectangle") kotlin.math.max(2, (realOtherPos.seats + 1) / 2) else 2
                                                        val oW = snapSizePx * otherSquares
                                                        val oH = snapSizePx * 2
                                                        
                                                        val otherIsRotated = (realOtherPos.rotation % 180f) > 45f && (realOtherPos.rotation % 180f) < 135f
                                                        val otherVW = if (otherIsRotated) oH else oW
                                                        val otherVH = if (otherIsRotated) oW else oH
                                                        
                                                        val oShiftX = if (otherIsRotated) (oW - oH) / 2f else 0f
                                                        val oShiftY = if (otherIsRotated) (oH - oW) / 2f else 0f
                                                        
                                                        val otherLeft = realOtherPos.x + oShiftX
                                                        val otherRight = otherLeft + otherVW - marginPx
                                                        val otherTop = realOtherPos.y + oShiftY
                                                        val otherBottom = otherTop + otherVH - marginPx
                                                        
                                                        // AABB Collision (intersect)
                                                        if (rectLeft < otherRight && rectRight > otherLeft &&
                                                            rectTop < otherBottom && rectBottom > otherTop) {
                                                            isCellOccupied = true
                                                            break
                                                        }
                                                    }
                                                }
                                            }

                                            if (isCellOccupied) {
                                                // Se a célula desejada já tem dono, mesa atual escorrega de volta à origem dela
                                                offsetX = startDragX
                                                offsetY = startDragY
                                            } else {
                                                // A célula tá livre: aceita a posição grudadinha nela
                                                offsetX = offsetCellX
                                                offsetY = offsetCellY
                                                val newPositions = tablePositions.toMutableMap()
                                                newPositions[tableNum.toString()] = TablePosition(offsetX, offsetY, pos.shape, pos.seats, pos.rotation)
                                                tablePositions = newPositions
                                            }
                                        }
                                    },
                                    onDragCancel = {
                                        offsetX = startDragX
                                        offsetY = startDragY
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        currentDragX += dragAmount.x
                                        currentDragY += dragAmount.y
                                        offsetX = currentDragX
                                        offsetY = currentDragY
                                    }
                                )
                            }
                    ) {
                        TableCard(
                            tableNumber = tableNum,
                            status = "FREE",
                            totalBill = 0.0,
                            shape = pos.shape,
                            seats = pos.seats,
                            rotationAngle = currentRotation
                        )
                        
                        val canRotate = pos.shape == "rectangle" || (pos.shape == "square" && pos.seats == 2)
                        
                        if (canRotate) {
                            // Icone de Rotação (Sensível a pan/drag)
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .offset(x = (-4).dp, y = 4.dp)
                                    .size(28.dp)
                                    .background(Color.White.copy(alpha = 0.9f), CircleShape)
                                    .clickable {
                                        currentRotation = if (currentRotation == 0f) 90f else 0f
                                        val newPositions = tablePositions.toMutableMap()
                                        newPositions[tableNum.toString()] = TablePosition(offsetX, offsetY, pos.shape, pos.seats, currentRotation)
                                        tablePositions = newPositions
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Girar Mesa",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        IconButton(
                            onClick = { showConfigDialog = true },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Configurar",
                                modifier = Modifier.size(16.dp),
                                tint = Color.Gray
                            )
                        }
                    }
                }
            }
        }
    }
}
