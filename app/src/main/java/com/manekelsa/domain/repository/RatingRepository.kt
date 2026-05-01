package com.manekelsa.domain.repository

interface RatingRepository {
    suspend fun updateRating(workerId: String, newRating: Float): Result<Unit>
    suspend fun hasUserRatedToday(workerId: String): Boolean
}
