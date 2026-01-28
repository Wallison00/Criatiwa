package com.walli.flexcriatiwa

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

// --- ESTRUTURA DE DADOS PARA UM ITEM OPCIONAL ---
// Se você já tem essa data class em outro arquivo, não precisa copiar de novo.


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OptionalManagementScreen(
    managementViewModel: ManagementViewModel,
    onNavigateBack: () -> Unit
) {
    // --- Estados da Tela ---

    var showAddOptionalDialog by remember { mutableStateOf(false) }

    // --- Lógica do Diálogo ---
    if (showAddOptionalDialog) {
        AddOptionalDialog(
            onDismiss = { showAddOptionalDialog = false },
            onConfirm = { newOptional ->
                managementViewModel.addOptional(newOptional)
                showAddOptionalDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gerenciar Opcionais", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddOptionalDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar Opcional")
            }
        }
    ) { innerPadding ->
        // --- Lista de Opcionais ---
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(managementViewModel.optionals) { optionalItem ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Coluna para Nome e Preço
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = optionalItem.name,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "R$ ${"%.2f".format(optionalItem.price)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        // Botão de Excluir
                        IconButton(onClick = { managementViewModel.deleteOptional(optionalItem) }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Excluir Opcional",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}

// No final do arquivo OptionalManagementScreen.kt

@Composable
private fun AddOptionalDialog(
    onDismiss: () -> Unit,
    onConfirm: (OptionalItem) -> Unit
) {
    var optionalName by remember { mutableStateOf("") }
    var optionalPrice by remember { mutableStateOf("") } // Usamos String para a máscara

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Adicionar Novo Opcional") },
        text = {
            // Column para organizar os dois campos de texto
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = optionalName,
                    onValueChange = { optionalName = it },
                    label = { Text("Nome do Opcional") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = optionalPrice,
                    onValueChange = { optionalPrice = it.filter { char -> char.isDigit() } },
                    label = { Text("Preço") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    // Reutilizando a nossa máscara de moeda!
                    visualTransformation = CurrencyVisualTransformation()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val priceDouble = (optionalPrice.toLongOrNull() ?: 0L) / 100.0
                    if (optionalName.isNotBlank() && priceDouble > 0) {
                        val newOptional = OptionalItem(name = optionalName.trim(), price = priceDouble)
                        onConfirm(newOptional)
                    }
                },
                // Botão só é habilitado se ambos os campos forem válidos
                enabled = optionalName.isNotBlank() && (optionalPrice.toLongOrNull() ?: 0L) > 0
            ) {
                Text("Adicionar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
