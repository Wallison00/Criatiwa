package com.walli.flexcriatiwa

import com.google.firebase.Timestamp

// Dados do Usuário
data class UserProfile(
    val uid: String = "",
    val email: String = "",
    val name: String = "",
    val companyId: String? = null,
    val role: String = "owner" // owner, waiter, kitchen, employee
)

// Dados da Empresa (Com shareCode e expiresAt)
data class Company(
    val id: String = "",
    val name: String = "",
    val ownerId: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    val status: String = "active", // active, blocked
    val plan: String = "free",
    val expiresAt: Timestamp? = null, // null = Acesso Vitalício
    val shareCode: String = "" // <--- O CAMPO QUE FALTAVA
)