package com.manekelsa.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.manekelsa.data.local.dao.WorkerDao
import com.manekelsa.domain.repository.RatingRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RatingRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firebaseDatabase: FirebaseDatabase,
    private val workerDao: WorkerDao
) : RatingRepository {

    override suspend fun updateRating(workerId: String, newRating: Float): Result<Unit> = withContext(Dispatchers.IO) {
        val userId = auth.currentUser?.uid ?: return@withContext Result.failure(Exception("User not logged in"))
        val ratingKey = "${userId}_$workerId"
        val currentTime = System.currentTimeMillis()
        
        try {
            // 1. Check for duplicate rating today
            if (hasUserRatedToday(workerId)) {
                return@withContext Result.failure(Exception("You have already rated this worker today"))
            }

            // 2. Fetch current worker stats
            val workerRef = firebaseDatabase.reference.child("workers").child(workerId)
            val snapshot = workerRef.get().await()
            
            val currentAverage = snapshot.child("averageRating").getValue(Float::class.java) ?: 0f
            val totalRatings = snapshot.child("totalRatings").getValue(Int::class.java) ?: 0
            
            // 3. Calculate new average
            val newTotalRatings = totalRatings + 1
            val newAverage = ((currentAverage * totalRatings) + newRating) / newTotalRatings
            
            // 4. Update Firebase
            val updates = mapOf(
                "averageRating" to newAverage,
                "totalRatings" to newTotalRatings,
                "lastUpdated" to currentTime
            )
            
            val ratingData = mapOf(
                "rating" to newRating,
                "timestamp" to currentTime
            )
            
            workerRef.updateChildren(updates).await()
            firebaseDatabase.reference.child("ratings").child(ratingKey).setValue(ratingData).await()
            
            // 5. Update Local Room DB
            val localWorker = workerDao.getWorkerById(workerId)
            if (localWorker != null) {
                workerDao.updateWorker(localWorker.copy(
                    averageRating = newAverage,
                    totalRatings = newTotalRatings,
                    lastUpdated = currentTime
                ))
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun hasUserRatedToday(workerId: String): Boolean = withContext(Dispatchers.IO) {
        val userId = auth.currentUser?.uid ?: return@withContext false
        val ratingKey = "${userId}_$workerId"
        try {
            val snapshot = firebaseDatabase.reference.child("ratings").child(ratingKey).get().await()
            if (!snapshot.exists()) return@withContext false
            
            val lastRatingTime = snapshot.child("timestamp").getValue(Long::class.java) ?: 0L
            isSameDay(lastRatingTime, System.currentTimeMillis())
        } catch (e: Exception) {
            false
        }
    }

    private fun isSameDay(time1: Long, time2: Long): Boolean {
        val cal1 = Calendar.getInstance().apply { timeInMillis = time1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = time2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }
}
