package com.manekelsa.di

import com.manekelsa.data.repository.CallLogRepositoryImpl
import com.manekelsa.data.repository.RatingRepositoryImpl
import com.manekelsa.data.repository.WorkerRepositoryImpl
import com.manekelsa.domain.repository.CallLogRepository
import com.manekelsa.domain.repository.RatingRepository
import com.manekelsa.domain.repository.WorkerRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindWorkerRepository(
        workerRepositoryImpl: WorkerRepositoryImpl
    ): WorkerRepository

    @Binds
    @Singleton
    abstract fun bindRatingRepository(
        ratingRepositoryImpl: RatingRepositoryImpl
    ): RatingRepository

    @Binds
    @Singleton
    abstract fun bindCallLogRepository(
        callLogRepositoryImpl: CallLogRepositoryImpl
    ): CallLogRepository
}
