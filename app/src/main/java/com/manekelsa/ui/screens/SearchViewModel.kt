package com.manekelsa.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.manekelsa.data.local.entity.WorkerEntity
import com.manekelsa.domain.repository.CallLogRepository
import com.manekelsa.domain.repository.WorkerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val searchQuery: String = "",
    val selectedCategory: String = "All",
    val workers: List<WorkerEntity> = emptyList(),
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

    private val _allWorkers = workerRepository.getAllWorkers()
        .catch { e -> _error.value = e.message }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    @OptIn(kotlinx.coroutines.FlowPreview::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<SearchUiState> = combine(
        _searchQuery,
        _selectedCategory,
        _allWorkers,
        _error
    ) { query, category, workers, error ->
        SearchUiState(
            searchQuery = query,
            selectedCategory = category,
            workers = workers,
            isLoading = workers.isEmpty() && error == null,
            error = error
        )
    }.flatMapLatest { state ->
        // We debounce the filtering logic but keep the query immediate in the state
        val queryFlow = if (state.searchQuery.isEmpty()) flowOf(state.searchQuery) else flowOf(state.searchQuery).debounce(300)
        queryFlow.map { debouncedQuery ->
            state.copy(workers = filterWorkers(_allWorkers.value, debouncedQuery, state.selectedCategory))
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

    fun retry() {
        _error.value = null
    }
}
