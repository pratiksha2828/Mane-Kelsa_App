package com.manekelsa.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.manekelsa.data.local.entity.WorkerEntity
import com.manekelsa.data.account.AccountDeletionManager
import com.manekelsa.domain.repository.CallLogRepository
import com.manekelsa.domain.repository.WorkerRepository
import com.manekelsa.utils.LocalizationManager
import com.manekelsa.ui.model.SkillOption
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class WorkerProfileUiState(
    val fullName: String = "",
    val phoneNumber: String = "",
    val localPhotoUri: Uri? = null,
    val profilePhotoUrl: String? = null,
    val dailyWage: String = "",
    val areaStreet: String = "",
    val experienceYears: String = "0-1",
    val selectedSkills: Set<String> = emptySet(),
    val isAvailable: Boolean = false,
    val lastActive: String = "Never",
    val totalJobs: Int = 0,
    val averageRating: Float = 0f,
    val likes: Int = 0,
    val timesContacted: Int = 0,
    val currentLanguage: String = "en",
    val isEditMode: Boolean = false,
    val isLoading: Boolean = false,
    val message: String? = null
)

@HiltViewModel
class WorkerProfileViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val auth: FirebaseAuth,
    private val storage: FirebaseStorage,
    private val database: FirebaseDatabase,
    private val repository: WorkerRepository,
    private val callLogRepository: CallLogRepository,
    private val accountDeletionManager: AccountDeletionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(WorkerProfileUiState())
    val uiState: StateFlow<WorkerProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
        loadStats()
        _uiState.update { it.copy(currentLanguage = LocalizationManager.getLanguage()) }
    }

    private fun loadProfile() {
        val userId = auth.currentUser?.uid ?: return
        _uiState.update { it.copy(phoneNumber = auth.currentUser?.phoneNumber ?: "", isLoading = true) }
        
        viewModelScope.launch {
            try {
                val prefs = appContext.getSharedPreferences("ResidentProfile_$userId", Context.MODE_PRIVATE)
                val prefName = prefs.getString("name", "").orEmpty().trim()
                val prefPhone = prefs.getString("phoneNumber", "").orEmpty().trim()
                val prefArea = prefs.getString("area", "").orEmpty().trim()
                val prefAddress = prefs.getString("address", "").orEmpty().trim()

                var worker = repository.getWorkerProfile(userId)
                if (worker == null && (prefName.isNotEmpty() || prefPhone.isNotEmpty() || prefArea.isNotEmpty())) {
                    repository.mergeResidentContactIntoWorkerProfile(
                        userId,
                        prefName,
                        prefPhone,
                        prefArea,
                        prefAddress
                    )
                    worker = repository.getWorkerProfile(userId)
                }
                if (worker != null) {
                    updateStateWithWorker(worker)
                }
                _uiState.update { st ->
                    st.copy(
                        fullName = st.fullName.ifBlank { prefName },
                        phoneNumber = st.phoneNumber.ifBlank { prefPhone },
                        areaStreet = st.areaStreet.ifBlank { prefArea }
                    )
                }
            } catch (e: Exception) {
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun updateStateWithWorker(worker: WorkerEntity) {
        _uiState.update { it.copy(
            fullName = worker.name,
            profilePhotoUrl = worker.photoUrl,
            phoneNumber = worker.phoneNumber,
            dailyWage = if (worker.dailyWage > 0) worker.dailyWage.toInt().toString() else "",
            areaStreet = worker.area,
            experienceYears = when(worker.experience) {
                0, 1 -> "0-1"
                2, 3 -> "1-3"
                4, 5 -> "3-5"
                else -> "5+"
            },
            selectedSkills = worker.skillsList.map { SkillOption.normalize(it) }.toSet(),
            isAvailable = worker.isAvailable,
            lastActive = formatTimestamp(worker.lastUpdated),
            totalJobs = worker.totalRatings,
            averageRating = worker.averageRating,
            likes = worker.likes,
            isEditMode = false
        ) }
    }

    private fun loadStats() {
        viewModelScope.launch {
            callLogRepository.getTotalCallCount().collectLatest { count ->
                _uiState.update { it.copy(timesContacted = count) }
            }
        }
    }

    fun updateUiState(reducer: (WorkerProfileUiState) -> WorkerProfileUiState) {
        _uiState.update(reducer)
    }

    fun setInitialContactInfo(name: String, phone: String) {
        _uiState.update { st ->
            st.copy(
                fullName = name.takeIf { it.isNotBlank() } ?: st.fullName,
                phoneNumber = phone.takeIf { it.isNotBlank() } ?: st.phoneNumber
            )
        }
    }

    fun saveProfile(context: Context, onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            val message = "Please sign in to save your profile"
            _uiState.update { it.copy(message = message) }
            onError(message)
            return
        }
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val currentState = _uiState.value
                val now = System.currentTimeMillis()
                var downloadUrl = currentState.profilePhotoUrl
                
                val experienceInt = when(currentState.experienceYears) {
                    "0-1" -> 1
                    "1-3" -> 3
                    "3-5" -> 5
                    "5+" -> 10
                    else -> 0
                }

                var workerEntity = WorkerEntity(
                    id = userId,
                    name = currentState.fullName,
                    photoUrl = downloadUrl,
                    skillsList = currentState.selectedSkills.map { SkillOption.normalize(it) },
                    dailyWage = currentState.dailyWage.toDoubleOrNull() ?: 0.0,
                    area = currentState.areaStreet,
                    experience = experienceInt,
                    phoneNumber = currentState.phoneNumber,
                    averageRating = currentState.averageRating,
                    totalRatings = currentState.totalJobs,
                    likes = currentState.likes,
                    isAvailable = currentState.isAvailable,
                    lastUpdated = now
                )

                // Save locally first so changes persist even if Firebase fails.
                repository.saveWorkerProfile(workerEntity)

                var syncError: String? = null
                if (currentState.localPhotoUri != null) {
                    try {
                        val compressedImage = compressImage(context, currentState.localPhotoUri)
                        if (compressedImage != null) {
                            // 1. Save locally to internal storage
                            val dir = java.io.File(context.filesDir, "profile_pics")
                            if (!dir.exists()) dir.mkdirs()
                            val localFile = java.io.File(dir, "worker_$userId.jpg")
                            java.io.FileOutputStream(localFile).use { it.write(compressedImage) }
                            
                            downloadUrl = "file://" + localFile.absolutePath
                            workerEntity = workerEntity.copy(photoUrl = downloadUrl, lastUpdated = System.currentTimeMillis())
                            repository.saveWorkerProfile(workerEntity)

                            // 2. Try to sync to Firebase Storage
                            kotlinx.coroutines.withTimeout(15000L) {
                                val storageRef = storage.reference.child("worker_profiles/$userId.jpg")
                                val remoteUrl = kotlinx.coroutines.suspendCancellableCoroutine { cont ->
                                    storageRef.putBytes(compressedImage)
                                        .addOnSuccessListener {
                                            storageRef.downloadUrl
                                                .addOnSuccessListener { uri ->
                                                    if (cont.isActive) cont.resumeWith(Result.success(uri.toString()))
                                                }
                                                .addOnFailureListener { e ->
                                                    if (cont.isActive) cont.resumeWith(Result.failure(e))
                                                }
                                        }
                                        .addOnFailureListener { e ->
                                            if (cont.isActive) cont.resumeWith(Result.failure(e))
                                        }
                                }
                                downloadUrl = remoteUrl
                                workerEntity = workerEntity.copy(photoUrl = downloadUrl)
                                repository.saveWorkerProfile(workerEntity)
                            }
                        }
                    } catch (e: Exception) {
                        syncError = e.message ?: "Firebase Sync Failed"
                    }
                }

                try {
                    kotlinx.coroutines.withTimeout(5000L) {
                        val updateRequest = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                            .setDisplayName(currentState.fullName)
                            .build()
                        auth.currentUser?.updateProfile(updateRequest)?.await()
                    }
                } catch (e: Exception) {
                    if (syncError == null) {
                        syncError = e.message
                    }
                }
                try {
                    kotlinx.coroutines.withTimeout(5000L) {
                        withContext(Dispatchers.IO) {
                            context.getSharedPreferences("ResidentProfile_$userId", Context.MODE_PRIVATE).edit()
                                .putString("name", currentState.fullName)
                                .putString("phoneNumber", currentState.phoneNumber)
                                .putString("area", currentState.areaStreet)
                                .apply()
                        }
                        database.reference.child("residents").child(userId).setValue(
                            mapOf(
                                "id" to userId,
                                "name" to currentState.fullName,
                                "phoneNumber" to currentState.phoneNumber,
                                "area" to currentState.areaStreet,
                                "address" to "",
                                "updatedAt" to System.currentTimeMillis()
                            )
                        ).await()
                    }
                } catch (_: Exception) {
                }
                _uiState.update { it.copy(
                    message = if (syncError == null) {
                        "Profile Updated Successfully"
                    } else {
                        "Saved locally. Sync failed: $syncError"
                    },
                    isEditMode = false,
                    profilePhotoUrl = downloadUrl,
                    localPhotoUri = null
                ) }
                onSuccess()
            } catch (e: Exception) {
                val errorMessage = "Update Failed: ${e.message}"
                _uiState.update { it.copy(message = errorMessage) }
                onError(errorMessage)
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun setLanguage(code: String) {
        LocalizationManager.setLanguage(code)
        _uiState.update { it.copy(currentLanguage = code) }
    }

    fun toggleEditMode() {
        _uiState.update { it.copy(isEditMode = !it.isEditMode) }
    }

    fun deleteAccount(context: Context, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val result = accountDeletionManager.deleteAccount(context)
                if (result.isSuccess) {
                    _uiState.update { it.copy(message = "Account deleted successfully", isEditMode = false) }
                    withContext(Dispatchers.Main) {
                        onSuccess()
                    }
                } else {
                    val signedOut = auth.currentUser == null
                    if (signedOut) {
                        withContext(Dispatchers.Main) {
                            onSuccess()
                        }
                    } else {
                        val ex = result.exceptionOrNull()
                        val errorMessage = if (ex.isRecentLoginRequired()) {
                            "Please sign in again to delete your account"
                        } else {
                            "Delete Failed: ${ex?.message}"
                        }
                        _uiState.update { it.copy(message = errorMessage) }
                    }
                }
            } catch (e: Exception) {
                val signedOut = auth.currentUser == null
                if (signedOut) {
                    withContext(Dispatchers.Main) {
                        onSuccess()
                    }
                } else {
                    val errorMessage = if (e.isRecentLoginRequired()) {
                        "Please sign in again to delete your account"
                    } else {
                        "Delete Failed: ${e.message}"
                    }
                    _uiState.update { it.copy(message = errorMessage) }
                }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun Throwable?.isRecentLoginRequired(): Boolean {
        var t: Throwable? = this
        while (t != null) {
            if (t is FirebaseAuthRecentLoginRequiredException) return true
            t = t.cause
        }
        return false
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    private suspend fun compressImage(context: Context, uri: Uri): ByteArray? = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream) ?: return@withContext null
                inputStream?.close()

            var quality = 100
            var byteArray: ByteArray
            val outputStream = ByteArrayOutputStream()

            do {
                outputStream.reset()
                originalBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
                byteArray = outputStream.toByteArray()
                quality -= 10
            } while (byteArray.size > 500 * 1024 && quality > 10)

            byteArray
        } catch (e: Exception) {
            null
        }
    }

    private fun formatTimestamp(timestamp: Long): String {
        if (timestamp == 0L) return "Never"
        val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}
