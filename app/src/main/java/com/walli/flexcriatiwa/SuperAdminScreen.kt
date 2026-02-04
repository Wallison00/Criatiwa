package com.walli.flexcriatiwa

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuperAdminScreen(
    authViewModel: AuthViewModel, // Recebe o AuthViewModel para poder entrar na empresa
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
                        onToggleStatus = { viewModel.toggleCompanyStatus(company) },
                        onDelete = { viewModel.deleteCompany(company.id) },
                        // Ação de Acessar
                        onAccess = { authViewModel.enterCompanyMode(company.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun CompanyAdminCard(
    company: Company,
    onToggleStatus: () -> Unit,
    onDelete: () -> Unit,
    onAccess: () -> Unit
) {
    val isActive = company.status == "active"
    val statusColor = if (isActive) Color(0xFF4CAF50) else Color(0xFFE53935)

    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Excluir Empresa?") },
            text = { Text("Tem certeza que deseja apagar '${company.name}'?") },
            confirmButton = {
                Button(onClick = { onDelete(); showDeleteDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                    Text("Excluir")
                }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancelar") } }
        )
    }

    Card(elevation = CardDefaults.cardElevation(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(company.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(if(isActive) "Ativo" else "Bloqueado", color = statusColor, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }

            Row {
                // BOTÃO NOVO: ACESSAR (OLHO)
                IconButton(onClick = onAccess) {
                    Icon(Icons.Default.Visibility, "Acessar App", tint = MaterialTheme.colorScheme.primary)
                }

                IconButton(onClick = onToggleStatus) {
                    if (isActive) Icon(Icons.Default.Block, "Bloquear", tint = Color.Gray)
                    else Icon(Icons.Default.CheckCircle, "Ativar", tint = Color.Green)
                }

                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(Icons.Default.Delete, "Excluir", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}