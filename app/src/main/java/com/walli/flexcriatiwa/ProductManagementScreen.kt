package com.walli.flexcriatiwa

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductManagementScreen(
    managementViewModel: ManagementViewModel,
    onNavigateBack: () -> Unit,
    onAddProduct: () -> Unit,
    onEditProduct: (ManagedProduct) -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var productToDelete by remember { mutableStateOf<ManagedProduct?>(null) }

    // Ordena a lista pelo código (1, 2, 3...)
    val sortedProducts = remember(managementViewModel.products) {
        managementViewModel.products.sortedBy { it.code }
    }

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
                ) { Text("Deletar") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancelar") }
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
                    IconButton(onClick = { /* TODO */ }) { Icon(Icons.Default.Search, contentDescription = "Buscar") }
                    IconButton(onClick = { /* TODO */ }) { Icon(Icons.Default.MoreVert, contentDescription = "Mais opções") }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddProduct,
                containerColor = MaterialTheme.colorScheme.error
            ) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar Produto")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(sortedProducts, key = { it.id }) { product ->
                ProductListItem(
                    product = product,
                    onClick = { onEditProduct(product) },
                    onDeleteClick = {
                        productToDelete = product
                        showDeleteDialog = true
                    }
                )
            }
        }
    }
}

// --- CLASSE DE DADOS (IMPORTANTE: MANTENHA O CAMPO CODE AQUI) ---
data class ManagedProduct(
    val id: String,
    val code: Int = 0, // O padrão é 0 para produtos antigos
    val name: String,
    val price: Double,
    val imageUrl: String,
    val isActive: Boolean,
    val category: String,
    val ingredients: Set<String>,
    val optionals: Set<OptionalItem>
)

@Composable
fun ProductListItem(
    product: ManagedProduct,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val decodedBitmap = remember(product.imageUrl) {
        try {
            if (product.imageUrl.startsWith("data:image")) {
                val base64String = product.imageUrl.substringAfter(",")
                val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)?.asImageBitmap()
            } else null
        } catch (e: Exception) { null }
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(modifier = Modifier.size(80.dp), shape = RoundedCornerShape(12.dp)) {
                if (decodedBitmap != null) {
                    Image(bitmap = decodedBitmap, contentDescription = product.name, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else {
                    Image(painter = rememberAsyncImagePainter(product.imageUrl), contentDescription = product.name, modifier = Modifier.fillMaxSize().background(Color.LightGray), contentScale = ContentScale.Crop)
                }
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                // MOSTRA O CÓDIGO DO PRODUTO (Ex: #001)
                Text(
                    text = "#%03d - %s".format(product.code, product.name),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text("R$ ${"%.2f".format(product.price)}", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(modifier = Modifier.size(8.dp).background(color = if (product.isActive) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error, shape = CircleShape))
                    Text(text = if (product.isActive) "Ativo" else "Inativo", style = MaterialTheme.typography.bodySmall, color = if (product.isActive) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error)
                }
            }

            IconButton(onClick = onDeleteClick) {
                Icon(Icons.Default.Delete, contentDescription = "Deletar", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}