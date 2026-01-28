package com.walli.flexcriatiwa

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Delete
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import coil.compose.rememberAsyncImagePainter

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductManagementScreen(
    managementViewModel: ManagementViewModel,
    onNavigateBack: () -> Unit,
    onAddProduct: () -> Unit,
    onEditProduct: (ManagedProduct) -> Unit
) {

    // Estado para controlar a visibilidade do diálogo de confirmação
    var showDeleteDialog by remember { mutableStateOf(false) }
    // Estado para guardar qual produto foi selecionado para ser deletado
    var productToDelete by remember { mutableStateOf<ManagedProduct?>(null) }

    // --- DIÁLOGO DE CONFIRMAÇÃO DE EXCLUSÃO ---
    if (showDeleteDialog && productToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Confirmar Exclusão") },
            text = { Text("Tem certeza de que deseja deletar o produto '${productToDelete!!.name}'? Esta ação não pode ser desfeita.") },
            confirmButton = {
                Button(
                    onClick = {
                        managementViewModel.deleteProduct(productToDelete!!)
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Deletar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Produtos", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO: Ação de busca */ }) {
                        Icon(Icons.Default.Search, contentDescription = "Buscar")
                    }
                    IconButton(onClick = { /* TODO: Ação de mais opções */ }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Mais opções")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddProduct,
                containerColor = MaterialTheme.colorScheme.error // Cor vermelha/destaque
            ) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar Produto")
            }
        }
    ) { innerPadding ->
        // A lista de produtos virá aqui depois
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(managementViewModel.products, key = { it.id }) { product -> // Adicionar a key é uma boa prática
                ProductListItem(
                    product = product,
                    onClick = { onEditProduct(product) },
                    // --- PASSE A AÇÃO DE DELEÇÃO AQUI ---
                    onDeleteClick = {
                        productToDelete = product // Guarda o produto a ser deletado
                        showDeleteDialog = true   // Abre o diálogo de confirmação
                    }
                )
            }
        }
    }
}

// --- PASSO 4.1: DATA CLASS PARA O PRODUTO ---
data class ManagedProduct(
    val id: String,
    val name: String,
    val price: Double,
    val imageUrl: String,
    val isActive: Boolean,
    val category: String,
    val ingredients: Set<String>,
    val optionals: Set<OptionalItem>
)

// --- PASSO 4.2: COMPONENTE PARA O ITEM DA LISTA ---
@Composable
fun ProductListItem(
    product: ManagedProduct,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        // Usamos o mesmo estilo de card da sua tela de pedido
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Imagem do produto
            Card(
                modifier = Modifier.size(80.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Image(
                    painter = rememberAsyncImagePainter(product.imageUrl),
                    contentDescription = product.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            // Coluna com as informações
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(product.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("R$ ${"%.2f".format(product.price)}", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)

                // Status (Ativo/Inativo)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                color = if (product.isActive) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error,
                                shape = CircleShape
                            )
                    )
                    Text(
                        text = if (product.isActive) "Ativo" else "Inativo",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (product.isActive) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                    )
                }
            }

            // --- BOTÃO DE DELETAR ADICIONADO AQUI ---
            IconButton(onClick = onDeleteClick) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Deletar ${product.name}",
                    tint = MaterialTheme.colorScheme.error
                )
            }

        }
    }
}