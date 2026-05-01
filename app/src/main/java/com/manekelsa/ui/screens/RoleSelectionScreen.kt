package com.manekelsa.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.HomeRepairService
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.app.Activity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import androidx.compose.material3.Surface
import androidx.compose.ui.unit.sp
import android.widget.Toast
import com.manekelsa.R
import com.manekelsa.ui.model.UserRole
import com.manekelsa.utils.LocalizationManager

@Composable
fun RoleSelectionScreen(
    onContinue: (UserRole, String, String) -> Unit
) {
    var selectedRole by rememberSaveable { mutableStateOf<UserRole?>(null) }
    var fullName by rememberSaveable { mutableStateOf("") }
    var phoneNumber by rememberSaveable { mutableStateOf("") }
    var languageCode by rememberSaveable { mutableStateOf(LocalizationManager.getLanguage()) }
    var detailsSubmitted by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val activity = context as? Activity
    val auth = remember { FirebaseAuth.getInstance() }

    val googleSignInClient = remember {
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        GoogleSignIn.getClient(context, options)
    }

    val googleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            auth.signInWithCredential(credential).addOnCompleteListener { signInTask ->
                if (signInTask.isSuccessful) {
                    fullName = account.displayName.orEmpty()
                    phoneNumber = phoneNumber.ifBlank { "" }
                    detailsSubmitted = true
                } else {
                    Toast.makeText(
                        context,
                        context.getString(R.string.google_sign_in_failed),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        } catch (e: ApiException) {
            Toast.makeText(
                context,
                context.getString(R.string.google_sign_in_failed),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "M",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
            LanguageToggle(
                languageCode = languageCode,
                onLanguageChange = { newCode ->
                    if (newCode != languageCode) {
                        LocalizationManager.setLanguage(newCode)
                        languageCode = newCode
                        activity?.recreate()
                    }
                }
            )
        }

        Text(
            text = stringResource(R.string.enter_details_title),
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
        )
        Text(
            text = stringResource(R.string.enter_details_desc),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
            value = fullName,
            onValueChange = { fullName = it },
            label = { Text(stringResource(R.string.full_name)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        OutlinedTextField(
            value = phoneNumber,
            onValueChange = { phoneNumber = it },
            label = { Text(stringResource(R.string.phone_number)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            shape = RoundedCornerShape(12.dp)
        )

        val phoneDigits = phoneNumber.filter { it.isDigit() }
        val hasIdentity = fullName.isNotBlank() && phoneDigits.length >= 10
        Button(
            onClick = { detailsSubmitted = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(14.dp),
            enabled = hasIdentity
        ) {
            Text(text = stringResource(R.string.next))
        }

        Text(
            text = stringResource(R.string.or_only),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        TextButton(
            onClick = { googleLauncher.launch(googleSignInClient.signInIntent) },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text(text = stringResource(R.string.continue_with_google), fontSize = 14.sp)
        }

        if (detailsSubmitted) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = stringResource(R.string.role_title),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = stringResource(R.string.role_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                RoleOptionCard(
                    title = stringResource(R.string.role_hire_title),
                    description = stringResource(R.string.role_hire_desc),
                    icon = Icons.Default.HomeRepairService,
                    isSelected = selectedRole == UserRole.HIRER,
                    accentColor = Color(0xFFFFF3E0),
                    onClick = { selectedRole = UserRole.HIRER }
                )

                RoleOptionCard(
                    title = stringResource(R.string.role_work_title),
                    description = stringResource(R.string.role_work_desc),
                    icon = Icons.Default.Handyman,
                    isSelected = selectedRole == UserRole.WORKER,
                    accentColor = Color(0xFFE8F5E9),
                    onClick = { selectedRole = UserRole.WORKER }
                )

                val canContinue = selectedRole != null && hasIdentity
                Button(
                    onClick = { selectedRole?.let { onContinue(it, fullName.trim(), phoneDigits) } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(14.dp),
                    enabled = canContinue
                ) {
                    Text(text = stringResource(R.string.role_continue))
                }
            }
        }
    }
}

@Composable
private fun LanguageToggle(
    languageCode: String,
    onLanguageChange: (String) -> Unit
) {
    Row(
        modifier = Modifier.widthIn(min = 140.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val isEnglish = languageCode.startsWith("en")
        OutlinedButton(
            onClick = { onLanguageChange("en") },
            enabled = !isEnglish,
            modifier = Modifier.height(36.dp).weight(1f),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(
                text = stringResource(R.string.lang_english),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 12.sp
            )
        }
        OutlinedButton(
            onClick = { onLanguageChange("kn") },
            enabled = isEnglish,
            modifier = Modifier.height(36.dp).weight(1f),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(
                text = stringResource(R.string.lang_kannada),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun RoleOptionCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
    val containerColor = if (isSelected) accentColor else MaterialTheme.colorScheme.surface

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(20.dp),
        border = if (isSelected) BorderStroke(2.dp, borderColor) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(accentColor, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
