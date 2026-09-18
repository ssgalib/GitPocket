package com.gitpocket.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.gitpocket.app.ui.navigation.GitPocketNavHost
import com.gitpocket.app.ui.theme.GitPocketTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GitPocketTheme {
                GitPocketNavHost()
            }
        }
    }
}