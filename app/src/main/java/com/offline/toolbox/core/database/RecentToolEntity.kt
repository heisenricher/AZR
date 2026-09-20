package com.offline.toolbox.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recents")
data class RecentToolEntity(
    @PrimaryKey val toolId: String,
    val lastUsedTimestamp: Long,
    val useCount: Int = 1
)
