package com.walli.flexcriatiwa

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt // Ícone seguro que existe em todos os Androids
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

@Composable
fun LoginScreen(
    authViewModel: AuthViewModel,
    onNavigateToRegister: () -> Unit,
    onNavigateToQRCode: () -> Unit // <--- O PARÂMETRO QUE FALTAVA
) {
    val context = LocalContext.current

    // Configuração de Segurança Local
    val securePreferences = remember {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "secure_login_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    var email by remember { mutableStateOf(securePreferences.getString("saved_email", "") ?: "") }
    var savedPassword = securePreferences.getString("saved_password", "") ?: ""
    var rememberMe by remember { mutableStateOf(securePreferences.getBoolean("remember_me", false)) }
    var password by remember { mutableStateOf(if (rememberMe) savedPassword else "") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Default.Store, null, Modifier.size(80.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(32.dp))
        Text("FlexCriatiwa", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(32.dp))

        OutlinedTextField(
            value = email, onValueChange = { email = it },
            label = { Text("E-mail") }, leadingIcon = { Icon(Icons.Default.Email, null) },
            modifier = Modifier.fillMaxWidth(), singleLine = true
        )
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = password, onValueChange = { password = it },
            label = { Text("Senha") }, leadingIcon = { Icon(Icons.Default.Lock, null) },
            modifier = Modifier.fillMaxWidth(), singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, null)
                }
            }
        )

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = rememberMe, onCheckedChange = { rememberMe = it })
            Text("Lembrar e-mail e senha", modifier = Modifier.clickable { rememberMe = !rememberMe })
        }

        if (errorMessage != null) {
            Spacer(Modifier.height(8.dp))
            Text(errorMessage!!, color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                if (email.isNotBlank() && password.isNotBlank()) {
                    isLoading = true
                    errorMessage = null
                    val editor = securePreferences.edit()
                    if (rememberMe) {
                        editor.putString("saved_email", email)
                        editor.putString("saved_password", password)
                        editor.putBoolean("remember_me", true)
                    } else {
                        editor.clear()
                    }
                    editor.apply()

                    Firebase.auth.signInWithEmailAndPassword(email, password)
                        .addOnSuccessListener {
                            isLoading = false
                            authViewModel.checkAuthStatus()
                        }
                        .addOnFailureListener {
                            val localPass = securePreferences.getString("saved_password", "")
                            if (rememberMe && localPass == password) {
                                authViewModel.forceOfflineLogin(email)
                                if (authViewModel.authState is AuthState.Error) {
                                    isLoading = false
                                    errorMessage = (authViewModel.authState as AuthState.Error).message
                                }
                            } else {
                                isLoading = false
                                errorMessage = "Erro de conexão e senha não verificada."
                            }
                        }
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp), enabled = !isLoading
        ) {
            if (isLoading) CircularProgressIndicator(color = Color.White) else Text("Entrar")
        }

        Spacer(Modifier.height(24.dp))

        // --- BOTÃO DE ACESSO RÁPIDO ---
        OutlinedButton(
            onClick = onNavigateToQRCode,
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Icon(Icons.Default.CameraAlt, null) // Trocado para ícone de Câmera (Scanner)
            Spacer(Modifier.width(8.dp))
            Text("Acessar com QR Code")
        }

        Spacer(Modifier.height(16.dp))
        TextButton(onClick = {
            if(email.isNotBlank() && password.isNotBlank()) {
                Firebase.auth.createUserWithEmailAndPassword(email, password)
                    .addOnSuccessListener { onNavigateToRegister() }
                    .addOnFailureListener { errorMessage = it.message }
            } else errorMessage = "Preencha dados para cadastrar"
        }) { Text("Não tem conta? Cadastre-se") }
    }
}