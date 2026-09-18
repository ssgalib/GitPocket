package com.gitpocket.app.ui.addrepo

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gitpocket.app.GitPocketApp
import com.gitpocket.app.data.local.RepoEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AddRepoViewModel(app: Application) : AndroidViewModel(app) {
    private val container = (app as GitPocketApp).container
    private val repoDao = container.database.repoDao()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _done = MutableStateFlow(false)
    val done: StateFlow<Boolean> = _done.asStateFlow()

    fun addRepo(url: String, name: String, branch: String, token: String) {
        if (_loading.value) return
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            var id = 0L
            try {
                id = repoDao.insert(
                    RepoEntity(
                        url = url.trim(),
                        name = name.trim().ifEmpty { defaultName(url) },
                        branch = branch.trim(),
                        hasToken = token.isNotBlank(),
                    )
                )
                container.gitManager.clone(url.trim(), token.trim().ifEmpty { null }, id)
                val actualBranch = container.gitManager.currentBranch(id)
                repoDao.update(
                    RepoEntity(
                        id = id,
                        url = url.trim(),
                        name = name.trim().ifEmpty { defaultName(url) },
                        branch = actualBranch,
                        hasToken = token.isNotBlank(),
                    )
                )
                _done.value = true
            } catch (e: Exception) {
                if (id != 0L) {
                    container.credentialStore.deleteToken(id)
                    repoDao.delete(id)
                    container.gitManager.localDir(id).deleteRecursively()
                }
                _error.value = e.message ?: "Clone failed. Check the URL and token."
            } finally {
                _loading.value = false
            }
        }
    }

    private fun defaultName(url: String): String {
        val clean = url.trim().removeSuffix("/").removeSuffix(".git")
        return clean.substringAfterLast('/').ifEmpty { clean }
    }
}