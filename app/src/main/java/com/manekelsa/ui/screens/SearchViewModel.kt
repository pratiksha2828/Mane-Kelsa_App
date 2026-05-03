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

    private val _searchQuery = MutableStateFlow("")
    private val _selectedCategory = MutableStateFlow("All")
    private val _error = MutableStateFlow<String?>(null)

    private val fallbackWorkers: List<WorkerEntity> = com.manekelsa.data.local.MockData.fallbackWorkers

    private val _allWorkers = workerRepository.getAllWorkers()
        .map { workers ->
            workers.map { worker ->
                val normalizedName = WorkerNameNormalizer.normalize(worker.name, worker.id, worker.phoneNumber)
                if (normalizedName != worker.name) {
                    viewModelScope.launch { workerRepository.updateLocalWorkerName(worker.id, normalizedName) }
                }
                worker.copy(name = normalizedName)
            }
        }
        .catch { e -> _error.value = e.message }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _contactedWorkerIds = callLogRepository.getRecentCallLogs()
        .map { logs -> logs.map { it.workerId }.toSet() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    private val _hireRequests = workerRepository.getEmployerRequests("local_employer")
        .map { requests -> requests.associate { it.workerId to it.status } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    @OptIn(kotlinx.coroutines.FlowPreview::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<SearchUiState> = combine(
        _searchQuery,
        _selectedCategory,
        _allWorkers,
        _contactedWorkerIds,
        _hireRequests,
        _error
    ) { params ->
        val query = params[0] as String
        val category = params[1] as String
        val workers = params[2] as List<WorkerEntity>
        val contactedIds = params[3] as Set<String>
        val requests = params[4] as Map<String, String>
        val error = params[5] as String?

        SearchUiState(
            searchQuery = query,
            selectedCategory = category,
            workers = workers,
            contactedWorkerIds = contactedIds,
            hireRequests = requests,
            isLoading = false,
            error = error
        )
    }.flatMapLatest { state ->
        val queryFlow = if (state.searchQuery.isEmpty()) flowOf(state.searchQuery) else flowOf(state.searchQuery).debounce(300)
        queryFlow.map { debouncedQuery ->
            val data = if (state.workers.isEmpty() && state.error == null) fallbackWorkers else state.workers
            state.copy(
                workers = filterWorkers(data, debouncedQuery, state.selectedCategory),
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
            compareByDescending<WorkerEntity> { worker ->
                if (searchText.isBlank()) 0
                else if (worker.area.equals(searchText, ignoreCase = true)) 2
                else if (worker.area.contains(searchText, ignoreCase = true)) 1
                else 0
            }.thenByDescending { it.isAvailable }
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

    fun onRequestHire(workerId: String) {
        viewModelScope.launch {
            workerRepository.createHireRequest(
                workerId = workerId,
                employerId = "local_employer",
                employerName = "Employer User"
            )
        }
    }

    fun onRateWorker(workerId: String) {
        viewModelScope.launch {
            workerRepository.rateWorker(workerId)
        }
    }

    fun retry() {
        _error.value = null
    }
}
