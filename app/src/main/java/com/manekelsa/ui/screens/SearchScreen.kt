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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.PhoneCallback
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
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
import coil.compose.AsyncImage
import com.manekelsa.ui.model.SkillOption
import com.manekelsa.ui.components.SkillChipLabel
import com.manekelsa.ui.components.glassmorphism
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import com.manekelsa.R
import com.manekelsa.data.local.entity.WorkerEntity
import com.manekelsa.utils.LocalizationManager
import com.manekelsa.utils.TranslationUtils

@Composable
fun SearchScreen(
    initialCategory: String? = null,
    viewModel: SearchViewModel = hiltViewModel()
) {
    androidx.compose.runtime.LaunchedEffect(initialCategory) {
        if (initialCategory != null) {
            viewModel.onCategorySelect(initialCategory)
        }
    }
    val uiState by viewModel.uiState.collectAsState()
    val likedWorkers by viewModel.likedWorkers.collectAsState()
    val ratedWorkers by viewModel.ratedWorkers.collectAsState()
    val context = LocalContext.current
    val requestedToastText = stringResource(R.string.status_requested)
    val speechRecognizerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS)?.get(0)
            if (!spokenText.isNullOrEmpty()) {
                viewModel.onSearchQueryChange(spokenText)
            }
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
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
                            } else {
                                IconButton(onClick = {
                                    try {
                                        val languageTag = if (LocalizationManager.getLanguage().startsWith("kn")) {
                                            "kn-IN"
                                        } else {
                                            "en-IN"
                                        }
                                        val intent = Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                            putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                            putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE, languageTag)
                                            putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, languageTag)
                                        }
                                        speechRecognizerLauncher.launch(intent)
                                    } catch (e: Exception) {
                                        android.widget.Toast.makeText(context, "Voice search not available", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }) {
                                    Icon(Icons.Default.Mic, contentDescription = null)
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

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(
                                R.string.search_workers_summary,
                                uiState.availableWorkersTotal,
                                uiState.allWorkersTotal
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = uiState.maxDailyBudget?.toString().orEmpty(),
                            onValueChange = { input ->
                                val digits = input.filter { it.isDigit() }.take(6)
                                viewModel.onMaxDailyBudgetChange(digits.toIntOrNull())
                            },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            label = { Text(stringResource(R.string.search_max_daily_wage)) },
                            placeholder = { Text(stringResource(R.string.search_max_price_hint)) },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                        TextButton(onClick = { viewModel.onMaxDailyBudgetChange(null) }) {
                            Text(stringResource(R.string.search_max_price_clear))
                        }
                    }
                }
            }
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            var selectedWorker by remember { mutableStateOf<WorkerEntity?>(null) }
            
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
                            MapPreviewCard(context = context, mapQuery = uiState.mapQuery)
                        }
                        items(uiState.workers, key = { it.id }) { worker ->
                            WorkerResultCard(
                                worker = worker,
                                hireStatus = uiState.hireRequests[worker.id],
                                isThumbed = likedWorkers.contains(worker.id),
                                onCall = { viewModel.onCallWorker(worker) },
                                onCardClick = { selectedWorker = worker },
                                onThumbsUp = { viewModel.onThumbsUp(worker.id, uiState.hireRequests[worker.id]) },
                                onRequestHire = {
                                    viewModel.onRequestHire(worker.id)
                                    Toast.makeText(context, requestedToastText, Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                    
                    selectedWorker?.let { worker ->
                        WorkerProfileBottomSheet(
                            worker = worker,
                            hireStatus = uiState.hireRequests[worker.id],
                            isThumbed = likedWorkers.contains(worker.id),
                            hasStarRated = ratedWorkers.contains(worker.id),
                            onDismiss = { selectedWorker = null },
                            onCall = { viewModel.onCallWorker(worker) },
                            onThumbsUp = { viewModel.onThumbsUp(worker.id, uiState.hireRequests[worker.id]) },
                            onStarRate = { rating -> viewModel.onStarRating(worker.id, rating, uiState.hireRequests[worker.id]) },
                            onRequestHire = {
                                viewModel.onRequestHire(worker.id)
                                Toast.makeText(context, requestedToastText, Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MapPreviewCard(context: Context, mapQuery: String) {
    val mapView = remember {
        MapView(context).apply {
            Configuration.getInstance().userAgentValue = context.packageName
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

    androidx.compose.runtime.LaunchedEffect(mapQuery) {
        if (mapQuery.isNotBlank()) {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    val geocoder = android.location.Geocoder(context, java.util.Locale.getDefault())
                    val addresses = geocoder.getFromLocationName("$mapQuery, Bangalore", 1)
                    if (!addresses.isNullOrEmpty()) {
                        val location = addresses[0]
                        val geoPoint = GeoPoint(location.latitude, location.longitude)
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            mapView.controller.animateTo(geoPoint, 14.5, 1000L)
                            mapView.overlays.clear()
                            mapView.overlays.add(Marker(mapView).apply {
                                position = geoPoint
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                title = mapQuery
                            })
                            mapView.invalidate()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } else {
            val center = GeoPoint(12.9716, 77.5946)
            mapView.controller.animateTo(center, 13.0, 1000L)
            mapView.overlays.clear()
            mapView.overlays.add(Marker(mapView).apply {
                position = center
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                title = context.getString(R.string.map_default_pin)
            })
            mapView.invalidate()
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clickable {
                Toast.makeText(context, "Opening Map View...", Toast.LENGTH_SHORT).show()
            }
            .glassmorphism(cornerRadius = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { mapView }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkerResultCard(
    worker: WorkerEntity,
    hireStatus: String?,
    isThumbed: Boolean,
    onCardClick: () -> Unit,
    onCall: () -> Unit,
    onThumbsUp: () -> Unit,
    onRequestHire: () -> Unit
) {
    val context = LocalContext.current
    val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
    val isSelf = currentUserId != null && worker.id == currentUserId
    Card(
        onClick = onCardClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 16.dp)
            .glassmorphism(cornerRadius = 20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                    Text(
                        text = TranslationUtils.getTranslatedName(worker.name),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
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
                                SkillChipLabel(
                                    skill = skill,
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
                            text = TranslationUtils.getTranslatedArea(worker.area),
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
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onThumbsUp, enabled = hireStatus == "ACCEPTED" && !isThumbed) {
                    Icon(
                        Icons.Default.ThumbUp,
                        contentDescription = stringResource(R.string.rate_service),
                        tint = if (hireStatus == "ACCEPTED" && !isThumbed) MaterialTheme.colorScheme.primary else Color.Gray
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        onCall()
                        val intent = Intent(Intent.ACTION_DIAL).apply {
                            data = Uri.parse("tel:${worker.phoneNumber}")
                        }
                        context.startActivity(intent)
                    },
                    enabled = worker.isAvailable
                ) {
                    Icon(Icons.Default.Phone, contentDescription = stringResource(R.string.call_now), tint = if (worker.isAvailable) Color(0xFF4CAF50) else Color.Gray)
                }
                Spacer(modifier = Modifier.width(8.dp))
                
                Button(
                    onClick = onRequestHire,
                    modifier = Modifier.height(40.dp),
                    enabled = hireStatus == null || hireStatus == "REJECTED",
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        disabledContainerColor = Color.LightGray
                    ),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    val textStr = when (hireStatus) {
                        "PENDING" -> stringResource(R.string.status_requested)
                        "ACCEPTED" -> stringResource(R.string.status_hired)
                        "REJECTED" -> stringResource(R.string.action_request_hire)
                        else -> stringResource(R.string.action_request_hire)
                    }
                    Text(textStr, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkerProfileBottomSheet(
    worker: WorkerEntity,
    hireStatus: String?,
    isThumbed: Boolean,
    hasStarRated: Boolean,
    onDismiss: () -> Unit,
    onCall: () -> Unit,
    onThumbsUp: () -> Unit,
    onStarRate: (Int) -> Unit,
    onRequestHire: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
    val isSelf = currentUserId != null && worker.id == currentUserId
    var selectedStars by remember { mutableStateOf(0) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(androidx.compose.foundation.rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AsyncImage(
                model = worker.photoUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = ContentScale.Crop
            )
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = TranslationUtils.getTranslatedName(worker.name),
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "${TranslationUtils.getTranslatedArea(worker.area)} • ₹${worker.dailyWage.toInt()}${stringResource(R.string.per_day)} • ${stringResource(R.string.years_suffix, worker.experience.toString())}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                worker.skillsList.forEach { skill ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        SkillChipLabel(
                            skill = skill,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Card(
                    modifier = Modifier.weight(1f).glassmorphism(cornerRadius = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.DoneAll, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Text(worker.totalRatings.toString(), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                        Text(stringResource(R.string.stat_jobs), style = MaterialTheme.typography.labelSmall)
                    }
                }
                Card(
                    modifier = Modifier.weight(1f).glassmorphism(cornerRadius = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(20.dp))
                        Text("%.1f".format(worker.averageRating), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                        Text(stringResource(R.string.stat_rating), style = MaterialTheme.typography.labelSmall)
                    }
                }
                Card(
                    modifier = Modifier.weight(1f).glassmorphism(cornerRadius = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.AutoMirrored.Filled.PhoneCallback, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Text(worker.likes.toString(), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                        Text(stringResource(R.string.stat_calls), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                val context = LocalContext.current
                OutlinedButton(
                    onClick = onThumbsUp,
                    enabled = hireStatus == "ACCEPTED" && !isThumbed,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.ThumbUp, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isThumbed) stringResource(R.string.liked) else stringResource(R.string.rate_service))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Button(
                    onClick = {
                        onCall()
                        val intent = Intent(Intent.ACTION_DIAL).apply {
                            data = Uri.parse("tel:${worker.phoneNumber}")
                        }
                        context.startActivity(intent)
                    },
                    enabled = worker.isAvailable,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.call_now))
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.stat_rating),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    (1..5).forEach { star ->
                        IconButton(
                            onClick = { selectedStars = star },
                            enabled = hireStatus == "ACCEPTED" && !hasStarRated
                        ) {
                            Icon(
                                imageVector = if (selectedStars >= star) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = null,
                                tint = if (selectedStars >= star) Color(0xFFFFB300) else Color.Gray
                            )
                        }
                    }
                }
                Button(
                    onClick = { if (selectedStars > 0) onStarRate(selectedStars) },
                    enabled = hireStatus == "ACCEPTED" && !hasStarRated && selectedStars > 0,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = stringResource(R.string.submit))
                }
                if (hireStatus != "ACCEPTED") {
                    Text(
                        text = stringResource(R.string.rate_employed_only),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else if (hasStarRated) {
                    Text(
                        text = stringResource(R.string.thanks_for_rating),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Button(
                onClick = onRequestHire,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                enabled = hireStatus == null || hireStatus == "REJECTED",
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = Color.LightGray
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                val textStr = when (hireStatus) {
                    "PENDING" -> stringResource(R.string.status_requested)
                    "ACCEPTED" -> stringResource(R.string.status_hired)
                    "REJECTED" -> stringResource(R.string.action_request_hire)
                    else -> stringResource(R.string.action_request_hire)
                }
                Text(textStr, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            if (worker.totalRatings > 0 && hireStatus == "ACCEPTED") {
                // Reviews Section
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = stringResource(R.string.reviews_title),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    
                    val reviews = listOf(R.string.review_1, R.string.review_2, R.string.review_3, R.string.review_4, R.string.review_5)
                    // Pick 2 pseudo-random reviews based on worker id hash
                    val hash = worker.id.hashCode()
                    val reviewIndex1 = Math.abs(hash) % reviews.size
                    val reviewIndex2 = (Math.abs(hash) + 1) % reviews.size
                    
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassmorphism(cornerRadius = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = stringResource(reviews[reviewIndex1]),
                                style = MaterialTheme.typography.bodyMedium,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row {
                                repeat(5) { Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(14.dp)) }
                            }
                        }
                    }
                    
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassmorphism(cornerRadius = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = stringResource(reviews[reviewIndex2]),
                                style = MaterialTheme.typography.bodyMedium,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row {
                                repeat(4) { Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(14.dp)) }
                                Icon(Icons.Default.Star, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
