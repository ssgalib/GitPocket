package com.gitpocket.app.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.gitpocket.app.GitPocketApp
import com.gitpocket.app.data.SettingsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsViewModel(app: Application) : AndroidViewModel(app) {
    private val container = (app as GitPocketApp).container
    val store: SettingsStore = container.settingsStore

    private val _name = MutableStateFlow(store.gitUserName)
    val name: StateFlow<String> = _name.asStateFlow()

    private val _email = MutableStateFlow(store.gitUserEmail)
    val email: StateFlow<String> = _email.asStateFlow()

    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved.asStateFlow()

    fun onNameChange(value: String) {
        _name.value = value
    }

    fun onEmailChange(value: String) {
        _email.value = value
    }

    fun save() {
        store.gitUserName = _name.value.trim()
        store.gitUserEmail = _email.value.trim()
        _saved.value = true
    }
}