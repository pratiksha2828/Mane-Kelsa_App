package com.manekelsa.data.local.dao

import androidx.room.*
import com.manekelsa.data.local.entity.WorkerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkerDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorker(worker: WorkerEntity)

    @Update
    suspend fun updateWorker(worker: WorkerEntity)

    @Query("UPDATE workers SET isAvailable = :isAvailable, lastUpdated = :lastUpdated WHERE id = :workerId")
    suspend fun updateAvailability(workerId: String, isAvailable: Boolean, lastUpdated: Long)

    @Query("UPDATE workers SET name = :name WHERE id = :workerId")
    suspend fun updateWorkerName(workerId: String, name: String)

    @Query("UPDATE workers SET isAvailable = 0, lastUpdated = :lastUpdated")
    suspend fun resetAllAvailability(lastUpdated: Long)

    @Query("SELECT * FROM workers WHERE id = :workerId")
    suspend fun getWorkerById(workerId: String): WorkerEntity?

    @Query("SELECT isAvailable FROM workers WHERE id = :workerId")
    fun getAvailabilityFlow(workerId: String): Flow<Boolean?>

    @Query("SELECT * FROM workers")
    fun getAllWorkers(): Flow<List<WorkerEntity>>

    @Query("DELETE FROM workers")
    suspend fun deleteAllWorkers()

    @Delete
    suspend fun deleteWorker(worker: WorkerEntity)
}
