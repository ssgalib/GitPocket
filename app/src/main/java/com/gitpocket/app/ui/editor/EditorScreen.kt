package com.gitpocket.app.ui.editor

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gitpocket.app.GitPocketApp
import com.gitpocket.app.data.SettingsStore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    repoId: Long,
    path: String,
    onBack: () -> Unit,
    viewModel: EditorViewModel = viewModel(),
) {
    val content by viewModel.content.collectAsState()
    val fileName by viewModel.fileName.collectAsState()
    val busy by viewModel.busy.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showPushDialog by remember { mutableStateOf(false) }

    LaunchedEffect(repoId, path) {
        viewModel.load(repoId, path)
    }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(fileName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.save(repoId, path) }, enabled = !busy) {
                        Icon(Icons.Default.Save, contentDescription = "Save")
                    }
                    IconButton(onClick = { showPushDialog = true }, enabled = !busy) {
                        if (busy) {
                            CircularProgressIndicator(strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Save and push")
                        }
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            BasicTextField(
                value = content,
                onValueChange = viewModel::updateContent,
                textStyle = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                ),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
            )
        }
    }

    if (showPushDialog) {
        val context = LocalContext.current
        val settings = (context.applicationContext as GitPocketApp).container.settingsStore
        PushDialog(
            path = path,
            settings = settings,
            onConfirm = { message, name, email ->
                showPushDialog = false
                viewModel.saveAndPush(repoId, path, message, name, email)
            },
            onDismiss = { showPushDialog = false },
        )
    }
}

@Composable
private fun PushDialog(
    path: String,
    settings: SettingsStore,
    onConfirm: (message: String, name: String, email: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var message by remember { mutableStateOf("Update $path") }
    var name by remember { mutableStateOf(settings.gitUserName) }
    var email by remember { mutableStateOf(settings.gitUserEmail) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Commit and push") },
        text = {
            Column {
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Commit message") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Author name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Author email") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(message, name, email) },
                enabled = message.isNotBlank() && name.isNotBlank() && email.isNotBlank(),
            ) {
                Text("Commit & push")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}