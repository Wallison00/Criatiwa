package com.walli.flexcriatiwa

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IngredientManagementScreen(
    managementViewModel: ManagementViewModel,
    onNavigateBack: () -> Unit
) {
    // --- Estados da Tela ---
    var showAddIngredientDialog by remember { mutableStateOf(false) }

    // --- Lógica do Diálogo ---
    if (showAddIngredientDialog) {
        // Usaremos um diálogo genérico que já criamos mentalmente
        AddTextItemDialog(
            title = "Adicionar Novo Ingrediente",
            label = "Nome do Ingrediente",
            onDismiss = { showAddIngredientDialog = false },
            onConfirm = { newIngredientName ->
                managementViewModel.addIngredient(newIngredientName)
                showAddIngredientDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gerenciar Ingredientes", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddIngredientDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar Ingrediente")
            }
        }
    ) { innerPadding ->
        // --- Lista de Ingredientes ---
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(managementViewModel.ingredients) { ingredientName ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = ingredientName,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { managementViewModel.deleteIngredient(ingredientName) }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Excluir Ingrediente",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- DIÁLOGO GENÉRICO REUTILIZÁVEL ---
// Podemos colocar este diálogo em um arquivo separado "CommonUI.kt" no futuro.
// Por enquanto, vamos deixá-lo aqui.
@Composable
private fun AddTextItemDialog(
    title: String,
    label: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var textValue by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = textValue,
                onValueChange = { textValue = it },
                label = { Text(label) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (textValue.isNotBlank()) {
                        onConfirm(textValue.trim())
                    }
                },
                enabled = textValue.isNotBlank()
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
