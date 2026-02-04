package com.walli.flexcriatiwa

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class AuthState {
    object Loading : AuthState()
    object LoggedOut : AuthState()
    object NeedsCompanyRegistration : AuthState()
    data class LoggedIn(val companyId: String, val userRole: String) : AuthState()
    object SuperAdmin : AuthState() // <--- NOVO
    data class Error(val message: String) : AuthState()
}

class AuthViewModel : ViewModel() {
    private val auth = Firebase.auth
    private val db = Firebase.firestore

    // Defina aqui o seu e-mail de administrador
    private val SUPER_ADMIN_EMAIL = "wallisonfreitas00@gmail.com" // ALTERE PARA O SEU E-MAIL REAL

    var authState by mutableStateOf<AuthState>(AuthState.Loading)
        private set

    init {
        checkAuthStatus()
    }

    fun checkAuthStatus() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            authState = AuthState.LoggedOut
        } else {
            // --- CORREÇÃO: VERIFICAÇÃO PRIORITÁRIA ---
            // Verifica o email ignorando maiúsculas/minúsculas e espaços
            val currentEmail = currentUser.email?.trim() ?: ""

            if (currentEmail.equals(SUPER_ADMIN_EMAIL.trim(), ignoreCase = true)) {
                authState = AuthState.SuperAdmin
                return // <--- PARA TUDO E VAI PARA TELA DE ADMIN
            }

            // Se não for admin, segue o fluxo normal
            fetchUserProfile(currentUser.uid)
        }
    }

    private fun fetchUserProfile(uid: String) {
        val currentUser = auth.currentUser

        viewModelScope.launch {
            try {
                val doc = db.collection("users").document(uid).get().await()
                if (doc.exists()) {
                    val companyId = doc.getString("companyId")
                    val role = doc.getString("role") ?: "employee"

                    if (!companyId.isNullOrBlank()) {
                        // Verifica se a empresa está bloqueada
                        val companyDoc = db.collection("companies").document(companyId).get().await()
                        val status = companyDoc.getString("status") ?: "active"

                        if (status == "blocked") {
                            authState = AuthState.Error("Acesso bloqueado. Contate o suporte.")
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
            mapOf("name" to "Lanches", "defaultIngredients" to listOf("Pão", "Carne"), "availableOptionals" to emptyList<Any>()),
            mapOf("name" to "Bebidas", "defaultIngredients" to listOf("Gelo"), "availableOptionals" to emptyList<Any>())
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