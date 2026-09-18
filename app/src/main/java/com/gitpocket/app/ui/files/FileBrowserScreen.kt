package com.gitpocket.app.ui.files

import android.content.ActivityNotFoundException
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gitpocket.app.GitPocketApp
import com.gitpocket.app.data.files.FileEntry
import kotlinx.coroutines.launch
import java.io.File
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
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val entries by viewModel.entries.collectAsState()
    val repoName by viewModel.repoName.collectAsState()
    val path by viewModel.path.collectAsState()

    LaunchedEffect(repoId) {
        viewModel.load(repoId)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                            viewModel.openEntry(repoId, entry.path) { editable ->
                                if (editable) {
                                    onOpenFile(entry.path)
                                } else {
                                    val error = openWithExternalApp(context, repoId, entry.path)
                                    if (error != null) {
                                        scope.launch { snackbarHostState.showSnackbar(error) }
                                    }
                                }
                            }
                        }
                    },
                )
            }
        }
    }
}

private fun openWithExternalApp(context: android.content.Context, repoId: Long, path: String): String? {
    return try {
        val root = (context.applicationContext as GitPocketApp).container.gitManager.localDir(repoId)
        val file = File(root, path)
        val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val mime = guessMimeType(file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mime)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Open with"))
        null
    } catch (e: ActivityNotFoundException) {
        "No app installed to open this file type"
    } catch (e: Exception) {
        "Could not open file: ${e.message}"
    }
}

private fun guessMimeType(file: File): String {
    val ext = file.extension.lowercase(Locale.ROOT)
    MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)?.let { return it }
    return MIME_TYPES[ext] ?: "application/octet-stream"
}

private val MIME_TYPES = mapOf(
    "pdf" to "application/pdf",
    "png" to "image/png",
    "jpg" to "image/jpeg",
    "jpeg" to "image/jpeg",
    "gif" to "image/gif",
    "webp" to "image/webp",
    "bmp" to "image/bmp",
    "svg" to "image/svg+xml",
    "mp4" to "video/mp4",
    "m4v" to "video/mp4",
    "mkv" to "video/x-matroska",
    "webm" to "video/webm",
    "3gp" to "video/3gpp",
    "avi" to "video/x-msvideo",
    "mp3" to "audio/mpeg",
    "m4a" to "audio/mp4",
    "wav" to "audio/x-wav",
    "ogg" to "audio/ogg",
    "opus" to "audio/opus",
    "flac" to "audio/flac",
    "zip" to "application/zip",
    "gz" to "application/gzip",
    "tgz" to "application/gzip",
    "tar" to "application/x-tar",
    "7z" to "application/x-7z-compressed",
    "rar" to "application/vnd.rar",
    "doc" to "application/msword",
    "docx" to "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    "xls" to "application/vnd.ms-excel",
    "xlsx" to "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    "ppt" to "application/vnd.ms-powerpoint",
    "pptx" to "application/vnd.openxmlformats-officedocument.presentationml.presentation",
    "epub" to "application/epub+zip",
    "apk" to "application/vnd.android.package-archive",
)

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