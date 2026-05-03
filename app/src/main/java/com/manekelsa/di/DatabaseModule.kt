package com.manekelsa.di

import android.content.Context
import androidx.room.Room
import com.manekelsa.data.local.AppDatabase
import com.manekelsa.data.local.dao.CallLogDao
import com.manekelsa.data.local.dao.PendingSyncDao
import com.manekelsa.data.local.dao.WorkerDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "manekelsa_db"
        )
        .fallbackToDestructiveMigration()
        .build()
    }

    @Provides
    fun provideWorkerDao(database: AppDatabase): WorkerDao {
        return database.workerDao()
    }

    @Provides
    fun providePendingSyncDao(database: AppDatabase): PendingSyncDao {
        return database.pendingSyncDao()
    }

    @Provides
    fun provideCallLogDao(database: AppDatabase): CallLogDao {
        return database.callLogDao()
    }

    @Provides
    fun provideHireRequestDao(database: AppDatabase): com.manekelsa.data.local.dao.HireRequestDao {
        return database.hireRequestDao()
    }
}
