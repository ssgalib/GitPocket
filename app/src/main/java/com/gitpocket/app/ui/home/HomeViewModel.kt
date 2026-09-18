package com.gitpocket.app.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gitpocket.app.GitPocketApp
import com.gitpocket.app.data.local.RepoEntity
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(app: Application) : AndroidViewModel(app) {
    private val container = (app as GitPocketApp).container
    private val repoDao = container.database.repoDao()

    val repos: StateFlow<List<RepoEntity>> = repoDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _syncing = MutableStateFlow<Set<Long>>(emptySet())
    val syncing: StateFlow<Set<Long>> = _syncing

    private val _messages = MutableSharedFlow<String>()
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    fun pull(repo: RepoEntity) {
        viewModelScope.launch {
            if (repo.id in _syncing.value) return@launch
            _syncing.update { it + repo.id }
            try {
                val result = container.gitManager.pull(repo.id)
                if (result.success) {
                    repoDao.update(repo.copy(lastSyncAt = System.currentTimeMillis()))
                }
                val msg = when {
                    !result.success -> result.error ?: "Pull failed"
                    result.behind > 0 -> "Pulled ${result.behind} new commit(s)"
                    result.ahead > 0 -> "Local is ${result.ahead} commit(s) ahead"
                    else -> "Already up to date"
                }
                _messages.emit(msg)
            } catch (e: Exception) {
                _messages.emit("Pull failed: ${e.message}")
            } finally {
                _syncing.update { it - repo.id }
            }
        }
    }

    fun remove(repo: RepoEntity) {
        viewModelScope.launch {
            container.credentialStore.deleteToken(repo.id)
            repoDao.delete(repo.id)
            container.gitManager.localDir(repo.id).deleteRecursively()
            _messages.emit("Removed ${repo.name}")
        }
    }
}