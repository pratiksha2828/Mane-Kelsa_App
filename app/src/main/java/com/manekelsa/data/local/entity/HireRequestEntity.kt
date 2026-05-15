package com.manekelsa.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "hire_requests")
data class HireRequestEntity(
    @PrimaryKey
    val id: String,
    val workerId: String,
    val employerId: String,
    val employerName: String,
    val status: String,
    val timestamp: Long
)
