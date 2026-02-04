package com.walli.flexcriatiwa

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Warning // <--- O IMPORT QUE FALTAVA
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

    // --- BLOQUEIO PARA ADMIN ---
    // Se o usuário logado for o Admin, ele não deve ver o formulário de criar empresa
    if (authViewModel.isUserSuperAdmin) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Ação Proibida",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "O Super Admin não deve criar uma empresa.",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "Use o Painel de Gestão.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = { authViewModel.signOut() }) {
                    Text("Sair e Logar Corretamente")
                }
            }
        }
        return // Impede que o restante da tela seja desenhado
    }

    // --- TELA NORMAL PARA CLIENTES ---
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Business,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Bem-vindo!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Registre sua empresa para começar.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

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
            if (isLoading) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
            } else {
                Text("Criar Empresa")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = { authViewModel.signOut() }) {
            Text("Sair")
        }
    }
}