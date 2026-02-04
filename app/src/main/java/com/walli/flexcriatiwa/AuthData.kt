package com.walli.flexcriatiwa

import com.google.firebase.Timestamp

// Dados do Usuário (salvo em /users/{uid})
data class UserProfile(
    val uid: String = "",
    val email: String = "",
    val name: String = "",
    val companyId: String? = null, // Se nulo, usuário ainda não tem empresa
    val role: String = "owner" // owner, manager, employee
)

// Dados da Empresa (salvo em /companies/{companyId})
data class Company(
    val id: String = "",
    val name: String = "",
    val ownerId: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    val status: String = "active", // active, blocked
    val plan: String = "free"
)