package com.walli.flexcriatiwa

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentConfigScreen(
    companyId: String,
    managementViewModel: ManagementViewModel,
    onNavigateBack: () -> Unit
) {
    val currentConfig = managementViewModel.paymentConfig

    // Estados para os campos de texto
    var token by remember(currentConfig) { mutableStateOf(currentConfig?.mercadoPagoAccessToken ?: "") }
    var deviceId by remember(currentConfig) { mutableStateOf(currentConfig?.activeDeviceId ?: "") }
    var isSaving by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configuração de Pagamento") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            isSaving = true
                            val db = Firebase.firestore
                            val newConfig = mapOf(
                                "mercadoPagoAccessToken" to token,
                                "activeDeviceId" to deviceId
                            )
                            db.collection("companies").document(companyId)
                                .collection("config").document("payments")
                                .set(newConfig)
                                .addOnCompleteListener { isSaving = false }
                        },
                        enabled = !isSaving
                    ) {
                        if (isSaving) CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        else Icon(Icons.Default.Save, contentDescription = "Salvar")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Mercado Pago Point",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                "Insira as credenciais da sua aplicação Mercado Pago para integrar a Point Pro 3.",
                style = MaterialTheme.typography.bodyMedium
            )

            OutlinedTextField(
                value = token,
                onValueChange = { token = it },
                label = { Text("Access Token (Produção ou Teste)") },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("APP_USR-...") }
            )

            OutlinedTextField(
                value = deviceId,
                onValueChange = { deviceId = it },
                label = { Text("ID da Maquininha (S/N)") },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Ex: 12345678") }
            )

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Como encontrar o S/N?",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelLarge
                    )
                    Text(
                        "Na sua Point Pro 3, vá em Menu > Ajustes > Sobre o dispositivo. Use o Número de Série (S/N).",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}