package com.gitpocket.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "repos")
data class RepoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val url: String,
    val name: String,
    val branch: String,
    val gitUsername: String = "",
    val hasToken: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val lastSyncAt: Long = 0L,
)