package com.example.engine

import com.example.model.ParsedLuaResult
import com.example.model.SteamDepot
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.regex.Pattern
import java.util.zip.InflaterInputStream

object LuaParser {

    private val ADD_APPID_PATTERN = Pattern.compile(
        """(?i)addappid\((\d+)(?:\s*,\s*(\d+)\s*,\s*"([a-f0-9]+)")?\)"""
    )
    private val SET_MANIFEST_PATTERN = Pattern.compile(
        """(?i)setManifestid\((\d+)\s*,\s*"(\d+)"(?:\s*,\s*(\d+))?[^)]*\)"""
    )
    private val VDF_KEY_PATTERN = Pattern.compile(
        """"(\d+)"\s*\{[^}]*?"DecryptionKey"\s*"([0-9a-fA-F]+)"""",
        Pattern.DOTALL
    )

    fun parseLuaContent(content: String): ParsedLuaResult {
        val depotMap = mutableMapOf<Long, SteamDepot>()
        var mainAppId: Long? = null
        val allAppIds = mutableListOf<Long>()

        val addMatcher = ADD_APPID_PATTERN.matcher(content)
        while (addMatcher.find()) {
            val appId = addMatcher.group(1)?.toLongOrNull() ?: continue
            val key = addMatcher.group(3)

            if (!allAppIds.contains(appId)) {
                allAppIds.add(appId)
            }

            if (key.isNullOrBlank()) {
                if (mainAppId == null) {
                    mainAppId = appId
                }
            } else {
                val existing = depotMap[appId]
                depotMap[appId] = existing?.copy(depotKey = key)
                    ?: SteamDepot(depotId = appId, depotKey = key)
            }
        }

        val manifestMatcher = SET_MANIFEST_PATTERN.matcher(content)
        while (manifestMatcher.find()) {
            val depotId = manifestMatcher.group(1)?.toLongOrNull() ?: continue
            val manifestId = manifestMatcher.group(2)
            val sizeBytes = manifestMatcher.group(3)?.toLongOrNull()

            val existing = depotMap[depotId]
            if (existing != null) {
                depotMap[depotId] = existing.copy(
                    manifestId = manifestId,
                    sizeBytes = sizeBytes ?: existing.sizeBytes
                )
            } else {
                depotMap[depotId] = SteamDepot(
                    depotId = depotId,
                    manifestId = manifestId,
                    sizeBytes = sizeBytes
                )
            }
        }

        if (mainAppId == null && allAppIds.isNotEmpty()) {
            mainAppId = allAppIds.first()
        }

        return ParsedLuaResult(
            mainAppId = mainAppId,
            depots = depotMap.values.toList(),
            allAppIds = allAppIds
        )
    }

    fun parseStFile(buffer: ByteArray): ParsedLuaResult {
        if (buffer.size < 12) {
            throw IllegalArgumentException("File size (${buffer.size}) too small for .st format")
        }

        val byteBuffer = ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN)
        val xorKeyRaw = byteBuffer.int
        val size = byteBuffer.int

        val xorKey = (((xorKeyRaw xor -88888) and 0xFF).toByte()) // 0xFFFEA4C8 = -88888 signed 32-bit int
        val end = (12 + size).coerceAtMost(buffer.size)
        val decrypted = ByteArray(end - 12)
        for (i in 12 until end) {
            decrypted[i - 12] = (buffer[i].toInt() xor xorKey.toInt()).toByte()
        }

        val inflater = InflaterInputStream(ByteArrayInputStream(decrypted))
        val decompressed = ByteArrayOutputStream()
        val temp = ByteArray(4096)
        var read: Int
        while (inflater.read(temp).also { read = it } != -1) {
            decompressed.write(temp, 0, read)
        }
        inflater.close()

        val decompressedBytes = decompressed.toByteArray()
        if (decompressedBytes.size <= 512) {
            throw IllegalArgumentException(".st decompressed payload is too small")
        }

        // Skip 512-byte header to get the Lua script
        val luaText = String(decompressedBytes, 512, decompressedBytes.size - 512, Charsets.UTF_8)
        return parseLuaContent(luaText)
    }

    fun parseVdfKeys(text: String): Map<Long, String> {
        val keys = mutableMapOf<Long, String>()
        val matcher = VDF_KEY_PATTERN.matcher(text)
        while (matcher.find()) {
            val id = matcher.group(1)?.toLongOrNull() ?: continue
            val key = matcher.group(2) ?: continue
            keys[id] = key
        }
        return keys
    }
}
