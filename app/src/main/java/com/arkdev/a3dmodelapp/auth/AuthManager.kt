package com.arkdev.a3dmodelapp.auth


import android.content.Context

class AuthManager(context: Context) {
    private val prefs = context.getSharedPreferences("auth", Context.MODE_PRIVATE)

    companion object {
        const val ROLE_ADMIN = "ADMIN"
        const val ROLE_USER = "USER"
    }

    fun login(username: String, password: String): Boolean {
        val role = when {
            username == "admin" && password == "admin123" -> ROLE_ADMIN
            username == "user" && password == "user123" -> ROLE_USER
            else -> return false
        }

        prefs.edit().apply {
            putBoolean("logged_in", true)
            putString("role", role)
            apply()
        }
        return true
    }

    fun logout() = prefs.edit().clear().apply()

    fun isLoggedIn() = prefs.getBoolean("logged_in", false)

    fun getRole() = prefs.getString("role", null)
}
