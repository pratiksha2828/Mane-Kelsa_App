package com.manekelsa.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.manekelsa.data.local.entity.CallLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CallLogDao {
    @Insert
    suspend fun insertCallLog(callLog: CallLogEntity)

    @Query("SELECT * FROM call_logs ORDER BY timestamp DESC LIMIT 10")
    fun getRecentCallLogs(): Flow<List<CallLogEntity>>

    @Query("SELECT COUNT(*) FROM call_logs")
    fun getTotalCallCount(): Flow<Int>

    @Query("DELETE FROM call_logs")
    suspend fun clearCallLogs()
}
