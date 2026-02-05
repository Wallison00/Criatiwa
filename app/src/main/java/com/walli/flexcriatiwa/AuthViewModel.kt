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
    data class LoggedIn(val companyId: String, val userRole: String, val isOfflineMode: Boolean = false) : AuthState()
    object SuperAdmin : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val auth = Firebase.auth
    private val db = Firebase.firestore
    private val offlineManager = OfflineSessionManager(application.applicationContext)

    // SEU E-MAIL DE SUPER ADMIN
    private val SUPER_ADMIN_EMAIL = "wallisonfreitas00@gmail.com"

    var authState by mutableStateOf<AuthState>(AuthState.Loading)
        private set

    val isUserSuperAdmin: Boolean
        get() {
            val current = auth.currentUser?.email?.trim()?.lowercase() ?: ""
            return current == SUPER_ADMIN_EMAIL.trim().lowercase()
        }

    init {
        checkAuthStatus()
    }

    // --- VERIFICAÇÃO DE STATUS ---

    fun checkAuthStatus() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            authState = AuthState.LoggedOut
        } else {
            val currentEmail = currentUser.email?.trim()?.lowercase() ?: ""
            if (currentEmail == SUPER_ADMIN_EMAIL.trim().lowercase()) {
                authState = AuthState.SuperAdmin
                return
            }
            fetchUserProfile(currentUser.uid)
        }
    }

    private fun fetchUserProfile(uid: String) {
        viewModelScope.launch {
            try {
                val doc = db.collection("users").document(uid).get().await()
                if (doc.exists()) {
                    val companyId = doc.getString("companyId")
                    val role = doc.getString("role") ?: "employee"
                    val email = doc.getString("email") ?: ""

                    if (!companyId.isNullOrBlank()) {
                        validateAndLoginCompany(companyId, role, email)
                    } else {
                        authState = AuthState.NeedsCompanyRegistration
                    }
                } else {
                    authState = AuthState.NeedsCompanyRegistration
                }
            } catch (e: Exception) {
                tryLoginOffline()
            }
        }
    }

    private suspend fun validateAndLoginCompany(companyId: String, role: String, email: String) {
        try {
            val companyDoc = db.collection("companies").document(companyId).get().await()
            val status = companyDoc.getString("status") ?: "active"
            val expiresAt = companyDoc.getTimestamp("expiresAt")
            val isExpired = expiresAt != null && expiresAt.seconds < Timestamp.now().seconds

            if (status == "blocked") {
                authState = AuthState.Error("Acesso bloqueado.")
                auth.signOut()
            } else if (isExpired) {
                authState = AuthState.Error("Assinatura vencida.")
            } else {
                // SALVA A SESSÃO PARA USO OFFLINE FUTURO
                offlineManager.saveSession(
                    email = email,
                    companyId = companyId,
                    role = role,
                    status = status,
                    expiresAtMillis = expiresAt?.toDate()?.time
                )
                authState = AuthState.LoggedIn(companyId, role, isOfflineMode = false)
            }
        } catch (e: Exception) {
            tryLoginOffline()
        }
    }

    // --- LÓGICA OFFLINE ---

    private fun tryLoginOffline() {
        val cached = offlineManager.getSession()
        if (cached != null) {
            validateSessionAndLogin(cached)
        } else {
            authState = AuthState.Error("Sem conexão e sem dados salvos.")
        }
    }

    fun forceOfflineLogin(email: String) {
        val cached = offlineManager.getSession()
        if (cached != null && cached.email.equals(email, ignoreCase = true)) {
            validateSessionAndLogin(cached)
        } else {
            authState = AuthState.Error("Sem dados offline para este usuário. Conecte-se uma vez.")
        }
    }

    private fun validateSessionAndLogin(cached: OfflineData) {
        val now = System.currentTimeMillis()
        val isExpired = cached.expiresAt != -1L && cached.expiresAt < now
        val isBlocked = cached.status == "blocked"

        if (isBlocked) {
            authState = AuthState.Error("Acesso bloqueado (Modo Offline).")
        } else if (isExpired) {
            authState = AuthState.Error("Assinatura vencida (Modo Offline).")
        } else {
            authState = AuthState.LoggedIn(cached.companyId, cached.role, isOfflineMode = true)
        }
    }

    // --- GESTÃO DE EMPRESA (CRIAR E ENTRAR) ---

    fun registerCompany(companyName: String, onSuccess: () -> Unit) {
        val user = auth.currentUser ?: return
        viewModelScope.launch {
            try {
                val newCompanyRef = db.collection("companies").document()
                val uniqueCode = (1..6).map { ('A'..'Z').random() }.joinToString("")

                val company = Company(
                    id = newCompanyRef.id,
                    name = companyName,
                    ownerId = user.uid,
                    ownerEmail = user.email ?: "",
                    shareCode = uniqueCode,
                    createdAt = Timestamp.now(),
                    updatedAt = Timestamp.now()
                )
                newCompanyRef.set(company).await()

                val userProfile = UserProfile(
                    uid = user.uid,
                    email = user.email ?: "",
                    name = user.displayName ?: "Dono",
                    companyId = company.id,
                    role = "owner"
                )
                db.collection("users").document(user.uid).set(userProfile).await()
                createDefaultMenuStructure(company.id)

                // Salva offline
                offlineManager.saveSession(user.email ?: "", company.id, "owner", "active", null)

                authState = AuthState.LoggedIn(company.id, "owner")
                onSuccess()
            } catch (e: Exception) {
                authState = AuthState.Error("Erro: ${e.message}")
            }
        }
    }

    fun joinCompanyWithCode(code: String, onSuccess: () -> Unit) {
        val user = auth.currentUser ?: return
        val cleanCode = code.trim().uppercase()

        viewModelScope.launch {
            try {
                val snapshot = db.collection("companies")
                    .whereEqualTo("shareCode", cleanCode)
                    .limit(1)
                    .get()
                    .await()

                if (snapshot.isEmpty) {
                    authState = AuthState.Error("Código inválido ou empresa não encontrada.")
                    return@launch
                }

                val companyDoc = snapshot.documents.first()
                val companyId = companyDoc.id
                val companyStatus = companyDoc.getString("status") ?: "active"

                val userProfile = UserProfile(
                    uid = user.uid,
                    email = user.email ?: "",
                    name = user.displayName ?: "Funcionário",
                    companyId = companyId,
                    role = "employee"
                )
                db.collection("users").document(user.uid).set(userProfile).await()

                // Salva offline
                offlineManager.saveSession(user.email ?: "", companyId, "employee", companyStatus, null)

                authState = AuthState.LoggedIn(companyId, "employee")
                onSuccess()
            } catch (e: Exception) {
                authState = AuthState.Error(e.message ?: "Erro desconhecido")
            }
        }
    }

    // --- LÓGICA DE QR CODE ---

    fun loginWithQRCode(scannedCode: String, userName: String, role: String, onSuccess: () -> Unit) {
        val cleanCode = scannedCode.trim().uppercase()

        viewModelScope.launch {
            try {
                // A. Valida se a empresa existe
                val snapshot = db.collection("companies")
                    .whereEqualTo("shareCode", cleanCode)
                    .limit(1)
                    .get()
                    .await()

                if (snapshot.isEmpty) {
                    authState = AuthState.Error("QR Code inválido ou empresa não encontrada.")
                    return@launch
                }

                val companyDoc = snapshot.documents.first()
                val companyId = companyDoc.id
                val companyStatus = companyDoc.getString("status") ?: "active"

                // B. Cria Login Anônimo (Se já não estiver logado)
                if (auth.currentUser == null) {
                    auth.signInAnonymously().await()
                }

                val user = auth.currentUser ?: return@launch

                // C. Cria/Atualiza o perfil do funcionário
                val userProfile = UserProfile(
                    uid = user.uid,
                    email = "qrcode_user_${user.uid.take(4)}",
                    name = userName,
                    companyId = companyId,
                    role = role
                )

                db.collection("users").document(user.uid).set(userProfile).await()

                // D. Salva sessão Offline
                offlineManager.saveSession("QR_ACCESS_${user.uid}", companyId, role, companyStatus, null)

                authState = AuthState.LoggedIn(companyId, role)
                onSuccess()

            } catch (e: Exception) {
                authState = AuthState.Error("Erro ao acessar com QR: ${e.message}")
            }
        }
    }

    // --- LÓGICA DE USUÁRIO QR EXISTENTE (NOVO) ---
    // Verifica se é usuário novo ou antigo para o fluxo do QR Code
    fun checkQRCodeUserStatus(scannedCode: String, onNewUser: () -> Unit, onExistingUser: () -> Unit) {
        val cleanCode = scannedCode.trim().uppercase()
        viewModelScope.launch {
            try {
                // Valida empresa
                val snapshot = db.collection("companies").whereEqualTo("shareCode", cleanCode).limit(1).get().await()
                if (snapshot.isEmpty) {
                    authState = AuthState.Error("QR Code inválido.")
                    return@launch
                }

                // Se não tem usuário logado, faz login anônimo temporário para verificar
                if (auth.currentUser == null) {
                    auth.signInAnonymously().await()
                }

                val user = auth.currentUser ?: return@launch
                val userDoc = db.collection("users").document(user.uid).get().await()

                // Verifica se o usuário já pertence à empresa do QR Code
                if (userDoc.exists() && userDoc.getString("companyId") == snapshot.documents.first().id) {
                    val companyId = userDoc.getString("companyId")!!
                    val role = userDoc.getString("role") ?: "employee"
                    val status = snapshot.documents.first().getString("status") ?: "active"

                    // Salva sessão e entra direto
                    offlineManager.saveSession("QR_EXISTING_${user.uid}", companyId, role, status, null)
                    authState = AuthState.LoggedIn(companyId, role)
                    onExistingUser()
                } else {
                    onNewUser()
                }
            } catch (e: Exception) {
                authState = AuthState.Error("Erro ao verificar QR: ${e.message}")
            }
        }
    }

    // Versão da registerQRCodeUser que chama a loginWithQRCode (para compatibilidade)
    fun registerQRCodeUser(scannedCode: String, userName: String, role: String, onSuccess: () -> Unit) {
        loginWithQRCode(scannedCode, userName, role, onSuccess)
    }

    // --- ADMIN E UTILITÁRIOS ---

    private suspend fun createDefaultMenuStructure(companyId: String) {
        val defaultCategories = listOf(
            mapOf("name" to "Lanches", "defaultIngredients" to listOf("Pão", "Carne"), "availableOptionals" to emptyList<Any>())
        )
        db.collection("companies").document(companyId)
            .collection("settings").document("menu_structure")
            .set(mapOf("categories" to defaultCategories)).await()
    }

    fun enterCompanyMode(companyId: String) {
        if (isUserSuperAdmin) {
            authState = AuthState.LoggedIn(companyId, "admin_viewer")
        }
    }

    fun exitCompanyMode() {
        if (isUserSuperAdmin) {
            authState = AuthState.SuperAdmin
        }
    }

    fun signOut() {
        auth.signOut()
        authState = AuthState.LoggedOut
    }
}