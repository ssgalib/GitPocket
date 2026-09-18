package com.gitpocket.app.ui.editor

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gitpocket.app.GitPocketApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditorViewModel(app: Application) : AndroidViewModel(app) {
    private val container = (app as GitPocketApp).container

    private val _content = MutableStateFlow("")
    val content: StateFlow<String> = _content.asStateFlow()

    private val _fileName = MutableStateFlow("")
    val fileName: StateFlow<String> = _fileName.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _messages = MutableSharedFlow<String>()
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    fun load(repoId: Long, path: String) {
        viewModelScope.launch {
            _busy.value = true
            _fileName.value = path.substringAfterLast('/')
            _content.value = withContext(Dispatchers.IO) {
                container.fileBrowser.readText(container.gitManager.localDir(repoId), path)
            }
            _busy.value = false
        }
    }

    fun updateContent(value: String) {
        _content.value = value
    }

    fun save(repoId: Long, path: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                container.fileBrowser.writeText(container.gitManager.localDir(repoId), path, _content.value)
            }
            _messages.emit("Saved locally. Remember to sync to push.")
        }
    }

    fun saveAndPush(repoId: Long, path: String, message: String, name: String, email: String) {
        viewModelScope.launch {
            _busy.value = true
            try {
                withContext(Dispatchers.IO) {
                    container.fileBrowser.writeText(container.gitManager.localDir(repoId), path, _content.value)
                }
                val error = container.gitManager.commitAndPush(repoId, message, name, email)
                _messages.emit(error ?: "Pushed to GitHub")
            } catch (e: Exception) {
                _messages.emit("Push failed: ${e.message}")
            } finally {
                _busy.value = false
            }
        }
    }
}