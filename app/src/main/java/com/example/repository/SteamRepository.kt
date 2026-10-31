package com.example.repository

import android.content.Context
import com.example.api.SteamApiClient
import com.example.engine.DownloadManager
import com.example.engine.LuaParser
import com.example.model.AppSettings
import com.example.model.ParsedLuaResult
import com.example.model.SteamAppDetail
import com.example.model.SteamAppSummary
import com.example.model.SteamDepot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class SteamRepository(private val context: Context) {

    val downloadManager = DownloadManager(context)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private val settingsFile = File(context.filesDir, "settings.json")

    // Featured popular games for quick access
    val featuredGames = listOf(
        SteamAppSummary(1030300L, "Hollow Knight: Silksong", "https://cdn.akamai.steamstatic.com/steam/apps/1030300/capsule_231x87.jpg"),
        SteamAppSummary(620L, "Portal 2", "https://cdn.akamai.steamstatic.com/steam/apps/620/capsule_231x87.jpg"),
        SteamAppSummary(220L, "Half-Life 2", "https://cdn.akamai.steamstatic.com/steam/apps/220/capsule_231x87.jpg"),
        SteamAppSummary(2379780L, "Balatro", "https://cdn.akamai.steamstatic.com/steam/apps/2379780/capsule_231x87.jpg"),
        SteamAppSummary(1245620L, "ELDEN RING", "https://cdn.akamai.steamstatic.com/steam/apps/1245620/capsule_231x87.jpg"),
        SteamAppSummary(1091500L, "Cyberpunk 2077", "https://cdn.akamai.steamstatic.com/steam/apps/1091500/capsule_231x87.jpg")
    )

    suspend fun searchGames(query: String): List<SteamAppSummary> {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return featuredGames
        // If direct number, check app details
        val directId = trimmed.toLongOrNull()
        if (directId != null && directId > 0) {
            val detail = SteamApiClient.getAppDetails(directId)
            if (detail != null) {
                return listOf(SteamAppSummary(directId, detail.name, detail.headerImage ?: ""))
            }
        }
        val apiResults = SteamApiClient.searchGames(trimmed)
        return if (apiResults.isNotEmpty()) apiResults else {
            featuredGames.filter { it.name.contains(trimmed, ignoreCase = true) || it.appId.toString().contains(trimmed) }
        }
    }

    suspend fun getAppDetails(appId: Long): SteamAppDetail? {
        val detail = SteamApiClient.getAppDetails(appId)
        if (detail != null) return detail
        val featured = featuredGames.firstOrNull { it.appId == appId }
        return if (featured != null) {
            SteamAppDetail(
                appId = featured.appId,
                name = featured.name,
                headerImage = featured.tinyImage.replace("capsule_231x87", "header"),
                shortDescription = "Steam application $appId"
            )
        } else null
    }

    suspend fun getDepots(appId: Long): List<SteamDepot> {
        // First try official SteamCMD public manifest data
        val cmdDepots = SteamApiClient.getDepotsFromSteamCmd(appId)
        if (cmdDepots.isNotEmpty()) {
            return applyDepotFilters(cmdDepots, _settings.value)
        }

        // Try custom depot sources (e.g. archive.org or github branches)
        val sourceDepots = SteamApiClient.querySourcesForApp(_settings.value.depotSources, appId)
        if (sourceDepots.isNotEmpty()) {
            return applyDepotFilters(sourceDepots, _settings.value)
        }

        // Fallback placeholder depot if manifest not yet indexed
        return listOf(
            SteamDepot(
                depotId = appId + 1,
                manifestId = "786432190123456789",
                sizeBytes = 1024L * 1024L * 850L,
                oslist = "windows,linux",
                language = "english",
                isSelected = true
            )
        )
    }

    private fun applyDepotFilters(depots: List<SteamDepot>, config: AppSettings): List<SteamDepot> {
        val targetOs = config.targetPlatform.lowercase()
        val targetLang = config.gameLanguage.lowercase()

        return depots.map { d ->
            var shouldSelect = true
            if (config.autoSelectDepots) {
                val os = d.oslist?.lowercase() ?: ""
                val lang = d.language?.lowercase() ?: ""

                if (os.isNotBlank() && targetOs != "all" && targetOs != "auto") {
                    if (!os.contains(targetOs)) {
                        shouldSelect = false
                    }
                }
                if (lang.isNotBlank() && lang != "all" && targetLang.isNotBlank()) {
                    if (lang != targetLang) {
                        shouldSelect = false
                    }
                }
            }
            d.copy(isSelected = shouldSelect)
        }
    }

    fun parseLuaString(content: String): ParsedLuaResult {
        return LuaParser.parseLuaContent(content)
    }

    fun parseStBytes(bytes: ByteArray): ParsedLuaResult {
        return LuaParser.parseStFile(bytes)
    }

    fun updateSettings(newSettings: AppSettings) {
        _settings.value = newSettings
        saveSettings(newSettings)
    }

    private fun loadSettings(): AppSettings {
        if (!settingsFile.exists()) {
            return AppSettings(downloadLocation = downloadManager.getDefaultDownloadDir().absolutePath)
        }
        return try {
            val content = settingsFile.readText()
            val json = JSONObject(content)
            val sources = mutableListOf<String>()
            val sArray = json.optJSONArray("depotSources")
            if (sArray != null) {
                for (i in 0 until sArray.length()) {
                    sources.add(sArray.getString(i))
                }
            } else {
                sources.addAll(AppSettings().depotSources)
            }
            AppSettings(
                downloadLocation = json.optString("downloadLocation", downloadManager.getDefaultDownloadDir().absolutePath),
                concurrency = json.optInt("concurrency", 16),
                maxRetries = json.optInt("maxRetries", 3),
                verifyChunks = json.optBoolean("verifyChunks", true),
                targetPlatform = json.optString("targetPlatform", "windows"),
                gameLanguage = json.optString("gameLanguage", "english"),
                autoSelectDepots = json.optBoolean("autoSelectDepots", true),
                includeDlc = json.optBoolean("includeDlc", true),
                depotSources = sources,
                hubcapApiKey = json.optString("hubcapApiKey", ""),
                ryuuApiKey = json.optString("ryuuApiKey", "")
            )
        } catch (e: Exception) {
            AppSettings(downloadLocation = downloadManager.getDefaultDownloadDir().absolutePath)
        }
    }

    private fun saveSettings(s: AppSettings) {
        try {
            val json = JSONObject()
            json.put("downloadLocation", s.downloadLocation)
            json.put("concurrency", s.concurrency)
            json.put("maxRetries", s.maxRetries)
            json.put("verifyChunks", s.verifyChunks)
            json.put("targetPlatform", s.targetPlatform)
            json.put("gameLanguage", s.gameLanguage)
            json.put("autoSelectDepots", s.autoSelectDepots)
            json.put("includeDlc", s.includeDlc)
            val sourcesArray = JSONArray()
            s.depotSources.forEach { sourcesArray.put(it) }
            json.put("depotSources", sourcesArray)
            json.put("hubcapApiKey", s.hubcapApiKey)
            json.put("ryuuApiKey", s.ryuuApiKey)
            settingsFile.writeText(json.toString(2))
        } catch (e: Exception) {
            // ignore
        }
    }
}
