package com.walli.flexcriatiwa

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit
) {
    val context = LocalContext.current

    // --- CONFIGURAÇÃO DE SEGURANÇA (EncryptedSharedPreferences) ---
    val securePreferences = remember {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            "secure_login_prefs", // Nome do arquivo
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    // Carrega dados salvos (se existirem)
    var email by remember { mutableStateOf(securePreferences.getString("saved_email", "") ?: "") }
    var password by remember { mutableStateOf(securePreferences.getString("saved_password", "") ?: "") }
    var rememberMe by remember { mutableStateOf(securePreferences.getBoolean("remember_me", false)) }

    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Store, null, Modifier.size(80.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(32.dp))
        Text("FlexCriatiwa", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text("Gestão Multi-Empresa", fontSize = 14.sp, color = Color.Gray)
        Spacer(Modifier.height(32.dp))

        OutlinedTextField(
            value = email, onValueChange = { email = it },
            label = { Text("E-mail") }, leadingIcon = { Icon(Icons.Default.Email, null) },
            modifier = Modifier.fillMaxWidth(), singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Senha") },
            leadingIcon = { Icon(Icons.Default.Lock, null) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                val image = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(imageVector = image, contentDescription = null)
                }
            }
        )

        // Checkbox "Lembrar de mim"
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = rememberMe,
                onCheckedChange = { isChecked -> rememberMe = isChecked }
            )
            Text(
                text = "Lembrar e-mail e senha",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.clickable { rememberMe = !rememberMe }
            )
        }

        if (errorMessage != null) {
            Spacer(Modifier.height(8.dp))
            Text(errorMessage!!, color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
        }
        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                if (email.isNotBlank() && password.isNotBlank()) {
                    isLoading = true
                    errorMessage = null

                    // Lógica de Salvar Preferência (CRIPTOGRAFADO)
                    val editor = securePreferences.edit()
                    if (rememberMe) {
                        editor.putString("saved_email", email)
                        editor.putString("saved_password", password) // Agora é seguro salvar
                        editor.putBoolean("remember_me", true)
                    } else {
                        editor.remove("saved_email")
                        editor.remove("saved_password")
                        editor.putBoolean("remember_me", false)
                    }
                    editor.apply()

                    Firebase.auth.signInWithEmailAndPassword(email, password)
                        .addOnSuccessListener { isLoading = false; onLoginSuccess() }
                        .addOnFailureListener {
                            isLoading = false
                            errorMessage = if(it.message?.contains("no user") == true) "Usuário não encontrado." else "Erro ao entrar: Verifique seus dados."
                        }
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp), enabled = !isLoading
        ) {
            if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp)) else Text("Entrar")
        }

        Spacer(Modifier.height(16.dp))

        TextButton(onClick = {
            if(email.isNotBlank() && password.isNotBlank()) {
                isLoading = true
                Firebase.auth.createUserWithEmailAndPassword(email, password)
                    .addOnSuccessListener { isLoading = false; onNavigateToRegister() }
                    .addOnFailureListener { e -> isLoading = false; errorMessage = e.localizedMessage }
            } else {
                errorMessage = "Preencha E-mail e Senha para criar conta."
            }
        }) {
            Text("Não tem conta? Cadastre-se")
        }
    }
}