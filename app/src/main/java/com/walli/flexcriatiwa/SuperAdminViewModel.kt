package com.walli.flexcriatiwa

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SuperAdminViewModel : ViewModel() {
    private val db = Firebase.firestore

    var companies by mutableStateOf<List<Company>>(emptyList())
        private set

    var isLoading by mutableStateOf(false)
        private set

    init {
        fetchAllCompanies()
    }

    fun fetchAllCompanies() {
        viewModelScope.launch {
            isLoading = true
            try {
                // Busca todas as empresas (requer regras de segurança permissivas para o admin)
                val snapshot = db.collection("companies").get().await()
                companies = snapshot.documents.mapNotNull { doc ->
                    try {
                        Company(
                            id = doc.id,
                            name = doc.getString("name") ?: "Sem Nome",
                            ownerId = doc.getString("ownerId") ?: "",
                            status = doc.getString("status") ?: "active"
                        )
                    } catch (e: Exception) { null }
                }
            } catch (e: Exception) {
                // Tratar erro (ex: permissão negada)
            } finally {
                isLoading = false
            }
        }
    }

    fun toggleCompanyStatus(company: Company) {
        viewModelScope.launch {
            val newStatus = if (company.status == "active") "blocked" else "active"
            try {
                db.collection("companies").document(company.id)
                    .update("status", newStatus)
                    .await()

                // Atualiza a lista localmente para refletir a mudança rápida
                companies = companies.map {
                    if (it.id == company.id) it.copy(status = newStatus) else it
                }
            } catch (e: Exception) {
                // Erro ao atualizar
            }
        }
    }
}