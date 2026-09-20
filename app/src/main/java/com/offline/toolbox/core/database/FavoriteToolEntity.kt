package com.offline.toolbox.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class FavoriteToolEntity(
    @PrimaryKey val toolId: String,
    val displayOrder: Int = 0,
    val addedTimestamp: Long = System.currentTimeMillis()
)
