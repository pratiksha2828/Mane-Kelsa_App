package com.manekelsa.domain.repository

import com.manekelsa.data.local.entity.CallLogEntity
import kotlinx.coroutines.flow.Flow

interface CallLogRepository {
    suspend fun addCallLog(workerId: String, workerName: String)
    fun getRecentCallLogs(): Flow<List<CallLogEntity>>
    fun getTotalCallCount(): Flow<Int>
    suspend fun clearAllLogs()
}
