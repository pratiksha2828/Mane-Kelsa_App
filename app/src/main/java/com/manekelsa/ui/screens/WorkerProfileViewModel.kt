package com.manekelsa.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.manekelsa.data.local.entity.WorkerEntity
import com.manekelsa.domain.repository.CallLogRepository
import com.manekelsa.domain.repository.WorkerRepository
import com.manekelsa.utils.LocalizationManager
import com.manekelsa.ui.model.SkillOption
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
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
    private val auth: FirebaseAuth,
    private val storage: FirebaseStorage,
    private val repository: WorkerRepository,
    private val callLogRepository: CallLogRepository
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
                // Try local first
                var worker = repository.getWorkerProfile(userId)
                
                if (worker != null) {
                    updateStateWithWorker(worker)
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
        if (_uiState.value.fullName.isEmpty() && name.isNotEmpty()) {
            _uiState.update { it.copy(fullName = name) }
        }
        if (_uiState.value.phoneNumber.isEmpty() && phone.isNotEmpty()) {
            _uiState.update { it.copy(phoneNumber = phone) }
        }
    }

    fun toggleAvailability(isAvailable: Boolean) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                repository.updateAvailability(userId, isAvailable)
                _uiState.update { it.copy(
                    isAvailable = isAvailable,
                    lastActive = formatTimestamp(System.currentTimeMillis())
                ) }
            } catch (e: Exception) {}
        }
    }

    fun saveProfile(context: Context, onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
        val userId = auth.currentUser?.uid ?: return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val currentState = _uiState.value
                var downloadUrl = currentState.profilePhotoUrl
                
                if (currentState.localPhotoUri != null) {
                    val compressedImage = compressImage(context, currentState.localPhotoUri)
                    if (compressedImage != null) {
                        val storageRef = storage.reference.child("worker_profiles/$userId.jpg")
                        storageRef.putBytes(compressedImage).await()
                        downloadUrl = storageRef.downloadUrl.await().toString()
                    }
                }

                val experienceInt = when(currentState.experienceYears) {
                    "0-1" -> 1
                    "1-3" -> 3
                    "3-5" -> 5
                    "5+" -> 10
                    else -> 0
                }

                val workerEntity = WorkerEntity(
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
                    lastUpdated = System.currentTimeMillis()
                )

                repository.saveWorkerProfile(workerEntity)
                _uiState.update { it.copy(
                    message = "Profile Updated Successfully", 
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
        val user = auth.currentUser
        if (user == null) {
            onSuccess()
            return
        }
        val uid = user.uid
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val db = com.google.firebase.database.FirebaseDatabase.getInstance()
                try {
                    db.reference.child("workers").child(uid).removeValue().await()
                    db.reference.child("residents").child(uid).removeValue().await()
                } catch (e: Exception) {
                    // Ignore DB errors to ensure auth account is still deleted
                }
                
                try {
                    repository.clearLocalWorkers()
                } catch (e: Exception) {}

                user.delete().await()
                _uiState.update { it.copy(message = "Account deleted successfully", isEditMode = false) }
                withContext(Dispatchers.Main) {
                    onSuccess()
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(message = "Delete Failed: ${e.message}, logging out instead") }
                auth.signOut()
                withContext(Dispatchers.Main) {
                    onSuccess()
                }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    private suspend fun compressImage(context: Context, uri: Uri): ByteArray? = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream) ?: return@withContext null
            inputStream?.close() ?: return@withContext null

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
