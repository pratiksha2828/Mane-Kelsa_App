package com.manekelsa.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.PhoneCallback
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.manekelsa.R
import com.manekelsa.ui.components.WrapRow
import com.manekelsa.ui.components.DeleteAccountDialog
import com.manekelsa.ui.model.SkillOption

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    displayName: String = "",
    phoneNumber: String = "",
    onDeleteAccount: () -> Unit = {},
    viewModel: WorkerProfileViewModel = hiltViewModel()
) {
    LaunchedEffect(displayName, phoneNumber) {
        viewModel.setInitialContactInfo(displayName, phoneNumber)
    }
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()
    val accountDeletionViewModel: AccountDeletionViewModel = hiltViewModel()

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.updateUiState { it.copy(localPhotoUri = uri) }
        }
    }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 8.dp,
                shadowElevation = 8.dp
            ) {
                Button(
                    onClick = { 
                        if (uiState.isEditMode) {
                            viewModel.saveProfile(context)
                        } else {
                            viewModel.toggleEditMode()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (uiState.isEditMode) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !uiState.isLoading
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text(
                            text = if (uiState.isEditMode) stringResource(R.string.save_profile) else stringResource(R.string.edit_profile), 
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // SECTION 1: PROFILE INFO
            ProfileInfoSection(
                uiState = uiState, 
                onPhotoClick = { 
                    if (uiState.isEditMode) {
                        photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                }, 
                viewModel = viewModel
            )

            // SECTION 2: WORK DETAILS
            WorkDetailsSection(uiState, viewModel)

            // SECTION 3: SETTINGS
            SettingsSection(uiState, viewModel, accountDeletionViewModel, onDeleteAccount)

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileInfoSection(uiState: WorkerProfileUiState, onPhotoClick: () -> Unit, viewModel: WorkerProfileViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(stringResource(R.string.profile_information), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
        
        Box(modifier = Modifier.align(Alignment.CenterHorizontally).clickable(enabled = uiState.isEditMode) { onPhotoClick() }) {
            AsyncImage(
                model = uiState.localPhotoUri ?: uiState.profilePhotoUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = ContentScale.Crop
            )
            Surface(
                modifier = Modifier.align(Alignment.BottomEnd).size(28.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                tonalElevation = 4.dp
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.padding(6.dp), tint = Color.White)
            }
        }

        OutlinedTextField(
            value = uiState.fullName,
            onValueChange = { name -> viewModel.updateUiState { it.copy(fullName = name) } },
            label = { Text(stringResource(R.string.label_name)) },
            modifier = Modifier.fillMaxWidth(),
            readOnly = !uiState.isEditMode,
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        val phoneEditable = uiState.isEditMode || uiState.phoneNumber.isBlank()
        OutlinedTextField(
            value = uiState.phoneNumber,
            onValueChange = { phone ->
                val normalized = phone.filter { it.isDigit() }.take(10)
                viewModel.updateUiState { it.copy(phoneNumber = normalized) }
            },
            label = {
                Text(
                    stringResource(
                        if (phoneEditable) R.string.phone_number else R.string.label_phone_readonly
                    )
                )
            },
            modifier = Modifier.fillMaxWidth(),
            readOnly = !phoneEditable,
            enabled = phoneEditable || uiState.isEditMode,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            prefix = { Text("+91") },
            shape = RoundedCornerShape(12.dp),
            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) }
        )

        OutlinedTextField(
            value = uiState.areaStreet,
            onValueChange = { area -> viewModel.updateUiState { it.copy(areaStreet = area) } },
            label = { Text(stringResource(R.string.label_area_street)) },
            modifier = Modifier.fillMaxWidth(),
            readOnly = !uiState.isEditMode,
            shape = RoundedCornerShape(12.dp),
            leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) }
        )

        // Experience Dropdown
        var expanded by remember { mutableStateOf(false) }
        val experienceOptions = listOf("0-1", "1-3", "3-5", "5+")
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { if (uiState.isEditMode) expanded = !expanded }
        ) {
            OutlinedTextField(
                value = stringResource(R.string.years_suffix, uiState.experienceYears),
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.label_experience)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                experienceOptions.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.years_suffix, option)) },
                        onClick = {
                            viewModel.updateUiState { it.copy(experienceYears = option) }
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WorkDetailsSection(uiState: WorkerProfileUiState, viewModel: WorkerProfileViewModel) {
    val skillsOptions = SkillOption.all
    val customSkills = uiState.selectedSkills.filterNot { skillsOptions.contains(it) }
    val allSkillOptions = (skillsOptions + customSkills).distinct()
    var customSkill by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(stringResource(R.string.work_details), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))

        Text(stringResource(R.string.skills_label), style = MaterialTheme.typography.bodyMedium)
        WrapRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalSpacing = 8.dp,
            verticalSpacing = 4.dp
        ) {
            allSkillOptions.forEach { skillId ->
                val isSelected = uiState.selectedSkills.contains(skillId)
                val labelRes = SkillOption.labelRes(skillId)
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        val newSkills = if (isSelected) uiState.selectedSkills - skillId else uiState.selectedSkills + skillId
                        viewModel.updateUiState { it.copy(selectedSkills = newSkills) }
                    },
                    label = { Text(labelRes?.let { stringResource(it) } ?: skillId) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFFF9933),
                        selectedLabelColor = Color.White
                    ),
                    enabled = uiState.isEditMode
                )
            }
        }

        if (uiState.isEditMode) {
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

        OutlinedTextField(
            value = uiState.dailyWage,
            onValueChange = { wage -> viewModel.updateUiState { it.copy(dailyWage = wage) } },
            label = { Text(stringResource(R.string.rate_per_day)) },
            prefix = { Text("₹") },
            modifier = Modifier.fillMaxWidth(),
            readOnly = !uiState.isEditMode,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(12.dp)
        )
    }
}

@Composable
fun SettingsSection(
    uiState: WorkerProfileUiState,
    viewModel: WorkerProfileViewModel,
    accountDeletionViewModel: AccountDeletionViewModel,
    onDeleteAccount: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(stringResource(R.string.settings), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
        
        Text(stringResource(R.string.language_switcher), style = MaterialTheme.typography.titleSmall)
        val context = LocalContext.current
        val activity = context as? ComponentActivity
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            LanguageRadio(stringResource(R.string.lang_english), "en", uiState.currentLanguage) {
                viewModel.setLanguage(it)
                activity?.recreate()
            }
            LanguageRadio(stringResource(R.string.lang_kannada), "kn", uiState.currentLanguage) {
                viewModel.setLanguage(it)
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
            Icon(Icons.Default.DeleteForever, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Delete Account")
        }
    }
}

@Composable
fun LanguageRadio(label: String, code: String, current: String, onSelect: (String) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { onSelect(code) }) {
        RadioButton(selected = code == current, onClick = { onSelect(code) })
        Text(label)
    }
}
