package com.manekelsa.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.manekelsa.data.local.entity.CallLogEntity
import com.manekelsa.data.local.entity.WorkerEntity
import com.manekelsa.domain.repository.CallLogRepository
import com.manekelsa.domain.repository.WorkerRepository
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
                workerRepository.getRemoteWorkers().collectLatest { workers ->
                    val available = workers.filter { it.isAvailable }
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
