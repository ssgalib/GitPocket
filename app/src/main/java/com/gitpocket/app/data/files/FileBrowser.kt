package com.gitpocket.app.data.files

import java.io.File

data class FileEntry(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long,
)

class FileBrowser {

    fun listDir(root: File, relativeDir: String): List<FileEntry> {
        val dir = if (relativeDir.isBlank()) root else File(root, relativeDir)
        return (dir.listFiles() ?: emptyArray())
            .filter { it.name != ".git" }
            .map { FileEntry(it.name, relativePath(root, it), it.isDirectory, it.length()) }
            .sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
    }

    fun readText(root: File, relative: String): String = File(root, relative).readText()

    fun writeText(root: File, relative: String, content: String) {
        File(root, relative).writeText(content)
    }

    fun isEditableText(root: File, relative: String): Boolean {
        val f = File(root, relative)
        if (!f.isFile) return false
        if (f.length() > MAX_TEXT_SIZE) return false
        return f.extension.lowercase() in TEXT_EXTENSIONS
    }

    fun file(root: File, relative: String): File = File(root, relative)

    private fun relativePath(root: File, child: File): String =
        child.toRelativeString(root)

    private companion object {
        const val MAX_TEXT_SIZE = 5_000_000L
        val TEXT_EXTENSIONS = setOf(
            "txt", "md", "markdown", "json", "xml", "yml", "yaml", "html", "htm",
            "css", "js", "mjs", "cjs", "ts", "tsx", "jsx", "kt", "kts", "java",
            "py", "c", "h", "cpp", "hpp", "cc", "cs", "go", "rs", "rb", "php",
            "sh", "bat", "ps1", "sql", "ini", "cfg", "conf", "toml", "log", "csv",
            "tsv", "gradle", "properties", "env", "gitignore", "gitattributes",
            "lock", "md5", "sha", "patch", "diff", "scss", "sass", "less", "vue",
            "svelte", "dockerfile", "makefile", "mk", "svg", "editorconfig"
        )
    }
}