package com.quy.lookimage

import android.content.Context

object PasswordHelper {
    private const val PREF_NAME = "app_prefs"
    private const val PASSWORD_KEY = "user_password"

    fun setPassword(context: Context, password: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(PASSWORD_KEY, password).apply()
    }

    fun getPassword(context: Context): String? {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getString(PASSWORD_KEY, null)
    }

    fun hasPassword(context: Context): Boolean {
        return getPassword(context) != null
    }
}