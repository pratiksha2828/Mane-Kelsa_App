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

    private val fallbackWorkers: List<WorkerEntity> = listOf(
        WorkerEntity(
            id = "local_1",
            name = "Shobha Rao",
            photoUrl = null,
            skillsList = listOf("Cleaner", "Cook"),
            dailyWage = 420.0,
            area = "Vijayanagar",
            experience = 4,
            phoneNumber = "9011223344",
            averageRating = 4.4f,
            totalRatings = 11,
            isAvailable = true,
            lastUpdated = System.currentTimeMillis()
        ),
        WorkerEntity(
            id = "local_2",
            name = "Venkatesh Naik",
            photoUrl = null,
            skillsList = listOf("Electrician"),
            dailyWage = 620.0,
            area = "Vijayanagar",
            experience = 7,
            phoneNumber = "9022334455",
            averageRating = 4.5f,
            totalRatings = 15,
            isAvailable = true,
            lastUpdated = System.currentTimeMillis()
        ),
        WorkerEntity(
            id = "local_3",
            name = "Bhavana Rao",
            photoUrl = null,
            skillsList = listOf("Babysitter", "Caretaker"),
            dailyWage = 520.0,
            area = "Banashankari",
            experience = 6,
            phoneNumber = "9122334455",
            averageRating = 4.8f,
            totalRatings = 19,
            isAvailable = true,
            lastUpdated = System.currentTimeMillis()
        ),
        WorkerEntity(
            id = "local_4",
            name = "Praveen Kumar",
            photoUrl = null,
            skillsList = listOf("Driver"),
            dailyWage = 650.0,
            area = "Koramangala",
            experience = 8,
            phoneNumber = "9344556677",
            averageRating = 4.2f,
            totalRatings = 13,
            isAvailable = false,
            lastUpdated = System.currentTimeMillis()
        ),
        WorkerEntity(
            id = "local_5",
            name = "Aparna Shetty",
            photoUrl = null,
            skillsList = listOf("Cook"),
            dailyWage = 480.0,
            area = "Koramangala",
            experience = 5,
            phoneNumber = "9098123456",
            averageRating = 4.3f,
            totalRatings = 17,
            isAvailable = true,
            lastUpdated = System.currentTimeMillis()
        ),
        WorkerEntity(
            id = "local_6",
            name = "Sunita Devi",
            photoUrl = null,
            skillsList = listOf("Gardener", "Cleaner"),
            dailyWage = 380.0,
            area = "JP Nagar",
            experience = 3,
            phoneNumber = "9887766553",
            averageRating = 4.1f,
            totalRatings = 9,
            isAvailable = true,
            lastUpdated = System.currentTimeMillis()
        ),
        WorkerEntity(
            id = "local_7",
            name = "Iqbal Khan",
            photoUrl = null,
            skillsList = listOf("Plumber"),
            dailyWage = 720.0,
            area = "RT Nagar",
            experience = 9,
            phoneNumber = "9098123556",
            averageRating = 4.3f,
            totalRatings = 17,
            isAvailable = true,
            lastUpdated = System.currentTimeMillis()
        ),
        WorkerEntity(
            id = "local_8",
            name = "Farah Siddiqui",
            photoUrl = null,
            skillsList = listOf("Cleaner"),
            dailyWage = 360.0,
            area = "RT Nagar",
            experience = 2,
            phoneNumber = "9988776653",
            averageRating = 4.0f,
            totalRatings = 6,
            isAvailable = true,
            lastUpdated = System.currentTimeMillis()
        ),
        WorkerEntity(
            id = "local_9",
            name = "Meghana Iyer",
            photoUrl = null,
            skillsList = listOf("Nurse", "Caretaker"),
            dailyWage = 700.0,
            area = "Indiranagar",
            experience = 8,
            phoneNumber = "9822001122",
            averageRating = 4.7f,
            totalRatings = 21,
            isAvailable = true,
            lastUpdated = System.currentTimeMillis()
        ),
        WorkerEntity(
            id = "local_10",
            name = "Srinivas Reddy",
            photoUrl = null,
            skillsList = listOf("Painter", "Carpenter"),
            dailyWage = 600.0,
            area = "Whitefield",
            experience = 7,
            phoneNumber = "9001100223",
            averageRating = 4.2f,
            totalRatings = 12,
            isAvailable = true,
            lastUpdated = System.currentTimeMillis()
        )
    )

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

    @OptIn(kotlinx.coroutines.FlowPreview::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<SearchUiState> = combine(
        _searchQuery,
        _selectedCategory,
        _allWorkers,
        _contactedWorkerIds,
        _error
    ) { query, category, workers, contactedIds, error ->
        SearchUiState(
            searchQuery = query,
            selectedCategory = category,
            workers = workers,
            contactedWorkerIds = contactedIds,
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
