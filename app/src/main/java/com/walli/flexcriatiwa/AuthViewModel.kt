package com.walli.flexcriatiwa

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class AuthState {
    object Loading : AuthState()
    object LoggedOut : AuthState()
    object NeedsCompanyRegistration : AuthState()
    object PendingApproval : AuthState()
    data class LoggedIn(val companyId: String, val userRole: String, val isOfflineMode: Boolean = false) : AuthState()
    object SuperAdmin : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val auth = Firebase.auth
    private val db = Firebase.firestore
    private val offlineManager = OfflineSessionManager(application.applicationContext)
    private val SUPER_ADMIN_EMAIL = "wallisonfreitas00@gmail.com"

    var authState by mutableStateOf<AuthState>(AuthState.Loading)
        private set

    var currentUserProfile by mutableStateOf<UserProfile?>(null)
        private set

    val isUserSuperAdmin: Boolean
        get() {
            val current = auth.currentUser?.email?.trim()?.lowercase() ?: ""
            return current == SUPER_ADMIN_EMAIL.trim().lowercase()
        }

    init {
        checkAuthStatus()
    }

    private fun startListeningToMyProfile(uid: String) {
        db.collection("users").document(uid).addSnapshotListener { snapshot, _ ->
            if (snapshot != null && snapshot.exists()) {
                val profile = snapshot.toObject(UserProfile::class.java)
                currentUserProfile = profile
                if (profile != null) {
                    if (profile.status == "active" && !profile.companyId.isNullOrBlank()) {
                        authState = AuthState.LoggedIn(profile.companyId, profile.role)
                    } else if (profile.status == "pending_approval") {
                        authState = AuthState.PendingApproval
                    } else if (profile.status == "blocked") {
                        authState = AuthState.Error("Acesso bloqueado.")
                        auth.signOut()
                    }
                }
            }
        }
    }

    fun checkAuthStatus() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            authState = AuthState.LoggedOut
            currentUserProfile = null
        } else {
            val currentEmail = currentUser.email?.trim()?.lowercase() ?: ""
            if (currentEmail == SUPER_ADMIN_EMAIL.trim().lowercase()) {
                authState = AuthState.SuperAdmin
                return
            }
            startListeningToMyProfile(currentUser.uid)
        }
    }

    fun approveUser(userId: String, assignedRole: String) {
        viewModelScope.launch {
            try {
                db.collection("users").document(userId)
                    .update(mapOf("role" to assignedRole, "status" to "active")).await()
            } catch (e: Exception) { }
        }
    }

    // --- NOVA LÓGICA: LOGIN OU CADASTRO INTELIGENTE ---
    fun loginOrRegisterEmployee(shareCode: String, name: String, password: String, onResult: (Boolean, String?) -> Unit) {
        val cleanCode = shareCode.trim().uppercase()
        val cleanName = name.trim().replace(" ", "").lowercase() // Remove espaços para o email

        // E-mail gerado automaticamente para consistência
        val generatedEmail = "$cleanName.$cleanCode@flexcriatiwa.app"

        viewModelScope.launch {
            try {
                // 1. Valida a Empresa
                val snapshot = db.collection("companies").whereEqualTo("shareCode", cleanCode).limit(1).get().await()
                if (snapshot.isEmpty) {
                    onResult(false, "Código da empresa inválido.")
                    return@launch
                }
                val companyId = snapshot.documents.first().id

                // 2. Tenta fazer LOGIN primeiro
                try {
                    auth.signInWithEmailAndPassword(generatedEmail, password).await()
                    // Se passar aqui, o login funcionou! O listener checkAuthStatus fará o resto.
                    onResult(true, null)
                    return@launch
                } catch (e: Exception) {
                    // Se falhar o login, assumimos que é um cadastro novo (ou senha errada)
                    // Vamos tentar CADASTRAR
                }

                // 3. Tenta CADASTRAR
                try {
                    val authResult = auth.createUserWithEmailAndPassword(generatedEmail, password).await()
                    val user = authResult.user ?: throw Exception("Erro ao criar usuário")

                    // Salva o perfil
                    val userProfile = UserProfile(
                        uid = user.uid,
                        email = generatedEmail,
                        name = name,
                        companyId = companyId,
                        role = "pending",
                        status = "pending_approval"
                    )
                    db.collection("users").document(user.uid).set(userProfile).await()

                    // Salva sessão local
                    offlineManager.saveSession(generatedEmail, companyId, "pending", "pending_approval", null)

                    startListeningToMyProfile(user.uid)
                    onResult(true, null)

                } catch (e: Exception) {
                    // Se der erro no cadastro também (ex: senha fraca, ou email já existe e senha estava errada antes)
                    onResult(false, "Erro: Senha incorreta ou usuário já existe. (${e.message})")
                }

            } catch (e: Exception) {
                onResult(false, "Erro de conexão: ${e.message}")
            }
        }
    }

    // --- MÉTODOS MANTIDOS (Register Company, etc) ---

    fun registerCompany(companyName: String, onSuccess: () -> Unit) {
        val user = auth.currentUser ?: return
        viewModelScope.launch {
            try {
                val newCompanyRef = db.collection("companies").document()
                val uniqueCode = (1..6).map { ('A'..'Z').random() }.joinToString("")
                val company = Company(id = newCompanyRef.id, name = companyName, ownerId = user.uid, ownerEmail = user.email ?: "", shareCode = uniqueCode)
                newCompanyRef.set(company).await()

                val userProfile = UserProfile(uid = user.uid, email = user.email ?: "", name = user.displayName ?: "Dono", companyId = company.id, role = "owner", status = "active")
                db.collection("users").document(user.uid).set(userProfile).await()

                currentUserProfile = userProfile
                createDefaultMenuStructure(company.id)
                offlineManager.saveSession(user.email ?: "", company.id, "owner", "active", null)
                authState = AuthState.LoggedIn(company.id, "owner")
                onSuccess()
            } catch (e: Exception) { authState = AuthState.Error("Erro: ${e.message}") }
        }
    }

    fun joinCompanyWithCode(code: String, onSuccess: () -> Unit) {
        // ... (Mantido para compatibilidade, mas o fluxo principal agora é o de cima)
    }

    // Mantemos o método antigo vazio ou redirecionando se necessário, mas removemos a lógica anônima pura.
    fun checkQRCodeUserStatus(scannedCode: String, onNewUser: () -> Unit, onExistingUser: () -> Unit) {
        // No novo fluxo, sempre abrimos o modal para pedir senha, então não verificamos antes.
        // Podemos deixar essa função apenas chamando onNewUser para compatibilidade
        onNewUser()
    }

    // Compatibilidade
    fun registerQRCodeUser(scannedCode: String, userName: String, role: String, onSuccess: () -> Unit) {
        // Não usado mais diretamente
    }

    private suspend fun createDefaultMenuStructure(companyId: String) {
        val defaultCategories = listOf(mapOf("name" to "Lanches", "defaultIngredients" to listOf("Pão", "Carne"), "availableOptionals" to emptyList<Any>()))
        db.collection("companies").document(companyId).collection("settings").document("menu_structure").set(mapOf("categories" to defaultCategories)).await()
    }

    fun enterCompanyMode(companyId: String) { if (isUserSuperAdmin) authState = AuthState.LoggedIn(companyId, "admin_viewer") }
    fun exitCompanyMode() { if (isUserSuperAdmin) authState = AuthState.SuperAdmin }
    fun signOut() { auth.signOut(); authState = AuthState.LoggedOut; currentUserProfile = null }
    fun forceOfflineLogin(email: String) { /* Mantido */ }
}