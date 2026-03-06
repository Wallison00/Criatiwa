package com.walli.flexcriatiwa

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.zIndex
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.layout.onGloballyPositioned
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryManagementScreen(
    managementViewModel: ManagementViewModel,
    onNavigateBack: () -> Unit
) {
    var showAddCategoryDialog by remember { mutableStateOf(false) }

    if (showAddCategoryDialog) {
        SimpleInputDialog(
            title = "Nova Categoria",
            label = "Nome (Ex: Lanches)",
            onDismiss = { showAddCategoryDialog = false },
            onConfirm = {
                managementViewModel.addCategory(it)
                showAddCategoryDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Estrutura do Cardápio", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    // --- MUDANÇA AQUI: Botão movido para a TopBar ---
                    IconButton(onClick = { showAddCategoryDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Nova Categoria")
                    }
                }
            )
        }
        // FloatingActionButton removido daqui
    ) { innerPadding ->
        if (managementViewModel.categoryConfigs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Menu, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.Gray)
                    Spacer(Modifier.height(16.dp))
                    Text("Nenhuma categoria configurada.", fontSize = 18.sp, fontWeight = FontWeight.Medium, color = Color.Gray)
                    Text("Clique no '+' acima para começar.", fontSize = 14.sp, color = Color.Gray)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(managementViewModel.categoryConfigs) { categoryConfig ->
                    CategoryConfigCard(
                        config = categoryConfig,
                        onDeleteCategory = { managementViewModel.deleteCategory(categoryConfig.name) },
                        onAddIngredient = { ing -> managementViewModel.addIngredientToCategory(categoryConfig.name, ing) },
                        onRemoveIngredient = { ing -> managementViewModel.removeIngredientFromCategory(categoryConfig.name, ing) },
                        onUpdateIngredientsList = { list -> managementViewModel.updateIngredientsList(categoryConfig.name, list) },
                        onAddOptional = { opt -> managementViewModel.addOptionalToCategory(categoryConfig.name, opt) },
                        onRemoveOptional = { opt -> managementViewModel.removeOptionalFromCategory(categoryConfig.name, opt) },
                        onUpdateOptionalsList = { list -> managementViewModel.updateOptionalsList(categoryConfig.name, list) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryConfigCard(
    config: CategoryConfig,
    onDeleteCategory: () -> Unit,
    onAddIngredient: (String) -> Unit,
    onRemoveIngredient: (String) -> Unit,
    onUpdateIngredientsList: (List<String>) -> Unit,
    onAddOptional: (OptionalItem) -> Unit,
    onRemoveOptional: (OptionalItem) -> Unit,
    onUpdateOptionalsList: (List<OptionalItem>) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var showAddIngredientDialog by remember { mutableStateOf(false) }
    var showAddOptionalDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Excluir Categoria") },
            text = { Text("Deseja realmente excluir a categoria '${config.name}' e todas as suas configurações em cascata?") },
            confirmButton = {
                Button(onClick = { showDeleteConfirmDialog = false; onDeleteCategory() }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                    Text("Excluir")
                }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirmDialog = false }) { Text("Cancelar") } }
        )
    }

    if (showAddIngredientDialog) {
        SimpleInputDialog("Novo Ingrediente", "Nome (Ex: Alface, Pão...)", { showAddIngredientDialog = false }) {
            onAddIngredient(it)
            showAddIngredientDialog = false
        }
    }
    if (showAddOptionalDialog) {
        OptionalInputDialog({ showAddOptionalDialog = false }) { name, price ->
            onAddOptional(OptionalItem(name, price))
            showAddOptionalDialog = false
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(config.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = { showDeleteConfirmDialog = true }) { 
                    Icon(Icons.Default.Delete, "Excluir", tint = MaterialTheme.colorScheme.error) 
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(Modifier.padding(top = 16.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    Spacer(Modifier.height(16.dp))

                    // Base Ingredients Section
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.List, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Ingredientes Padrão", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                                TextButton(onClick = { showAddIngredientDialog = true }, contentPadding = PaddingValues(horizontal = 8.dp)) { 
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Adicionar") 
                                }
                            }
                            
                            if (config.defaultIngredients.isEmpty()) {
                                Text("Nenhum ingrediente padrão cadastrado.", style = MaterialTheme.typography.bodySmall, color = Color.Gray, modifier = Modifier.padding(top = 8.dp))
                            } else {
                                Box(modifier = Modifier.padding(top = 8.dp)) {
                                    SortableList(
                                        items = config.defaultIngredients,
                                        onUpdateList = onUpdateIngredientsList
                                    ) { ing, isDragging ->
                                        StandardCategoryItem(
                                            title = ing,
                                            isDragging = isDragging,
                                            onRemove = { onRemoveIngredient(ing) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Optionals Section
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AddCircleOutline, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Opcionais / Adicionais", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                                }
                                TextButton(onClick = { showAddOptionalDialog = true }, contentPadding = PaddingValues(horizontal = 8.dp)) { 
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Adicionar") 
                                }
                            }
                            
                            if (config.availableOptionals.isEmpty()) {
                                Text("Nenhum item opcional cadastrado.", style = MaterialTheme.typography.bodySmall, color = Color.Gray, modifier = Modifier.padding(top = 8.dp))
                            } else {
                                Box(modifier = Modifier.padding(top = 8.dp)) {
                                    SortableList(
                                        items = config.availableOptionals,
                                        onUpdateList = onUpdateOptionalsList
                                    ) { opt, isDragging ->
                                        StandardCategoryItem(
                                            title = opt.name,
                                            price = "+ R$ ${"%.2f".format(opt.price)}",
                                            isDragging = isDragging,
                                            onRemove = { onRemoveOptional(opt) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SimpleInputDialog(title: String, label: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text(label) }, singleLine = true) },
        confirmButton = { Button(onClick = { if(text.isNotBlank()) onConfirm(text) }) { Text("Adicionar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
fun OptionalInputDialog(onDismiss: () -> Unit, onConfirm: (String, Double) -> Unit) {
    var name by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Novo Opcional") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nome (Ex: Bacon)") })
                OutlinedTextField(
                    value = price,
                    onValueChange = { if(it.all { c -> c.isDigit() }) price = it },
                    label = { Text("Preço (Centavos)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    visualTransformation = CurrencyVisualTransformation()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                if(name.isNotBlank() && price.isNotBlank()) onConfirm(name, (price.toLongOrNull() ?: 0L) / 100.0)
            }) { Text("Adicionar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
fun StandardCategoryItem(
    title: String,
    price: String? = null,
    isDragging: Boolean,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (isDragging) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.DragHandle, contentDescription = "Segure para Reordenar", tint = Color.Gray, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(12.dp))
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (price != null) {
                Text(price, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
            }
            IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun <T> SortableList(
    items: List<T>,
    onUpdateList: (List<T>) -> Unit,
    itemContent: @Composable (item: T, isDragging: Boolean) -> Unit
) {
    if (items.isEmpty()) return
    
    var localItems by remember(items) { mutableStateOf(items) }
    var draggedIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetY by remember { mutableStateOf(0f) }
    var itemHeightPx by remember { mutableStateOf(0) }
    val density = androidx.compose.ui.platform.LocalDensity.current.density

    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(items) { 
                detectDragGesturesAfterLongPress(
                    onDragStart = { offset ->
                        if (itemHeightPx > 0) {
                            draggedIndex = (offset.y / itemHeightPx).toInt().coerceIn(0, localItems.lastIndex)
                            dragOffsetY = 0f
                        }
                    },
                    onDragEnd = {
                        draggedIndex = null
                        dragOffsetY = 0f
                        onUpdateList(localItems)
                    },
                    onDragCancel = {
                        draggedIndex = null
                        dragOffsetY = 0f
                        localItems = items
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragOffsetY += dragAmount.y
                        
                        val currentIndex = draggedIndex
                        if (currentIndex != null && itemHeightPx > 0) {
                            val targetIndex = (currentIndex + (dragOffsetY / itemHeightPx).roundToInt()).coerceIn(0, localItems.lastIndex)
                            if (targetIndex != currentIndex) {
                                // Swap
                                val list = localItems.toMutableList()
                                val temp = list[currentIndex]
                                list[currentIndex] = list[targetIndex]
                                list[targetIndex] = temp
                                localItems = list
                                
                                draggedIndex = targetIndex
                                dragOffsetY -= (targetIndex - currentIndex) * itemHeightPx
                            }
                        }
                    }
                )
            }
    ) {
        localItems.forEachIndexed { index, item ->
            val isDragging = index == draggedIndex
            val offset = if (isDragging) dragOffsetY else 0f
            val zIndex = if (isDragging) 1f else 0f

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .zIndex(zIndex)
                    .offset { IntOffset(0, offset.roundToInt()) }
                    .onGloballyPositioned { coordinates ->
                        if (itemHeightPx == 0) itemHeightPx = coordinates.size.height + (4 * density).toInt()
                    }
            ) {
                itemContent(item, isDragging)
            }
        }
    }
}