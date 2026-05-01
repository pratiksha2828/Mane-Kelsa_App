package com.manekelsa.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.manekelsa.data.local.entity.CallLogEntity
import com.manekelsa.domain.repository.CallLogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CallHistoryUiState(
    val logs: List<CallLogEntity> = emptyList(),
    val totalCount: Int = 0,
    val isLoading: Boolean = true
)

@HiltViewModel
class CallHistoryViewModel @Inject constructor(
    private val repository: CallLogRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CallHistoryUiState())
    val uiState: StateFlow<CallHistoryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getRecentCallLogs().collectLatest { logs ->
                _uiState.value = _uiState.value.copy(
                    logs = logs,
                    isLoading = false
                )
            }
        }

        viewModelScope.launch {
            repository.getTotalCallCount().collectLatest { total ->
                _uiState.value = _uiState.value.copy(totalCount = total)
            }
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            repository.clearAllLogs()
        }
    }
}
