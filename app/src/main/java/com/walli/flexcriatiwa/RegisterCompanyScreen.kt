package com.walli.flexcriatiwa

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun RegisterCompanyScreen(
    authViewModel: AuthViewModel,
    onRegistered: () -> Unit,      // <--- Parâmetro necessário
    onNavigateToJoin: () -> Unit   // <--- Parâmetro necessário
) {
    var companyName by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    // --- BLOQUEIO PARA ADMIN ---
    if (authViewModel.isUserSuperAdmin) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(64.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("Ação Proibida", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                Text("O Super Admin não deve criar uma empresa.", style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = { authViewModel.signOut() }) { Text("Sair") }
            }
        }
        return
    }

    // --- TELA NORMAL PARA CLIENTES ---
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Business, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(24.dp))

        Text("Bem-vindo, Chefe!", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Registre sua empresa para começar.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = companyName,
            onValueChange = { companyName = it },
            label = { Text("Nome da Empresa") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (companyName.isNotBlank()) {
                    isLoading = true
                    authViewModel.registerCompany(companyName) {
                        isLoading = false
                        onRegistered()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            enabled = !isLoading && companyName.isNotBlank()
        ) {
            if (isLoading) CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp)) else Text("Criar Minha Empresa")
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onNavigateToJoin) {
            Text("É funcionário? Entre com o Código")
        }

        TextButton(onClick = { authViewModel.signOut() }) {
            Text("Sair")
        }
    }
}