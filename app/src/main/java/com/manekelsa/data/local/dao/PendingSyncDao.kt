package com.manekelsa.data.local.dao

import androidx.room.*
import com.manekelsa.data.local.entity.PendingSync

@Dao
interface PendingSyncDao {
    @Insert
    suspend fun insert(pendingSync: PendingSync)

    @Query("SELECT * FROM pending_syncs ORDER BY timestamp ASC")
    suspend fun getAllPendingSyncs(): List<PendingSync>

    @Delete
    suspend fun delete(pendingSync: PendingSync)

    @Query("DELETE FROM pending_syncs WHERE workerId = :workerId AND operation = :operation")
    suspend fun deleteByWorkerAndOperation(workerId: String, operation: String)

    @Query("DELETE FROM pending_syncs")
    suspend fun clearAll()
}
