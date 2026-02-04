package com.walli.flexcriatiwa

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date

class SuperAdminViewModel : ViewModel() {
    private val db = Firebase.firestore

    var companies by mutableStateOf<List<Company>>(emptyList())
        private set

    var isLoading by mutableStateOf(false)
        private set

    // Canal para enviar mensagens de erro/sucesso para a tela
    private val _uiMessage = MutableSharedFlow<String>()
    val uiMessage = _uiMessage.asSharedFlow()

    init {
        fetchAllCompanies()
    }

    fun fetchAllCompanies() {
        viewModelScope.launch {
            isLoading = true
            try {
                val snapshot = db.collection("companies").get().await()
                companies = snapshot.documents.mapNotNull { doc ->
                    try {
                        // Agora o AuthData.kt já tem o campo shareCode, então isso vai compilar
                        Company(
                            id = doc.id,
                            name = doc.getString("name") ?: "Sem Nome",
                            ownerId = doc.getString("ownerId") ?: "",
                            status = doc.getString("status") ?: "active",
                            shareCode = doc.getString("shareCode") ?: "", // <--- AQUI ESTAVA O ERRO ANTES
                            expiresAt = doc.getTimestamp("expiresAt")
                        )
                    } catch (e: Exception) { null }
                }.filter {
                    it.name.isNotBlank() && it.name != "Sem Nome"
                }.sortedBy { it.name }
            } catch (e: Exception) {
                _uiMessage.emit("Erro ao carregar: ${e.message}")
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
                _uiMessage.emit("Status alterado para $newStatus")
            } catch (e: Exception) {
                _uiMessage.emit("Erro ao alterar status: ${e.message}")
            }
        }
    }

    fun deleteCompany(companyId: String) {
        viewModelScope.launch {
            try {
                db.collection("companies").document(companyId).delete().await()
                companies = companies.filter { it.id != companyId }
                _uiMessage.emit("Empresa excluída com sucesso.")
            } catch (e: Exception) {
                _uiMessage.emit("Erro ao excluir: ${e.message}")
            }
        }
    }

    fun updateExpirationDate(companyId: String, newTimestamp: Long?) {
        viewModelScope.launch {
            try {
                val updateData = if (newTimestamp != null) {
                    val date = Date(newTimestamp)
                    val firebaseTimestamp = Timestamp(date)
                    mapOf("expiresAt" to firebaseTimestamp)
                } else {
                    mapOf("expiresAt" to null)
                }

                db.collection("companies").document(companyId)
                    .update(updateData)
                    .await()

                _uiMessage.emit("Data salva com sucesso!")
                fetchAllCompanies()
            } catch (e: Exception) {
                val erro = e.message ?: "Erro desconhecido"
                _uiMessage.emit("ERRO AO SALVAR DATA: $erro")
                android.util.Log.e("SuperAdminViewModel", "Erro updateExpirationDate", e)
            }
        }
    }

    fun formatDate(timestamp: Timestamp?): String {
        if (timestamp == null) return "Vitalício"
        val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale("pt", "BR"))
        return sdf.format(timestamp.toDate())
    }
}