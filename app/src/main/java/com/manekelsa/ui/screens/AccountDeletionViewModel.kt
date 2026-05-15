package com.manekelsa.ui.screens

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.manekelsa.data.account.AccountDeletionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AccountDeletionViewModel @Inject constructor(
    private val accountDeletionManager: AccountDeletionManager,
    private val auth: FirebaseAuth
) : ViewModel() {

    fun deleteAccount(
        context: Context,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        /** Called when the user is no longer signed in (e.g. partial delete signed them out). */
        onSignedOut: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val result = accountDeletionManager.deleteAccount(context)
            if (result.isSuccess) {
                onSuccess()
            } else {
                onError(result.exceptionOrNull()?.message ?: "Delete failed")
                if (auth.currentUser == null) {
                    onSignedOut()
                }
            }
        }
    }
}
