package com.manekelsa.data.repository

import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.manekelsa.data.local.dao.WorkerDao
import com.manekelsa.data.local.entity.WorkerEntity
import com.manekelsa.domain.repository.WorkerRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkerRepositoryImpl @Inject constructor(
    private val workerDao: WorkerDao,
    private val firebaseDatabase: FirebaseDatabase
) : WorkerRepository {

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var availabilityListener: ValueEventListener? = null

    override suspend fun saveWorkerProfile(worker: WorkerEntity) {
        withContext(Dispatchers.IO) {
            workerDao.insertWorker(worker)
            try {
                syncToFirebase(worker)
            } catch (e: Exception) {
            }
        }
    }

    override suspend fun getWorkerProfile(workerId: String): WorkerEntity? {
        return withContext(Dispatchers.IO) {
            workerDao.getWorkerById(workerId)
        }
    }

    override fun getAllWorkers(): Flow<List<WorkerEntity>> {
        return workerDao.getAllWorkers()
    }

    override fun getRemoteWorkers(): Flow<List<WorkerEntity>> = callbackFlow {
        val workersRef = firebaseDatabase.reference.child("workers")
        workersRef.keepSynced(true)
        
        val listener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                mapSnapshotToWorker(snapshot)?.let { worker ->
                    repositoryScope.launch {
                        workerDao.insertWorker(worker)
                    }
                    trySend(listOf(worker)) // For immediate UI updates if observing this flow
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                mapSnapshotToWorker(snapshot)?.let { worker ->
                    repositoryScope.launch {
                        workerDao.insertWorker(worker)
                    }
                    trySend(listOf(worker))
                }
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                val id = snapshot.child("id").getValue(String::class.java)
                if (id != null) {
                    repositoryScope.launch {
                        workerDao.getWorkerById(id)?.let { workerDao.deleteWorker(it) }
                    }
                }
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        workersRef.addChildEventListener(listener)
        awaitClose { workersRef.removeEventListener(listener) }
    }

    private fun mapSnapshotToWorker(snapshot: DataSnapshot): WorkerEntity? {
        return try {
            val id = snapshot.child("id").getValue(String::class.java) ?: ""
            val name = snapshot.child("name").getValue(String::class.java)
                ?: snapshot.child("fullName").getValue(String::class.java).orEmpty()
            val photoUrl = snapshot.child("photoUrl").getValue(String::class.java)
                ?: snapshot.child("profilePhotoUrl").getValue(String::class.java)
            val skillsList = snapshot.child("skillsList").children.mapNotNull { it.getValue(String::class.java) }
                .ifEmpty { snapshot.child("skills").children.mapNotNull { it.getValue(String::class.java) } }
            val dailyWage = snapshot.child("dailyWage").getValue(Double::class.java)
                ?: snapshot.child("dailyWage").getValue(String::class.java)?.toDoubleOrNull()
                ?: 0.0
            val area = snapshot.child("area").getValue(String::class.java)
                ?: snapshot.child("areaStreet").getValue(String::class.java).orEmpty()
            val experience = snapshot.child("experience").getValue(Int::class.java)
                ?: snapshot.child("experienceYears").getValue(String::class.java)?.toIntOrNull()
                ?: 0
            val phoneNumber = snapshot.child("phoneNumber").getValue(String::class.java) ?: ""
            val isAvailable = snapshot.child("isAvailable").getValue(Boolean::class.java) ?: false
            val averageRating = snapshot.child("averageRating").getValue(Float::class.java) ?: 0f
            val totalRatings = snapshot.child("totalRatings").getValue(Int::class.java) ?: 0
            val lastUpdated = snapshot.child("lastUpdated").getValue(Long::class.java) ?: 0L

            WorkerEntity(
                id = id,
                name = name,
                photoUrl = photoUrl,
                skillsList = skillsList,
                dailyWage = dailyWage,
                area = area,
                experience = experience,
                phoneNumber = phoneNumber,
                averageRating = averageRating,
                totalRatings = totalRatings,
                isAvailable = isAvailable,
                lastUpdated = lastUpdated
            )
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun syncWorkerProfile(workerId: String) {
        withContext(Dispatchers.IO) {
            val worker = workerDao.getWorkerById(workerId)
            if (worker != null) {
                syncToFirebase(worker)
            }
        }
    }

    override suspend fun updateAvailability(workerId: String, isAvailable: Boolean) {
        withContext(Dispatchers.IO) {
            val lastUpdated = System.currentTimeMillis()
            workerDao.updateAvailability(workerId, isAvailable, lastUpdated)
            
            val updates = mapOf(
                "isAvailable" to isAvailable,
                "lastUpdated" to lastUpdated
            )
            firebaseDatabase.reference
                .child("workers")
                .child(workerId)
                .updateChildren(updates)
                .await()
        }
    }

    override fun getAvailability(workerId: String): Flow<Boolean?> {
        return workerDao.getAvailabilityFlow(workerId)
    }

    override fun startAvailabilitySync(workerId: String) {
        if (availabilityListener != null) return

        val availabilityRef = firebaseDatabase.reference
            .child("workers")
            .child(workerId)

        availabilityListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val isAvailable = snapshot.child("isAvailable").getValue(Boolean::class.java) ?: return
                val lastUpdated = snapshot.child("lastUpdated").getValue(Long::class.java) ?: System.currentTimeMillis()
                
                repositoryScope.launch {
                    workerDao.updateAvailability(workerId, isAvailable, lastUpdated)
                }
            }

            override fun onCancelled(error: DatabaseError) {
            }
        }

        availabilityRef.addValueEventListener(availabilityListener!!)
    }

    override suspend fun resetAllAvailability() {
        withContext(Dispatchers.IO) {
            val lastUpdated = System.currentTimeMillis()
            workerDao.resetAllAvailability(lastUpdated)
        }
    }

    override suspend fun rateWorker(workerId: String) {
        withContext(Dispatchers.IO) {
            val workerRef = firebaseDatabase.reference.child("workers").child(workerId)
            
            workerRef.child("averageRating").get().await().getValue(Float::class.java)?.let { currentRating ->
                val newRating = (currentRating + 1f).coerceAtMost(5f)
                val totalRatings = (workerRef.child("totalRatings").get().await().getValue(Int::class.java) ?: 0) + 1
                
                val updates = mapOf(
                    "averageRating" to newRating,
                    "totalRatings" to totalRatings,
                    "lastUpdated" to System.currentTimeMillis()
                )
                
                workerRef.updateChildren(updates).await()
                
                workerDao.getWorkerById(workerId)?.let { localWorker ->
                    workerDao.updateWorker(localWorker.copy(
                        averageRating = newRating,
                        totalRatings = totalRatings,
                        lastUpdated = updates["lastUpdated"] as Long
                    ))
                }
            }
        }
    }

    private suspend fun syncToFirebase(worker: WorkerEntity) {
        firebaseDatabase.reference
            .child("workers")
            .child(worker.id)
            .setValue(worker)
            .await()
    }
}
