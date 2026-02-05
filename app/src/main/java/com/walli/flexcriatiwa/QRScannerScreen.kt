package com.walli.flexcriatiwa

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.budiyev.android.codescanner.CodeScanner
import com.budiyev.android.codescanner.CodeScannerView
import com.budiyev.android.codescanner.DecodeCallback

@Composable
fun QRScannerScreen(
    authViewModel: AuthViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasPermission = granted }
    )

    LaunchedEffect(Unit) {
        if (!hasPermission) launcher.launch(Manifest.permission.CAMERA)
    }

    if (hasPermission) {
        ScannerContent(authViewModel, onNavigateBack)
    } else {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("É necessária permissão da câmera para ler o QR Code.")
        }
    }
}

@Composable
fun ScannerContent(authViewModel: AuthViewModel, onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    var scannedCode by remember { mutableStateOf<String?>(null) }
    var showSetupDialog by remember { mutableStateOf(false) }

    // Variáveis do Funcionário
    var employeeName by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf("waiter") } // waiter, kitchen, counter
    var isLoading by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        // CÂMERA
        if (scannedCode == null) {
            AndroidView(
                factory = { ctx ->
                    CodeScannerView(ctx).apply {
                        val scanner = CodeScanner(ctx, this)
                        scanner.decodeCallback = DecodeCallback { result ->
                            // Roda na UI Thread
                            (ctx as? android.app.Activity)?.runOnUiThread {
                                scannedCode = result.text
                                showSetupDialog = true
                            }
                        }
                        scanner.startPreview()
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Overlay de Texto
            Text(
                "Aponte para o QR Code da Empresa",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 48.dp)
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(16.dp)
            )
        }

        // DIÁLOGO DE CADASTRO RÁPIDO
        if (showSetupDialog) {
            AlertDialog(
                onDismissRequest = {
                    showSetupDialog = false
                    scannedCode = null // Reinicia scanner se cancelar
                },
                title = { Text("Bem-vindo à Equipe!") },
                text = {
                    Column {
                        Text("Código lido com sucesso.")
                        Spacer(Modifier.height(16.dp))

                        OutlinedTextField(
                            value = employeeName,
                            onValueChange = { employeeName = it },
                            label = { Text("Seu Nome") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(16.dp))
                        Text("Qual sua função?", style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.height(8.dp))

                        // Seleção de Função
                        RoleRadioButton("Garçom / Salão", "waiter", selectedRole) { selectedRole = it }
                        RoleRadioButton("Cozinha", "kitchen", selectedRole) { selectedRole = it }
                        RoleRadioButton("Balcão / Caixa", "counter", selectedRole) { selectedRole = it }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (employeeName.isNotBlank() && scannedCode != null) {
                                isLoading = true
                                authViewModel.loginWithQRCode(scannedCode!!, employeeName, selectedRole) {
                                    isLoading = false
                                    // Sucesso: O AuthState mudará e a MainActivity trocará a tela
                                }
                            } else {
                                Toast.makeText(context, "Informe seu nome.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = !isLoading
                    ) {
                        if(isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp)) else Text("Entrar")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showSetupDialog = false
                        scannedCode = null
                    }) { Text("Cancelar") }
                }
            )
        }
    }
}

@Composable
fun RoleRadioButton(text: String, roleValue: String, selectedValue: String, onSelect: (String) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        RadioButton(selected = (roleValue == selectedValue), onClick = { onSelect(roleValue) })
        Text(text, modifier = Modifier.padding(start = 8.dp))
    }
}