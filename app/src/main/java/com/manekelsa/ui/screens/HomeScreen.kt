package com.manekelsa.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.manekelsa.R
import com.manekelsa.data.local.entity.CallLogEntity
import com.manekelsa.data.local.entity.WorkerEntity
import com.manekelsa.ui.components.AvailabilityToggleCard
import com.manekelsa.ui.model.UserRole
import android.text.format.DateUtils

@Composable
fun HomeScreen(
    onNavigateToProfile: () -> Unit = {},
    onNavigateToCallHistory: () -> Unit = {},
    userRole: UserRole = UserRole.HIRER,
    onChangeRole: () -> Unit = {},
    displayName: String = "",
    phoneNumber: String = "",
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // 1. Welcome Card
        val resolvedName = when {
            displayName.isNotBlank() -> displayName
            !uiState.userName.isNullOrBlank() -> uiState.userName ?: ""
            else -> stringResource(R.string.user_placeholder)
        }
        WelcomeCard(name = resolvedName)

        RoleModeCard(userRole = userRole, onChangeRole = onChangeRole)

        // 2. Stats or Profile Banner
        if (uiState.isProfileComplete) {
            StatsRow(count = uiState.availableWorkersCount)
        } else {
            CompleteProfileBanner(onClick = onNavigateToProfile)
        }

        // 3. Featured Workers Section
        if (uiState.featuredWorkers.isNotEmpty()) {
            FeaturedWorkersSection(workers = uiState.featuredWorkers)
        }

        // 4. Availability toggle for workers
        if (userRole == UserRole.WORKER) {
            AvailabilitySection()
        }

        // 5. Recent calls
        RecentCallsSection(
            recentCalls = uiState.recentCalls,
            onViewAll = onNavigateToCallHistory
        )
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun RoleModeCard(userRole: UserRole, onChangeRole: () -> Unit) {
    val (title, subtitle, color) = when (userRole) {
        UserRole.HIRER -> Triple(
            stringResource(R.string.role_mode_hire_title),
            stringResource(R.string.role_mode_hire_desc),
            Color(0xFFFFF3E0)
        )
        UserRole.WORKER -> Triple(
            stringResource(R.string.role_mode_work_title),
            stringResource(R.string.role_mode_work_desc),
            Color(0xFFE8F5E9)
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = color),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = onChangeRole) {
                Text(text = stringResource(R.string.change_role))
            }
        }
    }
}

@Composable
private fun WelcomeCard(name: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier.padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1.0f)) {
                Text(
                    text = stringResource(R.string.welcome),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
                Text(
                    text = name,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 32.sp
                    ),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Icon(
                imageVector = Icons.Default.WavingHand,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun StatsRow(count: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFF2E7D32), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Groups, contentDescription = null, tint = Color.White)
            }
            Text(
                text = stringResource(R.string.available_workers_today),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF1B5E20)
                )
            )
        }
    }
}

@Composable
private fun CompleteProfileBanner(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.Default.Error, 
                contentDescription = null, 
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(32.dp)
            )
            Column {
                Text(
                    text = stringResource(R.string.complete_profile_title),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    text = stringResource(R.string.complete_profile_desc),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

@Composable
private fun FeaturedWorkersSection(workers: List<WorkerEntity>) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = stringResource(R.string.featured_workers),
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(end = 16.dp)
        ) {
            items(workers) { worker ->
                WorkerCard(worker)
            }
        }
    }
}

@Composable
private fun WorkerCard(worker: WorkerEntity) {
    Card(
        modifier = Modifier.width(280.dp),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = worker.photoUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(70.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = worker.name,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1
                    )
                    Text(
                        text = worker.area,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                worker.skillsList.take(2).forEach { skill ->
                    SuggestionChip(
                        onClick = { },
                        label = { Text(skill, fontWeight = FontWeight.Medium) },
                        modifier = Modifier.height(32.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "₹${worker.dailyWage.toInt()}${stringResource(R.string.per_day)}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.primary
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFBC02D), modifier = Modifier.size(20.dp))
                    Text(
                        text = " ${worker.averageRating}", 
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

@Composable
private fun AvailabilitySection(viewModel: AvailabilityViewModel = hiltViewModel()) {
    val isAvailable by viewModel.isAvailable.collectAsState()
    val context = LocalContext.current

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(R.string.availability_title),
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
        )
        AvailabilityToggleCard(
            isAvailable = isAvailable,
            onAvailableChange = { available -> viewModel.updateAvailability(context, available) }
        )
        Text(
            text = stringResource(R.string.availability_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun RecentCallsSection(
    recentCalls: List<CallLogEntity>,
    onViewAll: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.recent_calls_title),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            )
            TextButton(onClick = onViewAll) {
                Text(text = stringResource(R.string.view_all))
            }
        }

        if (recentCalls.isEmpty()) {
            Text(
                text = stringResource(R.string.no_recent_calls),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                recentCalls.take(3).forEach { call ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = call.workerName,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = DateUtils.getRelativeTimeSpanString(
                                            call.timestamp,
                                            System.currentTimeMillis(),
                                            DateUtils.MINUTE_IN_MILLIS
                                        ).toString(),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
