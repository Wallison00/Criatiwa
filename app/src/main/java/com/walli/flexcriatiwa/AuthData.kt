package com.walli.flexcriatiwa

import com.google.firebase.Timestamp

data class UserProfile(
    val uid: String = "",
    val email: String = "",
    val name: String = "",
    val companyId: String? = null,
    val role: String = "owner"
)

data class Company(
    val id: String = "",
    val name: String = "",
    val ownerId: String = "",
    // NOVO: Email do dono (para contato/identificação)
    val ownerEmail: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    // NOVO: Data da última modificação
    val updatedAt: Timestamp = Timestamp.now(),
    val status: String = "active",
    val plan: String = "free",
    val expiresAt: Timestamp? = null,
    val shareCode: String = ""
)