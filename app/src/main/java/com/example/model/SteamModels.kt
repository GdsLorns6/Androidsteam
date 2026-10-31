package com.example.model

data class SteamAppSummary(
    val appId: Long,
    val name: String,
    val tinyImage: String = ""
)

data class SteamAppDetail(
    val appId: Long,
    val name: String,
    val headerImage: String? = null,
    val shortDescription: String? = null,
    val type: String? = null
)

data class SteamDepot(
    val depotId: Long,
    val manifestId: String? = null,
    val depotKey: String? = null,
    val sizeBytes: Long? = null,
    val downloadBytes: Long? = null,
    val oslist: String? = null,
    val osarch: String? = null,
    val language: String? = null,
    val isSelected: Boolean = true
)

data class ParsedLuaResult(
    val mainAppId: Long?,
    val depots: List<SteamDepot>,
    val allAppIds: List<Long>
)

data class ManifestChunk(
    val sha: String,
    val offset: Long = 0L,
    val uncompressedSize: Int = 0,
    val size: Int = 0,
    val crc: Long = 0L
)

data class ManifestFile(
    val filename: String,
    val size: Long = 0L,
    val flags: Int = 0,
    val linktarget: String? = null,
    val chunks: List<ManifestChunk> = emptyList(),
    val isDirectory: Boolean = false,
    val isSymlink: Boolean = false
)

enum class JobStatus {
    PENDING,
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    CANCELLED_RESUMABLE,
    ERROR
}

data class DownloadJob(
    val id: String,
    val appId: Long,
    val gameName: String,
    val headerImage: String? = null,
    val depots: List<SteamDepot>,
    val status: JobStatus = JobStatus.PENDING,
    val totalBytes: Long = 0L,
    val completedBytes: Long = 0L,
    val networkBytes: Long = 0L,
    val totalChunks: Int = 0,
    val doneChunks: Int = 0,
    val skippedChunks: Int = 0,
    val speedBytesPerSec: Long = 0L,
    val etaSeconds: Long = 0L,
    val currentDepotIndex: Int = 0,
    val currentDepotId: Long = 0L,
    val logs: List<String> = emptyList(),
    val downloadDir: String = "",
    val startedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val errorMessage: String? = null
)

data class HistoryEntry(
    val id: String,
    val appId: Long,
    val gameName: String,
    val headerImage: String? = null,
    val depotCount: Int = 0,
    val depotsDownloaded: Int = 0,
    val status: String = "completed",
    val downloadDir: String = "",
    val startedAt: String = "",
    val completedAt: String = "",
    val sizeFormatted: String = ""
)

data class AppSettings(
    val downloadLocation: String = "",
    val concurrency: Int = 16,
    val maxRetries: Int = 3,
    val verifyChunks: Boolean = true,
    val targetPlatform: String = "windows", // "auto", "windows", "linux", "macos", "android"
    val gameLanguage: String = "english",
    val autoSelectDepots: Boolean = true,
    val includeDlc: Boolean = true,
    val depotSources: List<String> = listOf(
        "https://archive.org/download/manifest-hub-repo/NEW-depot-keys.zip/",
        "https://archive.org/download/manifest-hub-repo/branches.zip/"
    ),
    val hubcapApiKey: String = "",
    val ryuuApiKey: String = ""
)
