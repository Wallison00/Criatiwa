package com.walli.flexcriatiwa

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.budiyev.android.codescanner.CodeScanner
import com.budiyev.android.codescanner.CodeScannerView
import com.budiyev.android.codescanner.DecodeCallback

@Composable
fun QRScannerScreen(authViewModel: AuthViewModel, onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { hasPermission = it }
    LaunchedEffect(Unit) { if (!hasPermission) launcher.launch(Manifest.permission.CAMERA) }

    if (hasPermission) ScannerContent(authViewModel, onNavigateBack)
    else Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Permissão de câmera necessária.") }
}

@Composable
fun ScannerContent(authViewModel: AuthViewModel, onNavigateBack: () -> Unit) {
    var scannedCode by remember { mutableStateOf<String?>(null) }
    var showSetupDialog by remember { mutableStateOf(false) }

    // Novos campos
    var employeeName by remember { mutableStateOf("") }
    var employeePassword by remember { mutableStateOf("") }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (scannedCode == null) {
            AndroidView(factory = { ctx ->
                CodeScannerView(ctx).apply {
                    val scanner = CodeScanner(ctx, this)
                    scanner.decodeCallback = DecodeCallback { result ->
                        (ctx as? android.app.Activity)?.runOnUiThread {
                            scannedCode = result.text // Apenas guarda o código e abre o modal
                            showSetupDialog = true
                        }
                    }
                    scanner.startPreview()
                }
            }, modifier = Modifier.fillMaxSize())

            Button(onClick = onNavigateBack, modifier = Modifier.align(Alignment.TopStart).padding(16.dp)) { Text("Voltar") }
            Text("Aponte para o QR Code", color = Color.White, modifier = Modifier.align(Alignment.BottomCenter).padding(48.dp).background(Color.Black.copy(0.5f), RoundedCornerShape(8.dp)).padding(16.dp))
        }

        if (showSetupDialog) {
            AlertDialog(
                onDismissRequest = { showSetupDialog = false; scannedCode = null; errorMessage = null },
                title = { Text("Acesso do Funcionário") },
                text = {
                    Column {
                        Text("Empresa identificada.")
                        Text("Entre com seus dados. Se já tiver cadastro, use a mesma senha para entrar.")
                        Spacer(Modifier.height(16.dp))

                        OutlinedTextField(
                            value = employeeName,
                            onValueChange = { employeeName = it },
                            label = { Text("Seu Nome (Único)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = employeePassword,
                            onValueChange = { employeePassword = it },
                            label = { Text("Crie uma Senha (PIN)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                        )

                        if (errorMessage != null) {
                            Spacer(Modifier.height(8.dp))
                            Text(errorMessage!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (employeeName.isNotBlank() && employeePassword.length >= 4 && scannedCode != null) {
                                isLoading = true
                                errorMessage = null
                                // Chama a nova função de Login/Cadastro Híbrido
                                authViewModel.loginOrRegisterEmployee(scannedCode!!, employeeName, employeePassword) { success, error ->
                                    isLoading = false
                                    if (!success) {
                                        errorMessage = error ?: "Erro ao acessar."
                                    }
                                    // Se sucesso, o AuthState muda e a tela fecha sozinha
                                }
                            } else {
                                errorMessage = "Preencha o nome e uma senha de 4+ dígitos."
                            }
                        },
                        enabled = !isLoading
                    ) { Text("Entrar / Cadastrar") }
                },
                dismissButton = { TextButton(onClick = { showSetupDialog = false; scannedCode = null }) { Text("Cancelar") } }
            )
        }
    }
}