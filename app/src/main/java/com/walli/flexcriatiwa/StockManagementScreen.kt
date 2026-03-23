package com.walli.flexcriatiwa

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.budiyev.android.codescanner.CodeScanner
import com.budiyev.android.codescanner.CodeScannerView
import com.budiyev.android.codescanner.DecodeCallback
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

fun Double.formatQty(): String = if (this % 1.0 == 0.0) this.toLong().toString() else this.toString()

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun StockManagementScreen(
    managementViewModel: ManagementViewModel,
    userName: String,
    onNavigateBack: () -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var itemsToDelete by remember { mutableStateOf<Set<StockItem>>(emptySet()) }
    var selectedItems by remember { mutableStateOf<Set<StockItem>>(emptySet()) }
    var itemToEdit by remember { mutableStateOf<StockItem?>(null) }
    var initialBarcodeForAdd by remember { mutableStateOf("") }
    
    // States para o Menu do FAB e os novos Dialogs
    var fabMenuExpanded by remember { mutableStateOf(false) }
    var showEntryDialog by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }


    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Estoque de Insumos", fontWeight = FontWeight.Bold)
                        Text(
                            text = "Company ID: ${managementViewModel.currentCompany?.id ?: "Desconhecido"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar") }
                }
            )
        },
        floatingActionButton = {
            Box {
                FloatingActionButton(
                    onClick = { fabMenuExpanded = true },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Dehaze, "Ações do Estoque")
                }
                DropdownMenu(
                    expanded = fabMenuExpanded,
                    onDismissRequest = { fabMenuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Cadastro de Insumo") },
                        leadingIcon = { Icon(Icons.Default.AddCircle, null) },
                        onClick = { fabMenuExpanded = false; itemToEdit = null; initialBarcodeForAdd = ""; showAddDialog = true }
                    )
                    DropdownMenuItem(
                        text = { Text("Adicionar Saldo") },
                        leadingIcon = { Icon(Icons.Default.QrCodeScanner, null) },
                        onClick = { fabMenuExpanded = false; showEntryDialog = true }
                    )
                    DropdownMenuItem(
                        text = { Text("Histórico") },
                        leadingIcon = { Icon(Icons.Default.History, null) },
                        onClick = { fabMenuExpanded = false; showHistoryDialog = true }
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            val stockItems = managementViewModel.stockItems
            val alertCount = stockItems.count { it.quantity <= it.minQuantity }

            managementViewModel.errorMessage?.let { errorMsg ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    Text(
                        text = errorMsg,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // --- RESUMO DE ESTOQUE ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StockSummaryCard("Total Itens", "${stockItems.size}", Icons.Default.Inventory2, Color(0xFF2196F3), Modifier.weight(1f))
                StockSummaryCard("Alerta Mínimo", "$alertCount", Icons.Default.Warning, if (alertCount > 0) Color.Red else Color(0xFFFF9800), Modifier.weight(1f))
            }

            Spacer(Modifier.height(24.dp))

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("Lista de Insumos", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                if (selectedItems.isNotEmpty()) {
                    AssistChip(
                        onClick = { itemsToDelete = selectedItems },
                        label = { Text("Apagar (${selectedItems.size})", color = Color.Red, fontWeight = FontWeight.Bold) },
                        colors = AssistChipDefaults.assistChipColors(containerColor = Color(0xFFFFEBEE))
                    )
                }
            }
            Spacer(Modifier.height(16.dp))

            if (stockItems.isEmpty()) {
                // Interface placeholder para lista vazia
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Inventory, null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
                        Spacer(Modifier.height(16.dp))
                        Text("Nenhum insumo cadastrado", color = Color.Gray)
                        Text("Utilize o botão de ações para cadastrar", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                }
            } else {
                val density = androidx.compose.ui.platform.LocalDensity.current
                val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
                val maxSwipePx = with(density) { 80.dp.toPx() } // Abre exatamente 80dp

                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
                    items(stockItems, key = { it.id }) { item ->
                        val isAlert = item.quantity <= item.minQuantity
                        val offsetX = remember { Animatable(0f) }
                        val scope = rememberCoroutineScope()

                        Box(modifier = Modifier.fillMaxWidth()) {
                            // CORTINA REVELADA (FUNDO VERMELHO)
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .padding(vertical = 4.dp)
                                    .background(Color.Red.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                    .clickable {
                                        itemsToDelete = setOf(item)
                                        scope.launch { offsetX.animateTo(0f) } // fecha de volta
                                    }
                                    .padding(end = 24.dp),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = Color.Red)
                            }

                            // CARTÃO DO ITEM POR CIMA DA CORTINA
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(end = with(density) { (-offsetX.value).toDp() }) // Reduz o tamanho horizontalmente, espremendo o card
                                    .pointerInput(selectedItems.isEmpty()) { 
                                        // O arrasto só funciona se NÃO houver múltiplos itens selecionados
                                        if (selectedItems.isEmpty()) {
                                            detectHorizontalDragGestures(
                                                onDragEnd = {
                                                    scope.launch {
                                                        if (offsetX.value < -maxSwipePx / 2) offsetX.animateTo(-maxSwipePx)
                                                        else offsetX.animateTo(0f)
                                                    }
                                                }
                                            ) { change, dragAmount ->
                                                change.consume()
                                                scope.launch {
                                                    offsetX.snapTo((offsetX.value + dragAmount).coerceIn(-maxSwipePx, 0f))
                                                }
                                            }
                                        }
                                    }
                                    .combinedClickable(
                                        onLongClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            selectedItems = setOf(item)
                                        },
                                        onClick = {
                                            if (selectedItems.isNotEmpty()) {
                                                selectedItems = if (selectedItems.contains(item)) selectedItems - item else selectedItems + item
                                            } else {
                                                itemToEdit = item; initialBarcodeForAdd = ""; showAddDialog = true
                                            }
                                        }
                                    ),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                    elevation = CardDefaults.cardElevation(2.dp)
                                ) {
                                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        // CHECKBOX
                                        if (selectedItems.isNotEmpty()) {
                                            Checkbox(
                                                checked = selectedItems.contains(item),
                                                onCheckedChange = { isChecked ->
                                                    selectedItems = if (isChecked) selectedItems + item else selectedItems - item
                                                }
                                            )
                                            Spacer(Modifier.width(8.dp))
                                        }

                                // IMAGEM
                                Box(
                                    modifier = Modifier.size(64.dp).background(Color.White, RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val bitmap = remember(item.imageUrl) {
                                        if (item.imageUrl.isNotBlank() && item.imageUrl.startsWith("data:image")) {
                                            try {
                                                val base64String = item.imageUrl.substringAfter(",")
                                                val decodedBytes = android.util.Base64.decode(base64String, android.util.Base64.DEFAULT)
                                                val btm = android.graphics.BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                                                btm?.asImageBitmap()
                                            } catch (_: Exception) { null }
                                        } else null
                                    }

                                    if (bitmap != null) {
                                        Image(bitmap = bitmap, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                    } else {
                                        Icon(Icons.Default.Inventory, null, tint = Color.LightGray)
                                    }
                                }

                                Spacer(Modifier.width(16.dp))

                                // INFOS
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineSmall)
                                    if (item.barcode.isNotBlank()) Text("Cód: ${item.barcode}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                }
                                
                                // QUANTIDADE E STATUS
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("${item.quantity.formatQty()} ${item.unit}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = if (isAlert) Color.Red else MaterialTheme.colorScheme.onSurface)
                                    if (isAlert) {
                                        Text("Estoque Baixo!", style = MaterialTheme.typography.labelSmall, color = Color.Red, fontWeight = FontWeight.Bold)
                                    } else {
                                        Text("Mín: ${item.minQuantity.formatQty()}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    }
                                }
                            } // fecha Row
                        } // fecha Card
                    } // fecha Box do Swipe-to-Reveal
            } // fecha items
        } // fecha LazyColumn
    } // fecha else
} // fecha Column principal do Scaffold
} // fecha o content padding do Scaffold



    if (showAddDialog) {
        StockAddDialog(
            managementViewModel = managementViewModel,
            userName = userName,
            itemToEdit = itemToEdit,
            initialBarcode = initialBarcodeForAdd,
            onDismiss = { showAddDialog = false; itemToEdit = null }
        )
    }

    if (showEntryDialog) {
        StockEntryDialog(
            managementViewModel = managementViewModel,
            userName = userName,
            onDismiss = { showEntryDialog = false },
            onRequestNewItem = { code ->
                initialBarcodeForAdd = code
                itemToEdit = null
                showEntryDialog = false
                showAddDialog = true
            }
        )
    }

    if (showHistoryDialog) {
        StockHistoryDialog(
            historyItems = managementViewModel.stockHistoryItems,
            onDismiss = { showHistoryDialog = false }
        )
    }

    if (itemsToDelete.isNotEmpty()) {
        val qty = itemsToDelete.size
        val msg = if (qty == 1) "Tem certeza que deseja apagar o insumo '${itemsToDelete.first().name}'? Esta ação não pode ser desfeita." else "Tem certeza que deseja apagar ${qty} insumos? Esta ação não pode ser desfeita."
        AlertDialog(
            onDismissRequest = { itemsToDelete = emptySet() },
            title = { Text(if (qty == 1) "Apagar Insumo" else "Apagar $qty Insumos") },
            text = { Text(msg) },
            confirmButton = {
                Button(onClick = {
                    itemsToDelete.forEach { managementViewModel.deleteStockItem(it, userName) }
                    selectedItems = emptySet()
                    itemsToDelete = emptySet()
                }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) {
                    Text("Apagar")
                }
            },
            dismissButton = { TextButton(onClick = { itemsToDelete = emptySet() }) { Text("Cancelar") } }
        )
    }
}

@Composable
fun StockSummaryCard(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Column(Modifier.padding(16.dp)) {
            Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(8.dp))
            Text(title, style = MaterialTheme.typography.labelMedium, color = color)
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockAddDialog(managementViewModel: ManagementViewModel, userName: String, itemToEdit: StockItem? = null, initialBarcode: String = "", onDismiss: () -> Unit) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(itemToEdit?.name ?: "") }
    var minQuantity by remember { mutableStateOf(itemToEdit?.minQuantity?.formatQty() ?: "") }
    var barcode by remember { mutableStateOf(if (itemToEdit != null) itemToEdit.barcode else initialBarcode) }
    
    // Dropdown state
    var expanded by remember { mutableStateOf(false) }
    val options = listOf("Unidade", "Kg", "Grama", "Litro", "ML", "Lata", "Garrafa", "Fardo", "Caixa", "Pacote")
    var selectedUnit by remember { mutableStateOf(itemToEdit?.unit ?: options[0]) }

    // Image state
    var newSelectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var showImageSourceDialog by remember { mutableStateOf(false) }
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }
    var initialBase64Image by remember { mutableStateOf(itemToEdit?.imageUrl) }
    
    // Scanner state
    var isScanning by remember { mutableStateOf(false) }
    var hasCameraPermission by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) }
    
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        hasCameraPermission = it
        if (it) isScanning = true
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> 
        if (uri != null) newSelectedImageUri = uri 
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && tempCameraUri != null) newSelectedImageUri = tempCameraUri
    }

    if (isScanning && hasCameraPermission) {
        DialogWithScanner(
            onScanResult = {
                barcode = it
                isScanning = false
            },
            onClose = { isScanning = false }
        )
        return
    }

    if (showImageSourceDialog) {
        AlertDialog(
            onDismissRequest = { showImageSourceDialog = false },
            title = { Text("Selecionar Imagem") },
            text = { Text("De onde você deseja adicionar a imagem? \n(Recomendamos tirar foto do produto)") },
            confirmButton = {
                TextButton(onClick = {
                    showImageSourceDialog = false
                    try {
                        val uri = createImageFileUri(context) // Usa a função existente global
                        tempCameraUri = uri
                        cameraLauncher.launch(uri)
                    } catch (e: Exception) { Log.e("Camera", "Erro ao criar arquivo", e) }
                }) {
                    Icon(Icons.Default.CameraAlt, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Câmera")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showImageSourceDialog = false
                    galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }) {
                    Icon(Icons.Default.PhotoLibrary, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Galeria")
                }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Novo Insumo / Produto") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // --- CARD DE IMAGEM ---
                Card(
                    modifier = Modifier.fillMaxWidth().height(160.dp).clickable { showImageSourceDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        if (newSelectedImageUri != null) {
                            AsyncImage(model = newSelectedImageUri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        } else if (!initialBase64Image.isNullOrBlank() && initialBase64Image!!.startsWith("data:image")) {
                            val bitmap = remember(initialBase64Image) {
                                try {
                                    val b64 = initialBase64Image!!.substringAfter(",")
                                    val bytes = android.util.Base64.decode(b64, android.util.Base64.DEFAULT)
                                    val btm = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                    btm?.asImageBitmap()
                                } catch (_: Exception) { null }
                            }
                            if (bitmap != null) Image(bitmap = bitmap, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            else Icon(Icons.Default.Image, null)
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.AddAPhoto, "Add Foto", Modifier.size(40.dp), tint = Color.Gray)
                                Spacer(Modifier.height(8.dp))
                                Text("Tocar para foto", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.replaceFirstChar(Char::uppercase) },
                    label = { Text("Nome do Produto") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                )
                
                // --- CÓDIGO DE BARRAS, DROPDOWN E MIN. QTD ---
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = barcode,
                        onValueChange = { barcode = it },
                        label = { Text("Cód. Barras") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    IconButton(
                        onClick = { 
                            if (hasCameraPermission) isScanning = true
                            else permissionLauncher.launch(Manifest.permission.CAMERA)
                        },
                        modifier = Modifier.size(56.dp).background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(8.dp))
                    ) {
                        Icon(Icons.Default.QrCodeScanner, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    // --- DROPDOWN UNIDADE ---
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = selectedUnit,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Unidade") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            options.forEach { selectionOption ->
                                DropdownMenuItem(
                                    text = { Text(selectionOption) },
                                    onClick = {
                                        selectedUnit = selectionOption
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(value = minQuantity, onValueChange = { minQuantity = it }, label = { Text("Alerta Min") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                }
            }
        },
        confirmButton = { 
            Button(
                onClick = {
                    val newItem = StockItem(
                        id = itemToEdit?.id ?: "",
                        name = name,
                        barcode = barcode,
                        quantity = itemToEdit?.quantity ?: 0.0,
                        minQuantity = minQuantity.toDoubleOrNull() ?: 0.0,
                        unit = selectedUnit,
                        imageUrl = itemToEdit?.imageUrl ?: ""
                    )
                    managementViewModel.saveStockItem(
                        context = context,
                        item = newItem,
                        newImageUri = newSelectedImageUri,
                        handledByName = userName,
                        onSuccess = { onDismiss() }
                    )
                },
                enabled = !managementViewModel.isUploading && name.isNotBlank()
            ) { 
                if (managementViewModel.isUploading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("Salvar") 
                }
            } 
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
fun DialogWithScanner(onScanResult: (String) -> Unit, onClose: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(factory = { ctx ->
            CodeScannerView(ctx).apply {
                val scanner = CodeScanner(ctx, this)
                scanner.decodeCallback = DecodeCallback { result ->
                    (ctx as? Activity)?.runOnUiThread { onScanResult(result.text) }
                }
                scanner.startPreview()
            }
        }, modifier = Modifier.fillMaxSize())
        
        Button(onClick = onClose, modifier = Modifier.align(Alignment.TopStart).padding(32.dp)) {
            Text("Cancelar Scanner")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockEntryDialog(managementViewModel: ManagementViewModel, userName: String, onDismiss: () -> Unit, onRequestNewItem: (String) -> Unit) {
    val context = LocalContext.current
    var searchedBarcode by remember { mutableStateOf("") }
    var foundItem by remember { mutableStateOf<StockItem?>(null) }
    var addQuantityStr by remember { mutableStateOf("") }
    var searchStatus by remember { mutableStateOf("Digite ou escaneie o código.") }

    var isScanning by remember { mutableStateOf(false) }
    val hasCameraPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) isScanning = true
    }

    if (isScanning) {
        DialogWithScanner(onScanResult = { code ->
            searchedBarcode = code
            isScanning = false
            foundItem = managementViewModel.stockItems.find { it.barcode == code }
            searchStatus = if (foundItem != null) "Insumo encontrado!" else "Nenhum insumo encontrado."
        }, onClose = { isScanning = false })
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.QrCodeScanner, null, tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(8.dp)); Text("Entrada Rápida", style = MaterialTheme.typography.titleLarge) } },
        text = {
            Column(Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = searchedBarcode,
                    onValueChange = { 
                        searchedBarcode = it 
                        foundItem = managementViewModel.stockItems.find { item -> item.barcode == it }
                        searchStatus = if (foundItem != null) "Insumo encontrado!" else if (it.isNotBlank()) "Nenhum insumo com este código." else "Digite ou escaneie o código."
                    },
                    label = { Text("Código de Barras") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    trailingIcon = {
                        IconButton(onClick = { if (hasCameraPermission) isScanning = true else permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                            Icon(Icons.Default.CameraAlt, "Scan", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                )
                
                Spacer(Modifier.height(8.dp))
                if (foundItem == null && searchedBarcode.isNotBlank()) {
                    Column {
                        Text(searchStatus, color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { onRequestNewItem(searchedBarcode) }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.AddCircle, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Cadastrar Novo Insumo")
                        }
                    }
                } else if (foundItem != null) {
                    Text(searchStatus, color = Color(0xFF4CAF50), style = MaterialTheme.typography.bodySmall)
                } else {
                    Text(searchStatus, color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                }

                foundItem?.let { item ->
                    Spacer(Modifier.height(16.dp))
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(Modifier.padding(12.dp).fillMaxWidth()) {
                            Text("Insumo: ${item.name}", style = MaterialTheme.typography.titleSmall)
                            Text("Quantidade atual: ${item.quantity.formatQty()} ${item.unit}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    
                    OutlinedTextField(
                        value = addQuantityStr,
                        onValueChange = { addQuantityStr = it },
                        label = { Text("Quantidade Adicionada") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            }
        },
        confirmButton = {
            if (foundItem != null) {
                Button(
                    onClick = {
                        val qtyToAdd = addQuantityStr.toDoubleOrNull() ?: 0.0
                        if (qtyToAdd > 0 && foundItem != null) {
                            managementViewModel.registerStockEntry(foundItem!!, qtyToAdd, userName) {
                                searchedBarcode = ""
                                addQuantityStr = ""
                                foundItem = null
                                onDismiss()
                            }
                        }
                    },
                    enabled = addQuantityStr.toDoubleOrNull()?.let { it > 0 } == true && !managementViewModel.isUploading
                ) {
                    if (managementViewModel.isUploading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                    else Text("Dar Entrada")
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Fechar") } }
    )
}

@Composable
fun StockHistoryDialog(historyItems: List<StockHistory>, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.History, null, tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(8.dp)); Text("Histórico de Entrada/Edição", style = MaterialTheme.typography.titleLarge) } },
        text = {
            if (historyItems.isEmpty()) {
                Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    Text("Nenhum histórico disponível.", color = Color.Gray)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().height(500.dp)) {
                    items(historyItems) { log ->
                        val icon = when (log.type) {
                            "CADASTRO" -> Icons.Default.AddCircle
                            "ENTRADA" -> Icons.Default.ArrowDownward
                            "EDICAO" -> Icons.Default.Edit
                            else -> Icons.Default.Delete
                        }
                        val color = when (log.type) {
                            "CADASTRO" -> Color(0xFF4CAF50)
                            "ENTRADA" -> Color(0xFF2196F3)
                            "EDICAO" -> Color(0xFFFF9800)
                            else -> Color.Red
                        }
                        val labelText = when (log.type) {
                            "CADASTRO" -> "+ Cadastrado"
                            "ENTRADA" -> "+ Entrou (${log.changeAmount.formatQty()})"
                            "EDICAO" -> "Mudança Qtd"
                            else -> "Removido"
                        }
                        
                        Column(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(log.itemName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Spacer(Modifier.weight(1f))
                                Text(labelText, color = color, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                            }
                            val df = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
                            Text("${df.format(log.timestamp.toDate())} por ${log.handledByName}", style = MaterialTheme.typography.bodySmall, color = Color.Gray, modifier = Modifier.padding(start = 28.dp))
                            HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("OK") } }
    )
}
