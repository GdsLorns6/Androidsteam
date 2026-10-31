package com.example.api

import com.example.engine.LuaParser
import com.example.model.SteamAppDetail
import com.example.model.SteamAppSummary
import com.example.model.SteamDepot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

object SteamApiClient {

    private val client = OkHttpClient.Builder()
        .connectionPool(ConnectionPool(32, 5, TimeUnit.MINUTES))
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val CDN_HOSTS = listOf(
        "steampipe.akamaized.net",
        "cdn.steamcontent.com"
    )

    private val MANIFEST_CODE_URLS = listOf(
        "https://manifest.steam.run/api/manifest/%s",
        "http://gmrc.wudrm.com/manifest/%s"
    )

    suspend fun searchGames(query: String): List<SteamAppSummary> = withContext(Dispatchers.IO) {
        val encoded = URLEncoder.encode(query.trim(), "UTF-8")
        val url = "https://store.steampowered.com/api/storesearch/?term=$encoded&l=english&cc=US"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "SteamManifestDownloader/1.5.0 (Android)")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val body = response.body?.string() ?: return@withContext emptyList()
                val json = JSONObject(body)
                val items = json.optJSONArray("items") ?: return@withContext emptyList()
                val list = mutableListOf<SteamAppSummary>()
                for (i in 0 until items.length()) {
                    val obj = items.getJSONObject(i)
                    val id = obj.optLong("id", 0L)
                    val name = obj.optString("name", "Unknown Game")
                    val tinyImage = obj.optString("tiny_image", "")
                    if (id > 0) {
                        list.add(SteamAppSummary(appId = id, name = name, tinyImage = tinyImage))
                    }
                }
                list
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getAppDetails(appId: Long): SteamAppDetail? = withContext(Dispatchers.IO) {
        val url = "https://store.steampowered.com/api/appdetails?appids=$appId"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "SteamManifestDownloader/1.5.0 (Android)")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string() ?: return@withContext null
                val root = JSONObject(body)
                val appObj = root.optJSONObject(appId.toString()) ?: return@withContext null
                if (!appObj.optBoolean("success", false)) return@withContext null
                val data = appObj.optJSONObject("data") ?: return@withContext null
                SteamAppDetail(
                    appId = appId,
                    name = data.optString("name", "App $appId"),
                    headerImage = data.optString("header_image").takeIf { it.isNotBlank() },
                    shortDescription = data.optString("short_description").takeIf { it.isNotBlank() },
                    type = data.optString("type").takeIf { it.isNotBlank() }
                )
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getDepotsFromSteamCmd(appId: Long): List<SteamDepot> = withContext(Dispatchers.IO) {
        val url = "https://api.steamcmd.net/v1/info/$appId"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "SteamManifestDownloader/1.5.0 (Android)")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val body = response.body?.string() ?: return@withContext emptyList()
                val root = JSONObject(body)
                val data = root.optJSONObject("data") ?: return@withContext emptyList()
                val appData = data.optJSONObject(appId.toString()) ?: return@withContext emptyList()
                val depotsObj = appData.optJSONObject("depots") ?: return@withContext emptyList()

                val depots = mutableListOf<SteamDepot>()
                val keys = depotsObj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    val depotId = k.toLongOrNull() ?: continue
                    val depotItem = depotsObj.optJSONObject(k) ?: continue

                    val config = depotItem.optJSONObject("config")
                    val oslist = config?.optString("oslist")
                    val osarch = config?.optString("osarch")
                    val language = config?.optString("language")

                    val manifests = depotItem.optJSONObject("manifests")
                    val publicManifest = manifests?.optJSONObject("public")
                    val gid = publicManifest?.optString("gid")
                    val sizeBytes = publicManifest?.optLong("size", 0L).takeIf { it != null && it > 0 }
                    val downloadBytes = publicManifest?.optLong("download", 0L).takeIf { it != null && it > 0 }

                    depots.add(
                        SteamDepot(
                            depotId = depotId,
                            manifestId = gid,
                            sizeBytes = sizeBytes,
                            downloadBytes = downloadBytes,
                            oslist = oslist,
                            osarch = osarch,
                            language = language,
                            isSelected = true
                        )
                    )
                }
                depots.sortedBy { it.depotId }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getManifestRequestCode(manifestGid: String): Long = withContext(Dispatchers.IO) {
        for (template in MANIFEST_CODE_URLS) {
            try {
                val url = String.format(template, manifestGid)
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Valve/Steam HTTP Client 1.0")
                    .build()
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val text = response.body?.string()?.trim() ?: ""
                        if (text.isNotBlank()) {
                            try {
                                val json = JSONObject(text)
                                val code = json.optLong("content", 0L)
                                    .takeIf { it != 0L }
                                    ?: json.optLong("manifest_request_code", 0L)
                                if (code != 0L) return@withContext code
                            } catch (e: Exception) {
                                val direct = text.toLongOrNull()
                                if (direct != null && direct != 0L) return@withContext direct
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // try next
            }
        }
        0L
    }

    suspend fun fetchManifestBinary(depotId: Long, manifestGid: String, requestCode: Long): ByteArray? =
        withContext(Dispatchers.IO) {
            val urls = mutableListOf<String>()
            if (requestCode > 0) {
                for (host in CDN_HOSTS) {
                    urls.add("https://$host/depot/$depotId/manifest/$manifestGid/5/$requestCode")
                }
            }
            for (host in CDN_HOSTS) {
                urls.add("https://$host/depot/$depotId/manifest/$manifestGid/5")
            }

            for (url in urls) {
                try {
                    val request = Request.Builder()
                        .url(url)
                        .header("User-Agent", "Valve/Steam HTTP Client 1.0")
                        .header("Accept", "*/*")
                        .build()
                    client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            val bytes = response.body?.bytes()
                            if (bytes != null && bytes.isNotEmpty()) {
                                return@withContext bytes
                            }
                        }
                    }
                } catch (e: Exception) {
                    // try next CDN url
                }
            }
            null
        }

    fun downloadChunkDirect(depotId: Long, chunkSha: String): ByteArray {
        var lastException: Exception? = null
        for (host in CDN_HOSTS) {
            val url = "https://$host/depot/$depotId/chunk/$chunkSha"
            try {
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Valve/Steam HTTP Client 1.0")
                    .header("Accept", "*/*")
                    .build()
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val bytes = response.body?.bytes()
                        if (bytes != null && bytes.isNotEmpty()) {
                            return bytes
                        }
                    }
                }
            } catch (e: Exception) {
                lastException = e
            }
        }
        throw lastException ?: IOException("Could not download chunk $chunkSha for depot $depotId")
    }

    suspend fun querySourcesForApp(sources: List<String>, appId: Long): List<SteamDepot> =
        withContext(Dispatchers.IO) {
            for (source in sources) {
                try {
                    val trimmed = source.trim().trimEnd('/')
                    val luaUrl = "$trimmed/$appId/$appId.lua"
                    val req = Request.Builder().url(luaUrl).build()
                    val luaResp = client.newCall(req).execute()
                    if (luaResp.isSuccessful) {
                        val luaContent = luaResp.body?.string() ?: ""
                        luaResp.close()
                        val parsed = LuaParser.parseLuaContent(luaContent)
                        if (parsed.depots.isNotEmpty()) {
                            return@withContext parsed.depots
                        }
                    } else {
                        luaResp.close()
                    }
                } catch (e: Exception) {
                    // continue
                }
            }
            emptyList()
        }
}
