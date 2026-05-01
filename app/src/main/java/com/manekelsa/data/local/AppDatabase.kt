package com.manekelsa.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.manekelsa.data.local.dao.CallLogDao
import com.manekelsa.data.local.dao.PendingSyncDao
import com.manekelsa.data.local.dao.WorkerDao
import com.manekelsa.data.local.entity.CallLogEntity
import com.manekelsa.data.local.entity.Converters
import com.manekelsa.data.local.entity.PendingSync
import com.manekelsa.data.local.entity.WorkerEntity

@Database(
    entities = [WorkerEntity::class, PendingSync::class, CallLogEntity::class],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun workerDao(): WorkerDao
    abstract fun pendingSyncDao(): PendingSyncDao
    abstract fun callLogDao(): CallLogDao
}
