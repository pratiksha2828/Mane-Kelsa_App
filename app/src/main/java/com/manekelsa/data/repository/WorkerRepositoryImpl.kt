package com.manekelsa.data.repository

import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.manekelsa.data.local.dao.WorkerDao
import com.manekelsa.data.local.entity.WorkerEntity
import com.manekelsa.domain.repository.WorkerRepository
import com.manekelsa.utils.WorkerNameNormalizer
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
    private val firebaseDatabase: FirebaseDatabase,
    private val hireRequestDao: com.manekelsa.data.local.dao.HireRequestDao,
    private val auth: com.google.firebase.auth.FirebaseAuth
) : WorkerRepository {

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var availabilityListener: ValueEventListener? = null
    private var hireRequestsListener: ChildEventListener? = null


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
                    ?: snapshot.key
                if (!id.isNullOrBlank()) {
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
            val id = snapshot.child("id").getValue(String::class.java)?.trim().orEmpty()
                .ifBlank { snapshot.key?.trim().orEmpty() }
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
            val normalizedName = WorkerNameNormalizer.normalize(name, id, phoneNumber)
            val isAvailable = snapshot.child("isAvailable").getValue(Boolean::class.java) ?: false
            val averageRating = snapshot.child("averageRating").getValue(Float::class.java) ?: 0f
            val totalRatings = snapshot.child("totalRatings").getValue(Int::class.java) ?: 0
            val likes = snapshot.child("likes").getValue(Int::class.java) ?: 0
            val lastUpdated = snapshot.child("lastUpdated").getValue(Long::class.java) ?: 0L

            if (id.isBlank()) return null

            if (normalizedName != name) {
                repositoryScope.launch {
                    firebaseDatabase.reference
                        .child("workers")
                        .child(id)
                        .child("name")
                        .setValue(normalizedName)
                }
            }

            val built = WorkerEntity(
                id = id,
                name = normalizedName,
                photoUrl = photoUrl,
                skillsList = skillsList,
                dailyWage = dailyWage,
                area = area,
                experience = experience,
                phoneNumber = phoneNumber,
                averageRating = averageRating,
                totalRatings = totalRatings,
                likes = likes,
                isAvailable = isAvailable,
                lastUpdated = lastUpdated
            )
            if (com.manekelsa.data.local.MockData.shouldHideWorker(built)) return null
            built
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
            if (workerDao.getWorkerById(workerId) == null) {
                val stub = minimalWorkerEntity(
                    userId = workerId,
                    name = auth.currentUser?.displayName?.trim().orEmpty(),
                    phoneDigits = auth.currentUser?.phoneNumber?.filter { it.isDigit() }?.take(10).orEmpty(),
                    area = "",
                    isAvailable = isAvailable,
                    lastUpdated = lastUpdated
                )
                workerDao.insertWorker(stub)
                try {
                    syncToFirebase(stub)
                } catch (_: Exception) {
                }
            } else {
                workerDao.updateAvailability(workerId, isAvailable, lastUpdated)
            }

            val updates = mapOf(
                "isAvailable" to isAvailable,
                "lastUpdated" to lastUpdated
            )
            try {
                firebaseDatabase.reference
                    .child("workers")
                    .child(workerId)
                    .updateChildren(updates)
                    .await()
            } catch (_: Exception) {
            }
        }
    }

    private fun minimalWorkerEntity(
        userId: String,
        name: String,
        phoneDigits: String,
        area: String,
        isAvailable: Boolean,
        lastUpdated: Long
    ): WorkerEntity {
        val resolvedName = name.ifBlank { "Worker" }
        return WorkerEntity(
            id = userId,
            name = resolvedName,
            photoUrl = null,
            skillsList = emptyList(),
            dailyWage = 0.0,
            area = area,
            experience = 0,
            phoneNumber = phoneDigits,
            averageRating = 0f,
            totalRatings = 0,
            likes = 0,
            isAvailable = isAvailable,
            lastUpdated = lastUpdated
        )
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
                val isAvailable = snapshot.child("isAvailable").getValue(Boolean::class.java) ?: false
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
            val userId = auth.currentUser?.uid ?: return@withContext
            val likeKey = "${userId}_$workerId"
            val likesRef = firebaseDatabase.reference.child("worker_likes").child(likeKey)
            val existing = likesRef.get().await()
            if (existing.exists()) return@withContext

            val workerRef = firebaseDatabase.reference.child("workers").child(workerId)
            val currentLikes = workerRef.child("likes").get().await().getValue(Int::class.java) ?: 0
            val newLikes = currentLikes + 1
            val now = System.currentTimeMillis()

            val updates = mapOf(
                "likes" to newLikes,
                "lastUpdated" to now
            )

            workerRef.updateChildren(updates).await()
            likesRef.setValue(mapOf("timestamp" to now)).await()

            workerDao.getWorkerById(workerId)?.let { localWorker ->
                workerDao.updateWorker(localWorker.copy(
                    likes = newLikes,
                    lastUpdated = now
                ))
            }
        }
    }

    override suspend fun clearLocalWorkers() {
        withContext(Dispatchers.IO) {
            workerDao.deleteAllWorkers()
        }
    }

    override suspend fun updateLocalWorkerName(workerId: String, name: String) {
        withContext(Dispatchers.IO) {
            workerDao.updateWorkerName(workerId, name)
        }
    }

    private suspend fun syncToFirebase(worker: WorkerEntity) {
        kotlinx.coroutines.withTimeoutOrNull(5000L) {
            firebaseDatabase.reference
                .child("workers")
                .child(worker.id)
                .setValue(
                    mapOf(
                        "id" to worker.id,
                        "name" to worker.name,
                        "photoUrl" to worker.photoUrl,
                        "skillsList" to worker.skillsList,
                        "dailyWage" to worker.dailyWage,
                        "area" to worker.area,
                        "experience" to worker.experience,
                        "phoneNumber" to worker.phoneNumber,
                        "averageRating" to worker.averageRating,
                        "totalRatings" to worker.totalRatings,
                        "likes" to worker.likes,
                        "isAvailable" to worker.isAvailable,
                        "lastUpdated" to worker.lastUpdated
                    )
                )
                .await()
        }
    }

    override suspend fun createHireRequest(workerId: String, employerId: String, employerName: String) {
        val request = com.manekelsa.data.local.entity.HireRequestEntity(
            id = java.util.UUID.randomUUID().toString(),
            workerId = workerId,
            employerId = employerId,
            employerName = employerName,
            status = "PENDING",
            timestamp = System.currentTimeMillis()
        )
        withContext(Dispatchers.IO) {
            hireRequestDao.insertRequest(request)
            try {
                firebaseDatabase.reference
                    .child("hire_requests")
                    .child(request.id)
                    .setValue(
                        mapOf(
                            "workerId" to request.workerId,
                            "employerId" to request.employerId,
                            "employerName" to request.employerName,
                            "status" to request.status,
                            "timestamp" to request.timestamp
                        )
                    ).await()
            } catch (_: Exception) {
            }
        }
    }

    override fun getAllHireRequests(): Flow<List<com.manekelsa.data.local.entity.HireRequestEntity>> {
        return hireRequestDao.getAllHireRequests()
    }

    override fun getWorkerRequests(workerId: String): Flow<List<com.manekelsa.data.local.entity.HireRequestEntity>> {
        return hireRequestDao.getRequestsForWorker(workerId)
    }

    override fun getEmployerRequests(employerId: String): Flow<List<com.manekelsa.data.local.entity.HireRequestEntity>> {
        return hireRequestDao.getRequestsForEmployer(employerId)
    }

    override suspend fun updateRequestStatus(requestId: String, status: String) {
        withContext(Dispatchers.IO) {
            hireRequestDao.updateRequestStatus(requestId, status)
            try {
                firebaseDatabase.reference
                    .child("hire_requests")
                    .child(requestId)
                    .child("status")
                    .setValue(status)
                    .await()
            } catch (_: Exception) {
            }
        }
    }

    override fun startHireRequestsSync() {
        if (hireRequestsListener != null) return
        val ref = firebaseDatabase.reference.child("hire_requests")
        val listener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                mapSnapshotToHireRequest(snapshot)?.let { req ->
                    repositoryScope.launch { hireRequestDao.insertRequest(req) }
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                mapSnapshotToHireRequest(snapshot)?.let { req ->
                    repositoryScope.launch { hireRequestDao.insertRequest(req) }
                }
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                val id = snapshot.key ?: return
                repositoryScope.launch { hireRequestDao.deleteById(id) }
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}

            override fun onCancelled(error: DatabaseError) {}
        }
        hireRequestsListener = listener
        ref.addChildEventListener(listener)
    }

    override suspend fun mergeResidentContactIntoWorkerProfile(
        userId: String,
        name: String,
        phone: String,
        area: String,
        address: String
    ) {
        withContext(Dispatchers.IO) {
            val existing = workerDao.getWorkerById(userId)
            val phoneDigits = phone.filter { it.isDigit() }.take(10)
            val now = System.currentTimeMillis()
            val base = existing ?: minimalWorkerEntity(
                userId = userId,
                name = name.trim().ifBlank { auth.currentUser?.displayName?.trim().orEmpty() },
                phoneDigits = phoneDigits,
                area = area.trim(),
                isAvailable = false,
                lastUpdated = now
            )
            val merged = base.copy(
                name = name.trim().ifBlank { base.name },
                phoneNumber = phoneDigits.ifBlank { base.phoneNumber },
                area = area.trim().ifBlank { base.area },
                lastUpdated = now
            )
            saveWorkerProfile(merged)
            try {
                firebaseDatabase.reference.child("residents").child(userId).setValue(
                    mapOf(
                        "id" to userId,
                        "name" to name.trim(),
                        "phoneNumber" to phone.filter { it.isDigit() }.take(10),
                        "area" to area.trim(),
                        "address" to address.trim(),
                        "updatedAt" to System.currentTimeMillis()
                    )
                ).await()
            } catch (_: Exception) {
            }
        }
    }

    private fun mapSnapshotToHireRequest(snapshot: DataSnapshot): com.manekelsa.data.local.entity.HireRequestEntity? {
        val id = snapshot.key ?: return null
        val workerId = snapshot.child("workerId").getValue(String::class.java) ?: return null
        val employerId = snapshot.child("employerId").getValue(String::class.java) ?: return null
        val employerName = snapshot.child("employerName").getValue(String::class.java).orEmpty()
        val status = snapshot.child("status").getValue(String::class.java) ?: "PENDING"
        val timestamp = snapshot.child("timestamp").getValue(Long::class.java) ?: System.currentTimeMillis()
        return com.manekelsa.data.local.entity.HireRequestEntity(
            id = id,
            workerId = workerId,
            employerId = employerId,
            employerName = employerName,
            status = status,
            timestamp = timestamp
        )
    }
}
