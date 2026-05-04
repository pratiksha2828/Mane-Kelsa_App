package com.manekelsa.ui.screens

import android.widget.Toast
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
import com.manekelsa.utils.LocalizationManager
import com.manekelsa.utils.TranslationUtils
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

@Composable
fun ResidentProfileScreen(
    displayName: String,
    phoneNumber: String,
    onDeleteAccount: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val auth = FirebaseAuth.getInstance()
    val database = FirebaseDatabase.getInstance()
    val viewModel: HomeViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()

    val uid = auth.currentUser?.uid ?: "default"
    val sharedPref = context.getSharedPreferences("ResidentProfile_$uid", android.content.Context.MODE_PRIVATE)
    var localName by remember { mutableStateOf(sharedPref.getString("name", displayName) ?: displayName) }
    var address by remember { mutableStateOf(sharedPref.getString("address", "") ?: "") }
    var area by remember { mutableStateOf(sharedPref.getString("area", "") ?: "") }
    var isEditMode by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(PaddingValues(16.dp)),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
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
            value = phoneNumber,
            onValueChange = {},
            label = { Text(stringResource(R.string.phone_number)) },
            modifier = Modifier.fillMaxWidth(),
            readOnly = true
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
                    val id = auth.currentUser?.uid ?: UUID.randomUUID().toString()
                    val resident = mapOf(
                        "id" to id,
                        "name" to localName,
                        "phoneNumber" to phoneNumber,
                        "area" to area,
                        "address" to address,
                        "updatedAt" to System.currentTimeMillis()
                    )
                    scope.launch {
                        // Save locally first to guarantee functionality even if Firebase is unconfigured
                        sharedPref.edit()
                            .putString("name", localName)
                            .putString("area", area)
                            .putString("address", address)
                            .apply()
                            
                        try {
                            val updateRequest = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                                .setDisplayName(localName)
                                .build()
                            auth.currentUser?.updateProfile(updateRequest)?.await()
                            database.reference.child("residents").child(id).setValue(resident).await()
                        } catch(e: Exception) {
                            // Firebase failed (likely rules or not enabled), but local save succeeded
                        }
                        
                        Toast.makeText(context, context.getString(R.string.profile_saved), Toast.LENGTH_SHORT).show()
                        isEditMode = false
                    }
                } else {
                    isEditMode = true
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            enabled = if (isEditMode) area.isNotBlank() && address.isNotBlank() else true
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
                LanguageRadio(stringResource(R.string.lang_english), "en", currentLanguage) { LocalizationManager.setLanguage(it) }
                LanguageRadio(stringResource(R.string.lang_kannada), "kn", currentLanguage) { LocalizationManager.setLanguage(it) }
            }

            Button(
                onClick = { 
                    val user = auth.currentUser
                    if (user == null) {
                        context.getSharedPreferences("ResidentProfile_default", android.content.Context.MODE_PRIVATE).edit().clear().apply()
                        onDeleteAccount()
                        return@Button
                    }
                    val userUid = user.uid
                    scope.launch {
                        try {
                            try {
                                database.reference.child("residents").child(userUid).removeValue().await()
                            } catch (e: Exception) {
                                // Ignore Firebase DB errors to allow auth account to be deleted
                            }
                            
                            context.getSharedPreferences("ResidentProfile_$userUid", android.content.Context.MODE_PRIVATE).edit().clear().apply()
                            
                            user.delete().await()
                            Toast.makeText(context, "Account deleted successfully", Toast.LENGTH_SHORT).show()
                            onDeleteAccount()
                        } catch (e: Exception) {
                            // If delete fails, force logout so user isn't stuck
                            Toast.makeText(context, "Delete Failed: ${e.message}, logging out instead", Toast.LENGTH_SHORT).show()
                            auth.signOut()
                            onDeleteAccount()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
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
