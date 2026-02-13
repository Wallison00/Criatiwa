package com.walli.flexcriatiwa

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeManagementScreen(
    managementViewModel: ManagementViewModel,
    onNavigateBack: () -> Unit
) {
    val employees = managementViewModel.employees
    val errorMessage = managementViewModel.errorMessage

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gerenciar Equipe") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (errorMessage != null) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Text(text = errorMessage, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
                }
            }

            if (employees.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().height(200.dp), contentAlignment = Alignment.Center) {
                    Text("Nenhum funcionário encontrado.", color = Color.Gray)
                }
            } else {
                Text("Total: ${employees.size} colaboradores", style = MaterialTheme.typography.labelLarge, color = Color.Gray)
                employees.forEach { user ->
                    EmployeeCard(
                        user = user,
                        onToggleStatus = { managementViewModel.toggleUserStatus(user) },
                        onDelete = { managementViewModel.deleteUser(user.uid) },
                        onUpdateRole = { newRole -> managementViewModel.updateUserRole(user.uid, newRole) }
                    )
                }
            }
        }
    }
}

@Composable
fun EmployeeCard(user: UserProfile, onToggleStatus: () -> Unit, onDelete: () -> Unit, onUpdateRole: (String) -> Unit) {
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showRoleDialog by remember { mutableStateOf(false) }

    val isOwner = user.role == "owner"
    val isBlocked = user.status == "blocked"
    val cardAlpha = if (isBlocked) 0.6f else 1f

    Card(
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.alpha(cardAlpha)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = if(isBlocked) Color.Gray else MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.size(40.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = user.name.take(1).uppercase(), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = user.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = if(isBlocked) Color.Red else Color.Unspecified)
                val cargo = when(user.role) { "owner" -> "Dono"; "waiter" -> "Garçom"; "kitchen" -> "Cozinha"; "counter" -> "Balcão"; else -> "Indefinido" }
                Text(text = if(isBlocked) "$cargo (BLOQUEADO)" else cargo, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            if (isOwner) Icon(Icons.Default.Star, null, tint = Color(0xFFFFD700))
            else {
                Box {
                    IconButton(onClick = { showMenu = true }) { Icon(Icons.Default.MoreVert, "Opções") }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(text = { Text("Alterar Cargo") }, onClick = { showMenu = false; showRoleDialog = true }, leadingIcon = { Icon(Icons.Default.Badge, null) })
                        Divider()
                        DropdownMenuItem(text = { Text(if (isBlocked) "Desbloquear" else "Bloquear") }, onClick = { showMenu = false; onToggleStatus() }, leadingIcon = { Icon(if(isBlocked) Icons.Default.LockOpen else Icons.Default.Lock, null) })
                        Divider()
                        DropdownMenuItem(text = { Text("Excluir", color = Color.Red) }, onClick = { showMenu = false; showDeleteDialog = true }, leadingIcon = { Icon(Icons.Default.Delete, null, tint = Color.Red) })
                    }
                }
            }
        }
    }

    if (showRoleDialog) {
        var selected by remember { mutableStateOf(user.role) }
        AlertDialog(
            onDismissRequest = { showRoleDialog = false },
            title = { Text("Cargo de ${user.name}") },
            text = { Column { listOf("waiter" to "Garçom", "kitchen" to "Cozinha", "counter" to "Balcão").forEach { (id, label) -> Row(Modifier.fillMaxWidth().selectable(selected == id) { selected = id }.padding(8.dp), verticalAlignment = Alignment.CenterVertically) { RadioButton(selected == id, { selected = id }); Text(label) } } } },
            confirmButton = { Button(onClick = { onUpdateRole(selected); showRoleDialog = false }) { Text("Salvar") } },
            dismissButton = { TextButton(onClick = { showRoleDialog = false }) { Text("Cancelar") } }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Excluir?") },
            text = { Text("Remover ${user.name} do sistema?") },
            confirmButton = { Button(onClick = { onDelete(); showDeleteDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("Excluir") } },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancelar") } }
        )
    }
}