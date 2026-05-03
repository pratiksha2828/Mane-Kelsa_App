package com.manekelsa.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.manekelsa.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkerListScreen(
    viewModel: WorkerListViewModel = hiltViewModel(),
    onWorkerClick: (String) -> Unit
) {
    val workers by viewModel.workers.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val ratedWorkersToday by viewModel.ratedWorkersToday.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    val filters = listOf("All", "Cleaners", "Gardeners", "Drivers", "Others")

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            Surface(shadowElevation = 4.dp) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    // Search Bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = viewModel::onSearchQueryChange,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search by name, area or skill...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        shape = MaterialTheme.shapes.medium,
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFFF9933),
                            unfocusedBorderColor = Color.LightGray,
                            focusedLabelColor = Color(0xFFFF9933)
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Filter Chips Row
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 8.dp)
                    ) {
                        items(filters) { filter ->
                            FilterChip(
                                selected = selectedFilter == filter,
                                onClick = { viewModel.onFilterChange(filter) },
                                label = { Text(filter) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFFF9933),
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when {
                error != null -> {
                    if (error?.contains("network", ignoreCase = true) == true) {
                        NetworkErrorScreen(onRetry = viewModel::retry)
                    } else {
                        ServerErrorScreen(onRetry = viewModel::retry)
                    }
                }
                isLoading -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(5) { WorkerCardShimmer() }
                    }
                }
                workers.isEmpty() -> {
                    EmptyStateScreen(
                        title = if (searchQuery.isEmpty()) "No workers yet." else "No results found",
                        description = "Try adjusting your search or check again later."
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(workers, key = { it.id }) { worker ->
                            WorkerCard(
                                worker = worker,
                                hasRatedToday = ratedWorkersToday.contains(worker.id),
                                onClick = { onWorkerClick(worker.id) },
                                onRateClick = { viewModel.rateWorker(worker.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}
