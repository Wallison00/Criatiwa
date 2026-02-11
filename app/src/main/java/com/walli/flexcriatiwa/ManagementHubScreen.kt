package com.walli.flexcriatiwa

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManagementHubScreen(
    managementViewModel: ManagementViewModel,
    onOpenDrawer: () -> Unit,
    onNavigateToProducts: () -> Unit,
    onNavigateToCategories: () -> Unit,
    onNavigateToIngredients: () -> Unit,
    onNavigateToOptionals: () -> Unit
) {
    val company = managementViewModel.currentCompany
    val errorMessage = managementViewModel.errorMessage
    val activeUsers = managementViewModel.activeUsers // <--- LISTA DE EQUIPE

    val qrBitmap = remember(company?.shareCode) {
        if (!company?.shareCode.isNullOrBlank()) {
            QRCodeUtils.generateQRCode(company!!.shareCode)
        } else null
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Gestão", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) { Icon(Icons.Default.Menu, "Menu") }
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
            verticalArrangement = Arrangement.spacedBy(24.dp) // Mais espaço entre seções
        ) {
            // --- CARTÃO DE ACESSO ---
            if (company != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Acesso para Funcionários", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Spacer(Modifier.height(16.dp))

                        if (qrBitmap != null) {
                            Image(
                                bitmap = qrBitmap.asImageBitmap(),
                                contentDescription = "QR Code",
                                modifier = Modifier.size(180.dp).clip(RoundedCornerShape(12.dp)).background(Color.White).padding(8.dp)
                            )
                        } else {
                            Icon(Icons.Default.QrCode, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }

                        Spacer(Modifier.height(12.dp))
                        Text("Código: ${company.shareCode}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
                    }
                }
            } else if (errorMessage != null) {
                // ... (Erro) ...
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Erro", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                        Text(errorMessage, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            } else {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            // --- LISTA DE EQUIPE (NOVO BLOCO) ---
            if (activeUsers.isNotEmpty()) {
                Column {
                    Text("Equipe (${activeUsers.size})", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))

                    activeUsers.forEach { user ->
                        EmployeeCard(user)
                        Spacer(Modifier.height(8.dp))
                    }
                }
            } else if (company != null) {
                Text("Nenhum funcionário ativo ainda.", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            }

            // --- MENU DE CADASTROS ---
            Column {
                Text("Cadastros", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    ManagementCard(title = "Produtos", icon = Icons.Default.Fastfood, color = Color(0xFF4CAF50), modifier = Modifier.weight(1f), onClick = onNavigateToProducts)
                    ManagementCard(title = "Estrutura", icon = Icons.Default.Category, color = Color(0xFF2196F3), modifier = Modifier.weight(1f), onClick = onNavigateToCategories)
                }
            }
        }
    }
}

// --- CARD DE FUNCIONÁRIO ---
@Composable
fun EmployeeCard(user: UserProfile) {
    Card(
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Ícone com a inicial
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = user.name.take(1).uppercase(),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(user.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)

                // Tradução do Cargo
                val cargo = when(user.role) {
                    "owner" -> "Dono / Gerente"
                    "waiter" -> "Garçom"
                    "kitchen" -> "Cozinha"
                    "counter" -> "Balcão"
                    else -> "Funcionário"
                }

                Text(cargo, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }

            // Ícone indicando status ativo
            if (user.role == "owner") {
                Icon(Icons.Default.Star, null, tint = Color(0xFFFFD700)) // Estrela para o dono
            } else {
                Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50))
            }
        }
    }
}

@Composable
fun ManagementCard(title: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(modifier = modifier.height(120.dp).clickable(onClick = onClick), elevation = CardDefaults.cardElevation(4.dp)) {
        Box(modifier = Modifier.fillMaxSize()) {
            Icon(imageVector = icon, contentDescription = null, tint = color.copy(alpha = 0.2f), modifier = Modifier.size(80.dp).align(Alignment.BottomEnd).offset(x = 16.dp, y = 16.dp))
            Column(modifier = Modifier.padding(16.dp).align(Alignment.TopStart)) {
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(32.dp))
                Spacer(Modifier.weight(1f))
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}