package com.manekelsa.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.manekelsa.R
import com.manekelsa.domain.repository.WorkerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AvailabilityViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val repository: WorkerRepository
) : ViewModel() {

    private val userId = auth.currentUser?.uid ?: ""

    init {
        if (userId.isNotEmpty()) {
            repository.startAvailabilitySync(userId)
        }
    }

    val isAvailable: StateFlow<Boolean> = repository.getAvailability(userId)
        .map { it ?: false }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    fun updateAvailability(context: Context, available: Boolean) {
        if (userId.isEmpty()) return

        viewModelScope.launch {
            try {
                repository.updateAvailability(userId, available)
                Toast.makeText(context, context.getString(R.string.availability_updated), Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    context.getString(R.string.availability_update_failed, e.message ?: ""),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    fun debugResetAvailability(context: Context) {
        viewModelScope.launch {
            try {
                repository.resetAllAvailability()
                Toast.makeText(context, context.getString(R.string.availability_reset_success), Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    context.getString(R.string.availability_reset_failed, e.message ?: ""),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}
