package com.manekelsa

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.*
import com.manekelsa.data.worker.AvailabilityResetWorker
import com.manekelsa.data.worker.OfflineSyncWorker
import com.manekelsa.data.local.entity.WorkerEntity
import com.google.firebase.database.FirebaseDatabase
import dagger.hilt.android.HiltAndroidApp
import com.manekelsa.utils.CrashLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class ManeKelsaApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        CrashLogger.install(this)
        scheduleAvailabilityReset()
        scheduleOfflineSync()
        seedSampleWorkers()
    }

    private fun seedSampleWorkers() {
        val database = FirebaseDatabase.getInstance()
        val workersRef = database.reference.child("workers")
        CoroutineScope(Dispatchers.IO).launch {
            val snapshot = workersRef.get().await()
            if (snapshot.hasChildren()) return@launch

            val now = System.currentTimeMillis()
            val samples = listOf(
                WorkerEntity(
                    id = UUID.randomUUID().toString(),
                    name = "Asha Rao",
                    photoUrl = null,
                    skillsList = listOf("Cleaner", "Caretaker"),
                    dailyWage = 350.0,
                    area = "Indiranagar",
                    experience = 3,
                    phoneNumber = "9876543210",
                    averageRating = 4.6f,
                    totalRatings = 12,
                    isAvailable = true,
                    lastUpdated = now
                ),
                WorkerEntity(
                    id = UUID.randomUUID().toString(),
                    name = "Ravi Shetty",
                    photoUrl = null,
                    skillsList = listOf("Driver", "Cook"),
                    dailyWage = 500.0,
                    area = "Koramangala",
                    experience = 5,
                    phoneNumber = "9123456780",
                    averageRating = 4.4f,
                    totalRatings = 18,
                    isAvailable = true,
                    lastUpdated = now
                ),
                WorkerEntity(
                    id = UUID.randomUUID().toString(),
                    name = "Meera Iyer",
                    photoUrl = null,
                    skillsList = listOf("Gardener", "Painter"),
                    dailyWage = 400.0,
                    area = "Jayanagar",
                    experience = 4,
                    phoneNumber = "9988776655",
                    averageRating = 4.7f,
                    totalRatings = 22,
                    isAvailable = true,
                    lastUpdated = now
                ),
                WorkerEntity(
                    id = UUID.randomUUID().toString(),
                    name = "Naveen Kumar",
                    photoUrl = null,
                    skillsList = listOf("Carpenter", "Painter"),
                    dailyWage = 600.0,
                    area = "Rajajinagar",
                    experience = 6,
                    phoneNumber = "9001122334",
                    averageRating = 4.5f,
                    totalRatings = 16,
                    isAvailable = true,
                    lastUpdated = now
                ),
                WorkerEntity(
                    id = UUID.randomUUID().toString(),
                    name = "Lakshmi Devi",
                    photoUrl = null,
                    skillsList = listOf("Cook", "Cleaner"),
                    dailyWage = 450.0,
                    area = "Basavanagudi",
                    experience = 7,
                    phoneNumber = "9090909090",
                    averageRating = 4.8f,
                    totalRatings = 30,
                    isAvailable = true,
                    lastUpdated = now
                ),
                WorkerEntity(
                    id = UUID.randomUUID().toString(),
                    name = "Suresh Gowda",
                    photoUrl = null,
                    skillsList = listOf("Plumber", "Electrician"),
                    dailyWage = 700.0,
                    area = "Malleshwaram",
                    experience = 8,
                    phoneNumber = "9812345678",
                    averageRating = 4.3f,
                    totalRatings = 20,
                    isAvailable = false,
                    lastUpdated = now
                ),
                WorkerEntity(
                    id = UUID.randomUUID().toString(),
                    name = "Divya Nair",
                    photoUrl = null,
                    skillsList = listOf("Babysitter", "Caretaker"),
                    dailyWage = 550.0,
                    area = "HSR Layout",
                    experience = 5,
                    phoneNumber = "9345678901",
                    averageRating = 4.9f,
                    totalRatings = 28,
                    isAvailable = true,
                    lastUpdated = now
                ),
                WorkerEntity(
                    id = UUID.randomUUID().toString(),
                    name = "Prakash Reddy",
                    photoUrl = null,
                    skillsList = listOf("Driver"),
                    dailyWage = 650.0,
                    area = "Whitefield",
                    experience = 9,
                    phoneNumber = "9900112233",
                    averageRating = 4.2f,
                    totalRatings = 14,
                    isAvailable = true,
                    lastUpdated = now
                ),
                WorkerEntity(
                    id = UUID.randomUUID().toString(),
                    name = "Anita Joshi",
                    photoUrl = null,
                    skillsList = listOf("Cleaner", "Gardener"),
                    dailyWage = 380.0,
                    area = "Yelahanka",
                    experience = 2,
                    phoneNumber = "9887766554",
                    averageRating = 4.1f,
                    totalRatings = 9,
                    isAvailable = true,
                    lastUpdated = now
                )
            )

            samples.forEach { worker ->
                workersRef.child(worker.id).setValue(worker)
            }
        }
    }

    private fun scheduleAvailabilityReset() {
        val currentDate = Calendar.getInstance()
        val dueDate = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 5)
            set(Calendar.SECOND, 0)
        }

        if (dueDate.before(currentDate)) {
            dueDate.add(Calendar.HOUR_OF_DAY, 24)
        }

        val initialDelay = dueDate.timeInMillis - currentDate.timeInMillis

        val workRequest = PeriodicWorkRequestBuilder<AvailabilityResetWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiresBatteryNotLow(true)
                    .build()
            )
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "AvailabilityResetWork",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    private fun scheduleOfflineSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<OfflineSyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "OfflineSyncWork",
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }
}
