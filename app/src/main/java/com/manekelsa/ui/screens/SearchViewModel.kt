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
import com.manekelsa.utils.TranslationUtils
import com.manekelsa.ui.model.SkillOption

data class SearchUiState(
    val searchQuery: String = "",
    val selectedCategory: String = "All",
    val maxDailyBudget: Int? = null,
    val workers: List<WorkerEntity> = emptyList(),
    val contactedWorkerIds: Set<String> = emptySet(),
    val hireRequests: Map<String, String> = emptyMap(),
    val isLoading: Boolean = false,
    val availableWorkersTotal: Int = 0,
    val allWorkersTotal: Int = 0,
    val mapQuery: String = "",
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
    private val _maxDailyBudget = MutableStateFlow<Int?>(null)
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
        _maxDailyBudget,
        _contactedWorkerIds,
        _hireRequests,
        _error
    ) { params ->
        val query = params[0] as String
        val category = params[1] as String
        val budget = params[2] as Int?
        @Suppress("UNCHECKED_CAST")
        val contactedIds = params[3] as Set<String>
        @Suppress("UNCHECKED_CAST")
        val requests = params[4] as Map<String, String>
        val error = params[5] as String?
        SearchUiState(
            searchQuery = query,
            selectedCategory = category,
            maxDailyBudget = budget,
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
            val data = (allWorkers + fallbackWorkers).distinctBy { it.id }
            val filtered = filterWorkers(data, state.searchQuery, state.selectedCategory, state.maxDailyBudget, localEmployerId)
            val others = data.filter { w -> localEmployerId == "local_employer" || w.id != localEmployerId }
            state.copy(
                workers = filtered,
                isLoading = data.isEmpty() && state.error == null,
                availableWorkersTotal = others.count { it.isAvailable },
                allWorkersTotal = others.size,
                mapQuery = resolveMapQuery(state.searchQuery)
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
        maxDailyBudget: Int?,
        excludeUserId: String?
    ): List<WorkerEntity> {
        var pool = workers
        val budgetCap = maxDailyBudget?.takeIf { it > 0 }
        if (budgetCap != null) {
            pool = pool.filter { it.dailyWage <= budgetCap }
        }

        val normalizedQuery = SearchMatcher.normalizeQuery(
            TranslationUtils.normalizeSearchText(searchText)
        )

        val extractedSkills = TranslationUtils.extractSkillIds(searchText)

        var skillFiltered = pool.filter { worker ->
            if (selectedSkill.isNotBlank() && selectedSkill != SkillOption.ALL) {
                SearchMatcher.matchesSkill(worker, selectedSkill)
            } else if (extractedSkills.isNotEmpty()) {
                worker.skillsList.any { skill ->
                    extractedSkills.contains(SkillOption.normalize(skill))
                }
            } else {
                true
            }
        }

        if (normalizedQuery.isBlank()) {
            return sortWorkers(skillFiltered)
        }

        val exactMatches = skillFiltered.filter { worker -> SearchMatcher.matchesQuery(worker, normalizedQuery) }
        return sortWorkers(exactMatches)
    }

    private fun sortWorkers(workers: List<WorkerEntity>): List<WorkerEntity> {
        return workers.sortedWith(
            compareByDescending<WorkerEntity> { it.isAvailable }
                .thenByDescending { it.averageRating }
        )
    }

    fun onMaxDailyBudgetChange(maxRupees: Int?) {
        _maxDailyBudget.value = maxRupees?.takeIf { it > 0 }
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

    fun onThumbsUp(workerId: String, hireStatus: String?) {
        if (hireStatus != "ACCEPTED") return
        viewModelScope.launch {
            if (!_likedWorkers.value.contains(workerId)) {
                workerRepository.rateWorker(workerId)
                _likedWorkers.value = _likedWorkers.value + workerId
            }
        }
    }

    fun onStarRating(workerId: String, rating: Int, hireStatus: String?) {
        if (hireStatus != "ACCEPTED") return
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

    fun resolveMapQuery(query: String): String {
        val normalized = TranslationUtils.normalizeSearchText(query)
        if (normalized.isBlank()) return ""

        val placeHints = listOf(
            ",",
            "whitefield", "ವೈಟ್‌ಫೀಲ್ಡ್",
            "koramangala", "ಕೋರಮಂಗಲ",
            "indiranagar", "ಇಂದಿರಾನಗರ",
            "jayanagar", "ಜಯನಗರ",
            "malleshwaram", "ಮಲ್ಲೇಶ್ವರಂ",
            "rajajinagar", "ರಾಜಾಜಿನಗರ",
            "electronic city", "ಎಲೆಕ್ಟ್ರಾನಿಕ್ ಸಿಟಿ",
            "vijayanagar", "ವಿಜಯನಗರ",
            "hsr", "ಎಚ್‌ಎಸ್‌ಆರ್",
            "btm", "ಬಿಟಿಎಂ",
            "layout", "ಲೇಔಟ್",
            "nagar", "ನಗರ",
            "road", "ರಸ್ತೆ",
            "street",
            "st",
            "cross", "ಕ್ರಾಸ್",
            "main", "ಮುಖ್ಯ",
            "colony", "ವಸತಿ",
            "phase", "ಹಂತ",
            "block", "ಬ್ಲಾಕ್",
            "sector", "ವಲಯ",
            "circle",
            "market",
            "city",
            "ಬೆಂಗಳೂರು", "bangalore", "bengaluru"
        )
        val looksLikePlace = placeHints.any { hint -> normalized.contains(hint) }
        return if (looksLikePlace) normalized else ""
    }
}
