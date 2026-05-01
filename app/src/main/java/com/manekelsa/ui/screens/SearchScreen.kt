package com.manekelsa.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import android.content.Context
import com.manekelsa.ui.model.SkillOption
import coil.compose.AsyncImage
import com.manekelsa.R
import com.manekelsa.data.local.entity.WorkerEntity

@Composable
fun SearchScreen(
    viewModel: SearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            Surface(shadowElevation = 4.dp) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(top = 8.dp)
                ) {
                    // 1. Search Bar
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = { viewModel.onSearchQueryChange(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        placeholder = { Text(stringResource(R.string.search_placeholder)) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (uiState.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                                    Icon(Icons.Default.Close, contentDescription = null)
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFFF9933),
                            unfocusedBorderColor = Color.LightGray
                        )
                    )

                    // 2. Filter Chips Row
                    CategoryChipsRow(
                        selectedCategory = uiState.selectedCategory,
                        onCategorySelected = { viewModel.onCategorySelect(it) }
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.error != null -> {
                    ErrorState(message = uiState.error!!, onRetry = { viewModel.retry() })
                }
                uiState.isLoading -> {
                    LoadingState()
                }
                uiState.workers.isEmpty() -> {
                    EmptyState()
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            MapPreviewCard(context = context)
                        }
                        items(uiState.workers, key = { it.id }) { worker ->
                            WorkerResultCard(
                                worker = worker,
                                onCall = { viewModel.onCallWorker(worker) },
                                onRate = { viewModel.onRateWorker(worker.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MapPreviewCard(context: Context) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = {
                Configuration.getInstance().userAgentValue = context.packageName
                MapView(it).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    controller.setZoom(13.0)
                    val center = GeoPoint(12.9716, 77.5946)
                    controller.setCenter(center)
                    overlays.add(Marker(this).apply {
                        position = center
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        title = context.getString(R.string.map_default_pin)
                    })
                }
            }
        )
    }
}

@Composable
fun CategoryChipsRow(
    selectedCategory: String,
    onCategorySelected: (String) -> Unit
) {
    val categories = listOf(
        stringResource(R.string.filter_all) to SkillOption.ALL,
        stringResource(R.string.skill_cleaner) to SkillOption.CLEANER,
        stringResource(R.string.skill_gardener) to SkillOption.GARDENER,
        stringResource(R.string.skill_driver) to SkillOption.DRIVER,
        stringResource(R.string.skill_cook) to SkillOption.COOK,
        stringResource(R.string.skill_plumber) to SkillOption.PLUMBER,
        stringResource(R.string.skill_electrician) to SkillOption.ELECTRICIAN
    )
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(categories) { (label, category) ->
            FilterChip(
                selected = selectedCategory == category,
                onClick = { onCategorySelected(category) },
                label = { Text(label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFFFF9933),
                    selectedLabelColor = Color.White
                ),
                shape = RoundedCornerShape(20.dp)
            )
        }
    }
}

@Composable
fun WorkerResultCard(
    worker: WorkerEntity,
    onCall: () -> Unit,
    onRate: () -> Unit
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Profile Photo
            Box {
                AsyncImage(
                    model = worker.photoUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentScale = ContentScale.Crop
                )
                // Available Dot indicator
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 2.dp, end = 2.dp)
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .padding(2.dp)
                        .clip(CircleShape)
                        .background(if (worker.isAvailable) Color(0xFF4CAF50) else Color.Gray)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = worker.name,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    // Available Today Badge
                    Surface(
                        color = if (worker.isAvailable) Color(0xFFE8F5E9) else Color(0xFFF5F5F5),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = if (worker.isAvailable) stringResource(R.string.available_today) else stringResource(R.string.not_available),
                            color = if (worker.isAvailable) Color(0xFF2E7D32) else Color.Gray,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }

                // Skills
                Row(
                    modifier = Modifier.padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    worker.skillsList.take(3).forEach { skill ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = skill,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Rating
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(18.dp))
                    Text(
                        text = " ${worker.averageRating} ",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = stringResource(R.string.ratings_count, worker.totalRatings),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }

                // Area
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                    Text(
                        text = worker.area,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }

                // Wage
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    Icon(Icons.Default.CurrencyRupee, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Text(
                        text = "${worker.dailyWage.toInt()}${stringResource(R.string.per_day)}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Buttons
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = {
                        onCall()
                        val intent = Intent(Intent.ACTION_DIAL).apply {
                            data = Uri.parse("tel:${worker.phoneNumber}")
                        }
                        context.startActivity(intent)
                    },
                    modifier = Modifier
                        .size(56.dp)
                        .background(Color(0xFF4CAF50), CircleShape)
                ) {
                    Icon(Icons.Default.Phone, contentDescription = stringResource(R.string.call_now), tint = Color.White)
                }
                Spacer(modifier = Modifier.height(8.dp))
                IconButton(onClick = onRate) {
                    Icon(Icons.Default.ThumbUp, contentDescription = stringResource(R.string.rate_service), tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
fun LoadingState() {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        repeat(3) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.LightGray.copy(alpha = 0.2f))
            ) {
                // Skeleton UI structure
                Row(modifier = Modifier.padding(12.dp).fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(90.dp).clip(CircleShape).background(Color.LightGray.copy(alpha = 0.3f)))
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Box(modifier = Modifier.fillMaxWidth(0.6f).height(20.dp).background(Color.LightGray.copy(alpha = 0.3f)))
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(modifier = Modifier.fillMaxWidth(0.4f).height(15.dp).background(Color.LightGray.copy(alpha = 0.3f)))
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.SearchOff, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.Gray)
        Spacer(modifier = Modifier.height(16.dp))
        Text(stringResource(R.string.no_workers_found), color = Color.Gray, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(stringResource(R.string.error_prefix, message), color = MaterialTheme.colorScheme.error)
        Button(onClick = onRetry, modifier = Modifier.padding(top = 16.dp)) {
            Text(stringResource(R.string.retry))
        }
    }
}
