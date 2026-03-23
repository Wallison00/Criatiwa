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
    val shareCode: String = "",
    val tableCount: Int = 20,
    val tablePositions: Map<String, Map<String, Any>> = emptyMap()
)

data class TablePosition(
    val x: Float = 0f,
    val y: Float = 0f,
    val shape: String = "square", // "square", "round", "rectangle"
    val seats: Int = 4,
    val rotation: Float = 0f,
    val isLocked: Boolean = false
)

data class StockItem(
    val id: String = "",
    val name: String = "",
    val barcode: String = "",
    val quantity: Double = 0.0,
    val minQuantity: Double = 0.0,
    val unit: String = "Unidade",
    val imageUrl: String = ""
)

data class StockHistory(
    val id: String = "",
    val stockItemId: String = "",
    val itemName: String = "",
    val type: String = "ENTRADA", // "CADASTRO", "ENTRADA", "EDICAO", "EXCLUSAO"
    val changeAmount: Double = 0.0,
    val finalQuantity: Double = 0.0,
    val timestamp: Timestamp = Timestamp.now(),
    val handledByName: String = ""
)