package com.walli.flexcriatiwa

import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuperAdminScreen(
    authViewModel: AuthViewModel,
    onSignOut: () -> Unit
) {
    val viewModel: SuperAdminViewModel = viewModel()
    val context = LocalContext.current

    LaunchedEffect(key1 = true) {
        viewModel.uiMessage.collectLatest { message ->
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    // Estados para o Date Picker
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedCompanyId by remember { mutableStateOf<String?>(null) }
    val datePickerState = rememberDatePickerState()

    LaunchedEffect(showDatePicker) {
        if (showDatePicker && selectedCompanyId != null) {
            val company = viewModel.companies.find { it.id == selectedCompanyId }
            val currentMillis = company?.expiresAt?.toDate()?.time ?: System.currentTimeMillis()
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

        if (showDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        val selectedDateMillis = datePickerState.selectedDateMillis
                        if (selectedCompanyId != null && selectedDateMillis != null) {
                            viewModel.updateExpirationDate(selectedCompanyId!!, selectedDateMillis)
                            showDatePicker = false
                        }
                    }) { Text("Confirmar") }
                },
                dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") } }
            ) {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    DatePicker(state = datePickerState)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            if (selectedCompanyId != null) viewModel.updateExpirationDate(selectedCompanyId!!, null)
                            showDatePicker = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                        modifier = Modifier.padding(horizontal = 24.dp).fillMaxWidth()
                    ) { Text("Tornar Vitalício") }
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(viewModel.companies) { company ->
                    CompanyExpandableCard(
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
fun CompanyExpandableCard(
    company: Company,
    viewModel: SuperAdminViewModel,
    onToggleStatus: () -> Unit,
    onDelete: () -> Unit,
    onAccess: () -> Unit,
    onOpenCalendar: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val isActive = company.status == "active"
    val statusColor = if (isActive) Color(0xFF4CAF50) else Color(0xFFE53935)

    // Formatação de Datas
    val createdDate = viewModel.formatDate(company.createdAt)
    val updatedDate = viewModel.formatDate(company.updatedAt)
    val expirationText = if(company.expiresAt == null) "Vitalício" else viewModel.formatDate(company.expiresAt).split(" ")[0] // Mostra só a data sem hora para validade
    val isExpired = company.expiresAt != null && company.expiresAt.toDate().time < System.currentTimeMillis()

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

    Card(
        elevation = CardDefaults.cardElevation(4.dp),
        modifier = Modifier.fillMaxWidth().animateContentSize(
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // --- LINHA 1: Título e Ícone de Expandir ---
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(company.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(modifier = Modifier.size(8.dp), shape = MaterialTheme.shapes.small, color = statusColor) {}
                        Spacer(Modifier.width(6.dp))
                        Text(if(isActive) "Ativo" else "Bloqueado", color = statusColor, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(12.dp))

                        // Validade Resumida
                        Icon(Icons.Default.CalendarMonth, null, modifier = Modifier.size(16.dp), tint = if(isExpired) Color.Red else Color.Gray)
                        Spacer(Modifier.width(4.dp))
                        Text(expirationText, color = if(isExpired) Color.Red else Color.Gray, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = "Ver detalhes"
                    )
                }
            }

            // --- CONTEÚDO EXPANDIDO (DETALHES) ---
            if (expanded) {
                Divider(modifier = Modifier.padding(vertical = 12.dp))

                DetailRow("E-mail:", company.ownerEmail)
                DetailRow("Código Equipe:", company.shareCode)
                Spacer(Modifier.height(8.dp))
                DetailRow("Criado em:", createdDate)
                DetailRow("Atualizado em:", updatedDate)

                Spacer(Modifier.height(16.dp))

                // --- AÇÕES DO ADMIN ---
                Text("Ações Administrativas", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    FilledTonalIconButton(onClick = onAccess) { Icon(Icons.Default.Visibility, "Espiar") }
                    FilledTonalIconButton(onClick = onOpenCalendar) { Icon(Icons.Default.CalendarMonth, "Validade") }
                    FilledTonalIconButton(onClick = onToggleStatus) {
                        if (isActive) Icon(Icons.Default.Block, "Bloquear", tint = Color.Gray)
                        else Icon(Icons.Default.CheckCircle, "Ativar", tint = Color.Green)
                    }
                    FilledTonalIconButton(onClick = { showDeleteDialog = true }, colors = IconButtonDefaults.filledTonalIconButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                        Icon(Icons.Default.Delete, "Excluir")
                    }
                }
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = Color.Gray, modifier = Modifier.width(100.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Normal)
    }
}