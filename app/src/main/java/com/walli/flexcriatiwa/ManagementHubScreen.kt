package com.walli.flexcriatiwa

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.Menu

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManagementHubScreen(
    onOpenDrawer: () -> Unit,
    onNavigateToProducts: () -> Unit, // <-- ADICIONE ESTE NOVO PARÂMETRO
    onNavigateToCategories: () -> Unit,
    onNavigateToIngredients: () -> Unit,
    onNavigateToOptionals: () -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar( // <-- MUDE PARA CenterAlignedTopAppBar
                title = { Text("Gestão", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) { // <-- USE O NOVO PARÂMETRO
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
            // --- ADICIONE ESTE NOVO CARD NO TOPO ---
            ManagementCard(
                title = "Gerenciar Produtos",
                subtitle = "Adicione ou edite os produtos do seu cardápio.",
                icon = Icons.Default.Fastfood, // Ou outro ícone
                onClick = onNavigateToProducts // Use o novo parâmetro aqui
            )

            // Card para gerenciar Categorias
            ManagementCard(
                title = "Gerenciar Categorias",
                subtitle = "Adicione ou edite as categorias dos produtos (Ex: Lanches, Bebidas).",
                icon = Icons.Default.Category,
                onClick = onNavigateToCategories
            )

            // Card para gerenciar Ingredientes
            ManagementCard(
                title = "Gerenciar Ingredientes",
                subtitle = "Cadastre os ingredientes base que compõem seus produtos.",
                icon = Icons.Default.Fastfood,
                onClick = onNavigateToIngredients
            )

            // Card para gerenciar Opcionais
            ManagementCard(
                title = "Gerenciar Opcionais",
                subtitle = "Cadastre os itens adicionais e seus respectivos preços.",
                icon = Icons.Default.PlaylistAdd,
                onClick = onNavigateToOptionals
            )
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