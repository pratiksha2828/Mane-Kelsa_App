package com.manekelsa.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.manekelsa.data.local.entity.HireRequestEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HireRequestDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRequest(request: HireRequestEntity)

    @Query("SELECT * FROM hire_requests ORDER BY timestamp DESC")
    fun getAllHireRequests(): Flow<List<HireRequestEntity>>

    @Query("SELECT * FROM hire_requests WHERE workerId = :workerId ORDER BY timestamp DESC")
    fun getRequestsForWorker(workerId: String): Flow<List<HireRequestEntity>>

    @Query("SELECT * FROM hire_requests WHERE employerId = :employerId ORDER BY timestamp DESC")
    fun getRequestsForEmployer(employerId: String): Flow<List<HireRequestEntity>>

    @Query("UPDATE hire_requests SET status = :status WHERE id = :requestId")
    suspend fun updateRequestStatus(requestId: String, status: String)

    @Query("DELETE FROM hire_requests WHERE employerId = :uid OR workerId = :uid")
    suspend fun deleteAllForUser(uid: String)

    @Query("DELETE FROM hire_requests")
    suspend fun deleteAll()

    @Query("DELETE FROM hire_requests WHERE id = :id")
    suspend fun deleteById(id: String)
}
