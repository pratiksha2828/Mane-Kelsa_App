package com.manekelsa.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.manekelsa.data.local.entity.WorkerEntity
import com.manekelsa.domain.repository.CallLogRepository
import com.manekelsa.domain.repository.WorkerRepository
import com.manekelsa.utils.WorkerNameNormalizer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val searchQuery: String = "",
    val selectedCategory: String = "All",
    val workers: List<WorkerEntity> = emptyList(),
    val contactedWorkerIds: Set<String> = emptySet(),
    val hireRequests: Map<String, String> = emptyMap(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val workerRepository: WorkerRepository,
    private val callLogRepository: CallLogRepository
) : ViewModel() {

    private val localEmployerId = "local_employer"
    private val localEmployerName = "Local Employer"

    private val _searchQuery = MutableStateFlow("")
    private val _selectedCategory = MutableStateFlow("All")
    private val _error = MutableStateFlow<String?>(null)

    private val fallbackWorkers: List<WorkerEntity> = com.manekelsa.data.local.MockData.fallbackWorkers

    private val _allWorkers = workerRepository.getAllWorkers()
        .map { workers ->
            workers.map { worker ->
                val normalizedName = WorkerNameNormalizer.normalize(worker.name, worker.id, worker.phoneNumber)
                worker.copy(name = normalizedName)
            }
        }
        .catch { e -> _error.value = e.message }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _contactedWorkerIds = callLogRepository.getRecentCallLogs()
        .map { logs -> logs.map { it.workerId }.toSet() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    private val _hireRequests = workerRepository.getEmployerRequests(localEmployerId)
        .map { requests -> requests.associate { it.workerId to it.status } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    // IMMEDIATE UI STATE for the search bar
    private val _baseUiState = combine(
        _searchQuery,
        _selectedCategory,
        _contactedWorkerIds,
        _hireRequests,
        _error
    ) { query, category, contactedIds, requests, error ->
        SearchUiState(
            searchQuery = query,
            selectedCategory = category,
            contactedWorkerIds = contactedIds,
            hireRequests = requests,
            isLoading = false,
            error = error
        )
    }

    @OptIn(kotlinx.coroutines.FlowPreview::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<SearchUiState> = _baseUiState.flatMapLatest { state ->
        // Debounce only the search query for filtering, but emit the immediate query in the state
        _allWorkers.map { allWorkers ->
            val data = if (allWorkers.isEmpty() && state.error == null) fallbackWorkers else allWorkers
            state.copy(
                workers = filterWorkers(data, state.searchQuery, state.selectedCategory),
                isLoading = data.isEmpty() && state.error == null
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SearchUiState(isLoading = true))

    init {
        viewModelScope.launch {
            workerRepository.getRemoteWorkers().collect { }
        }
    }

    private fun filterWorkers(
        workers: List<WorkerEntity>,
        searchText: String,
        selectedSkill: String
    ): List<WorkerEntity> {
        return workers.filter { worker ->
            val matchesQuery = if (searchText.isBlank()) {
                true
            } else {
                worker.name.contains(searchText, ignoreCase = true) ||
                worker.area.contains(searchText, ignoreCase = true) ||
                worker.skillsList.any { it.contains(searchText, ignoreCase = true) }
            }

            val matchesSkill = if (selectedSkill == "All") {
                true
            } else {
                worker.skillsList.any { it.equals(selectedSkill, ignoreCase = true) }
            }

            matchesQuery && matchesSkill
        }.sortedWith(
            compareByDescending<WorkerEntity> { it.isAvailable }
                .thenByDescending { it.averageRating }
        )
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onCategorySelect(category: String) {
        _selectedCategory.value = category
    }

    fun onCallWorker(worker: WorkerEntity) {
        viewModelScope.launch {
            callLogRepository.addCallLog(worker.id, worker.name)
        }
    }

    fun onRateWorker(workerId: String) {
        viewModelScope.launch {
            workerRepository.rateWorker(workerId)
        }
    }

    fun onRequestHire(workerId: String) {
        viewModelScope.launch {
            workerRepository.createHireRequest(workerId, localEmployerId, localEmployerName)
        }
    }

    fun retry() {
        _error.value = null
    }
}
