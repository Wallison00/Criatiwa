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
                    onAddOptional = { opt -> managementViewModel.addOptionalToCategory(categoryConfig.name, opt) },
                    onRemoveOptional = { opt -> managementViewModel.removeOptionalFromCategory(categoryConfig.name, opt) }
                )
            }
        }
    }
}

@Composable
fun CategoryConfigCard(
    config: CategoryConfig,
    onDeleteCategory: () -> Unit,
    onAddIngredient: (String) -> Unit,
    onRemoveIngredient: (String) -> Unit,
    onAddOptional: (OptionalItem) -> Unit,
    onRemoveOptional: (OptionalItem) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var showAddIngredientDialog by remember { mutableStateOf(false) }
    var showAddOptionalDialog by remember { mutableStateOf(false) }

    if (showAddIngredientDialog) {
        SimpleInputDialog("Novo Ingrediente", "Nome (Ex: Alface)", { showAddIngredientDialog = false }) {
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
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(config.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Row {
                    IconButton(onClick = onDeleteCategory) { Icon(Icons.Default.Delete, "Excluir", tint = MaterialTheme.colorScheme.error) }
                    Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null)
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(Modifier.padding(top = 16.dp)) {
                    HorizontalDivider()
                    Spacer(Modifier.height(8.dp))

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Ingredientes Padrão", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        IconButton(onClick = { showAddIngredientDialog = true }, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.AddCircle, null, tint = MaterialTheme.colorScheme.primary) }
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                        config.defaultIngredients.forEach { ing ->
                            InputChip(selected = true, onClick = { onRemoveIngredient(ing) }, label = { Text(ing) }, trailingIcon = { Icon(Icons.Default.Close, null, Modifier.size(16.dp)) })
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Opcionais / Adicionais", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                        IconButton(onClick = { showAddOptionalDialog = true }, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.AddCircle, null, tint = MaterialTheme.colorScheme.secondary) }
                    }
                    Column(Modifier.padding(vertical = 8.dp)) {
                        config.availableOptionals.forEach { opt ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("${opt.name} (+ R$ ${"%.2f".format(opt.price)})", style = MaterialTheme.typography.bodyMedium)
                                IconButton(onClick = { onRemoveOptional(opt) }, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Close, null, tint = Color.Gray) }
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