package com.gitpocket.app

import android.app.Application
import com.gitpocket.app.data.AppContainer

class GitPocketApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}