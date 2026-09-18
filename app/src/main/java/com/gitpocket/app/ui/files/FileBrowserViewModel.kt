package com.gitpocket.app.ui.files

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gitpocket.app.GitPocketApp
import com.gitpocket.app.data.files.FileEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FileBrowserViewModel(app: Application) : AndroidViewModel(app) {
    private val container = (app as GitPocketApp).container

    private val _entries = MutableStateFlow<List<FileEntry>>(emptyList())
    val entries: StateFlow<List<FileEntry>> = _entries.asStateFlow()

    private val _path = MutableStateFlow("")
    val path: StateFlow<String> = _path.asStateFlow()

    private val _repoName = MutableStateFlow("")
    val repoName: StateFlow<String> = _repoName.asStateFlow()

    fun load(repoId: Long, startPath: String = "") {
        viewModelScope.launch {
            val repo = container.database.repoDao().getById(repoId)
            _repoName.value = repo?.name ?: "Repository"
            _path.value = startPath
            refresh(repoId, startPath)
        }
    }

    fun openDirectory(repoId: Long, dirPath: String) {
        _path.value = dirPath
        viewModelScope.launch { refresh(repoId, dirPath) }
    }

    fun openEntry(repoId: Long, entryPath: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val editable = withContext(Dispatchers.IO) {
                container.fileBrowser.isEditableText(container.gitManager.localDir(repoId), entryPath)
            }
            onResult(editable)
        }
    }

    private suspend fun refresh(repoId: Long, dirPath: String) {
        _entries.value = withContext(Dispatchers.IO) {
            container.fileBrowser.listDir(container.gitManager.localDir(repoId), dirPath)
        }
    }
}