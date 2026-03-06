package com.walli.flexcriatiwa

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import androidx.core.content.edit

data class RegisteredPrinter(val mac: String, val name: String, val alias: String)

class OfflineSessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("flex_criatiwa_session", Context.MODE_PRIVATE)

    fun saveSession(email: String, companyId: String, role: String, status: String, userName: String?) {
        prefs.edit {
            putString("email", email)
            putString("companyId", companyId)
            putString("role", role)
            putString("status", status)
            if (userName != null) putString("userName", userName)
        }
    }

    // --- NOVAS FUNÇÕES PARA CONFIGURAÇÃO DE NOTIFICAÇÃO ---

    fun setNotifyKitchen(enabled: Boolean) {
        prefs.edit { putBoolean("notify_kitchen", enabled) }
    }

    fun getNotifyKitchen(): Boolean {
        // Padrão: True se for Owner ou Kitchen, False outros (mas o usuário pode mudar)
        val role = prefs.getString("role", "")
        val default = role == "owner" || role == "kitchen"
        return prefs.getBoolean("notify_kitchen", default)
    }

    fun setNotifyCounter(enabled: Boolean) {
        prefs.edit { putBoolean("notify_counter", enabled) }
    }

    fun getNotifyCounter(): Boolean {
        // Padrão: True se for Owner, Waiter ou Counter
        val role = prefs.getString("role", "")
        val default = role == "owner" || role == "waiter" || role == "counter"
        return prefs.getBoolean("notify_counter", default)
    }

    // --- FUNÇÕES DA IMPRESSORA BLUETOOTH ---
    fun setPrinterMacAddress(mac: String?) {
        prefs.edit { putString("printer_mac", mac) }
    }

    fun getPrinterMacAddress(): String? {
        return prefs.getString("printer_mac", null)
    }

    fun setPrinterAlias(alias: String?) {
        prefs.edit { putString("printer_alias", alias) }
    }

    // --- NOVA LISTA DE IMPRESSORAS CADASTRADAS ---
    fun getRegisteredPrinters(): List<RegisteredPrinter> {
        val json = prefs.getString("registered_printers", null) ?: return emptyList()
        val type = object : TypeToken<List<RegisteredPrinter>>() {}.type
        return try { Gson().fromJson(json, type) } catch(_: Exception) { emptyList() }
    }

    fun addRegisteredPrinter(printer: RegisteredPrinter) {
        val current = getRegisteredPrinters().toMutableList()
        current.removeAll { it.mac == printer.mac }
        current.add(printer)
        prefs.edit { putString("registered_printers", Gson().toJson(current)) }
    }

    fun removeRegisteredPrinter(mac: String) {
        val current = getRegisteredPrinters().toMutableList()
        current.removeAll { it.mac == mac }
        prefs.edit { putString("registered_printers", Gson().toJson(current)) }
    }
}