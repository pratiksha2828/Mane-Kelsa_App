package com.manekelsa.domain.repository

import com.manekelsa.data.local.entity.WorkerEntity
import kotlinx.coroutines.flow.Flow

interface WorkerRepository {
    suspend fun saveWorkerProfile(worker: WorkerEntity)
    suspend fun getWorkerProfile(workerId: String): WorkerEntity?
    fun getAllWorkers(): Flow<List<WorkerEntity>>
    fun getRemoteWorkers(): Flow<List<WorkerEntity>>
    suspend fun syncWorkerProfile(workerId: String)
    suspend fun updateAvailability(workerId: String, isAvailable: Boolean)
    fun getAvailability(workerId: String): Flow<Boolean?>
    fun startAvailabilitySync(workerId: String)
    suspend fun resetAllAvailability()
    suspend fun rateWorker(workerId: String)
    suspend fun clearLocalWorkers()
    suspend fun updateLocalWorkerName(workerId: String, name: String)
}
