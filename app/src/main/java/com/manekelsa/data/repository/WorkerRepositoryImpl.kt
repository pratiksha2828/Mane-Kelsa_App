package com.manekelsa.data.repository

import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.manekelsa.data.local.dao.WorkerDao
import com.manekelsa.data.local.entity.WorkerEntity
import com.manekelsa.data.local.entity.HireRequestEntity
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

    companion object {
        private const val BLOCKED_WORKER_NAME = "Pratiksha Bhat"
        private const val BLOCKED_WORKER_NAME_ALT = "Pratiksha Baht"
        private const val BLOCKED_WORKER_PHONE = "5555555555"
    }

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
                if (shouldRemoveWorker(snapshot)) return
                mapSnapshotToWorker(snapshot)?.let { worker ->
                    repositoryScope.launch {
                        workerDao.insertWorker(worker)
                    }
                    trySend(listOf(worker)) // For immediate UI updates if observing this flow
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                if (shouldRemoveWorker(snapshot)) return
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
            val phoneNumber = snapshot.child("phoneNumber").getValue(String::class.java) ?: ""
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
            val averageRating = snapshot.child("averageRating").getValue(Float::class.java) ?: 0f
            val totalRatings = snapshot.child("totalRatings").getValue(Int::class.java) ?: 0

            if (isTargetWorker(name, phoneNumber, area, dailyWage, experience, skillsList, averageRating, totalRatings)) {
                return null
            }
            val photoUrl = snapshot.child("photoUrl").getValue(String::class.java)
                ?: snapshot.child("profilePhotoUrl").getValue(String::class.java)
            val normalizedName = WorkerNameNormalizer.normalize(name, id, phoneNumber)
            val isAvailable = snapshot.child("isAvailable").getValue(Boolean::class.java) ?: false
            val likes = snapshot.child("likes").getValue(Int::class.java) ?: 0
            val lastUpdated = snapshot.child("lastUpdated").getValue(Long::class.java) ?: 0L

            if (id.isNotBlank() && normalizedName != name) {
                repositoryScope.launch {
                    firebaseDatabase.reference
                        .child("workers")
                        .child(id)
                        .child("name")
                        .setValue(normalizedName)
                }
            }

            WorkerEntity(
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
                .setValue(worker)
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
            firebaseDatabase.reference
                .child("hire_requests")
                .child(workerId)
                .child(request.id)
                .setValue(request)
                .await()
        }
    }

    override fun getAllHireRequests(): Flow<List<com.manekelsa.data.local.entity.HireRequestEntity>> {
        startHireRequestsSyncIfNeeded()
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
            val request = hireRequestDao.getRequestById(requestId)
            if (request != null) {
                firebaseDatabase.reference
                    .child("hire_requests")
                    .child(request.workerId)
                    .child(request.id)
                    .child("status")
                    .setValue(status)
                    .await()
            }
        }
    }

    private fun shouldRemoveWorker(snapshot: DataSnapshot): Boolean {
        val name = snapshot.child("name").getValue(String::class.java)
            ?: snapshot.child("fullName").getValue(String::class.java).orEmpty()
        val phoneNumber = snapshot.child("phoneNumber").getValue(String::class.java) ?: ""
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
        val averageRating = snapshot.child("averageRating").getValue(Float::class.java) ?: 0f
        val totalRatings = snapshot.child("totalRatings").getValue(Int::class.java) ?: 0

        if (isTargetWorker(name, phoneNumber, area, dailyWage, experience, skillsList, averageRating, totalRatings)) {
            val id = snapshot.child("id").getValue(String::class.java)
                ?: snapshot.key
                ?: ""
            snapshot.ref.removeValue()
            if (id.isNotBlank()) {
                repositoryScope.launch {
                    workerDao.getWorkerById(id)?.let { workerDao.deleteWorker(it) }
                }
            }
            return true
        }
        return false
    }

    private fun isTargetWorker(
        name: String,
        phoneNumber: String,
        area: String,
        dailyWage: Double,
        experience: Int,
        skills: List<String>,
        averageRating: Float,
        totalRatings: Int
    ): Boolean {
        val normalizedName = name.trim()
        val normalizedArea = area.trim().lowercase()
        val normalizedSkills = skills.map { it.trim().lowercase() }
        val isNameMatch = normalizedName.equals(BLOCKED_WORKER_NAME, ignoreCase = true) ||
            normalizedName.equals(BLOCKED_WORKER_NAME_ALT, ignoreCase = true)
        val isPhoneMatch = phoneNumber.trim() == BLOCKED_WORKER_PHONE
        val isAreaMatch = normalizedArea.contains("vijayanagar")
        val isSkillMatch = normalizedSkills.contains("cook") && normalizedSkills.contains("caretaker")
        val isWageMatch = dailyWage >= 3000.0
        val isExperienceMatch = experience <= 1
        val isRatingMatch = averageRating >= 4.9f && totalRatings == 1

        if (isPhoneMatch) return true
        return isNameMatch && isAreaMatch && isSkillMatch && isWageMatch && isExperienceMatch && isRatingMatch
    }

    private fun startHireRequestsSyncIfNeeded() {
        if (hireRequestsListener != null) return
        val uid = auth.currentUser?.uid ?: return
        val requestsRef = firebaseDatabase.reference.child("hire_requests").child(uid)

        hireRequestsListener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                mapSnapshotToHireRequest(snapshot, uid)?.let { request ->
                    repositoryScope.launch {
                        hireRequestDao.insertRequest(request)
                    }
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                mapSnapshotToHireRequest(snapshot, uid)?.let { request ->
                    repositoryScope.launch {
                        hireRequestDao.insertRequest(request)
                    }
                }
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                val requestId = snapshot.child("id").getValue(String::class.java)
                    ?: snapshot.key
                    ?: return
                repositoryScope.launch {
                    hireRequestDao.deleteById(requestId)
                }
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}

            override fun onCancelled(error: DatabaseError) {
                // Ignore cancelled sync.
            }
        }

        requestsRef.addChildEventListener(hireRequestsListener!!)
    }

    private fun mapSnapshotToHireRequest(snapshot: DataSnapshot, workerId: String): HireRequestEntity? {
        return try {
            val id = snapshot.child("id").getValue(String::class.java)
                ?: snapshot.key
                ?: return null
            val employerId = snapshot.child("employerId").getValue(String::class.java) ?: ""
            val employerName = snapshot.child("employerName").getValue(String::class.java) ?: ""
            val status = snapshot.child("status").getValue(String::class.java) ?: "PENDING"
            val timestamp = snapshot.child("timestamp").getValue(Long::class.java) ?: 0L

            HireRequestEntity(
                id = id,
                workerId = workerId,
                employerId = employerId,
                employerName = employerName,
                status = status,
                timestamp = timestamp
            )
        } catch (e: Exception) {
            null
        }
    }
}
