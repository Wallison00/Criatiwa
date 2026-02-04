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
                // Busca todas as empresas
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
                }.filter { company ->
                    // FILTRO DE LIMPEZA:
                    // 1. Remove empresas sem nome ou vazias
                    // 2. Opcional: Se você souber o ID da sua "empresa de teste" que quer esconder, filtre aqui:
                    // it.id != "ID_DA_SUA_EMPRESA_TESTE"
                    company.name.isNotBlank() && company.name != "Sem Nome"
                }
            } catch (e: Exception) {
                // Erro de permissão ou rede
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

                companies = companies.map {
                    if (it.id == company.id) it.copy(status = newStatus) else it
                }
            } catch (e: Exception) { }
        }
    }

    // --- NOVA FUNÇÃO: DELETAR EMPRESA ---
    fun deleteCompany(companyId: String) {
        viewModelScope.launch {
            try {
                // Deleta o documento da empresa
                db.collection("companies").document(companyId).delete().await()

                // Remove da lista local imediatamente
                companies = companies.filter { it.id != companyId }
            } catch (e: Exception) { }
        }
    }
}