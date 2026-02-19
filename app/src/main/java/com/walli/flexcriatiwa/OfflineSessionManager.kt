package com.walli.flexcriatiwa

import android.content.Context
import android.content.SharedPreferences

class OfflineSessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("flex_criatiwa_session", Context.MODE_PRIVATE)

    fun saveSession(email: String, companyId: String, role: String, status: String, userName: String?) {
        val editor = prefs.edit()
        editor.putString("email", email)
        editor.putString("companyId", companyId)
        editor.putString("role", role)
        editor.putString("status", status)
        if (userName != null) editor.putString("userName", userName)
        editor.apply()
    }

    fun getSession(): Map<String, String?> {
        return mapOf(
            "email" to prefs.getString("email", null),
            "companyId" to prefs.getString("companyId", null),
            "role" to prefs.getString("role", null),
            "status" to prefs.getString("status", null),
            "userName" to prefs.getString("userName", null)
        )
    }

    fun clearSession() {
        prefs.edit().clear().apply()
    }

    // --- NOVAS FUNÇÕES PARA CONFIGURAÇÃO DE NOTIFICAÇÃO ---

    fun setNotifyKitchen(enabled: Boolean) {
        prefs.edit().putBoolean("notify_kitchen", enabled).apply()
    }

    fun getNotifyKitchen(): Boolean {
        // Padrão: True se for Owner ou Kitchen, False outros (mas o usuário pode mudar)
        val role = prefs.getString("role", "")
        val default = role == "owner" || role == "kitchen"
        return prefs.getBoolean("notify_kitchen", default)
    }

    fun setNotifyCounter(enabled: Boolean) {
        prefs.edit().putBoolean("notify_counter", enabled).apply()
    }

    fun getNotifyCounter(): Boolean {
        // Padrão: True se for Owner, Waiter ou Counter
        val role = prefs.getString("role", "")
        val default = role == "owner" || role == "waiter" || role == "counter"
        return prefs.getBoolean("notify_counter", default)
    }
}