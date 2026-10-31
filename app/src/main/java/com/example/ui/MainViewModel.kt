package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.AppSettings
import com.example.model.DownloadJob
import com.example.model.HistoryEntry
import com.example.model.ParsedLuaResult
import com.example.model.SteamAppDetail
import com.example.model.SteamAppSummary
import com.example.model.SteamDepot
import com.example.repository.SteamRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class NavTab {
    SEARCH,
    UPLOAD,
    DOWNLOADS,
    HISTORY,
    SETTINGS
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SteamRepository(application)
    val downloadManager = repository.downloadManager

    val currentJob: StateFlow<DownloadJob?> = downloadManager.currentJob
    val history: StateFlow<List<HistoryEntry>> = downloadManager.history
    val settings: StateFlow<AppSettings> = repository.settings

    private val _currentTab = MutableStateFlow(NavTab.SEARCH)
    val currentTab: StateFlow<NavTab> = _currentTab.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<SteamAppSummary>>(repository.featuredGames)
    val searchResults: StateFlow<List<SteamAppSummary>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _selectedApp = MutableStateFlow<SteamAppDetail?>(null)
    val selectedApp: StateFlow<SteamAppDetail?> = _selectedApp.asStateFlow()

    private val _depots = MutableStateFlow<List<SteamDepot>>(emptyList())
    val depots: StateFlow<List<SteamDepot>> = _depots.asStateFlow()

    private val _isLoadingDepots = MutableStateFlow(false)
    val isLoadingDepots: StateFlow<Boolean> = _isLoadingDepots.asStateFlow()

    private val _luaParseResult = MutableStateFlow<ParsedLuaResult?>(null)
    val luaParseResult: StateFlow<ParsedLuaResult?> = _luaParseResult.asStateFlow()

    private var searchDebounceJob: Job? = null

    fun setTab(tab: NavTab) {
        _currentTab.value = tab
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        searchDebounceJob?.cancel()
        searchDebounceJob = viewModelScope.launch {
            delay(300)
            performSearch(query)
        }
    }

    private suspend fun performSearch(query: String) {
        _isSearching.value = true
        try {
            val results = repository.searchGames(query)
            _searchResults.value = results
        } finally {
            _isSearching.value = false
        }
    }

    fun selectApp(summary: SteamAppSummary) {
        viewModelScope.launch {
            _isLoadingDepots.value = true
            _selectedApp.value = SteamAppDetail(
                appId = summary.appId,
                name = summary.name,
                headerImage = summary.tinyImage.replace("capsule_231x87", "header")
            )
            val detail = repository.getAppDetails(summary.appId)
            if (detail != null) {
                _selectedApp.value = detail
            }
            val loadedDepots = repository.getDepots(summary.appId)
            _depots.value = loadedDepots
            _isLoadingDepots.value = false
        }
    }

    fun clearSelectedApp() {
        _selectedApp.value = null
        _depots.value = emptyList()
    }

    fun toggleDepotSelection(depotId: Long) {
        _depots.update { list ->
            list.map { if (it.depotId == depotId) it.copy(isSelected = !it.isSelected) else it }
        }
    }

    fun selectAllDepots(selectAll: Boolean) {
        _depots.update { list ->
            list.map { it.copy(isSelected = selectAll) }
        }
    }

    fun autoSelectLikeSteam() {
        _depots.update { list ->
            val targetOs = settings.value.targetPlatform.lowercase()
            val targetLang = settings.value.gameLanguage.lowercase()
            list.map { d ->
                val os = d.oslist?.lowercase() ?: ""
                val lang = d.language?.lowercase() ?: ""
                var keep = true
                if (os.isNotBlank() && targetOs != "all" && targetOs != "auto" && !os.contains(targetOs)) {
                    keep = false
                }
                if (lang.isNotBlank() && lang != "all" && lang != targetLang) {
                    keep = false
                }
                d.copy(isSelected = keep)
            }
        }
    }

    fun startDownloadCurrentApp() {
        val app = _selectedApp.value ?: return
        val selected = _depots.value.filter { it.isSelected }
        if (selected.isEmpty()) return

        downloadManager.startDownload(
            appId = app.appId,
            gameName = app.name,
            headerImage = app.headerImage,
            selectedDepots = selected,
            settings = settings.value
        )
        _currentTab.value = NavTab.DOWNLOADS
    }

    fun parseLuaCode(code: String) {
        val res = repository.parseLuaString(code)
        _luaParseResult.value = res
        if (res.mainAppId != null) {
            viewModelScope.launch {
                val detail = repository.getAppDetails(res.mainAppId)
                if (detail != null) {
                    _selectedApp.value = detail
                } else {
                    _selectedApp.value = SteamAppDetail(
                        appId = res.mainAppId,
                        name = "Steam App ${res.mainAppId}"
                    )
                }
                _depots.value = res.depots.map { it.copy(isSelected = true) }
            }
        }
    }

    fun parseStBytes(bytes: ByteArray) {
        try {
            val res = repository.parseStBytes(bytes)
            _luaParseResult.value = res
            if (res.mainAppId != null) {
                viewModelScope.launch {
                    val detail = repository.getAppDetails(res.mainAppId)
                    _selectedApp.value = detail ?: SteamAppDetail(
                        appId = res.mainAppId,
                        name = "Steam App ${res.mainAppId}"
                    )
                    _depots.value = res.depots.map { it.copy(isSelected = true) }
                }
            }
        } catch (e: Exception) {
            // Error handling
        }
    }

    fun updateSettings(newSettings: AppSettings) {
        repository.updateSettings(newSettings)
    }

    fun clearHistory() {
        downloadManager.clearHistory()
    }

    fun removeHistoryItem(id: String) {
        downloadManager.removeHistoryItem(id)
    }

    fun resumeDownloadFromHistory(entry: HistoryEntry) {
        viewModelScope.launch {
            val loadedDepots = repository.getDepots(entry.appId)
            downloadManager.startDownload(
                appId = entry.appId,
                gameName = entry.gameName,
                headerImage = entry.headerImage,
                selectedDepots = loadedDepots,
                settings = settings.value
            )
            _currentTab.value = NavTab.DOWNLOADS
        }
    }
}
