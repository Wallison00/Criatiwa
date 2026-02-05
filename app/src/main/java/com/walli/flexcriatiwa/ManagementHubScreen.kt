package com.walli.flexcriatiwa

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
// Removemos o import de viewModel() pois não vamos mais criar um aqui dentro

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManagementHubScreen(
    managementViewModel: ManagementViewModel, // <--- RECEBEMOS O VIEWMODEL CARREGADO
    onOpenDrawer: () -> Unit,
    onNavigateToProducts: () -> Unit,
    onNavigateToCategories: () -> Unit,
    onNavigateToIngredients: () -> Unit,
    onNavigateToOptionals: () -> Unit
) {
    // Removemos a linha: val managementViewModel: ManagementViewModel = viewModel()

    val company = managementViewModel.currentCompany
    val errorMessage = managementViewModel.errorMessage

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
            verticalArrangement = Arrangement.spacedBy(16.dp)
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
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.width(8.dp))
                            Text("Erro", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                        }
                        Text(errorMessage, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            } else {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text("Carregando perfil da empresa...", Modifier.padding(top = 8.dp), style = MaterialTheme.typography.bodySmall)
                }
            }

            Text("Cadastros", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ManagementCard(title = "Produtos", icon = Icons.Default.Fastfood, color = Color(0xFF4CAF50), modifier = Modifier.weight(1f), onClick = onNavigateToProducts)
                ManagementCard(title = "Estrutura", icon = Icons.Default.Category, color = Color(0xFF2196F3), modifier = Modifier.weight(1f), onClick = onNavigateToCategories)
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