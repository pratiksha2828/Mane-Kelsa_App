package com.manekelsa.ui.screens

import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.manekelsa.R
import com.manekelsa.ui.components.glassmorphism
import com.manekelsa.ui.components.DeleteAccountDialog
import com.manekelsa.utils.LocalizationManager
import com.manekelsa.utils.TranslationUtils
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

@Composable
fun ResidentProfileScreen(
    displayName: String,
    phoneNumber: String,
    onResidentProfileSaved: (String, String) -> Unit = { _, _ -> },
    onDeleteAccount: () -> Unit = {}
) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val scope = rememberCoroutineScope()
    val auth = FirebaseAuth.getInstance()
    val database = FirebaseDatabase.getInstance()
    val viewModel: HomeViewModel = hiltViewModel()
    val accountDeletionViewModel: AccountDeletionViewModel = hiltViewModel()
    val residentProfileViewModel: ResidentProfileViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()

    val uid = auth.currentUser?.uid?.takeIf { it.isNotBlank() } ?: "default"
    val sharedPref = context.getSharedPreferences("ResidentProfile_$uid", android.content.Context.MODE_PRIVATE)
    val savedName = sharedPref.getString("name", "") ?: ""
    var localName by remember(uid) {
        mutableStateOf(savedName.takeIf { it.isNotBlank() } ?: displayName)
    }
    var address by remember(uid) { mutableStateOf(sharedPref.getString("address", "") ?: "") }
    var area by remember(uid) { mutableStateOf(sharedPref.getString("area", "") ?: "") }
    var localPhone by remember(uid) { mutableStateOf(sharedPref.getString("phoneNumber", phoneNumber) ?: phoneNumber) }
    var isEditMode by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(PaddingValues(16.dp)),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        LaunchedEffect(displayName, savedName) {
            if (savedName.isBlank() && localName != displayName) {
                localName = displayName.takeIf { it.isNotBlank() } ?: localName
            } else if (localName.isBlank() && savedName.isNotBlank()) {
                localName = savedName
            }
        }

        Text(
            text = stringResource(R.string.resident_profile),
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
        )

        OutlinedTextField(
            value = localName,
            onValueChange = { localName = it },
            label = { Text(stringResource(R.string.full_name)) },
            modifier = Modifier.fillMaxWidth(),
            readOnly = !isEditMode,
            singleLine = true
        )

        OutlinedTextField(
            value = localPhone,
            onValueChange = { input ->
                val normalized = input.filter { it.isDigit() }.take(10)
                localPhone = normalized
            },
            label = { Text(stringResource(R.string.phone_number)) },
            modifier = Modifier.fillMaxWidth(),
            readOnly = !isEditMode,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone),
            prefix = { Text("+91") }
        )

        OutlinedTextField(
            value = area,
            onValueChange = { area = it },
            label = { Text(stringResource(R.string.preferred_area)) },
            modifier = Modifier.fillMaxWidth(),
            readOnly = !isEditMode,
            singleLine = true
        )

        OutlinedTextField(
            value = address,
            onValueChange = { address = it },
            label = { Text(stringResource(R.string.address_label)) },
            modifier = Modifier.fillMaxWidth(),
            readOnly = !isEditMode
        )

        Button(
            onClick = {
                if (isEditMode) {
                    if (localName.isBlank()) {
                        Toast.makeText(context, context.getString(R.string.enter_name_error), Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (area.isBlank() || address.isBlank()) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.resident_fill_area_address),
                            Toast.LENGTH_SHORT
                        ).show()
                        return@Button
                    }
                    val profileUserId = auth.currentUser?.uid?.takeIf { it.isNotBlank() }
                        ?: sharedPref.getString("offline_resident_uuid", null)?.takeIf { it.isNotBlank() }
                        ?: UUID.randomUUID().toString().also { gen ->
                            sharedPref.edit().putString("offline_resident_uuid", gen).apply()
                        }
                    val resident = mapOf(
                        "id" to profileUserId,
                        "name" to localName,
                        "phoneNumber" to localPhone,
                        "area" to area,
                        "address" to address,
                        "updatedAt" to System.currentTimeMillis()
                    )
                    scope.launch {
                        val localSaved = sharedPref.edit()
                            .putString("name", localName)
                            .putString("phoneNumber", localPhone)
                            .putString("area", area)
                            .putString("address", address)
                            .commit()

                        if (!localSaved) {
                            Toast.makeText(context, "Local save failed", Toast.LENGTH_SHORT).show()
                            return@launch
                        }

                        try {
                            kotlinx.coroutines.withTimeoutOrNull(5000L) {
                                val updateRequest = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                                    .setDisplayName(localName)
                                    .build()
                                auth.currentUser?.updateProfile(updateRequest)?.await()
                                database.reference.child("residents").child(profileUserId).setValue(resident).await()
                            }
                        } catch (_: Exception) {
                        }

                        auth.currentUser?.uid?.let { uid ->
                            if (uid == profileUserId) {
                                residentProfileViewModel.syncResidentToWorkerProfile(
                                    uid, localName, localPhone, area, address
                                )
                            }
                        }

                        Toast.makeText(context, context.getString(R.string.profile_saved), Toast.LENGTH_SHORT).show()
                        isEditMode = false
                        onResidentProfileSaved(localName, localPhone)
                    }
                } else {
                    isEditMode = true
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            enabled = true
        ) {
            Text(text = if (isEditMode) stringResource(R.string.save_profile) else stringResource(R.string.edit_profile))
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (uiState.employerRequests.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.status_requested) + " / " + stringResource(R.string.status_hired),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    uiState.employerRequests.forEach { req ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .glassmorphism(cornerRadius = 12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = TranslationUtils.getTranslatedName(req.workerName),
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "Status: ${req.status}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (req.status == "ACCEPTED") Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(stringResource(R.string.settings), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
            Text(stringResource(R.string.language_switcher), style = MaterialTheme.typography.titleSmall)
            
            val currentLanguage = LocalizationManager.getLanguage()
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                LanguageRadio(stringResource(R.string.lang_english), "en", currentLanguage) {
                    LocalizationManager.setLanguage(it)
                    activity?.recreate()
                }
                LanguageRadio(stringResource(R.string.lang_kannada), "kn", currentLanguage) {
                    LocalizationManager.setLanguage(it)
                    activity?.recreate()
                }
            }

            var isDeleting by remember { mutableStateOf(false) }
            var showDeleteDialog by remember { mutableStateOf(false) }

            if (showDeleteDialog) {
                DeleteAccountDialog(
                    onConfirm = {
                        isDeleting = true
                        accountDeletionViewModel.deleteAccount(
                            context = context,
                            onSuccess = {
                                isDeleting = false
                                showDeleteDialog = false
                                Toast.makeText(context, "Account deleted successfully", Toast.LENGTH_SHORT).show()
                                onDeleteAccount()
                            },
                            onError = { message ->
                                isDeleting = false
                                showDeleteDialog = false
                                Toast.makeText(context, "Delete Failed: $message", Toast.LENGTH_LONG).show()
                            },
                            onSignedOut = {
                                isDeleting = false
                                showDeleteDialog = false
                                onDeleteAccount()
                            }
                        )
                    },
                    onDismiss = { showDeleteDialog = false },
                    isDeleting = isDeleting
                )
            }

            Button(
                onClick = { showDeleteDialog = true },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isDeleting,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.error),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Delete, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Delete Account")
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}
