package com.walli.flexcriatiwa

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.Timestamp // Importante para comparar datas
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class AuthState {
    object Loading : AuthState()
    object LoggedOut : AuthState()
    object NeedsCompanyRegistration : AuthState()
    data class LoggedIn(val companyId: String, val userRole: String) : AuthState()
    object SuperAdmin : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel : ViewModel() {
    private val auth = Firebase.auth
    private val db = Firebase.firestore

    // SEU E-MAIL DE SUPER ADMIN
    private val SUPER_ADMIN_EMAIL = "wallisonfreitas00@gmail.com"

    var authState by mutableStateOf<AuthState>(AuthState.Loading)
        private set

    // Propriedade para saber se quem está logado é o Admin
    val isUserSuperAdmin: Boolean
        get() {
            val current = auth.currentUser?.email?.trim()?.lowercase() ?: ""
            val admin = SUPER_ADMIN_EMAIL.trim().lowercase()
            return current == admin
        }

    init {
        checkAuthStatus()
    }

    fun checkAuthStatus() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            authState = AuthState.LoggedOut
        } else {
            val currentEmail = currentUser.email?.trim()?.lowercase() ?: ""
            val targetAdminEmail = SUPER_ADMIN_EMAIL.trim().lowercase()

            Log.d("AUTH_DEBUG", "Email Logado: '$currentEmail'")

            if (currentEmail == targetAdminEmail) {
                authState = AuthState.SuperAdmin
                return
            }

            fetchUserProfile(currentUser.uid)
        }
    }

    // --- MODO ESPIÃO ---
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

    // --- LÓGICA DE LOGIN COM VALIDADE ---
    private fun fetchUserProfile(uid: String) {
        viewModelScope.launch {
            try {
                val doc = db.collection("users").document(uid).get().await()
                if (doc.exists()) {
                    val companyId = doc.getString("companyId")
                    val role = doc.getString("role") ?: "employee"

                    if (!companyId.isNullOrBlank()) {
                        val companyDoc = db.collection("companies").document(companyId).get().await()

                        // 1. Verifica Status (Bloqueio Manual)
                        val status = companyDoc.getString("status") ?: "active"

                        // 2. Verifica Validade (Bloqueio Automático)
                        val expiresAt = companyDoc.getTimestamp("expiresAt")
                        // Compara segundos Unix atuais com o do vencimento
                        val isExpired = expiresAt != null && expiresAt.seconds < Timestamp.now().seconds

                        if (status == "blocked") {
                            authState = AuthState.Error("Acesso bloqueado pelo administrador.")
                            auth.signOut()
                        } else if (isExpired) {
                            // BLOQUEIO POR DATA
                            authState = AuthState.Error("Sua assinatura venceu. Renove para continuar.")
                            auth.signOut()
                        } else {
                            authState = AuthState.LoggedIn(companyId, role)
                        }
                    } else {
                        authState = AuthState.NeedsCompanyRegistration
                    }
                } else {
                    authState = AuthState.NeedsCompanyRegistration
                }
            } catch (e: Exception) {
                authState = AuthState.Error("Erro ao carregar perfil: ${e.message}")
            }
        }
    }

    fun registerCompany(companyName: String, onSuccess: () -> Unit) {
        val user = auth.currentUser ?: return
        viewModelScope.launch {
            try {
                val newCompanyRef = db.collection("companies").document()
                val company = Company(id = newCompanyRef.id, name = companyName, ownerId = user.uid)
                newCompanyRef.set(company).await()

                val userProfile = UserProfile(
                    uid = user.uid,
                    email = user.email ?: "",
                    name = user.displayName ?: "Usuário",
                    companyId = company.id,
                    role = "owner"
                )
                db.collection("users").document(user.uid).set(userProfile).await()
                createDefaultMenuStructure(company.id)
                authState = AuthState.LoggedIn(company.id, "owner")
                onSuccess()
            } catch (e: Exception) {
                authState = AuthState.Error("Erro ao criar empresa: ${e.message}")
            }
        }
    }

    private suspend fun createDefaultMenuStructure(companyId: String) {
        val defaultCategories = listOf(
            mapOf("name" to "Lanches", "defaultIngredients" to listOf("Pão", "Carne"), "availableOptionals" to emptyList<Any>())
        )
        db.collection("companies").document(companyId)
            .collection("settings").document("menu_structure")
            .set(mapOf("categories" to defaultCategories)).await()
    }

    fun signOut() {
        auth.signOut()
        authState = AuthState.LoggedOut
    }
}