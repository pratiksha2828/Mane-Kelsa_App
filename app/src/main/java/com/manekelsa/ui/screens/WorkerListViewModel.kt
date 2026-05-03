package com.manekelsa.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.manekelsa.data.local.entity.WorkerEntity
import com.manekelsa.domain.repository.RatingRepository
import com.manekelsa.domain.repository.WorkerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WorkerListViewModel @Inject constructor(
    private val repository: WorkerRepository,
    private val ratingRepository: RatingRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow("All")
    val selectedFilter = _selectedFilter.asStateFlow()

    private val _selectedArea = MutableStateFlow("")
    val selectedArea = _selectedArea.asStateFlow()

    private val _userDefaultArea = MutableStateFlow("")

    private val _showAvailableOnly = MutableStateFlow(false)
    val showAvailableOnly = _showAvailableOnly.asStateFlow()

    private val _ratedWorkersToday = MutableStateFlow<Set<String>>(emptySet())
    val ratedWorkersToday = _ratedWorkersToday.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    init {
        loadUserDefaultArea()
    }

    private fun loadUserDefaultArea() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            val profile = repository.getWorkerProfile(userId)
            if (profile != null) {
                _userDefaultArea.value = profile.area
                if (_selectedArea.value.isEmpty()) {
                    _selectedArea.value = profile.area
                }
            }
        }
    }

    @OptIn(FlowPreview::class)
    private val debouncedSearchQuery = _searchQuery
        .debounce(500L)
        .distinctUntilChanged()

    val workers: StateFlow<List<WorkerEntity>> = combine(
        repository.getRemoteWorkers()
            .map { workers ->
                workers.map { worker ->
                    val normalizedName = com.manekelsa.utils.WorkerNameNormalizer.normalize(worker.name, worker.id, worker.phoneNumber)
                    worker.copy(name = normalizedName)
                }
            }
            .onStart { 
                _isLoading.value = true 
                _error.value = null
            }
            .onEach { _isLoading.value = false }
            .catch { e ->
                _isLoading.value = false
                _error.value = e.message ?: "Unknown error"
            },
        debouncedSearchQuery,
        _selectedFilter,
        _selectedArea,
        _showAvailableOnly,
        _userDefaultArea
    ) { args: Array<*> ->
        @Suppress("UNCHECKED_CAST")
        val allWorkers = args[0] as List<WorkerEntity>
        val query = args[1] as String
        val filter = args[2] as String
        val area = args[3] as String
        val availableOnly = args[4] as Boolean
        val defaultArea = args[5] as String

        allWorkers
            .asSequence()
            .filter { worker ->
                val matchesQuery = query.isEmpty() || 
                        worker.name.contains(query, ignoreCase = true) ||
                        worker.area.contains(query, ignoreCase = true) ||
                        worker.skillsList.any { it.contains(query, ignoreCase = true) }
                
                val matchesFilter = when (filter) {
                    "All" -> true
                    "Cleaners" -> worker.skillsList.any { it.equals("Cleaning", ignoreCase = true) }
                    "Gardeners" -> worker.skillsList.any { it.equals("Gardening", ignoreCase = true) }
                    "Drivers" -> worker.skillsList.any { it.equals("Driving", ignoreCase = true) }
                    "Others" -> {
                        val mainSkills = listOf("Cleaning", "Gardeners", "Drivers")
                        worker.skillsList.any { it !in mainSkills }
                    }
                    else -> true
                }

                val matchesArea = area.isEmpty() || 
                        worker.area.trim().equals(area.trim(), ignoreCase = true)
                
                val matchesAvailability = !availableOnly || worker.isAvailable

                matchesQuery && matchesFilter && matchesArea && matchesAvailability
            }
            .sortedWith(
                compareByDescending<WorkerEntity> { 
                    it.area.trim().equals(defaultArea.trim(), ignoreCase = true) 
                }.thenByDescending { it.isAvailable }
                 .thenByDescending { it.averageRating }
            )
            .toList()
    }.onEach { workers ->
        checkRatingsForWorkers(workers)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private fun checkRatingsForWorkers(workers: List<WorkerEntity>) {
        viewModelScope.launch {
            val ratedSet = mutableSetOf<String>()
            workers.forEach { worker ->
                if (ratingRepository.hasUserRatedToday(worker.id)) {
                    ratedSet.add(worker.id)
                }
            }
            _ratedWorkersToday.value = ratedSet
        }
    }

    fun retry() {
        _isLoading.value = true
        _error.value = null
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onFilterChange(filter: String) {
        _selectedFilter.value = filter
    }

    fun onAreaChange(area: String) {
        _selectedArea.value = area
    }

    fun toggleAvailabilityFilter() {
        _showAvailableOnly.value = !_showAvailableOnly.value
    }

    fun rateWorker(workerId: String) {
        viewModelScope.launch {
            val result = ratingRepository.updateRating(workerId, 1.0f)
            if (result.isSuccess) {
                _ratedWorkersToday.value += workerId
            }
        }
    }
}
