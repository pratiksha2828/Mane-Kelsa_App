package com.manekelsa.data.repository

import com.manekelsa.data.local.dao.CallLogDao
import com.manekelsa.data.local.entity.CallLogEntity
import com.manekelsa.domain.repository.CallLogRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CallLogRepositoryImpl @Inject constructor(
    private val callLogDao: CallLogDao
) : CallLogRepository {

    override suspend fun addCallLog(workerId: String, workerName: String) {
        withContext(Dispatchers.IO) {
            callLogDao.insertCallLog(
                CallLogEntity(workerId = workerId, workerName = workerName)
            )
        }
    }

    override fun getRecentCallLogs(): Flow<List<CallLogEntity>> {
        return callLogDao.getRecentCallLogs()
    }

    override fun getTotalCallCount(): Flow<Int> {
        return callLogDao.getTotalCallCount()
    }

    override suspend fun clearAllLogs() {
        withContext(Dispatchers.IO) {
            callLogDao.clearCallLogs()
        }
    }
}
