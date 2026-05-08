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
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EmployerRequestUiModel(
    val id: String,
    val workerId: String,
    val workerName: String,
    val status: String,
    val timestamp: Long
)

data class HomeUiState(
    val userName: String? = null,
    val isProfileComplete: Boolean = false,
    val totalJobs: Int = 0,
    val averageRating: Float = 0f,
    val timesContacted: Int = 0,
    val availableWorkersCount: Int = 0,
    val featuredWorkers: List<WorkerEntity> = emptyList(),
    val recentCalls: List<CallLogEntity> = emptyList(),
    val pendingRequests: List<com.manekelsa.data.local.entity.HireRequestEntity> = emptyList(),
    val employerRequests: List<EmployerRequestUiModel> = emptyList(),
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

    private var mockRequestGenerated = false

    init {
        loadData()
    }

    private val fallbackWorkers: List<WorkerEntity> = com.manekelsa.data.local.MockData.fallbackWorkers

    private fun loadData() {
        val currentUser = auth.currentUser
        val uid = currentUser?.uid

        viewModelScope.launch {
            if (uid != null) {
                val profile = workerRepository.getWorkerProfile(uid)
                _uiState.value = _uiState.value.copy(
                    userName = profile?.name ?: currentUser.displayName ?: currentUser.phoneNumber,
                    isProfileComplete = profile != null,
                    totalJobs = profile?.totalRatings ?: 0,
                    averageRating = profile?.averageRating ?: 0f
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

                    val data = (sanitizedWorkers + fallbackWorkers).distinctBy { it.id }
                    val available = data.filter { it.isAvailable }
                    _uiState.value = _uiState.value.copy(
                        availableWorkersCount = available.size,
                        featuredWorkers = available.take(3),
                        isLoading = false
                    )
                }
            }

            launch {
                callLogRepository.getTotalCallCount().collectLatest { count ->
                    _uiState.value = _uiState.value.copy(timesContacted = count)
                }
            }

            launch {
                callLogRepository.getRecentCallLogs().collectLatest { logs ->
                    _uiState.value = _uiState.value.copy(recentCalls = logs)
                }
            }

            launch {
                workerRepository.getAllHireRequests().collectLatest { requests ->
                    val workers = workerRepository.getAllWorkers().firstOrNull() ?: fallbackWorkers
                    val empRequests = requests
                        .filter { it.employerId == (uid ?: "local_employer") }
                        .mapNotNull { req -> 
                            val worker = workers.find { it.id == req.workerId } ?: fallbackWorkers.find { it.id == req.workerId }
                            if (worker != null) {
                                EmployerRequestUiModel(req.id, worker.id, worker.name, req.status, req.timestamp)
                            } else null
                        }

                    _uiState.value = _uiState.value.copy(
                        pendingRequests = requests.filter { it.status == "PENDING" && it.workerId == uid },
                        employerRequests = empRequests
                    )
                }
            }
        }
    }

    fun acceptRequest(requestId: String) {
        viewModelScope.launch { workerRepository.updateRequestStatus(requestId, "ACCEPTED") }
    }

    fun rejectRequest(requestId: String) {
        viewModelScope.launch { workerRepository.updateRequestStatus(requestId, "REJECTED") }
    }
}
