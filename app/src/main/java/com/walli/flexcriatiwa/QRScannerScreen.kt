package com.walli.flexcriatiwa

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.budiyev.android.codescanner.CodeScanner
import com.budiyev.android.codescanner.CodeScannerView
import com.budiyev.android.codescanner.DecodeCallback
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

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
    val context = LocalContext.current
    var scannedCode by remember { mutableStateOf<String?>(null) }
    var showSetupDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // --- CONFIGURAÇÃO GOOGLE ---
    // O ID "default_web_client_id" é gerado automaticamente a partir do google-services.json
    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken(context.getString(R.string.default_web_client_id))
        .requestEmail()
        .build()

    val googleSignInClient = remember { GoogleSignIn.getClient(context, gso) }

    val googleSignInLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account.idToken
                if (idToken != null && scannedCode != null) {
                    isLoading = true
                    authViewModel.loginWithGoogleCredential(scannedCode!!, idToken) { success, error ->
                        isLoading = false
                        if (!success) {
                            errorMessage = error
                            googleSignInClient.signOut()
                        }
                    }
                }
            } catch (e: ApiException) {
                errorMessage = "Erro Google: ${e.statusCode} (Verifique SHA-1)"
                isLoading = false
            }
        } else {
            isLoading = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (scannedCode == null) {
            AndroidView(factory = { ctx ->
                CodeScannerView(ctx).apply {
                    val scanner = CodeScanner(ctx, this)
                    scanner.decodeCallback = DecodeCallback { result ->
                        (ctx as? Activity)?.runOnUiThread {
                            scannedCode = result.text
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
                title = { Text("Empresa Identificada") },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Para solicitar acesso, entre com sua conta Google.")
                        Spacer(Modifier.height(24.dp))

                        Button(
                            onClick = {
                                isLoading = true
                                googleSignInLauncher.launch(googleSignInClient.signInIntent)
                            },
                            enabled = !isLoading,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4285F4))
                        ) {
                            Text("Entrar com Google", color = Color.White)
                        }

                        if (errorMessage != null) {
                            Spacer(Modifier.height(16.dp))
                            Text(errorMessage!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }
                        if (isLoading) CircularProgressIndicator(Modifier.padding(top = 16.dp))
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showSetupDialog = false; scannedCode = null }) { Text("Cancelar") }
                }
            )
        }
    }
}