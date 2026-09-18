package com.gitpocket.app.ui.files

import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gitpocket.app.data.files.FileEntry
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileBrowserScreen(
    repoId: Long,
    onBack: () -> Unit,
    onOpenFile: (String) -> Unit,
    viewModel: FileBrowserViewModel = viewModel(),
) {
    val context = LocalContext.current
    val entries by viewModel.entries.collectAsState()
    val repoName by viewModel.repoName.collectAsState()
    val path by viewModel.path.collectAsState()

    LaunchedEffect(repoId) {
        viewModel.load(repoId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(repoName, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (path.isNotBlank()) {
                            Text(
                                "/$path",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (path.isBlank()) onBack()
                            else viewModel.openDirectory(repoId, parentOf(path))
                        },
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            items(entries, key = { it.path }) { entry ->
                FileRow(
                    entry = entry,
                    onClick = {
                        if (entry.isDirectory) {
                            viewModel.openDirectory(repoId, entry.path)
                        } else {
                            openEntry(repoId, entry, viewModel, onOpenFile, context)
                        }
                    },
                )
            }
        }
    }
}

private fun openEntry(
    repoId: Long,
    entry: FileEntry,
    viewModel: FileBrowserViewModel,
    onOpenFile: (String) -> Unit,
    context: android.content.Context,
) {
    if (viewModel.isEditable(repoId, entry.path)) {
        onOpenFile(entry.path)
    } else {
        openWithExternalApp(context, repoId, entry.path)
    }
}

private fun openWithExternalApp(context: android.content.Context, repoId: Long, path: String) {
    val root = (context.applicationContext as com.gitpocket.app.GitPocketApp).container.gitManager.localDir(repoId)
    val file = java.io.File(root, path)
    val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension.lowercase(Locale.ROOT))
        ?: "application/octet-stream"
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, mime)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    runCatching { context.startActivity(intent) }
}

@Composable
private fun FileRow(entry: FileEntry, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (entry.isDirectory) Icons.Default.Folder else Icons.Default.Description,
            contentDescription = null,
            tint = if (entry.isDirectory) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(entry.name, style = MaterialTheme.typography.bodyLarge)
            if (!entry.isDirectory) {
                Text(
                    humanSize(entry.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun parentOf(path: String): String =
    path.substringBeforeLast('/', "")

private fun humanSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024.0)
    bytes < 1024 * 1024 * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
    else -> "%.1f GB".format(bytes / (1024.0 * 1024.0 * 1024.0))
}