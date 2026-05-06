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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
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
import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import androidx.compose.material3.Surface
import androidx.compose.ui.unit.sp
import android.widget.Toast
import com.manekelsa.R
import com.manekelsa.ui.components.MeshGradientBackground
import com.manekelsa.ui.components.glassmorphism
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
    var isGoogleSignedIn by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val activity = context as? Activity
    val auth = remember { FirebaseAuth.getInstance() }
    val webClientId = remember { getWebClientId(context) }

    androidx.compose.runtime.LaunchedEffect(auth.currentUser?.uid) {
        val user = auth.currentUser
        if (user != null) {
            val isGoogle = user.providerData.any { it.providerId == "google.com" }
            if (isGoogle) {
                isGoogleSignedIn = true
                if (fullName.isBlank()) {
                    fullName = user.displayName.orEmpty()
                }
                val authPhone = user.phoneNumber.orEmpty()
                if (phoneNumber.isBlank() && authPhone.isNotBlank()) {
                    phoneNumber = authPhone.filter { it.isDigit() }.takeLast(10)
                }
                if (fullName.isNotBlank()) {
                    detailsSubmitted = true
                }
            }
        }
    }

    val googleSignInClient = remember(webClientId) {
        val builder = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
        if (!webClientId.isNullOrBlank()) {
            builder.requestIdToken(webClientId)
        }
        GoogleSignIn.getClient(context, builder.build())
    }

    val googleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        val data = result.data
        if (data == null) {
            Toast.makeText(
                context,
                context.getString(R.string.google_sign_in_failed),
                Toast.LENGTH_SHORT
            ).show()
            return@rememberLauncherForActivityResult
        }
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        try {
            val account = task.getResult(ApiException::class.java)
            if (account == null || account.idToken.isNullOrBlank()) {
                Toast.makeText(
                    context,
                    context.getString(R.string.google_sign_in_not_configured),
                    Toast.LENGTH_SHORT
                ).show()
                return@rememberLauncherForActivityResult
            }
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            auth.signInWithCredential(credential).addOnCompleteListener { signInTask ->
                if (signInTask.isSuccessful) {
                    fullName = account.displayName.orEmpty()
                    val authPhone = auth.currentUser?.phoneNumber.orEmpty()
                    if (phoneNumber.isBlank() && authPhone.isNotBlank()) {
                        phoneNumber = authPhone.filter { it.isDigit() }.takeLast(10)
                    }
                    isGoogleSignedIn = true
                    if (fullName.isNotBlank()) {
                        detailsSubmitted = true
                    } else {
                        Toast.makeText(
                            context,
                            "Please enter your name",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
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

    MeshGradientBackground {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 500.dp)
                    .fillMaxWidth()
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

            androidx.compose.animation.Crossfade(
                targetState = detailsSubmitted,
                label = "role_selection_step"
            ) { isSubmitted ->
                if (!isSubmitted) {
                    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
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
                            isError = fullName.isNotBlank() && !isGoogleSignedIn && !isFullNameValid(fullName),
                            supportingText = {
                                if (fullName.isNotBlank() && !isGoogleSignedIn && !isFullNameValid(fullName)) {
                                    Text(text = stringResource(R.string.enter_first_last_name_error))
                                }
                            },
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = phoneNumber,
                            onValueChange = { input ->
                                val digits = input.filter { it.isDigit() }
                                val normalized = if (digits.startsWith("91") && digits.length > 10) {
                                    digits.drop(2)
                                } else {
                                    digits
                                }
                                phoneNumber = normalized.take(10)
                            },
                            label = { Text(stringResource(R.string.phone_number)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            prefix = { Text(text = "+91 ") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            shape = RoundedCornerShape(12.dp)
                        )

                        val phoneDigits = phoneNumber.filter { it.isDigit() }
                        val isNameValid = if (isGoogleSignedIn) fullName.isNotBlank() else isFullNameValid(fullName)
                        val hasIdentity = isNameValid && (phoneDigits.length == 10 || isGoogleSignedIn)
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
                            onClick = {
                                if (webClientId.isNullOrBlank()) {
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.google_sign_in_not_configured),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    googleLauncher.launch(googleSignInClient.signInIntent)
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = stringResource(R.string.continue_with_google), fontSize = 14.sp)
                        }
                    }
                } else {
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
                            icon = Icons.Default.Search,
                            isSelected = selectedRole == UserRole.HIRER,
                            accentColor = Color(0xFFE8F1FF),
                            onClick = { selectedRole = UserRole.HIRER }
                        )

                        RoleOptionCard(
                            title = stringResource(R.string.role_work_title),
                            description = stringResource(R.string.role_work_desc),
                            icon = Icons.Default.Handyman,
                            isSelected = selectedRole == UserRole.WORKER,
                            accentColor = Color(0xFFE6F4F0),
                            onClick = { selectedRole = UserRole.WORKER }
                        )

                        val phoneDigits = phoneNumber.filter { it.isDigit() }
                        if (!isGoogleSignedIn && phoneDigits.length < 10) {
                            OutlinedTextField(
                                value = phoneNumber,
                                onValueChange = { input ->
                                    val digits = input.filter { it.isDigit() }
                                    val normalized = if (digits.startsWith("91") && digits.length > 10) {
                                        digits.drop(2)
                                    } else {
                                        digits
                                    }
                                    phoneNumber = normalized.take(10)
                                } ,
                                label = { Text(stringResource(R.string.phone_number)) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                prefix = { Text(text = "+91 ") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                        val isNameValid = if (isGoogleSignedIn) fullName.isNotBlank() else isFullNameValid(fullName)
                        val hasIdentity = isNameValid && (phoneDigits.length == 10 || isGoogleSignedIn)
                        val canContinue = selectedRole != null && hasIdentity
                        Button(
                            onClick = {
                                val resolvedPhone = if (phoneDigits.length == 10) {
                                    "+91$phoneDigits"
                                } else {
                                    auth.currentUser?.phoneNumber.orEmpty()
                                }
                                selectedRole?.let { onContinue(it, fullName.trim(), resolvedPhone) }
                            },
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
        }
    }
}

private fun isFullNameValid(name: String): Boolean {
    val parts = name.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    return parts.size >= 2
}

private fun getWebClientId(context: Context): String? {
    val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
    if (resId == 0) return null
    return context.getString(resId)
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

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .glassmorphism(
                cornerRadius = 20.dp,
                fillAlpha = if (isSelected) 0.3f else 0.1f,
                borderAlpha = if (isSelected) 0.5f else 0.2f
            ),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
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
