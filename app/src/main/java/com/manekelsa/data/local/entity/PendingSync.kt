package com.manekelsa.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_syncs")
data class PendingSync(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val workerId: String,
    val operation: String, // e.g., "UPDATE_AVAILABILITY"
    val payload: String,    // JSON payload
    val timestamp: Long = System.currentTimeMillis()
)
