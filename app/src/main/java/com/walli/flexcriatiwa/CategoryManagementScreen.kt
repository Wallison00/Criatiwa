package com.walli.flexcriatiwa

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.Delete

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryManagementScreen(
    managementViewModel: ManagementViewModel,
    onNavigateBack: () -> Unit
) {
    // --- Estados da Tela ---

    // Estado para controlar o diálogo de adicionar nova categoria
    var showAddCategoryDialog by remember { mutableStateOf(false) }

    // --- Lógica do Diálogo ---
    if (showAddCategoryDialog) {
        AddCategoryDialog(
            onDismiss = { showAddCategoryDialog = false }, // Ação para fechar o diálogo
            onConfirm = { newCategoryName ->
                managementViewModel.addCategory(newCategoryName)
                // Fecha o diálogo após a confirmação
                showAddCategoryDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gerenciar Categorias", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddCategoryDialog = true }, // Abre o diálogo
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar Categoria")
            }
        }
    ) { innerPadding ->
        // --- Lista de Categorias ---
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(managementViewModel.categories) { categoryName ->
                // Card para cada item da lista
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = categoryName,
                            style = MaterialTheme.typography.titleMedium
                        )
                        IconButton(
                            onClick = {
                                // A lógica de exclusão virá aqui
                                managementViewModel.deleteCategory(categoryName)
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Excluir Categoria",
                                tint = MaterialTheme.colorScheme.error // Cor vermelha para indicar perigo
                            )
                        }
                        // TODO: Adicionar ícones de Editar/Excluir aqui no futuro
                    }
                }
            }
        }
    }
}

@Composable
private fun AddCategoryDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    // Estado para guardar o texto que o usuário digita no diálogo
    var categoryName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        // Título do diálogo
        title = {
            Text("Adicionar Nova Categoria")
        },
        // Conteúdo principal (o campo de texto)
        text = {
            OutlinedTextField(
                value = categoryName,
                onValueChange = { categoryName = it },
                label = { Text("Nome da Categoria") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        },
        // Botão de confirmação ("Adicionar")
        confirmButton = {
            Button(
                onClick = {
                    // Só confirma se o nome não estiver em branco
                    if (categoryName.isNotBlank()) {
                        onConfirm(categoryName.trim())
                    }
                },
                // O botão só é clicável se houver texto
                enabled = categoryName.isNotBlank()
            ) {
                Text("Adicionar")
            }
        },
        // Botão para cancelar
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
