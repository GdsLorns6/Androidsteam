package com.example.engine

import com.example.model.ManifestChunk
import com.example.model.ManifestFile
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.GZIPInputStream
import java.util.zip.ZipInputStream

object ProtobufManifestParser {

    private const val PAYLOAD_MAGIC = 0x71F617D0

    private fun readVarint(buf: ByteArray, startPos: Int): Pair<Long, Int> {
        var pos = startPos
        var result = 0L
        var shift = 0
        while (pos < buf.size) {
            val b = buf[pos++].toLong() and 0xFFL
            result = result or ((b and 0x7FL) shl shift)
            if ((b and 0x80L) == 0L) break
            shift += 7
        }
        return result to pos
    }

    fun parseManifestFiles(rawBytes: ByteArray, depotKeyHex: String? = null): List<ManifestFile> {
        var entry = rawBytes

        // Check if ZIP
        if (entry.size >= 4 && entry[0] == 0x50.toByte() && entry[1] == 0x4B.toByte() &&
            entry[2] == 0x03.toByte() && entry[3] == 0x04.toByte()
        ) {
            try {
                ZipInputStream(ByteArrayInputStream(entry)).use { zis ->
                    val zipEntry = zis.nextEntry
                    if (zipEntry != null) {
                        val buffer = ByteArray(8192)
                        val out = ByteArrayOutputStream()
                        var read: Int
                        while (zis.read(buffer).also { read = it } != -1) {
                            out.write(buffer, 0, read)
                        }
                        entry = out.toByteArray()
                    }
                }
            } catch (e: Exception) {
                // Ignore fallback to raw
            }
        } else if (entry.size >= 2 && (entry[0].toInt() and 0xFF) == 0x1F && (entry[1].toInt() and 0xFF) == 0x8B) {
            try {
                GZIPInputStream(ByteArrayInputStream(entry)).use { gis ->
                    val buffer = ByteArray(8192)
                    val out = ByteArrayOutputStream()
                    var read: Int
                    while (gis.read(buffer).also { read = it } != -1) {
                        out.write(buffer, 0, read)
                    }
                    entry = out.toByteArray()
                }
            } catch (e: Exception) {
                // Ignore fallback
            }
        }

        var payload = entry
        if (entry.size >= 8) {
            val buf = ByteBuffer.wrap(entry).order(ByteOrder.LITTLE_ENDIAN)
            val magic = buf.int
            val length = buf.int
            if (magic == PAYLOAD_MAGIC && entry.size >= 8 + length) {
                payload = entry.copyOfRange(8, 8 + length)
            } else {
                // Search for magic bytes
                val magicBytes = byteArrayOf(0xd0.toByte(), 0x17.toByte(), 0xf6.toByte(), 0x71.toByte())
                val idx = indexOf(entry, magicBytes)
                if (idx != -1 && entry.size >= idx + 8) {
                    val searchBuf = ByteBuffer.wrap(entry, idx + 4, 4).order(ByteOrder.LITTLE_ENDIAN)
                    val sLength = searchBuf.int
                    if (entry.size >= idx + 8 + sLength) {
                        payload = entry.copyOfRange(idx + 8, idx + 8 + sLength)
                    }
                }
            }
        }

        val keyBytes = if (!depotKeyHex.isNullOrBlank() && depotKeyHex.length == 64) {
            try { CryptoEngine.hexToBytes(depotKeyHex) } catch (e: Exception) { null }
        } else null

        val files = mutableListOf<ManifestFile>()
        var pos = 0

        while (pos < payload.size) {
            val (tag, nextPos) = readVarint(payload, pos)
            pos = nextPos
            val fieldNumber = (tag ushr 3).toInt()
            val wireType = (tag and 0x07L).toInt()

            when (wireType) {
                0 -> {
                    val (_, p) = readVarint(payload, pos)
                    pos = p
                }
                1 -> pos += 8
                5 -> pos += 4
                2 -> {
                    val (lenL, p) = readVarint(payload, pos)
                    pos = p
                    val length = lenL.toInt()
                    val endPos = (pos + length).coerceAtMost(payload.size)
                    val fieldBytes = payload.copyOfRange(pos, endPos)
                    pos = endPos

                    if (fieldNumber == 1) { // FileMapping
                        var fpos = 0
                        var filename = ""
                        var fileSize = 0L
                        var fileFlags = 0
                        var linkTarget: String? = null
                        val chunks = mutableListOf<ManifestChunk>()

                        while (fpos < fieldBytes.size) {
                            val (fTag, nextFPos) = readVarint(fieldBytes, fpos)
                            fpos = nextFPos
                            val fNum = (fTag ushr 3).toInt()
                            val fWt = (fTag and 0x07L).toInt()

                            when (fWt) {
                                0 -> {
                                    val (v, p) = readVarint(fieldBytes, fpos)
                                    fpos = p
                                    if (fNum == 2) fileSize = v
                                    else if (fNum == 3) fileFlags = v.toInt()
                                }
                                1 -> fpos += 8
                                5 -> fpos += 4
                                2 -> {
                                    val (flenL, fp) = readVarint(fieldBytes, fpos)
                                    fpos = fp
                                    val flen = flenL.toInt()
                                    val fend = (fpos + flen).coerceAtMost(fieldBytes.size)
                                    val fval = fieldBytes.copyOfRange(fpos, fend)
                                    fpos = fend

                                    when (fNum) {
                                        1 -> { // Filename
                                            filename = CryptoEngine.decryptSteamFilename(fval, keyBytes)
                                                .replace('\\', '/')
                                                .trim('/')
                                        }
                                        7 -> { // Link target
                                            linkTarget = CryptoEngine.decryptSteamFilename(fval, keyBytes)
                                        }
                                        6 -> { // ChunkData
                                            var cpos = 0
                                            var chunkSha = ""
                                            var chunkOffset = 0L
                                            var chunkUncomp = 0
                                            var chunkSize = 0
                                            var chunkCrc = 0L

                                            while (cpos < fval.size) {
                                                val (cTag, nextCPos) = readVarint(fval, cpos)
                                                cpos = nextCPos
                                                val cNum = (cTag ushr 3).toInt()
                                                val cWt = (cTag and 0x07L).toInt()

                                                when (cWt) {
                                                    0 -> {
                                                        val (cv, cp) = readVarint(fval, cpos)
                                                        cpos = cp
                                                        when (cNum) {
                                                            3 -> chunkOffset = cv
                                                            4 -> chunkUncomp = cv.toInt()
                                                            5 -> chunkSize = cv.toInt()
                                                        }
                                                    }
                                                    1 -> cpos += 8
                                                    5 -> {
                                                        if (cNum == 2 && cpos + 4 <= fval.size) {
                                                            val crcBuf = ByteBuffer.wrap(fval, cpos, 4).order(ByteOrder.LITTLE_ENDIAN)
                                                            chunkCrc = crcBuf.int.toLong() and 0xFFFFFFFFL
                                                        }
                                                        cpos += 4
                                                    }
                                                    2 -> {
                                                        val (clenL, cp) = readVarint(fval, cpos)
                                                        cpos = cp
                                                        val clen = clenL.toInt()
                                                        val cend = (cpos + clen).coerceAtMost(fval.size)
                                                        val cv = fval.copyOfRange(cpos, cend)
                                                        cpos = cend
                                                        if (cNum == 1) {
                                                            chunkSha = CryptoEngine.bytesToHex(cv)
                                                        }
                                                    }
                                                    else -> break
                                                }
                                            }

                                            if (chunkSha.isNotEmpty()) {
                                                chunks.add(
                                                    ManifestChunk(
                                                        sha = chunkSha,
                                                        offset = chunkOffset,
                                                        uncompressedSize = chunkUncomp,
                                                        size = chunkSize,
                                                        crc = chunkCrc
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                                else -> break
                            }
                        }

                        val isDirectory = (fileFlags and 64) != 0
                        val isSymlink = (fileFlags and 32) != 0

                        if (filename.isNotBlank()) {
                            files.add(
                                ManifestFile(
                                    filename = filename,
                                    size = fileSize,
                                    flags = fileFlags,
                                    linktarget = linkTarget,
                                    chunks = chunks,
                                    isDirectory = isDirectory,
                                    isSymlink = isSymlink
                                )
                            )
                        }
                    }
                }
                else -> break
            }
        }

        return files
    }

    private fun indexOf(source: ByteArray, target: ByteArray): Int {
        if (target.isEmpty() || source.size < target.size) return -1
        for (i in 0..source.size - target.size) {
            var found = true
            for (j in target.indices) {
                if (source[i + j] != target[j]) {
                    found = false
                    break
                }
            }
            if (found) return i
        }
        return -1
    }
}
