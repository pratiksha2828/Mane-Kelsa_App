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
import androidx.compose.runtime.setValue
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
import com.manekelsa.ui.components.glassmorphism
import com.manekelsa.ui.model.UserRole
import android.text.format.DateUtils
import com.manekelsa.utils.TranslationUtils

@Composable
fun HomeScreen(
    onNavigateToProfile: () -> Unit = {},
    onNavigateToCallHistory: () -> Unit = {},
    onNavigateToSearch: (String?) -> Unit = {},
    userRole: UserRole = UserRole.HIRER,
    onChangeRole: () -> Unit = {},
    displayName: String = "",
    phoneNumber: String = "",
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val searchViewModel: SearchViewModel = hiltViewModel()
    val searchUiState by searchViewModel.uiState.collectAsState()
    val likedWorkers by searchViewModel.likedWorkers.collectAsState()
    val ratedWorkers by searchViewModel.ratedWorkers.collectAsState()
    var selectedWorker by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<WorkerEntity?>(null) }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        val resolvedName = when {
            displayName.isNotBlank() -> displayName
            !uiState.userName.isNullOrBlank() -> uiState.userName ?: ""
            else -> stringResource(R.string.user_placeholder)
        }
        HomeHeader(
            name = resolvedName,
            onProfileClick = onNavigateToProfile
        )

        if (userRole == UserRole.HIRER) {
            HeroHireCard(
                onSearchClick = { onNavigateToSearch(null) },
                onExploreClick = { onNavigateToSearch(null) },
                onCategoryClick = { categoryId -> onNavigateToSearch(categoryId) }
            )
        } else {
            WorkerStatsOverview(
                totalJobs = uiState.totalJobs,
                averageRating = uiState.averageRating,
                timesContacted = uiState.timesContacted
            )
        }

        RoleModeCard(userRole = userRole, onChangeRole = onChangeRole)

        if (userRole == UserRole.HIRER || userRole == UserRole.WORKER) {
            StatsRow(count = uiState.availableWorkersCount)
        } else if (!uiState.isProfileComplete) {
            CompleteProfileBanner(onClick = onNavigateToProfile)
        }

        if (userRole == UserRole.HIRER && uiState.featuredWorkers.isNotEmpty()) {
            FeaturedWorkersSection(workers = uiState.featuredWorkers, onWorkerClick = { selectedWorker = it })
        }

        if (userRole == UserRole.WORKER) {
            AvailabilitySection()
        }

        RecentCallsSection(
            recentCalls = uiState.recentCalls,
            onViewAll = onNavigateToCallHistory,
            onWorkerClick = { workerId ->
                selectedWorker = searchUiState.workers.find { it.id == workerId } ?: uiState.featuredWorkers.find { it.id == workerId }
            }
        )
        
        Spacer(modifier = Modifier.height(12.dp))

        selectedWorker?.let { worker ->
            WorkerProfileBottomSheet(
                worker = worker,
                hireStatus = searchUiState.hireRequests[worker.id],
                isThumbed = likedWorkers.contains(worker.id),
                hasStarRated = ratedWorkers.contains(worker.id),
                onDismiss = { selectedWorker = null },
                onCall = { searchViewModel.onCallWorker(worker) },
                onThumbsUp = { searchViewModel.onThumbsUp(worker.id) },
                onStarRate = { rating -> searchViewModel.onStarRating(worker.id, rating) },
                onRequestHire = { searchViewModel.onRequestHire(worker.id) }
            )
        }
    }
}

@Composable
private fun RoleModeCard(userRole: UserRole, onChangeRole: () -> Unit) {
    val title: String
    val subtitle: String

    when (userRole) {
        UserRole.HIRER -> {
            title = stringResource(R.string.role_mode_hire_title)
            subtitle = stringResource(R.string.role_mode_hire_desc)
        }
        UserRole.WORKER -> {
            title = stringResource(R.string.role_mode_work_title)
            subtitle = stringResource(R.string.role_mode_work_desc)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .glassmorphism(cornerRadius = 24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        androidx.compose.foundation.layout.Box {
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
}

@Composable
private fun HomeHeader(name: String, onProfileClick: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? android.app.Activity
    var languageCode by androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf(com.manekelsa.utils.LocalizationManager.getLanguage())
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = stringResource(R.string.home_services_label),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.2.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.home_welcome_name, name),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .glassmorphism(cornerRadius = 14.dp)
                    .clickable { onProfileClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val isEnglish = languageCode.startsWith("en")
            LanguageChip(
                label = stringResource(R.string.lang_english),
                selected = isEnglish,
                onClick = {
                    com.manekelsa.utils.LocalizationManager.setLanguage("en")
                    languageCode = "en"
                    activity?.recreate()
                }
            )
            LanguageChip(
                label = stringResource(R.string.lang_kannada),
                selected = !isEnglish,
                onClick = {
                    com.manekelsa.utils.LocalizationManager.setLanguage("kn")
                    languageCode = "kn"
                    activity?.recreate()
                }
            )
        }
    }
}

@Composable
private fun LanguageChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val backgroundAlpha = if (selected) 0.35f else 0.18f
    Box(
        modifier = Modifier
            .height(32.dp)
            .glassmorphism(cornerRadius = 16.dp, fillAlpha = backgroundAlpha, borderAlpha = 0.3f)
            .clickable { onClick() }
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun WorkerStatsOverview(totalJobs: Int, averageRating: Float, timesContacted: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.stats), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(stringResource(R.string.stat_jobs), totalJobs.toString(), Icons.Default.DoneAll, Modifier.weight(1f))
            StatCard(stringResource(R.string.stat_rating), "%.1f".format(averageRating), Icons.Default.Star, Modifier.weight(1f))
            StatCard(stringResource(R.string.stat_calls), timesContacted.toString(), Icons.Default.Phone, Modifier.weight(1f))
        }
    }
}

@Composable
fun StatCard(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Text(value, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}

private data class HomeCategory(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val skillId: String)

@Composable
private fun HeroHireCard(
    onSearchClick: () -> Unit,
    onExploreClick: () -> Unit,
    onCategoryClick: (String) -> Unit
) {
    val categories = listOf(
        HomeCategory(stringResource(R.string.home_category_plumbing), Icons.Default.Build, com.manekelsa.ui.model.SkillOption.PLUMBER),
        HomeCategory(stringResource(R.string.home_category_electrical), Icons.Default.FlashOn, com.manekelsa.ui.model.SkillOption.ELECTRICIAN),
        HomeCategory(stringResource(R.string.home_category_cleaning), Icons.Default.Home, com.manekelsa.ui.model.SkillOption.CLEANER),
        HomeCategory(stringResource(R.string.home_category_ac_service), Icons.Default.Settings, com.manekelsa.ui.model.SkillOption.PAINTER)
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .glassmorphism(cornerRadius = 28.dp, fillAlpha = 0.32f, borderAlpha = 0.6f),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .align(Alignment.CenterHorizontally)
                    .glassmorphism(cornerRadius = 16.dp, fillAlpha = 0.34f, borderAlpha = 0.6f),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Build,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = stringResource(R.string.home_hire_worker_title),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                items(categories) { category ->
                    CategoryPill(category = category, onClick = { onCategoryClick(category.skillId) })
                }
            }
            HomeSearchBar(
                placeholder = stringResource(R.string.home_find_professional),
                onClick = onSearchClick
            )
            Button(
                onClick = onExploreClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White.copy(alpha = 0.8f),
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                Text(
                    text = stringResource(R.string.home_explore_services),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

@Composable
private fun CategoryPill(category: HomeCategory, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(78.dp)
            .glassmorphism(cornerRadius = 18.dp, fillAlpha = 0.3f, borderAlpha = 0.55f)
            .clickable { onClick() }
            .padding(vertical = 10.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .glassmorphism(cornerRadius = 10.dp, fillAlpha = 0.38f, borderAlpha = 0.5f),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = category.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
        }
        Text(
            text = category.label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun HomeSearchBar(placeholder: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .glassmorphism(cornerRadius = 18.dp, fillAlpha = 0.3f, borderAlpha = 0.55f)
            .clickable { onClick() }
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = placeholder,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StatsRow(count: Int) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .glassmorphism(cornerRadius = 18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.9f), CircleShape),
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
                    color = MaterialTheme.colorScheme.primary
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
            .clickable { onClick() }
            .glassmorphism(cornerRadius = 18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
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
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.complete_profile_desc),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.tap_to_proceed),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun FeaturedWorkersSection(workers: List<WorkerEntity>, onWorkerClick: (WorkerEntity) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(
            text = stringResource(R.string.featured_workers),
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(end = 16.dp)
        ) {
            items(workers) { worker ->
                WorkerPreviewCard(worker, onClick = { onWorkerClick(worker) })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkerPreviewCard(worker: WorkerEntity, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .width(190.dp)
            .glassmorphism(cornerRadius = 22.dp, fillAlpha = 0.3f, borderAlpha = 0.6f),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            AsyncImage(
                model = worker.photoUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = TranslationUtils.getTranslatedName(worker.name),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                maxLines = 1
            )
            Text(
                text = TranslationUtils.getTranslatedSkill(worker.skillsList.firstOrNull().orEmpty()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFFF4C430),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = " ${worker.averageRating}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
                Text(
                    text = "₹${worker.dailyWage.toInt()}${stringResource(R.string.per_day)}",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
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
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
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
    onViewAll: () -> Unit,
    onWorkerClick: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.recent_calls_title),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
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
                        onClick = { onWorkerClick(call.workerId) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassmorphism(cornerRadius = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .glassmorphism(cornerRadius = 10.dp, fillAlpha = 0.3f, borderAlpha = 0.3f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Phone,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = TranslationUtils.getTranslatedName(call.workerName),
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



