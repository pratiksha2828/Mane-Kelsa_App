package com.manekelsa.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@Entity(tableName = "workers")
data class WorkerEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val photoUrl: String?,
    val skillsList: List<String>,
    val dailyWage: Double,
    val area: String,
    val experience: Int,
    val phoneNumber: String,
    val averageRating: Float,
    val totalRatings: Int,
    val isAvailable: Boolean,
    val lastUpdated: Long
)

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        val listType = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, listType)
    }
}
