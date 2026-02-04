package com.walli.flexcriatiwa

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuperAdminScreen(
    onSignOut: () -> Unit
) {
    val viewModel: SuperAdminViewModel = viewModel()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Painel Super Admin") },
                actions = {
                    IconButton(onClick = { viewModel.fetchAllCompanies() }) {
                        Icon(Icons.Default.Refresh, "Atualizar")
                    }
                    IconButton(onClick = onSignOut) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, "Sair")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (viewModel.isLoading) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(viewModel.companies) { company ->
                    CompanyAdminCard(
                        company = company,
                        onToggleStatus = { viewModel.toggleCompanyStatus(company) }
                    )
                }
            }
        }
    }
}

@Composable
fun CompanyAdminCard(company: Company, onToggleStatus: () -> Unit) {
    val isActive = company.status == "active"
    val statusColor = if (isActive) Color(0xFF4CAF50) else Color(0xFFE53935)
    val statusText = if (isActive) "Ativo" else "Bloqueado"

    Card(elevation = CardDefaults.cardElevation(4.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(company.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("ID: ${company.id.take(8)}...", style = MaterialTheme.typography.bodySmall, color = Color.Gray)

                Spacer(Modifier.height(4.dp))

                Surface(
                    color = statusColor.copy(alpha = 0.2f),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = statusText,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            IconButton(onClick = onToggleStatus) {
                if (isActive) {
                    Icon(Icons.Default.Block, "Bloquear", tint = Color.Red)
                } else {
                    Icon(Icons.Default.CheckCircle, "Ativar", tint = Color.Green)
                }
            }
        }
    }
}