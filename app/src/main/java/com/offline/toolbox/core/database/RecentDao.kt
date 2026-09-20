package com.offline.toolbox.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentDao {
    @Query("SELECT * FROM recents ORDER BY lastUsedTimestamp DESC LIMIT :limit")
    fun getRecentTools(limit: Int = 10): Flow<List<RecentToolEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun recordUsage(recent: RecentToolEntity)

    @Query("DELETE FROM recents WHERE toolId = :toolId")
    suspend fun deleteRecent(toolId: String)

    @Query("DELETE FROM recents")
    suspend fun clearAllRecents()
}
