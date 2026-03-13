package com.walli.flexcriatiwa

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlin.math.roundToInt

// --- DESIGN TOKENS (Clean Minimalist Studio) ---
private val AppBackground = Color(0xFFF8FAFC) 
private val ConstructionWhite = Color.White
private val PrimaryAccent = Color(0xFF4F46E5)    
private val GridColor = Color(0xFFE2E8F0).copy(alpha = 0.5f)
private val CardBorderColor = Color(0xFFCBD5E1)
private val TextDark = Color(0xFF1E293B)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TableLayoutConfigScreen(
    companyId: String,
    onNavigateBack: () -> Unit
) {
    var tablePositions by remember { mutableStateOf<Map<String, TablePosition>>(emptyMap()) }
    var undoStack by remember { mutableStateOf(emptyList<Map<String, TablePosition>>()) }
    var redoStack by remember { mutableStateOf(emptyList<Map<String, TablePosition>>()) }

    val updatePositions = { newPos: Map<String, TablePosition> ->
        if (tablePositions != newPos) {
            val newUndo = undoStack.toMutableList()
            newUndo.add(tablePositions)
            if (newUndo.size > 30) newUndo.removeAt(0)
            undoStack = newUndo
            redoStack = emptyList()
            tablePositions = newPos
        }
    }
    var isLoading by remember { mutableStateOf(true) }
    var selectedTableId by remember { mutableStateOf<String?>(null) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    var zoomLevel by remember { mutableStateOf(1f) }
    var isInitialized by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val haptic = LocalHapticFeedback.current

    // Device Context
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600
    val baseSnapSize = 140f
    
    val dockHeight = if (isTablet) 110.dp else 120.dp

    // Firebase Data Sync
    LaunchedEffect(companyId) {
        Firebase.firestore.collection("companies").document(companyId).get()
            .addOnSuccessListener { snapshot ->
                val mapPos = snapshot.get("tablePositions") as? Map<*, *>
                if (mapPos != null) {
                    try {
                        tablePositions = mapPos.mapNotNull { entry ->
                            val key = entry.key as? String ?: return@mapNotNull null
                            val posMap = entry.value as? Map<*, *> ?: return@mapNotNull null
                            key to TablePosition(
                                x = (posMap["x"] as? Number)?.toFloat() ?: 0f,
                                y = (posMap["y"] as? Number)?.toFloat() ?: 0f,
                                shape = posMap["shape"] as? String ?: "square",
                                seats = (posMap["seats"] as? Number)?.toInt() ?: 4,
                                rotation = (posMap["rotation"] as? Number)?.toFloat() ?: 0f,
                                isLocked = (posMap["isLocked"] as? Boolean) ?: false
                            )
                        }.toMap()
                    } catch (_: Exception) { }
                }
                isLoading = false
            }
            .addOnFailureListener { isLoading = false }
    }

    // Auto-save logic
    LaunchedEffect(tablePositions) {
        if (!isLoading) {
            try {
                val updateMap = tablePositions.mapValues { 
                    mapOf(
                        "x" to it.value.x, "y" to it.value.y,
                        "shape" to it.value.shape, "seats" to it.value.seats,
                        "rotation" to it.value.rotation, "isLocked" to it.value.isLocked
                    )
                }
                val updates = mapOf(
                    "tablePositions" to updateMap,
                    "tableCount" to tablePositions.count { it.value.shape != "counter" }
                )
                Firebase.firestore.collection("companies").document(companyId).update(updates)
            } catch (_: Exception) { }
        }
    }

    var showAddTableModal by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = AppBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            ConfigTopBar(title = "Design do Salão", onBack = onNavigateBack)
        }
    ) { innerPadding ->
        if (isLoading) {
            LoadingState()
        } else {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(AppBackground)
            ) {
                val boxWidth = constraints.maxWidth.toFloat()
                val boxHeight = constraints.maxHeight.toFloat()
                val density = LocalDensity.current
                val currentSnapPx = baseSnapSize * zoomLevel
                val currentCardPx = 240f * zoomLevel
                val dockHeightPx = with(density) { dockHeight.toPx() }
                val activeAreaHeight = boxHeight - dockHeightPx

                val centerView = { positions: Map<String, TablePosition>? ->
                    val posToCenter = positions ?: tablePositions
                    if (posToCenter.isNotEmpty()) {
                        var minX = Float.MAX_VALUE; var minY = Float.MAX_VALUE
                        var maxX = Float.MIN_VALUE; var maxY = Float.MIN_VALUE

                        for (pos in posToCenter.values) {
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

                // Initial Centering
                LaunchedEffect(isLoading, boxWidth, activeAreaHeight) {
                    if (!isInitialized && !isLoading && boxWidth > 0) {
                        centerView(null)
                        isInitialized = true
                    }
                }

                // --- TABULEIRO (GRID & MESAS) ---
                Box(modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { selectedTableId = null })
                    }
                    .pointerInput(Unit) {
                        detectTransformGestures { centroid, pan, zoom, _ ->
                            val oldZoom = zoomLevel
                            zoomLevel = (zoomLevel * zoom).coerceIn(0.4f, 2.5f)
                            val zoomFactor = zoomLevel / oldZoom
                            panOffset = (panOffset * zoomFactor) + pan - (centroid * (zoomFactor - 1f))
                        }
                    }
                    .offset { IntOffset(panOffset.x.roundToInt(), panOffset.y.roundToInt()) }
                ) {
                    GridCanvas(currentSnapPx)

                    for ((tableId, pos) in tablePositions) {
                        key(tableId) {
                            val isRotated = (pos.rotation % 180f) != 0f
                            val squaresW = getTableWidthSquares(pos.shape, pos.seats)
                            
                            val extraMargin = 0f
                            val visualWidthPx = (if (isRotated) 2 * currentSnapPx else squaresW * currentSnapPx) + extraMargin
                            val visualHeightPx = (if (isRotated) squaresW * currentSnapPx else 2 * currentSnapPx) + extraMargin

                            DraggableTableItem(
                                tableId = tableId,
                                position = pos,
                                widthPx = visualWidthPx,
                                heightPx = visualHeightPx,
                                snapSize = currentSnapPx,
                                cardSize = currentCardPx,
                                zoomLevel = zoomLevel,
                                isSelected = selectedTableId == tableId,
                                isTablet = isTablet,
                                boundaryWidth = boxWidth,
                                boundaryHeight = activeAreaHeight,
                                panOffset = panOffset,
                                onSelect = { selectedTableId = it },
                                tablePositions = tablePositions,
                                onUpdate = { id, newPos -> 
                                    updatePositions(tablePositions.toMutableMap().apply { this[id] = newPos })
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                                onDelete = { id -> 
                                    updatePositions(tablePositions.toMutableMap().apply { remove(id) })
                                    selectedTableId = null
                                },
                                onCopy = {
                                    val id = if(pos.shape == "counter") "counter_${System.currentTimeMillis()}" else ((tablePositions.keys.mapNotNull { it.toIntOrNull() }.maxOrNull() ?: 0) + 1).toString()
                                    var targetX = pos.x
                                    var targetY = pos.y
                                    var found = false
                                    val maxRadius = 15
                                    for (radius in 0..maxRadius) {
                                        for (dx in -radius..radius) {
                                            for (dy in -radius..radius) {
                                                if (kotlin.math.abs(dx) != radius && kotlin.math.abs(dy) != radius && radius != 0) continue
                                                val testX = targetX + dx * currentSnapPx / zoomLevel
                                                val testY = targetY + dy * currentSnapPx / zoomLevel
                                                if (!hasTableCollision(testX, testY, pos.shape, pos.seats, pos.rotation, currentSnapPx / zoomLevel, tablePositions)) {
                                                    targetX = testX
                                                    targetY = testY
                                                    found = true
                                                    break
                                                }
                                            }
                                            if (found) break
                                        }
                                        if (found) break
                                    }
                                    
                                    val newPos = pos.copy(x = targetX, y = targetY, isLocked = false)
                                    val newMap = tablePositions.toMutableMap().apply { this[id] = newPos }
                                    updatePositions(newMap)
                                    selectedTableId = id
                                    
                                    val minVisX = -panOffset.x / zoomLevel
                                    val maxVisX = (boxWidth - panOffset.x) / zoomLevel
                                    val minVisY = -panOffset.y / zoomLevel
                                    val maxVisY = (activeAreaHeight - panOffset.y) / zoomLevel
                                    
                                    val isRot = (pos.rotation % 180f) != 0f
                                    val tW = if (isRot) 2f * baseSnapSize else getTableWidthSquares(pos.shape, pos.seats) * baseSnapSize
                                    val tH = if (isRot) getTableWidthSquares(pos.shape, pos.seats) * baseSnapSize else 2f * baseSnapSize
                                    
                                    if (targetX < minVisX || targetX + tW > maxVisX || targetY < minVisY || targetY + tH > maxVisY) {
                                        centerView(newMap)
                                    }
                                    
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                            )
                        }
                    }
                }

                // --- TOOLBOX SUPERIOR (ZOOM & RESET) ---
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp)
                        .shadow(8.dp, RoundedCornerShape(24.dp))
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.White.copy(alpha = 0.95f))
                        .border(1.dp, CardBorderColor, RoundedCornerShape(24.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { zoomLevel = (zoomLevel / 1.15f).coerceAtLeast(0.4f) }) {
                        Icon(Icons.Default.Remove, "Zoom Out", tint = PrimaryAccent)
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
                        Icon(Icons.Default.Add, "Zoom In", tint = PrimaryAccent)
                    }

                    VerticalDivider(modifier = Modifier.height(24.dp), thickness = 1.dp, color = CardBorderColor)
                    VerticalDivider(modifier = Modifier.height(24.dp), thickness = 1.dp, color = CardBorderColor)
                    IconButton(onClick = { centerView(null) }) {
                        Icon(Icons.Default.CenterFocusStrong, "Enquadrar Tudo", tint = PrimaryAccent)
                    }
                    VerticalDivider(modifier = Modifier.height(24.dp), thickness = 1.dp, color = CardBorderColor)
                    IconButton(
                        onClick = {
                            if (undoStack.isNotEmpty()) {
                                val newRedo = redoStack.toMutableList()
                                newRedo.add(tablePositions)
                                redoStack = newRedo
                                
                                val newUndo = undoStack.toMutableList()
                                tablePositions = newUndo.removeLast()
                                undoStack = newUndo
                            }
                        },
                        enabled = undoStack.isNotEmpty()
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Desfazer", tint = if (undoStack.isNotEmpty()) PrimaryAccent else Color.Gray)
                    }
                    IconButton(
                        onClick = {
                            if (redoStack.isNotEmpty()) {
                                val newUndo = undoStack.toMutableList()
                                newUndo.add(tablePositions)
                                undoStack = newUndo
                                
                                val newRedo = redoStack.toMutableList()
                                tablePositions = newRedo.removeLast()
                                redoStack = newRedo
                            }
                        },
                        enabled = redoStack.isNotEmpty()
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, "Refazer", tint = if (redoStack.isNotEmpty()) PrimaryAccent else Color.Gray)
                    }
                }

                // --- BOTÕES DE ADIÇÃO (MENU FAB) ---
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    AnimatedVisibility(
                        visible = showAddTableModal, 
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            val templates = listOf(
                                Triple("square", "Mesa Quadrada", 4),
                                Triple("round", "Mesa Redonda", 4),
                                Triple("rectangle", "Mesa Retangular", 6),
                                Triple("counter", "Balcão", 4)
                            )
                            templates.forEach { item ->
                                val shape = item.first
                                val label = item.second
                                val seats = item.third
                                val rot = if (shape == "rectangle" || shape == "counter") 90f else 0f
                                
                                ExtendedFloatingActionButton(
                                    text = { Text(label, fontWeight = FontWeight.Bold, fontSize = 14.sp) },
                                    icon = { Icon(Icons.Default.Add, null) },
                                    onClick = {
                                        val id = if(shape == "counter") "counter_${System.currentTimeMillis()}" else ((tablePositions.keys.mapNotNull { it.toIntOrNull() }.maxOrNull() ?: 0) + 1).toString()
                                        
                                        val centerX = (-panOffset.x + (boxWidth / 2f))
                                        val centerY = (-panOffset.y + (activeAreaHeight / 2f))
                                        
                                        var w = if (rot == 90f) 2f else getTableWidthSquares(shape, seats).toFloat()
                                        
                                        var targetX = (kotlin.math.round((centerX - (w * currentSnapPx / 2f)) / currentSnapPx) * currentSnapPx) / zoomLevel
                                        var targetY = (kotlin.math.round((centerY - currentSnapPx) / currentSnapPx) * currentSnapPx) / zoomLevel

                                        var found = false
                                        val maxRadius = 15
                                        for (radius in 0..maxRadius) {
                                            for (dx in -radius..radius) {
                                                for (dy in -radius..radius) {
                                                    if (kotlin.math.abs(dx) != radius && kotlin.math.abs(dy) != radius && radius != 0) continue
                                                    val testX = targetX + dx * currentSnapPx / zoomLevel
                                                    val testY = targetY + dy * currentSnapPx / zoomLevel
                                                    if (!hasTableCollision(testX, testY, shape, seats, rot, currentSnapPx / zoomLevel, tablePositions)) {
                                                        targetX = testX
                                                        targetY = testY
                                                        found = true
                                                        break
                                                    }
                                                }
                                                if (found) break
                                            }
                                            if (found) break
                                        }

                                        val newMap = tablePositions.toMutableMap().apply { 
                                            this[id] = TablePosition(targetX, targetY, shape, seats, rot)
                                        }
                                        updatePositions(newMap)
                                        selectedTableId = id
                                        
                                        val minVisX = -panOffset.x / zoomLevel
                                        val maxVisX = (boxWidth - panOffset.x) / zoomLevel
                                        val minVisY = -panOffset.y / zoomLevel
                                        val maxVisY = (activeAreaHeight - panOffset.y) / zoomLevel
                                        val tW = if (rot == 90f) 2f * baseSnapSize else getTableWidthSquares(shape, seats) * baseSnapSize
                                        val tH = if (rot == 90f) getTableWidthSquares(shape, seats) * baseSnapSize else 2f * baseSnapSize
                                        
                                        if (targetX < minVisX || targetX + tW > maxVisX || targetY < minVisY || targetY + tH > maxVisY) {
                                            centerView(newMap)
                                        }

                                        showAddTableModal = false
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    },
                                    containerColor = Color.White,
                                    contentColor = PrimaryAccent,
                                    elevation = FloatingActionButtonDefaults.elevation(4.dp)
                                )
                            }
                        }
                    }

                    ExtendedFloatingActionButton(
                        onClick = { showAddTableModal = !showAddTableModal },
                        containerColor = PrimaryAccent,
                        contentColor = Color.White,
                        icon = {
                            Icon(
                                imageVector = if (showAddTableModal) Icons.Default.Close else Icons.Default.Add,
                                contentDescription = "Adicionar móveis"
                            )
                        },
                        text = {
                            Text(if (showAddTableModal) "Fechar menu" else "Adicionar móveis", fontWeight = FontWeight.Bold)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun GridCanvas(snapSize: Float) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val color = GridColor
        // Grid ampliado para cobrir toda a área de pan
        for (i in -100..200) {
            drawLine(color, Offset(i * snapSize, -5000f), Offset(i * snapSize, 8000f), 1f)
            drawLine(color, Offset(-5000f, i * snapSize), Offset(8000f, i * snapSize), 1f)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigTopBar(title: String, onBack: () -> Unit) {
    CenterAlignedTopAppBar(
        title = { Text(title, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1E293B)) },
        navigationIcon = {
            IconButton(onClick = onBack) { 
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar", tint = PrimaryAccent) 
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = AppBackground),
        modifier = Modifier.shadow(2.dp)
    )
}

@Composable
fun DraggableTableItem(
    tableId: String,
    position: TablePosition,
    widthPx: Float,
    heightPx: Float,
    snapSize: Float,
    cardSize: Float,
    zoomLevel: Float,
    isSelected: Boolean,
    isTablet: Boolean,
    boundaryWidth: Float,
    boundaryHeight: Float,
    panOffset: Offset,
    tablePositions: Map<String, TablePosition>,
    onSelect: (String) -> Unit,
    onUpdate: (String, TablePosition) -> Unit,
    onDelete: (String) -> Unit,
    onCopy: () -> Unit
) {
    val density = LocalDensity.current
    val offsetX = remember { mutableStateOf(position.x * zoomLevel) }
    val offsetY = remember { mutableStateOf(position.y * zoomLevel) }

    var isDragging by remember { mutableStateOf(false) }

    LaunchedEffect(position.x, position.y, zoomLevel) {
        if (!isDragging) {
            offsetX.value = position.x * zoomLevel
            offsetY.value = position.y * zoomLevel
        }
    }
    val squaresW = getTableWidthSquares(position.shape, position.seats)
    val scale by animateFloatAsState(if (isSelected) 1.05f else 1f, label = "tableScale")
    val alpha by animateFloatAsState(if (isSelected) 1f else 0.85f, label = "tableAlpha")

    Box(
        modifier = Modifier
            .size(with(density) { widthPx.toDp() }, with(density) { heightPx.toDp() })
            .offset { IntOffset(offsetX.value.roundToInt(), offsetY.value.roundToInt()) }
            .scale(scale)
            .alpha(alpha)
            .pointerInput(tableId, isSelected) {
                detectTapGestures(
                    onTap = { onSelect(tableId) }
                )
            }
            .then(
                if (position.isLocked) Modifier 
                else Modifier.pointerInput(tableId, zoomLevel, position) {
                    var dragAccumulatorX = 0f
                    var dragAccumulatorY = 0f
                    detectDragGestures(
                        onDragStart = { 
                            isDragging = true
                            dragAccumulatorX = offsetX.value
                            dragAccumulatorY = offsetY.value
                        },
                        onDragEnd = {
                            isDragging = false
                            if (dragAccumulatorY + panOffset.y + (cardSize/2) >= boundaryHeight) {
                                onDelete(tableId)
                            } else {
                                var snX = kotlin.math.round(dragAccumulatorX / snapSize) * snapSize
                                var snY = kotlin.math.round(dragAccumulatorY / snapSize) * snapSize
                                val targetX = snX / zoomLevel
                                val targetY = snY / zoomLevel

                                if (hasTableCollision(targetX, targetY, position.shape, position.seats, position.rotation, snapSize / zoomLevel, tablePositions, tableId)) {
                                    // Collision: Revert position
                                    offsetX.value = position.x * zoomLevel
                                    offsetY.value = position.y * zoomLevel
                                } else {
                                    onUpdate(tableId, position.copy(x = targetX, y = targetY))
                                    offsetX.value = snX
                                    offsetY.value = snY
                                }
                            }
                        },
                        onDragCancel = { isDragging = false },
                        onDrag = { change, amount -> 
                            change.consume()
                            dragAccumulatorX += amount.x; dragAccumulatorY += amount.y
                            offsetX.value = dragAccumulatorX; offsetY.value = dragAccumulatorY
                        }
                    )
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(modifier = Modifier
            .fillMaxSize()
            .then(if (isSelected && !position.isLocked) {
                Modifier.border(2.dp, PrimaryAccent, RoundedCornerShape(12.dp))
            } else Modifier)
        ) {
            TableCard(
                tableNumber = tableId.toIntOrNull() ?: 0,
                status = "FREE",
                totalBill = 0.0,
                shape = position.shape,
                seats = position.seats,
                rotationAngle = position.rotation,
                snapSizePx = snapSize
            )
        }
        
        if (isSelected && !isDragging) {
            TableActionHUD(
                position = position,
                widthPx = widthPx,
                heightPx = heightPx,
                isTablet = isTablet,
                onSeatsChange = { inc -> 
                    val minSeats = if (position.shape == "counter") 1 else 2
                    var newSeats = (position.seats + inc).coerceAtLeast(minSeats)
                    var newShape = position.shape
                    
                    if (position.shape == "rectangle" && newSeats <= 4) {
                        newShape = "square"
                        newSeats = 4
                    } else if (position.shape == "square" && newSeats > 4) {
                        newShape = "rectangle"
                    }
                    
                    onUpdate(tableId, position.copy(seats = newSeats, shape = newShape)) 
                },
                onRotate = { onUpdate(tableId, position.copy(rotation = if(position.rotation == 0f) 90f else 0f)) },
                onToggleLock = { onUpdate(tableId, position.copy(isLocked = !position.isLocked)) },
                onDelete = { onDelete(tableId) },
                onCopy = onCopy
            )
        }
    }
}

fun hasTableCollision(
    x: Float,
    y: Float,
    shape: String,
    seats: Int,
    rotation: Float,
    snapSizePx: Float,
    existingTables: Map<String, TablePosition>,
    ignoreId: String? = null
): Boolean {
    val isRotated = (rotation % 180f) != 0f
    val wSq = getTableWidthSquares(shape, seats)
    val w = if (isRotated) 2f else wSq.toFloat()
    val h = if (isRotated) wSq.toFloat() else 2f
    
    val r1L = x + 1.5f; val r1R = x + w * snapSizePx - 1.5f
    val r1T = y + 1.5f; val r1B = y + h * snapSizePx - 1.5f
    
    for ((id, pos) in existingTables) {
        if (id == ignoreId) continue
        val posRotated = (pos.rotation % 180f) != 0f
        val posWSq = getTableWidthSquares(pos.shape, pos.seats)
        val posW = if (posRotated) 2f else posWSq.toFloat()
        val posH = if (posRotated) posWSq.toFloat() else 2f
        
        val r2L = pos.x + 1.5f; val r2R = pos.x + posW * snapSizePx - 1.5f
        val r2T = pos.y + 1.5f; val r2B = pos.y + posH * snapSizePx - 1.5f
        
        if (r1L < r2R && r1R > r2L && r1T < r2B && r1B > r2T) {
            return true
        }
    }
    return false
}

@Composable
fun TableActionHUD(
    position: TablePosition,
    widthPx: Float,
    heightPx: Float,
    isTablet: Boolean,
    onSeatsChange: (Int) -> Unit,
    onRotate: () -> Unit,
    onToggleLock: () -> Unit,
    onDelete: () -> Unit,
    onCopy: () -> Unit
) {
    val iconSize = if (isTablet) 42.dp else 48.dp
    val subIconSize = if (isTablet) 24.dp else 28.dp
    val density = LocalDensity.current
    val l = -widthPx / 2f
    val t = -heightPx / 2f
    val r = widthPx / 2f
    val b = heightPx / 2f

    @Composable
    fun ActionBubble(offset: Offset, icon: ImageVector, color: Color, onClick: () -> Unit) {
        Box(
            modifier = Modifier
                .offset { IntOffset(offset.x.roundToInt(), offset.y.roundToInt()) }
                .size(iconSize)
                .background(Color.White, CircleShape)
                .border(0.5.dp, Color.LightGray.copy(alpha=0.5f), CircleShape)
                .clickable(onClick = onClick),
            Alignment.Center
        ) { Icon(icon, null, Modifier.size(subIconSize), color) }
    }

    if (!position.isLocked) {
        ActionBubble(Offset(l, t), Icons.Default.Delete, Color.Red, onDelete)
        ActionBubble(Offset(r, t), if(position.shape == "counter") Icons.AutoMirrored.Filled.ArrowForward else Icons.Default.Add, Color(0xFF10B981), { onSeatsChange(2) })
        ActionBubble(Offset(r, b), if(position.shape == "counter") Icons.AutoMirrored.Filled.ArrowBack else Icons.Default.Remove, Color(0xFFF59E0B), { onSeatsChange(-2) })
        
        if (position.shape == "rectangle" || position.shape == "counter" || 
            (position.shape == "square" && position.seats == 2) || 
            (position.shape == "round" && position.seats == 2)) {
            ActionBubble(Offset(l, b), Icons.Default.Refresh, PrimaryAccent, onRotate)
        }
        
        // Botão de cópia posicionado centralizado à direita
        ActionBubble(Offset(r, 0f), Icons.Default.ContentCopy, PrimaryAccent, onCopy)
    }
    
    // O botão de trava/destrava fica sempre visível e no centro esquerdo da mesa
    ActionBubble(Offset(l, 0f), if (position.isLocked) Icons.Default.Lock else Icons.Default.LockOpen, PrimaryAccent, onToggleLock)
}

@Composable
fun ConstructionSidebar(
    modifier: Modifier,
    onDragStart: (Triple<String, String, Int>, Offset) -> Unit,
    onDragging: (Offset) -> Unit,
    onDragEnd: (Triple<String, String, Int>, Offset) -> Unit
) {
    val templates = listOf(
        Triple("square", "Quadrada", 4),
        Triple("round", "Redonda", 4),
        Triple("rectangle", "Familiar", 6),
        Triple("rectangle", "Grande", 10),
        Triple("round", "Bistrô", 2),
        Triple("counter", "Balcão", 4)
    )

    Column(
        modifier = modifier
            .background(Color.White.copy(alpha = 0.98f))
            .border(1.dp, CardBorderColor.copy(alpha = 0.5f), RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp))
            .padding(top = 80.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "ADICIONAR\nITENS", 
            fontSize = 11.sp, 
            fontWeight = FontWeight.ExtraBold, 
            color = PrimaryAccent.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            lineHeight = 14.sp
        )
        
        Spacer(Modifier.height(24.dp))
        
        LazyColumn(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(bottom = 40.dp)
        ) {
            items(templates) { item ->
                val shape = item.first
                val label = item.second
                val seats = item.third
                var localPos by remember { mutableStateOf(Offset.Zero) }
                var currentDragOffset by remember { mutableStateOf(Offset.Zero) }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .onGloballyPositioned { localPos = it.positionInRoot() }
                        .pointerInput(item) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { offset -> 
                                    currentDragOffset = localPos + offset
                                    onDragStart(item, currentDragOffset) 
                                },
                                onDrag = { change, _ -> 
                                    change.consume()
                                    currentDragOffset = localPos + change.position
                                    onDragging(currentDragOffset) 
                                },
                                onDragEnd = { onDragEnd(item, currentDragOffset) },
                                onDragCancel = { onDragEnd(item, Offset.Zero) }
                            )
                        }
                ) {
                    val rot = if (shape == "rectangle" || shape == "counter") 90f else 0f
                    Box(Modifier.scale(0.5f)) {
                        TableCard(0, "FREE", 0.0, shape, seats, rot, snapSizePx = 100f)
                    }
                    Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextDark)
                }
            }
        }
    }
}

@Composable
fun LoadingState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = PrimaryAccent, strokeWidth = 3.dp)
            Spacer(Modifier.height(16.dp))
            Text("Carregando Planta...", color = Color.Gray, fontWeight = FontWeight.Medium)
        }
    }
}
