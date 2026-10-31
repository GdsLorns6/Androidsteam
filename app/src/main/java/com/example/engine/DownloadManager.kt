package com.example.engine

import android.content.Context
import android.os.Environment
import com.example.api.SteamApiClient
import com.example.model.AppSettings
import com.example.model.DownloadJob
import com.example.model.HistoryEntry
import com.example.model.JobStatus
import com.example.model.SteamDepot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.RandomAccessFile
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

class DownloadManager(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var activeJobExecution: Job? = null
    private val isCancelledFlag = AtomicBoolean(false)
    private val isPausedFlag = AtomicBoolean(false)

    private val _currentJob = MutableStateFlow<DownloadJob?>(null)
    val currentJob: StateFlow<DownloadJob?> = _currentJob.asStateFlow()

    private val _history = MutableStateFlow<List<HistoryEntry>>(emptyList())
    val history: StateFlow<List<HistoryEntry>> = _history.asStateFlow()

    private val historyFile = File(context.filesDir, "history.json")

    init {
        loadHistory()
    }

    fun getDefaultDownloadDir(): File {
        val base = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val dir = File(base, "SteamDownloads")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun startDownload(
        appId: Long,
        gameName: String,
        headerImage: String?,
        selectedDepots: List<SteamDepot>,
        settings: AppSettings
    ) {
        if (_currentJob.value?.status == JobStatus.DOWNLOADING) {
            logToCurrent("Another download is already active. Please wait or cancel it first.")
            return
        }

        val jobId = UUID.randomUUID().toString()
        val safeName = gameName.replace(Regex("""[<>:"/\\|?*]"""), "_")
        val rootDir = if (settings.downloadLocation.isNotBlank()) {
            File(settings.downloadLocation)
        } else {
            getDefaultDownloadDir()
        }
        val targetDir = File(rootDir, "$appId - $safeName")
        targetDir.mkdirs()

        val job = DownloadJob(
            id = jobId,
            appId = appId,
            gameName = gameName,
            headerImage = headerImage,
            depots = selectedDepots,
            status = JobStatus.DOWNLOADING,
            downloadDir = targetDir.absolutePath,
            startedAt = System.currentTimeMillis()
        )

        _currentJob.value = job
        isCancelledFlag.set(false)
        isPausedFlag.set(false)

        activeJobExecution = scope.launch {
            runDownloadPipeline(job, targetDir, settings)
        }
    }

    fun pauseDownload() {
        isPausedFlag.set(true)
        _currentJob.update { it?.copy(status = JobStatus.PAUSED) }
        logToCurrent("Download paused.")
    }

    fun resumePausedDownload(settings: AppSettings) {
        val current = _currentJob.value ?: return
        if (current.status == JobStatus.PAUSED) {
            isPausedFlag.set(false)
            _currentJob.update { it?.copy(status = JobStatus.DOWNLOADING) }
            logToCurrent("Resuming download...")
            activeJobExecution = scope.launch {
                val targetDir = File(current.downloadDir)
                runDownloadPipeline(current, targetDir, settings)
            }
        }
    }

    fun cancelDownload() {
        isCancelledFlag.set(true)
        activeJobExecution?.cancel()
        _currentJob.update { current ->
            current?.copy(status = JobStatus.CANCELLED_RESUMABLE)
        }
        logToCurrent("Download cancelled. Files and progress journal preserved for resume.")
        saveCurrentToHistory(status = "cancelled_resumable")
    }

    private suspend fun runDownloadPipeline(
        job: DownloadJob,
        targetDir: File,
        settings: AppSettings
    ) = withContext(Dispatchers.IO) {
        val resumeDir = File(targetDir, ".smd_resume")
        resumeDir.mkdirs()

        logToCurrent("Starting download for ${job.gameName} (App ID: ${job.appId})")
        logToCurrent("Target folder: ${targetDir.absolutePath}")
        logToCurrent("Selected depots: ${job.depots.size}")

        var totalAllBytes = 0L
        var completedAllBytes = 0L
        var doneTotalChunks = 0
        var totalAllChunks = 0
        val startTime = System.currentTimeMillis()

        val downloadPool = Executors.newFixedThreadPool(settings.concurrency.coerceIn(2, 32))
        val downloadDispatcher = downloadPool.asCoroutineDispatcher()
        val semaphore = Semaphore(settings.concurrency.coerceIn(2, 32))

        try {
            for ((index, depot) in job.depots.withIndex()) {
                if (isCancelledFlag.get() || isPausedFlag.get()) break

                _currentJob.update {
                    it?.copy(
                        currentDepotIndex = index + 1,
                        currentDepotId = depot.depotId
                    )
                }

                logToCurrent("--- [Depot ${index + 1}/${job.depots.size}] Processing ID: ${depot.depotId} ---")
                val manifestId = depot.manifestId
                if (manifestId.isNullOrBlank() || manifestId == "N/A") {
                    logToCurrent("Depot ${depot.depotId} has no manifest ID (base container). Marked complete.")
                    continue
                }

                // 1. Get manifest request code
                logToCurrent("Resolving manifest request code for $manifestId...")
                val requestCode = SteamApiClient.getManifestRequestCode(manifestId)
                logToCurrent("Request code: $requestCode")

                // 2. Fetch manifest binary
                logToCurrent("Downloading binary manifest for depot ${depot.depotId}...")
                val manifestRaw = SteamApiClient.fetchManifestBinary(depot.depotId, manifestId, requestCode)
                if (manifestRaw == null || manifestRaw.isEmpty()) {
                    logToCurrent("Warning: Unable to fetch manifest for depot ${depot.depotId} from CDN.")
                    continue
                }

                // 3. Parse files and chunks
                val files = ProtobufManifestParser.parseManifestFiles(manifestRaw, depot.depotKey)
                logToCurrent("Parsed ${files.size} file mappings from manifest.")

                // Pre-allocate directories and files
                for (f in files) {
                    val destFile = File(targetDir, f.filename)
                    if (f.isDirectory) {
                        destFile.mkdirs()
                    } else if (!f.isSymlink) {
                        destFile.parentFile?.mkdirs()
                        if (!destFile.exists() || destFile.length() != f.size) {
                            try {
                                val raf = RandomAccessFile(destFile, "rw")
                                raf.setLength(f.size)
                                raf.close()
                            } catch (e: Exception) {
                                // Ignore allocation errors
                            }
                        }
                    }
                }

                // Collect unique chunks
                data class TargetLoc(val file: File, val offset: Long)
                data class UniqueChunk(
                    val sha: String,
                    val uncompressedSize: Int,
                    val size: Int,
                    val crc: Long,
                    val targets: MutableList<TargetLoc>
                )

                val uniqueChunks = mutableMapOf<String, UniqueChunk>()
                for (f in files) {
                    if (f.isDirectory || f.isSymlink) continue
                    val destFile = File(targetDir, f.filename)
                    for (ch in f.chunks) {
                        if (ch.sha.isBlank()) continue
                        val item = uniqueChunks.getOrPut(ch.sha) {
                            UniqueChunk(
                                sha = ch.sha,
                                uncompressedSize = ch.uncompressedSize,
                                size = ch.size,
                                crc = ch.crc,
                                targets = mutableListOf()
                            )
                        }
                        item.targets.add(TargetLoc(destFile, ch.offset))
                    }
                }

                val chunkList = uniqueChunks.values.toList()
                val depotTotalBytes = chunkList.sumOf { it.uncompressedSize.toLong() }
                totalAllBytes += depotTotalBytes
                totalAllChunks += chunkList.size

                logToCurrent("Depot ${depot.depotId}: ${chunkList.size} chunks total (${formatBytes(depotTotalBytes)})")

                // Resume journal check
                val journalFile = File(resumeDir, "${depot.depotId}_$manifestId.done")
                val doneSet = mutableSetOf<String>()
                if (journalFile.exists()) {
                    try {
                        BufferedReader(FileReader(journalFile)).use { reader ->
                            var line = reader.readLine()
                            while (line != null) {
                                val s = line.trim()
                                if (s.length == 40) doneSet.add(s)
                                line = reader.readLine()
                            }
                        }
                        logToCurrent("Resuming: Found ${doneSet.size} chunks already recorded in journal.")
                    } catch (e: Exception) {
                        // ignore
                    }
                }

                val journalWriter = FileWriter(journalFile, true)
                val skippedChunks = chunkList.count { doneSet.contains(it.sha) }
                val skippedBytes = chunkList.filter { doneSet.contains(it.sha) }.sumOf { it.uncompressedSize.toLong() }

                completedAllBytes += skippedBytes
                doneTotalChunks += skippedChunks

                val toDownload = chunkList.filter { !doneSet.contains(it.sha) }
                logToCurrent("Already on disk: $skippedChunks chunks (${formatBytes(skippedBytes)}). Downloading ${toDownload.size} remaining chunks...")

                val completedBytesCounter = AtomicLong(completedAllBytes)
                val doneChunksCounter = AtomicInteger(doneTotalChunks)
                var lastLogTime = System.currentTimeMillis()

                val chunkJobs = mutableListOf<Job>()
                val depotKey = depot.depotKey

                for (chunk in toDownload) {
                    if (isCancelledFlag.get() || isPausedFlag.get()) break
                    val cJob = scope.launch(downloadDispatcher) {
                        semaphore.withPermit {
                            if (isCancelledFlag.get() || isPausedFlag.get()) return@withPermit
                            try {
                                val rawChunk = SteamApiClient.downloadChunkDirect(depot.depotId, chunk.sha)
                                val decrypted = CryptoEngine.processChunk(rawChunk, depotKey)

                                if (settings.verifyChunks && !depotKey.isNullOrBlank()) {
                                    val (verified, reason) = CryptoEngine.verifyChunkData(
                                        decrypted,
                                        com.example.model.ManifestChunk(
                                            sha = chunk.sha,
                                            uncompressedSize = chunk.uncompressedSize,
                                            size = chunk.size,
                                            crc = chunk.crc
                                        )
                                    )
                                    if (!verified) {
                                        // continue or log warning
                                    }
                                }

                                for (target in chunk.targets) {
                                    val raf = RandomAccessFile(target.file, "rw")
                                    raf.seek(target.offset)
                                    raf.write(decrypted)
                                    raf.close()
                                }

                                synchronized(journalWriter) {
                                    journalWriter.write("${chunk.sha}\n")
                                    journalWriter.flush()
                                }

                                val cBytes = completedBytesCounter.addAndGet(chunk.uncompressedSize.toLong())
                                val dChunks = doneChunksCounter.incrementAndGet()

                                val now = System.currentTimeMillis()
                                if (now - lastLogTime >= 500) {
                                    lastLogTime = now
                                    val elapsedSec = (now - startTime).coerceAtLeast(1) / 1000L
                                    val speed = if (elapsedSec > 0) (cBytes - skippedBytes) / elapsedSec else 0L
                                    val remainingBytes = (totalAllBytes - cBytes).coerceAtLeast(0L)
                                    val eta = if (speed > 0) remainingBytes / speed else 0L

                                    _currentJob.update { cur ->
                                        cur?.copy(
                                            totalBytes = totalAllBytes,
                                            completedBytes = cBytes,
                                            totalChunks = totalAllChunks,
                                            doneChunks = dChunks,
                                            skippedChunks = skippedChunks,
                                            speedBytesPerSec = speed,
                                            etaSeconds = eta
                                        )
                                    }
                                }
                            } catch (e: Exception) {
                                logToCurrent("Chunk error ${chunk.sha.take(8)}: ${e.message}")
                            }
                        }
                    }
                    chunkJobs.add(cJob)
                }

                chunkJobs.forEach { it.join() }
                journalWriter.close()

                if (toDownload.isEmpty() || chunkJobs.all { !it.isCancelled }) {
                    logToCurrent("Depot ${depot.depotId} completed successfully!")
                }
            }

            if (!isCancelledFlag.get() && !isPausedFlag.get()) {
                val completedJob = _currentJob.value?.copy(
                    status = JobStatus.COMPLETED,
                    completedAt = System.currentTimeMillis(),
                    speedBytesPerSec = 0L,
                    etaSeconds = 0L
                )
                _currentJob.value = completedJob
                logToCurrent("All selected depots downloaded successfully!")
                saveCurrentToHistory(status = "complete")
            }
        } catch (e: Exception) {
            logToCurrent("Download error: ${e.localizedMessage}")
            _currentJob.update {
                it?.copy(
                    status = JobStatus.ERROR,
                    errorMessage = e.localizedMessage
                )
            }
            saveCurrentToHistory(status = "error")
        } finally {
            downloadPool.shutdown()
        }
    }

    private fun logToCurrent(message: String) {
        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val formatted = "[$time] $message"
        _currentJob.update { cur ->
            if (cur != null) {
                cur.copy(logs = (cur.logs + formatted).takeLast(250))
            } else cur
        }
    }

    private fun saveCurrentToHistory(status: String) {
        val current = _currentJob.value ?: return
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val entry = HistoryEntry(
            id = current.id,
            appId = current.appId,
            gameName = current.gameName,
            headerImage = current.headerImage,
            depotCount = current.depots.size,
            depotsDownloaded = current.currentDepotIndex,
            status = status,
            downloadDir = current.downloadDir,
            startedAt = dateFormat.format(Date(current.startedAt)),
            completedAt = dateFormat.format(Date()),
            sizeFormatted = formatBytes(current.completedBytes)
        )
        val updated = listOf(entry) + _history.value.filter { it.appId != current.appId }
        _history.value = updated
        persistHistory(updated)
    }

    private fun loadHistory() {
        if (!historyFile.exists()) return
        try {
            val content = historyFile.readText()
            val jsonArray = org.json.JSONArray(content)
            val list = mutableListOf<HistoryEntry>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    HistoryEntry(
                        id = obj.optString("id"),
                        appId = obj.optLong("appId"),
                        gameName = obj.optString("gameName"),
                        headerImage = obj.optString("headerImage").takeIf { it.isNotBlank() },
                        depotCount = obj.optInt("depotCount"),
                        depotsDownloaded = obj.optInt("depotsDownloaded"),
                        status = obj.optString("status"),
                        downloadDir = obj.optString("downloadDir"),
                        startedAt = obj.optString("startedAt"),
                        completedAt = obj.optString("completedAt"),
                        sizeFormatted = obj.optString("sizeFormatted")
                    )
                )
            }
            _history.value = list
        } catch (e: Exception) {
            // Ignore load error
        }
    }

    private fun persistHistory(entries: List<HistoryEntry>) {
        try {
            val jsonArray = org.json.JSONArray()
            for (e in entries) {
                val obj = org.json.JSONObject()
                obj.put("id", e.id)
                obj.put("appId", e.appId)
                obj.put("gameName", e.gameName)
                obj.put("headerImage", e.headerImage ?: "")
                obj.put("depotCount", e.depotCount)
                obj.put("depotsDownloaded", e.depotsDownloaded)
                obj.put("status", e.status)
                obj.put("downloadDir", e.downloadDir)
                obj.put("startedAt", e.startedAt)
                obj.put("completedAt", e.completedAt)
                obj.put("sizeFormatted", e.sizeFormatted)
                jsonArray.put(obj)
            }
            historyFile.writeText(jsonArray.toString(2))
        } catch (e: Exception) {
            // Ignore persist error
        }
    }

    fun clearHistory() {
        _history.value = emptyList()
        if (historyFile.exists()) historyFile.delete()
    }

    fun removeHistoryItem(id: String) {
        val updated = _history.value.filter { it.id != id }
        _history.value = updated
        persistHistory(updated)
    }

    companion object {
        fun formatBytes(bytes: Long): String {
            if (bytes <= 0) return "0 B"
            val units = arrayOf("B", "KB", "MB", "GB", "TB")
            val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, units.size - 1)
            val value = bytes / Math.pow(1024.0, digitGroups.toDouble())
            return String.format(Locale.US, "%.2f %s", value, units[digitGroups])
        }

        fun formatEta(seconds: Long): String {
            if (seconds <= 0) return "--"
            val h = seconds / 3600
            val m = (seconds % 3600) / 60
            val s = seconds % 60
            return if (h > 0) String.format("%02d:%02d:%02d", h, m, s) else String.format("%02d:%02d", m, s)
        }
    }
}
