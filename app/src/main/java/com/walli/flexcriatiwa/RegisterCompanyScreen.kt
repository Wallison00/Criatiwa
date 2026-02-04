package com.walli.flexcriatiwa

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun RegisterCompanyScreen(
    authViewModel: AuthViewModel,
    onRegistered: () -> Unit
) {
    var companyName by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Business, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(24.dp))
        Text("Bem-vindo!", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Registre sua empresa para começar.", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(32.dp))

        OutlinedTextField(
            value = companyName,
            onValueChange = { companyName = it },
            label = { Text("Nome da Empresa") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(Modifier.height(24.dp))

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
            if (isLoading) CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
            else Text("Criar Empresa")
        }

        Spacer(Modifier.height(16.dp))
        TextButton(onClick = { authViewModel.signOut() }) { Text("Sair") }
    }
}