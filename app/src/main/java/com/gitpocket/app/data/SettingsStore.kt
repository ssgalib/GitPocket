package com.gitpocket.app.data

import android.content.Context

class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("gitpocket_settings", Context.MODE_PRIVATE)

    var gitUserName: String
        get() = prefs.getString("git_name", "") ?: ""
        set(value) {
            prefs.edit().putString("git_name", value).apply()
        }

    var gitUserEmail: String
        get() = prefs.getString("git_email", "") ?: ""
        set(value) {
            prefs.edit().putString("git_email", value).apply()
        }
}