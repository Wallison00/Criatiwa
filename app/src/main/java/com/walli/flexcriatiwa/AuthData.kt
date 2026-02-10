package com.walli.flexcriatiwa

import com.google.firebase.Timestamp

data class UserProfile(
    val uid: String = "",
    val email: String = "",
    val name: String = "",
    val companyId: String? = null,
    val role: String = "employee", // Será definido pelo gestor
    // NOVO: Controla se o usuário pode entrar
    val status: String = "active" // "active", "pending_approval", "blocked"
)

data class Company(
    val id: String = "",
    val name: String = "",
    val ownerId: String = "",
    val ownerEmail: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now(),
    val status: String = "active",
    val plan: String = "free",
    val expiresAt: Timestamp? = null,
    val shareCode: String = ""
)