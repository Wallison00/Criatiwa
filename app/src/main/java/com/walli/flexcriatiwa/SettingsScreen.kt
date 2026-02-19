package com.walli.flexcriatiwa

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val offlineManager = remember { OfflineSessionManager(context) }

    var notifyKitchen by remember { mutableStateOf(offlineManager.getNotifyKitchen()) }
    var notifyCounter by remember { mutableStateOf(offlineManager.getNotifyCounter()) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Configurações", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            Text(
                "Notificações",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(Modifier.padding(16.dp)) {
                    // Switch 1: Novos Pedidos (Cozinha)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Novos Pedidos (Cozinha)", fontWeight = FontWeight.Bold)
                            Text(
                                "Receber alerta quando um novo pedido for enviado para preparo.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = notifyKitchen,
                            onCheckedChange = {
                                notifyKitchen = it
                                offlineManager.setNotifyKitchen(it)
                            }
                        )
                    }

                    HorizontalDivider(Modifier.padding(vertical = 12.dp))

                    // Switch 2: Pedidos Prontos (Balcão)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Pedidos Prontos (Balcão/Garçom)", fontWeight = FontWeight.Bold)
                            Text(
                                "Receber alerta quando um pedido estiver pronto para entrega.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = notifyCounter,
                            onCheckedChange = {
                                notifyCounter = it
                                offlineManager.setNotifyCounter(it)
                            }
                        )
                    }
                }
            }
        }
    }
}