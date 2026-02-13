package com.walli.flexcriatiwa

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
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
                        // --- ATUALIZADO AQUI: Trata 'deleted' como bloqueio também ---
                    } else if (profile.status == "blocked" || profile.status == "deleted") {
                        authState = AuthState.Error("Acesso revogado.")
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

    fun loginWithGoogleCredential(shareCode: String, idToken: String, onResult: (Boolean, String?) -> Unit) {
        val cleanCode = shareCode.trim().uppercase()

        viewModelScope.launch {
            try {
                // 1. Valida Empresa
                val snapshot = db.collection("companies").whereEqualTo("shareCode", cleanCode).limit(1).get().await()
                if (snapshot.isEmpty) {
                    onResult(false, "Código da empresa inválido.")
                    return@launch
                }
                val companyId = snapshot.documents.first().id

                // 2. Autentica no Firebase
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = auth.signInWithCredential(credential).await()
                val user = authResult.user ?: throw Exception("Falha na autenticação Google")

                // 3. Verifica/Cria Usuário
                val userDocRef = db.collection("users").document(user.uid)
                val userDoc = userDocRef.get().await()

                if (userDoc.exists()) {
                    // Usuário existente
                    val currentCompany = userDoc.getString("companyId")
                    val currentStatus = userDoc.getString("status")

                    // Se ele foi deletado/bloqueado DA MESMA empresa, não deixamos resetar o status.
                    // Mas se ele estiver mudando de empresa, damos uma nova chance (reset para pending)
                    if (currentCompany != companyId) {
                        userDocRef.update(mapOf(
                            "companyId" to companyId,
                            "role" to "pending",
                            "status" to "pending_approval"
                        )).await()
                    }
                    // Se for a mesma empresa, o status 'deleted' será pego pelo listener e bloqueará o login.
                } else {
                    // Novo usuário
                    val userProfile = UserProfile(
                        uid = user.uid,
                        email = user.email ?: "",
                        name = user.displayName ?: "Funcionário Google",
                        companyId = companyId,
                        role = "pending",
                        status = "pending_approval"
                    )
                    userDocRef.set(userProfile).await()
                }

                offlineManager.saveSession(user.email ?: "", companyId, "pending", "pending_approval", null)
                startListeningToMyProfile(user.uid)
                onResult(true, null)

            } catch (e: Exception) {
                onResult(false, "Erro: ${e.message}")
            }
        }
    }

    // --- MÉTODOS MANTIDOS ---
    fun approveUser(userId: String, assignedRole: String) {
        viewModelScope.launch { try { db.collection("users").document(userId).update(mapOf("role" to assignedRole, "status" to "active")).await() } catch (e: Exception) { } }
    }
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
    fun signOut() { auth.signOut(); authState = AuthState.LoggedOut; currentUserProfile = null }
    private suspend fun createDefaultMenuStructure(companyId: String) {
        val defaultCategories = listOf(mapOf("name" to "Lanches", "defaultIngredients" to listOf("Pão", "Carne"), "availableOptionals" to emptyList<Any>()))
        db.collection("companies").document(companyId).collection("settings").document("menu_structure").set(mapOf("categories" to defaultCategories)).await()
    }
    fun enterCompanyMode(companyId: String) { if(isUserSuperAdmin) authState = AuthState.LoggedIn(companyId, "admin_viewer") }
    fun exitCompanyMode() { if(isUserSuperAdmin) authState = AuthState.SuperAdmin }
    fun joinCompanyWithCode(code: String, onSuccess: () -> Unit) {}
    fun forceOfflineLogin(email: String) {}
    fun checkQRCodeUserStatus(code: String, onNew: () -> Unit, onOld: () -> Unit) { onNew() }
    fun registerQRCodeUser(code: String, name: String, role: String, success: () -> Unit) {}
}