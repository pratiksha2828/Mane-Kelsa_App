package com.manekelsa.ui.screens

import android.Manifest
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.manekelsa.R
import com.manekelsa.ui.components.LargeButton
import com.manekelsa.ui.components.WrapRow
import com.manekelsa.ui.model.SkillOption

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkerProfileFormScreen(
    viewModel: WorkerProfileViewModel,
    onPreviewClick: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.updateUiState { it.copy(localPhotoUri = uri) }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            photoPickerLauncher.launch("image/*")
        } else {
            Toast.makeText(context, context.getString(R.string.permission_denied), Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { 
                    Text(
                        if (uiState.isEditMode) stringResource(R.string.edit_profile) 
                        else stringResource(R.string.worker_profile), 
                        fontWeight = FontWeight.Bold 
                    ) 
                }
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading && uiState.fullName.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .clip(CircleShape)
                        .clickable {
                            val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                Manifest.permission.READ_MEDIA_IMAGES
                            } else {
                                Manifest.permission.READ_EXTERNAL_STORAGE
                            }
                            permissionLauncher.launch(permission)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    val imageSource = uiState.localPhotoUri ?: uiState.profilePhotoUrl
                    if (imageSource != null) {
                        AsyncImage(
                            model = imageSource,
                            contentDescription = stringResource(R.string.photo),
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddAPhoto,
                                    contentDescription = stringResource(R.string.add_photo),
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(stringResource(R.string.add_photo), style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }

                Text(
                    stringResource(R.string.personal_details),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = uiState.fullName,
                    onValueChange = { name -> viewModel.updateUiState { it.copy(fullName = name) } },
                    label = { Text(stringResource(R.string.full_name)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium
                )

                val skillsOptions = SkillOption.all
                val customSkills = uiState.selectedSkills.filterNot { skillsOptions.contains(it) }
                val allSkillOptions = (skillsOptions + customSkills).distinct()

                var customSkill by remember { mutableStateOf("") }

                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        stringResource(R.string.skills),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    WrapRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalSpacing = 8.dp,
                        verticalSpacing = 4.dp
                    ) {
                        allSkillOptions.forEach { skillId ->
                            val labelRes = SkillOption.labelRes(skillId)
                            FilterChip(
                                selected = uiState.selectedSkills.contains(skillId),
                                onClick = {
                                    val newSkills = if (uiState.selectedSkills.contains(skillId)) {
                                        uiState.selectedSkills - skillId
                                    } else {
                                        uiState.selectedSkills + skillId
                                    }
                                    viewModel.updateUiState { it.copy(selectedSkills = newSkills) }
                                },
                                label = { Text(labelRes?.let { stringResource(it) } ?: skillId) },
                                modifier = Modifier.height(40.dp)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = customSkill,
                        onValueChange = { customSkill = it },
                        label = { Text(stringResource(R.string.add_skill_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    TextButton(
                        onClick = {
                            val trimmed = customSkill.trim()
                            if (trimmed.isNotEmpty()) {
                                val normalized = SkillOption.normalize(trimmed)
                                if (!uiState.selectedSkills.contains(normalized)) {
                                    viewModel.updateUiState { it.copy(selectedSkills = it.selectedSkills + normalized) }
                                }
                                customSkill = ""
                            }
                        }
                    ) {
                        Text(text = stringResource(R.string.add_skill_button))
                    }
                }

                val phoneEditable = uiState.isEditMode || uiState.phoneNumber.isBlank()
                OutlinedTextField(
                    value = uiState.phoneNumber,
                    onValueChange = { phone ->
                        val normalized = phone.filter { it.isDigit() }.takeLast(10)
                        viewModel.updateUiState { it.copy(phoneNumber = normalized) }
                    },
                    label = { Text(stringResource(R.string.phone_number)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    prefix = { Text("+91") },
                    readOnly = !phoneEditable
                )

                OutlinedTextField(
                    value = uiState.dailyWage,
                    onValueChange = { wage -> viewModel.updateUiState { it.copy(dailyWage = wage) } },
                    label = { Text(stringResource(R.string.rate_per_day)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium
                )

                OutlinedTextField(
                    value = uiState.areaStreet,
                    onValueChange = { area -> viewModel.updateUiState { it.copy(areaStreet = area) } },
                    label = { Text(stringResource(R.string.area_street)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium
                )

                OutlinedTextField(
                    value = uiState.experienceYears,
                    onValueChange = { exp -> viewModel.updateUiState { it.copy(experienceYears = exp) } },
                    label = { Text(stringResource(R.string.experience_years)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium
                )

                Spacer(modifier = Modifier.height(24.dp))

                LargeButton(
                    text = if (uiState.isEditMode) stringResource(R.string.update_profile) 
                           else stringResource(R.string.review_profile),
                    onClick = {
                        if (uiState.fullName.isBlank()) {
                            Toast.makeText(context, context.getString(R.string.enter_name_error), Toast.LENGTH_SHORT).show()
                            return@LargeButton
                        }
                        if (uiState.phoneNumber.length < 10) {
                            Toast.makeText(context, context.getString(R.string.enter_phone_error), Toast.LENGTH_SHORT).show()
                            return@LargeButton
                        }
                        onPreviewClick()
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
