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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.clickable
import androidx.hilt.navigation.compose.hiltViewModel
import com.manekelsa.ui.screens.HomeViewModel
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.manekelsa.R
import com.manekelsa.ui.components.glassmorphism
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun ResidentProfileScreen(
    displayName: String,
    phoneNumber: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val auth = FirebaseAuth.getInstance()
    val database = FirebaseDatabase.getInstance()
    val viewModel: HomeViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()

    var address by remember { mutableStateOf("") }
    var area by remember { mutableStateOf("") }

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
            value = displayName,
            onValueChange = {},
            label = { Text(stringResource(R.string.full_name)) },
            modifier = Modifier.fillMaxWidth(),
            readOnly = true
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
            singleLine = true
        )

        OutlinedTextField(
            value = address,
            onValueChange = { address = it },
            label = { Text(stringResource(R.string.address_label)) },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                val id = auth.currentUser?.uid ?: UUID.randomUUID().toString()
                val resident = mapOf(
                    "id" to id,
                    "name" to displayName,
                    "phoneNumber" to phoneNumber,
                    "area" to area,
                    "address" to address,
                    "updatedAt" to System.currentTimeMillis()
                )
                scope.launch {
                    database.reference.child("residents").child(id).setValue(resident)
                        .addOnSuccessListener {
                            Toast.makeText(context, context.getString(R.string.profile_saved), Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener {
                            Toast.makeText(context, context.getString(R.string.profile_save_failed), Toast.LENGTH_SHORT).show()
                        }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            enabled = area.isNotBlank() && address.isNotBlank()
        ) {
            Text(text = stringResource(R.string.save_profile))
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (uiState.employerRequests.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(com.manekelsa.R.string.status_requested) + " / " + stringResource(com.manekelsa.R.string.status_hired),
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
                                        text = com.manekelsa.utils.TranslationUtils.getTranslatedName(req.workerName),
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
            
            val currentLanguage = com.manekelsa.utils.LocalizationManager.getLanguage()
            
            androidx.compose.foundation.layout.Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                LanguageRadio(stringResource(R.string.lang_english), "en", currentLanguage) { com.manekelsa.utils.LocalizationManager.setLanguage(it) }
                LanguageRadio(stringResource(R.string.lang_kannada), "kn", currentLanguage) { com.manekelsa.utils.LocalizationManager.setLanguage(it) }
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}
