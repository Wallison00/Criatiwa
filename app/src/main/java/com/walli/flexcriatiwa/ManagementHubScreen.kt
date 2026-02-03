package com.walli.flexcriatiwa

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManagementHubScreen(
    onOpenDrawer: () -> Unit,
    onNavigateToProducts: () -> Unit,
    onNavigateToCategories: () -> Unit,
    onNavigateToIngredients: () -> Unit, // Mantido apenas para compatibilidade, mas não usado
    onNavigateToOptionals: () -> Unit // Mantido apenas para compatibilidade, mas não usado
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Gestão", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "Abrir Menu")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ManagementCard(
                title = "Gerenciar Produtos",
                subtitle = "Adicione ou edite os produtos do seu cardápio.",
                icon = Icons.Default.Fastfood,
                onClick = onNavigateToProducts
            )

            ManagementCard(
                title = "Estrutura do Cardápio",
                subtitle = "Gerencie Categorias, Ingredientes e Adicionais.",
                icon = Icons.Default.Category,
                onClick = onNavigateToCategories
            )

            // Os botões individuais de ingredientes e opcionais foram removidos
            // pois agora estão dentro da Estrutura do Cardápio (Categorias)
        }
    }
}

@Composable
private fun ManagementCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(40.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(text = subtitle, style = MaterialTheme.typography.bodyMedium)
            }
            Icon(imageVector = Icons.Default.ArrowForwardIos, contentDescription = null)
        }
    }
}