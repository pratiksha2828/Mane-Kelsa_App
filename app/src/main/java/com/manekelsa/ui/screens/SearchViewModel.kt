package com.manekelsa.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.manekelsa.data.local.entity.WorkerEntity
import com.manekelsa.domain.repository.CallLogRepository
import com.manekelsa.domain.repository.WorkerRepository
import com.manekelsa.utils.SearchMatcher
import com.manekelsa.utils.WorkerNameNormalizer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val searchQuery: String = "",
    val selectedCategory: String = "All",
    val minPrice: String = "",
    val maxPrice: String = "",
    val sortByPriceAsc: Boolean? = null,
    val workers: List<WorkerEntity> = emptyList(),
    val contactedWorkerIds: Set<String> = emptySet(),
    val hireRequests: Map<String, String> = emptyMap(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val workerRepository: WorkerRepository,
    private val callLogRepository: CallLogRepository,
    private val ratingRepository: com.manekelsa.domain.repository.RatingRepository
) : ViewModel() {

    private val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
    private val localEmployerId: String
        get() = auth.currentUser?.uid ?: "local_employer"
    private val localEmployerName: String
        get() = auth.currentUser?.displayName.takeIf { !it.isNullOrBlank() } ?: "Local Employer"

    private val _searchQuery = MutableStateFlow("")
    private val _selectedCategory = MutableStateFlow("All")
    private val _minPrice = MutableStateFlow("")
    private val _maxPrice = MutableStateFlow("")
    private val _sortByPriceAsc = MutableStateFlow<Boolean?>(null)
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

    private val _likedWorkers = MutableStateFlow<Set<String>>(emptySet())
    val likedWorkers: StateFlow<Set<String>> = _likedWorkers.asStateFlow()

    private val _ratedWorkers = MutableStateFlow<Set<String>>(emptySet())
    val ratedWorkers: StateFlow<Set<String>> = _ratedWorkers.asStateFlow()

    // IMMEDIATE UI STATE for the search bar
    private val _baseUiState = combine(
        _searchQuery,
        _selectedCategory,
        _minPrice,
        _maxPrice,
        _sortByPriceAsc,
        _contactedWorkerIds,
        _hireRequests,
        _error
    ) { args ->
        val query = args[0] as String
        val category = args[1] as String
        val minPrice = args[2] as String
        val maxPrice = args[3] as String
        val sortByPriceAsc = args[4] as Boolean?
        val contactedIds = args[5] as Set<String>
        val requests = args[6] as Map<String, String>
        val error = args[7] as String?
        SearchUiState(
            searchQuery = query,
            selectedCategory = category,
            minPrice = minPrice,
            maxPrice = maxPrice,
            sortByPriceAsc = sortByPriceAsc,
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
            val data = (allWorkers + fallbackWorkers)
                .distinctBy { it.id }
                .filterNot { isBlockedWorker(it) }
                .filterNot { isCurrentUser(it) }
            state.copy(
                workers = filterWorkers(
                    data,
                    state.searchQuery,
                    state.selectedCategory,
                    state.minPrice,
                    state.maxPrice,
                    state.sortByPriceAsc
                ),
                isLoading = data.isEmpty() && state.error == null
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SearchUiState(isLoading = true))

    init {
        viewModelScope.launch {
            workerRepository.getRemoteWorkers().collect { }
        }
        viewModelScope.launch {
            _allWorkers.collectLatest { workers ->
                refreshRatedWorkers(workers)
            }
        }
    }

    private suspend fun refreshRatedWorkers(workers: List<WorkerEntity>) {
        val ratedSet = mutableSetOf<String>()
        workers.forEach { worker ->
            if (ratingRepository.hasUserRatedToday(worker.id)) {
                ratedSet.add(worker.id)
            }
        }
        _ratedWorkers.value = ratedSet
    }

    private fun filterWorkers(
        workers: List<WorkerEntity>,
        searchText: String,
        selectedSkill: String,
        minPrice: String,
        maxPrice: String,
        sortByPriceAsc: Boolean?
    ): List<WorkerEntity> {
        val normalizedQuery = SearchMatcher.normalizeQuery(searchText)
        val minValue = minPrice.toDoubleOrNull()
        val maxValue = maxPrice.toDoubleOrNull()
        val skillFiltered = workers.filter { worker ->
            SearchMatcher.matchesSkill(worker, selectedSkill) && matchesPrice(worker, minValue, maxValue)
        }

        if (normalizedQuery.isBlank()) {
            return sortWorkers(skillFiltered, sortByPriceAsc)
        }

        val exactMatches = skillFiltered.filter { worker ->
            SearchMatcher.matchesQuery(worker, normalizedQuery)
        }

        val results = if (exactMatches.isNotEmpty()) {
            sortWorkers(exactMatches, sortByPriceAsc)
        } else {
            skillFiltered
                .map { worker -> worker to SearchMatcher.similarityScore(worker, normalizedQuery) }
                .sortedWith(
                    compareBy<Pair<WorkerEntity, Int>> { it.second }
                        .thenByDescending { it.first.isAvailable }
                        .thenByDescending { it.first.averageRating }
                )
                .take(5)
                .map { it.first }
        }

        return results
    }

    private fun sortWorkers(workers: List<WorkerEntity>, sortByPriceAsc: Boolean?): List<WorkerEntity> {
        val baseSort = compareByDescending<WorkerEntity> { it.isAvailable }
        return when (sortByPriceAsc) {
            true -> workers.sortedWith(baseSort.thenBy { it.dailyWage })
            false -> workers.sortedWith(baseSort.thenByDescending { it.dailyWage })
            null -> workers.sortedWith(baseSort.thenByDescending { it.averageRating })
        }
    }

    private fun matchesPrice(worker: WorkerEntity, minValue: Double?, maxValue: Double?): Boolean {
        val wage = worker.dailyWage
        if (minValue != null && wage < minValue) return false
        if (maxValue != null && wage > maxValue) return false
        return true
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onCategorySelect(category: String) {
        _selectedCategory.value = category
    }

    fun onMinPriceChange(input: String) {
        _minPrice.value = input.filter { it.isDigit() }
    }

    fun onMaxPriceChange(input: String) {
        _maxPrice.value = input.filter { it.isDigit() }
    }

    fun togglePriceSort() {
        _sortByPriceAsc.value = when (_sortByPriceAsc.value) {
            null -> true
            true -> false
            false -> null
        }
    }

    fun onCallWorker(worker: WorkerEntity) {
        viewModelScope.launch {
            callLogRepository.addCallLog(worker.id, worker.name)
        }
    }

    fun onThumbsUp(workerId: String) {
        viewModelScope.launch {
            if (!_likedWorkers.value.contains(workerId)) {
                workerRepository.rateWorker(workerId)
                _likedWorkers.value = _likedWorkers.value + workerId
            }
        }
    }

    fun onStarRating(workerId: String, rating: Int) {
        viewModelScope.launch {
            val result = ratingRepository.updateRating(workerId, rating.toFloat())
            if (result.isSuccess) {
                _ratedWorkers.value = _ratedWorkers.value + workerId
            }
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

    private fun isBlockedWorker(worker: WorkerEntity): Boolean {
        val normalizedName = worker.name.trim()
        val normalizedArea = worker.area.trim().lowercase()
        val normalizedSkills = worker.skillsList.map { it.trim().lowercase() }
        val isNameMatch = normalizedName.equals("Pratiksha Bhat", ignoreCase = true) ||
            normalizedName.equals("Pratiksha Baht", ignoreCase = true)
        val isPhoneMatch = worker.phoneNumber.trim() == "5555555555"
        val isAreaMatch = normalizedArea.contains("vijayanagar")
        val isSkillMatch = normalizedSkills.contains("cook") && normalizedSkills.contains("caretaker")
        val isWageMatch = worker.dailyWage >= 3000.0
        val isRatingMatch = worker.averageRating >= 4.9f && worker.totalRatings == 1

        if (isPhoneMatch) return true
        return isNameMatch && isAreaMatch && isSkillMatch && isWageMatch && isRatingMatch
    }

    private fun isCurrentUser(worker: WorkerEntity): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        return worker.id == uid
    }
}
