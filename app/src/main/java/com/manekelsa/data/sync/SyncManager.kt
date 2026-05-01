package com.manekelsa.data.sync

import com.google.firebase.database.FirebaseDatabase
import com.google.gson.Gson
import com.manekelsa.data.local.dao.PendingSyncDao
import com.manekelsa.data.local.entity.PendingSync
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncManager @Inject constructor(
    private val pendingSyncDao: PendingSyncDao,
    private val firebaseDatabase: FirebaseDatabase,
    private val gson: Gson
) {
    companion object {
        const val OP_UPDATE_AVAILABILITY = "UPDATE_AVAILABILITY"
    }

    suspend fun addPendingSync(workerId: String, operation: String, payload: Any) {
        val jsonPayload = gson.toJson(payload)
        // If it's an availability update, we only care about the latest one
        if (operation == OP_UPDATE_AVAILABILITY) {
            pendingSyncDao.deleteByWorkerAndOperation(workerId, operation)
        }
        pendingSyncDao.insert(PendingSync(workerId = workerId, operation = operation, payload = jsonPayload))
    }

    suspend fun processPendingSyncs(): Boolean {
        val pendingSyncs = pendingSyncDao.getAllPendingSyncs()
        var allSuccessful = true

        for (sync in pendingSyncs) {
            val success = try {
                when (sync.operation) {
                    OP_UPDATE_AVAILABILITY -> {
                        val payload = gson.fromJson(sync.payload, Map::class.java)
                        firebaseDatabase.reference
                            .child("workers")
                            .child(sync.workerId)
                            .updateChildren(payload as Map<String, Any>)
                            .await()
                        true
                    }
                    else -> false
                }
            } catch (e: Exception) {
                false
            }

            if (success) {
                pendingSyncDao.delete(sync)
            } else {
                allSuccessful = false
            }
        }
        return allSuccessful
    }
}
