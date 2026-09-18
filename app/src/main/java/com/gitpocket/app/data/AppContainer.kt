package com.gitpocket.app.data

import android.content.Context
import com.gitpocket.app.data.files.FileBrowser
import com.gitpocket.app.data.git.CredentialStore
import com.gitpocket.app.data.git.GitManager
import com.gitpocket.app.data.local.RepoDatabase

class AppContainer(context: Context) {
    val database = RepoDatabase.get(context)
    val credentialStore = CredentialStore(context)
    val settingsStore = SettingsStore(context)
    val gitManager = GitManager(context, credentialStore)
    val fileBrowser = FileBrowser()
}