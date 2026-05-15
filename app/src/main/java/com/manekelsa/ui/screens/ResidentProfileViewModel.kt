package com.manekelsa.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.manekelsa.domain.repository.WorkerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ResidentProfileViewModel @Inject constructor(
    private val workerRepository: WorkerRepository
) : ViewModel() {

    fun syncResidentToWorkerProfile(
        userId: String,
        name: String,
        phone: String,
        area: String,
        address: String
    ) {
        viewModelScope.launch {
            workerRepository.mergeResidentContactIntoWorkerProfile(userId, name, phone, area, address)
        }
    }
}
