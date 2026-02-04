package com.walli.flexcriatiwa

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState // <--- IMPORTANTE
import androidx.compose.foundation.verticalScroll // <--- IMPORTANTE
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuperAdminScreen(
    authViewModel: AuthViewModel,
    onSignOut: () -> Unit
) {
    val viewModel: SuperAdminViewModel = viewModel()
    val context = LocalContext.current

    // Estados para o Date Picker
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedCompanyId by remember { mutableStateOf<String?>(null) }

    // Configuração do Estado do Calendário
    val datePickerState = rememberDatePickerState()

    // Lógica para sincronizar a data quando abrir o calendário
    LaunchedEffect(showDatePicker) {
        if (showDatePicker && selectedCompanyId != null) {
            val company = viewModel.companies.find { it.id == selectedCompanyId }
            val currentMillis = company?.expiresAt?.toDate()?.time ?: System.currentTimeMillis()
            // Define a data selecionada no calendário
            datePickerState.selectedDateMillis = currentMillis
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Painel Super Admin") },
                actions = {
                    IconButton(onClick = { viewModel.fetchAllCompanies() }) { Icon(Icons.Default.Refresh, "Atualizar") }
                    IconButton(onClick = onSignOut) { Icon(Icons.AutoMirrored.Filled.ExitToApp, "Sair") }
                }
            )
        }
    ) { innerPadding ->

        // DIÁLOGO DE DATA
        if (showDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        val selectedDateMillis = datePickerState.selectedDateMillis
                        if (selectedCompanyId != null && selectedDateMillis != null) {
                            // 1. Chama o ViewModel para salvar no Firebase
                            viewModel.updateExpirationDate(selectedCompanyId!!, selectedDateMillis)

                            // 2. Feedback visual
                            Toast.makeText(context, "Data salva! Atualizando...", Toast.LENGTH_SHORT).show()

                            // 3. Fecha o diálogo
                            showDatePicker = false
                        }
                    }) {
                        Text("Confirmar Data", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") }
                }
            ) {
                // Adicionei o scroll aqui para garantir que o botão "Tornar Vitalício" apareça em telas pequenas
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    DatePicker(state = datePickerState)

                    Spacer(modifier = Modifier.height(8.dp))

                    // Botão para tornar Vitalício
                    Button(
                        onClick = {
                            if (selectedCompanyId != null) {
                                viewModel.updateExpirationDate(selectedCompanyId!!, null)
                                Toast.makeText(context, "Validade removida (Vitalício)", Toast.LENGTH_SHORT).show()
                            }
                            showDatePicker = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp).fillMaxWidth()
                    ) {
                        Text("Remover Data (Tornar Vitalício)")
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        if (viewModel.isLoading) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(viewModel.companies) { company ->
                    CompanyAdminCard(
                        company = company,
                        viewModel = viewModel,
                        onToggleStatus = { viewModel.toggleCompanyStatus(company) },
                        onDelete = { viewModel.deleteCompany(company.id) },
                        onAccess = { authViewModel.enterCompanyMode(company.id) },
                        onOpenCalendar = {
                            selectedCompanyId = company.id
                            showDatePicker = true
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun CompanyAdminCard(
    company: Company,
    viewModel: SuperAdminViewModel,
    onToggleStatus: () -> Unit,
    onDelete: () -> Unit,
    onAccess: () -> Unit,
    onOpenCalendar: () -> Unit
) {
    val isActive = company.status == "active"
    val statusColor = if (isActive) Color(0xFF4CAF50) else Color(0xFFE53935)

    // Lógica visual de vencimento
    val isExpired = company.expiresAt != null && company.expiresAt.toDate().time < System.currentTimeMillis()
    val expirationColor = if (isExpired) Color.Red else Color.Gray
    val expirationText = viewModel.formatDate(company.expiresAt)

    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Excluir Empresa?") },
            text = { Text("Tem certeza que deseja apagar '${company.name}'?") },
            confirmButton = { Button(onClick = { onDelete(); showDeleteDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Excluir") } },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancelar") } }
        )
    }

    Card(elevation = CardDefaults.cardElevation(4.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            // CABEÇALHO: Nome e Botão de Espiar
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(company.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(modifier = Modifier.size(8.dp), shape = MaterialTheme.shapes.small, color = statusColor) {}
                        Spacer(Modifier.width(4.dp))
                        Text(if(isActive) "Ativo" else "Bloqueado", color = statusColor, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                }
                IconButton(onClick = onAccess) { Icon(Icons.Default.Visibility, "Espiar", tint = MaterialTheme.colorScheme.primary) }
            }

            Spacer(Modifier.height(8.dp))
            Divider()
            Spacer(Modifier.height(8.dp))

            // RODAPÉ: Data e Ações
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                // DATA
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CalendarMonth, null, tint = expirationColor, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(4.dp))
                    Column {
                        Text("Vencimento:", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Text(
                            text = expirationText,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = expirationColor
                        )
                    }
                }

                // AÇÕES
                Row {
                    IconButton(onClick = onOpenCalendar) { Icon(Icons.Default.CalendarMonth, "Alterar Data", tint = MaterialTheme.colorScheme.primary) }
                    IconButton(onClick = onToggleStatus) {
                        if (isActive) Icon(Icons.Default.Block, "Bloquear", tint = Color.Gray)
                        else Icon(Icons.Default.CheckCircle, "Ativar", tint = Color.Green)
                    }
                    IconButton(onClick = { showDeleteDialog = true }) { Icon(Icons.Default.Delete, "Excluir", tint = MaterialTheme.colorScheme.error) }
                }
            }
        }
    }
}