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
    // Estado de espera para funcionários novos
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

    // Guarda os dados do usuário logado para exibir no Menu Lateral
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

    // --- ESCUTA O PRÓPRIO USUÁRIO (Para atualizar tela do funcionário em tempo real) ---
    private fun startListeningToMyProfile(uid: String) {
        db.collection("users").document(uid).addSnapshotListener { snapshot, _ ->
            if (snapshot != null && snapshot.exists()) {
                val profile = snapshot.toObject(UserProfile::class.java)
                currentUserProfile = profile

                // LÓGICA DE LOGIN AUTOMÁTICO APÓS APROVAÇÃO
                if (profile != null) {
                    if (profile.status == "active" && !profile.companyId.isNullOrBlank()) {
                        // Se o gestor aprovou, libera o acesso
                        authState = AuthState.LoggedIn(profile.companyId, profile.role)
                    } else if (profile.status == "pending_approval") {
                        // Se ainda está pendente, mantém na tela de espera
                        authState = AuthState.PendingApproval
                    } else if (profile.status == "blocked") {
                        authState = AuthState.Error("Seu acesso foi bloqueado.")
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
            // Inicia a escuta em tempo real do perfil
            startListeningToMyProfile(currentUser.uid)
        }
    }

    // --- FUNÇÃO DO GESTOR: APROVAR FUNCIONÁRIO ---
    fun approveUser(userId: String, assignedRole: String) {
        viewModelScope.launch {
            try {
                db.collection("users").document(userId)
                    .update(mapOf(
                        "role" to assignedRole,
                        "status" to "active"
                    )).await()
            } catch (e: Exception) {
                // Erro silencioso ou log
            }
        }
    }

    // --- REGISTRO DE EMPRESA (DONO) ---
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
                    companyId = company.id, // ID CORRETO AQUI
                    role = "owner",
                    status = "active" // O Dono já nasce aprovado
                )
                db.collection("users").document(user.uid).set(userProfile).await()

                currentUserProfile = userProfile
                createDefaultMenuStructure(company.id)
                offlineManager.saveSession(user.email ?: "", company.id, "owner", "active", null)

                authState = AuthState.LoggedIn(company.id, "owner")
                onSuccess()
            } catch (e: Exception) {
                authState = AuthState.Error("Erro: ${e.message}")
            }
        }
    }

    // --- ENTRAR EM EMPRESA VIA CÓDIGO (COM EMAIL/SENHA) ---
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
                    authState = AuthState.Error("Código inválido.")
                    return@launch
                }

                val companyDoc = snapshot.documents.first()
                val companyId = companyDoc.id

                val userProfile = UserProfile(
                    uid = user.uid,
                    email = user.email ?: "",
                    name = user.displayName ?: "Funcionário",
                    companyId = companyId,
                    role = "pending", // Entra como pendente
                    status = "pending_approval"
                )
                db.collection("users").document(user.uid).set(userProfile).await()

                startListeningToMyProfile(user.uid)
                onSuccess()
            } catch (e: Exception) {
                authState = AuthState.Error(e.message ?: "Erro desconhecido")
            }
        }
    }

    // --- REGISTRO VIA QR CODE (NOVO USUÁRIO) ---
    // Esta é a função principal chamada pelo Scanner para novos usuários
    fun loginWithQRCode(scannedCode: String, userName: String, onSuccess: () -> Unit) {
        val cleanCode = scannedCode.trim().uppercase()
        viewModelScope.launch {
            try {
                // 1. Valida Empresa
                val snapshot = db.collection("companies").whereEqualTo("shareCode", cleanCode).limit(1).get().await()
                if (snapshot.isEmpty) {
                    authState = AuthState.Error("QR Code inválido.")
                    return@launch
                }
                val companyId = snapshot.documents.first().id

                // 2. Garante Usuário Anônimo
                if (auth.currentUser == null) {
                    auth.signInAnonymously().await()
                }
                val user = auth.currentUser ?: return@launch

                // 3. Cria Perfil "Pendente"
                val userProfile = UserProfile(
                    uid = user.uid,
                    email = "qrcode_user_${user.uid.take(4)}",
                    name = userName,
                    companyId = companyId,
                    role = "pending",       // Cargo indefinido
                    status = "pending_approval" // STATUS DE ESPERA
                )
                db.collection("users").document(user.uid).set(userProfile).await()

                // 4. Salva sessão local como pendente
                offlineManager.saveSession("QR_${user.uid}", companyId, "pending", "pending_approval", null)

                // 5. Inicia escuta para quando o chefe aprovar
                startListeningToMyProfile(user.uid)
                onSuccess()

            } catch (e: Exception) {
                authState = AuthState.Error("Erro: ${e.message}")
            }
        }
    }

    // Wrapper para compatibilidade com chamadas antigas que passavam 'role'
    fun registerQRCodeUser(scannedCode: String, userName: String, ignoredRole: String, onSuccess: () -> Unit) {
        loginWithQRCode(scannedCode, userName, onSuccess)
    }

    // --- VERIFICAÇÃO DE QR CODE (USUÁRIO JÁ EXISTENTE) ---
    fun checkQRCodeUserStatus(scannedCode: String, onNewUser: () -> Unit, onExistingUser: () -> Unit) {
        val cleanCode = scannedCode.trim().uppercase()
        viewModelScope.launch {
            try {
                val snapshot = db.collection("companies").whereEqualTo("shareCode", cleanCode).limit(1).get().await()
                if (snapshot.isEmpty) {
                    authState = AuthState.Error("QR Code inválido.")
                    return@launch
                }

                if (auth.currentUser == null) {
                    auth.signInAnonymously().await()
                }

                val user = auth.currentUser ?: return@launch
                val userDoc = db.collection("users").document(user.uid).get().await()

                if (userDoc.exists() && userDoc.getString("companyId") == snapshot.documents.first().id) {
                    // Já existe! Carrega o perfil e vê o status
                    val profile = userDoc.toObject(UserProfile::class.java)
                    currentUserProfile = profile

                    if (profile?.status == "active") {
                        authState = AuthState.LoggedIn(profile.companyId!!, profile.role)
                        onExistingUser()
                    } else {
                        // Se estiver pendente ou bloqueado, o listener (startListeningToMyProfile) vai cuidar disso
                        startListeningToMyProfile(user.uid)
                    }
                } else {
                    onNewUser()
                }
            } catch (e: Exception) {
                authState = AuthState.Error("Erro: ${e.message}")
            }
        }
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
        currentUserProfile = null
    }

    // Método auxiliar offline
    fun forceOfflineLogin(email: String) {
        val cached = offlineManager.getSession()
        if (cached != null && cached.email.equals(email, ignoreCase = true)) {
            // Em modo offline forçado, não temos listener em tempo real
            // Assumimos o estado salvo
            if (cached.status == "active") {
                authState = AuthState.LoggedIn(cached.companyId, cached.role, isOfflineMode = true)
            } else {
                authState = AuthState.Error("Acesso não está ativo (Offline).")
            }
        } else {
            authState = AuthState.Error("Sem dados offline.")
        }
    }
}