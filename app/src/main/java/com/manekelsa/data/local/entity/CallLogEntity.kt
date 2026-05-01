package com.manekelsa.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "call_logs")
data class CallLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val workerId: String,
    val workerName: String,
    val timestamp: Long = System.currentTimeMillis()
)
