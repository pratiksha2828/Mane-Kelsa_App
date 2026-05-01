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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.manekelsa.R
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
    }
}
