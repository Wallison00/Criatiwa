package com.walli.flexcriatiwa

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class OfflineSessionManager(context: Context) {

    private val sharedPreferences = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            "flex_offline_session",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        context.getSharedPreferences("flex_offline_fallback", Context.MODE_PRIVATE)
    }

    // Agora salvamos o EMAIL também
    fun saveSession(email: String, companyId: String, role: String, status: String, expiresAtMillis: Long?) {
        sharedPreferences.edit().apply {
            putString("saved_email_check", email) // Validação de segurança
            putString("companyId", companyId)
            putString("role", role)
            putString("status", status)
            putLong("expiresAt", expiresAtMillis ?: -1L)
            apply()
        }
    }

    fun getSession(): OfflineData? {
        val companyId = sharedPreferences.getString("companyId", null)
        val role = sharedPreferences.getString("role", null)

        if (companyId != null && role != null) {
            return OfflineData(
                email = sharedPreferences.getString("saved_email_check", "") ?: "",
                companyId = companyId,
                role = role,
                status = sharedPreferences.getString("status", "active") ?: "active",
                expiresAt = sharedPreferences.getLong("expiresAt", -1L)
            )
        }
        return null
    }

    fun clearSession() {
        sharedPreferences.edit().clear().apply()
    }
}

data class OfflineData(
    val email: String,
    val companyId: String,
    val role: String,
    val status: String,
    val expiresAt: Long
)