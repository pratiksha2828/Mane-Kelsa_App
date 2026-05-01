package com.manekelsa.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.manekelsa.data.local.entity.CallLogEntity
import com.manekelsa.data.local.entity.WorkerEntity
import com.manekelsa.domain.repository.CallLogRepository
import com.manekelsa.domain.repository.WorkerRepository
import com.manekelsa.utils.WorkerNameNormalizer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val userName: String? = null,
    val isProfileComplete: Boolean = false,
    val availableWorkersCount: Int = 0,
    val featuredWorkers: List<WorkerEntity> = emptyList(),
    val recentCalls: List<CallLogEntity> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val workerRepository: WorkerRepository,
    private val callLogRepository: CallLogRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private val fallbackWorkers: List<WorkerEntity> = listOf(
        WorkerEntity(
            id = "local_1",
            name = "Shobha Rao",
            photoUrl = null,
            skillsList = listOf("Cleaner", "Cook"),
            dailyWage = 420.0,
            area = "Vijayanagar",
            experience = 4,
            phoneNumber = "9011223344",
            averageRating = 4.4f,
            totalRatings = 11,
            isAvailable = true,
            lastUpdated = System.currentTimeMillis()
        ),
        WorkerEntity(
            id = "local_2",
            name = "Venkatesh Naik",
            photoUrl = null,
            skillsList = listOf("Electrician"),
            dailyWage = 620.0,
            area = "Vijayanagar",
            experience = 7,
            phoneNumber = "9022334455",
            averageRating = 4.5f,
            totalRatings = 15,
            isAvailable = true,
            lastUpdated = System.currentTimeMillis()
        ),
        WorkerEntity(
            id = "local_3",
            name = "Bhavana Rao",
            photoUrl = null,
            skillsList = listOf("Babysitter", "Caretaker"),
            dailyWage = 520.0,
            area = "Banashankari",
            experience = 6,
            phoneNumber = "9122334455",
            averageRating = 4.8f,
            totalRatings = 19,
            isAvailable = true,
            lastUpdated = System.currentTimeMillis()
        ),
        WorkerEntity(
            id = "local_4",
            name = "Praveen Kumar",
            photoUrl = null,
            skillsList = listOf("Driver"),
            dailyWage = 650.0,
            area = "Koramangala",
            experience = 8,
            phoneNumber = "9344556677",
            averageRating = 4.2f,
            totalRatings = 13,
            isAvailable = false,
            lastUpdated = System.currentTimeMillis()
        ),
        WorkerEntity(
            id = "local_5",
            name = "Aparna Shetty",
            photoUrl = null,
            skillsList = listOf("Cook"),
            dailyWage = 480.0,
            area = "Koramangala",
            experience = 5,
            phoneNumber = "9098123456",
            averageRating = 4.3f,
            totalRatings = 17,
            isAvailable = true,
            lastUpdated = System.currentTimeMillis()
        ),
        WorkerEntity(
            id = "local_6",
            name = "Sunita Devi",
            photoUrl = null,
            skillsList = listOf("Gardener", "Cleaner"),
            dailyWage = 380.0,
            area = "JP Nagar",
            experience = 3,
            phoneNumber = "9887766553",
            averageRating = 4.1f,
            totalRatings = 9,
            isAvailable = true,
            lastUpdated = System.currentTimeMillis()
        ),
        WorkerEntity(
            id = "local_7",
            name = "Iqbal Khan",
            photoUrl = null,
            skillsList = listOf("Plumber"),
            dailyWage = 720.0,
            area = "RT Nagar",
            experience = 9,
            phoneNumber = "9098123556",
            averageRating = 4.3f,
            totalRatings = 17,
            isAvailable = true,
            lastUpdated = System.currentTimeMillis()
        ),
        WorkerEntity(
            id = "local_8",
            name = "Farah Siddiqui",
            photoUrl = null,
            skillsList = listOf("Cleaner"),
            dailyWage = 360.0,
            area = "RT Nagar",
            experience = 2,
            phoneNumber = "9988776653",
            averageRating = 4.0f,
            totalRatings = 6,
            isAvailable = true,
            lastUpdated = System.currentTimeMillis()
        ),
        WorkerEntity(
            id = "local_9",
            name = "Meghana Iyer",
            photoUrl = null,
            skillsList = listOf("Nurse", "Caretaker"),
            dailyWage = 700.0,
            area = "Indiranagar",
            experience = 8,
            phoneNumber = "9822001122",
            averageRating = 4.7f,
            totalRatings = 21,
            isAvailable = true,
            lastUpdated = System.currentTimeMillis()
        ),
        WorkerEntity(
            id = "local_10",
            name = "Srinivas Reddy",
            photoUrl = null,
            skillsList = listOf("Painter", "Carpenter"),
            dailyWage = 600.0,
            area = "Whitefield",
            experience = 7,
            phoneNumber = "9001100223",
            averageRating = 4.2f,
            totalRatings = 12,
            isAvailable = true,
            lastUpdated = System.currentTimeMillis()
        )
    )

    private fun loadData() {
        val currentUser = auth.currentUser
        val uid = currentUser?.uid

        viewModelScope.launch {
            if (uid != null) {
                val profile = workerRepository.getWorkerProfile(uid)
                _uiState.value = _uiState.value.copy(
                    userName = profile?.name ?: currentUser.displayName ?: currentUser.phoneNumber,
                    isProfileComplete = profile != null
                )
            }

            launch {
                workerRepository.getRemoteWorkers().collectLatest { }
            }

            launch {
                workerRepository.getAllWorkers().collectLatest { workers ->
                    val sanitizedWorkers = workers.map { worker ->
                        val normalizedName = WorkerNameNormalizer.normalize(worker.name, worker.id, worker.phoneNumber)
                        if (normalizedName != worker.name) {
                            viewModelScope.launch {
                                workerRepository.updateLocalWorkerName(worker.id, normalizedName)
                            }
                        }
                        worker.copy(name = normalizedName)
                    }

                    val data = if (sanitizedWorkers.isEmpty()) fallbackWorkers else sanitizedWorkers
                    val available = data.filter { it.isAvailable }
                    _uiState.value = _uiState.value.copy(
                        availableWorkersCount = available.size,
                        featuredWorkers = available.take(3),
                        isLoading = false
                    )
                }
            }

            launch {
                callLogRepository.getRecentCallLogs().collectLatest { logs ->
                    _uiState.value = _uiState.value.copy(recentCalls = logs)
                }
            }
        }
    }
}
