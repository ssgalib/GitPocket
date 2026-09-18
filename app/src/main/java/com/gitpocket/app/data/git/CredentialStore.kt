package com.gitpocket.app.data.git

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class CredentialStore(context: Context) {
    private val prefs = EncryptedSharedPreferences.create(
        context,
        "gitpocket_secure",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveToken(repoId: Long, token: String) {
        prefs.edit().putString("token_$repoId", token).apply()
    }

    fun getToken(repoId: Long): String? = prefs.getString("token_$repoId", null)

    fun deleteToken(repoId: Long) {
        prefs.edit().remove("token_$repoId").apply()
    }
}