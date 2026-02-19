package com.walli.flexcriatiwa

import com.google.firebase.Timestamp

data class UserProfile(
    val uid: String = "",
    val email: String = "",
    val name: String = "",
    val companyId: String? = null,
    val companyName: String? = null, // <--- NOVO CAMPO ADICIONADO
    val role: String = "employee",
    val status: String = "active"
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