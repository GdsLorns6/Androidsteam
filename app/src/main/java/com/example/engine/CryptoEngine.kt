package com.example.engine

import com.example.model.ManifestChunk
import org.tukaani.xz.LZMAInputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import java.util.zip.Adler32
import java.util.zip.CRC32
import java.util.zip.InflaterInputStream
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

object CryptoEngine {

    fun hexToBytes(hex: String): ByteArray {
        val clean = hex.trim()
        val len = clean.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(clean[i], 16) shl 4) +
                    Character.digit(clean[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }

    fun bytesToHex(bytes: ByteArray): String {
        val sb = StringBuilder(bytes.size * 2)
        for (b in bytes) {
            sb.append(String.format("%02x", b.toInt() and 0xFF))
        }
        return sb.toString()
    }

    fun aesEcbDecrypt(keyBytes: ByteArray, data: ByteArray): ByteArray {
        val keySpec = SecretKeySpec(keyBytes, "AES")
        val cipher = Cipher.getInstance("AES/ECB/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, keySpec)
        return cipher.doFinal(data)
    }

    fun aesCbcDecrypt(keyBytes: ByteArray, iv: ByteArray, data: ByteArray, withPadding: Boolean = true): ByteArray {
        val keySpec = SecretKeySpec(keyBytes, "AES")
        val ivSpec = IvParameterSpec(iv)
        val transformation = if (withPadding) "AES/CBC/PKCS5Padding" else "AES/CBC/NoPadding"
        val cipher = Cipher.getInstance(transformation)
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)
        return cipher.doFinal(data)
    }

    @OptIn(ExperimentalEncodingApi::class)
    fun decryptSteamFilename(rawBytes: ByteArray, keyBytes: ByteArray?): String {
        if (keyBytes == null || keyBytes.size != 32) {
            return String(rawBytes, Charsets.UTF_8).trim()
        }
        return try {
            val text = String(rawBytes, Charsets.UTF_8).trim()
            val cleanBase64 = try {
                Base64.decode(text)
            } catch (e: Exception) {
                null
            }

            if (cleanBase64 == null || cleanBase64.size < 32 || cleanBase64.size % 16 != 0) {
                return text
            }

            // Decrypt IV (first 16 bytes) with AES-256-ECB
            val ivBlock = cleanBase64.copyOfRange(0, 16)
            val iv = aesEcbDecrypt(keyBytes, ivBlock)

            // Decrypt body with AES-256-CBC
            val body = cleanBase64.copyOfRange(16, cleanBase64.size)
            val decrypted = aesCbcDecrypt(keyBytes, iv, body, withPadding = false)

            val nullIdx = decrypted.indexOf(0.toByte())
            if (nullIdx >= 0) {
                String(decrypted, 0, nullIdx, Charsets.UTF_8)
            } else {
                String(decrypted, Charsets.UTF_8).trim()
            }
        } catch (e: Exception) {
            String(rawBytes, Charsets.UTF_8).trim()
        }
    }

    fun processChunk(rawChunk: ByteArray, depotKeyHex: String?): ByteArray {
        if (depotKeyHex.isNullOrBlank() || depotKeyHex.length != 64 || rawChunk.size < 16) {
            return rawChunk
        }

        return try {
            val keyBytes = hexToBytes(depotKeyHex)

            // 1. Decrypt IV with AES-256-ECB
            val ivBlock = rawChunk.copyOfRange(0, 16)
            val iv = aesEcbDecrypt(keyBytes, ivBlock)

            // 2. Decrypt body with AES-256-CBC
            val body = rawChunk.copyOfRange(16, rawChunk.size)
            val decrypted = aesCbcDecrypt(keyBytes, iv, body, withPadding = true)

            // 3. Decompress if LZMA (starts with "VZa") or zlib (0x78 0x9c)
            if (decrypted.size >= 16 &&
                decrypted[0] == 'V'.code.toByte() &&
                decrypted[1] == 'Z'.code.toByte() &&
                decrypted[2] == 'a'.code.toByte()
            ) {
                // Valve Zip LZMA format:
                // header: bytes 7..12 (5 bytes) = LZMA properties
                // uncompressed size is 4 bytes at end - 6 or from manifest
                val props = decrypted.copyOfRange(7, 12)
                val bodyEnd = decrypted.size - 10
                val lzmaBody = decrypted.copyOfRange(12, bodyEnd)

                val buffer = ByteBuffer.wrap(decrypted, bodyEnd, 8).order(ByteOrder.LITTLE_ENDIAN)
                val expectedCrc = buffer.int.toLong() and 0xFFFFFFFFL
                val expectedSize = buffer.int.toLong() and 0xFFFFFFFFL

                val sizeBytes = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong(expectedSize).array()

                val fullLzmaStream = ByteArrayOutputStream(props.size + sizeBytes.size + lzmaBody.size)
                fullLzmaStream.write(props)
                fullLzmaStream.write(sizeBytes)
                fullLzmaStream.write(lzmaBody)

                val lzmaIn = LZMAInputStream(ByteArrayInputStream(fullLzmaStream.toByteArray()))
                val out = ByteArrayOutputStream(expectedSize.toInt().coerceAtLeast(1024))
                val temp = ByteArray(8192)
                var read: Int
                while (lzmaIn.read(temp).also { read = it } != -1) {
                    out.write(temp, 0, read)
                }
                lzmaIn.close()
                out.toByteArray()
            } else if (decrypted.size >= 4 &&
                (decrypted[0].toInt() and 0xFF) == 0x78 &&
                (decrypted[1].toInt() and 0xFF) == 0x9C
            ) {
                val inflater = InflaterInputStream(ByteArrayInputStream(decrypted))
                val out = ByteArrayOutputStream()
                val temp = ByteArray(8192)
                var read: Int
                while (inflater.read(temp).also { read = it } != -1) {
                    out.write(temp, 0, read)
                }
                inflater.close()
                out.toByteArray()
            } else {
                decrypted
            }
        } catch (e: Exception) {
            rawChunk
        }
    }

    /**
     * Steam Adler-32 begins with a=0 (rather than Java's default a=1)
     */
    fun steamAdler32(data: ByteArray): Long {
        var a = 0L
        var b = 0L
        val mod = 65521L
        for (i in data.indices) {
            a = (a + (data[i].toInt() and 0xFF)) % mod
            b = (b + a) % mod
        }
        return ((b shl 16) or a) and 0xFFFFFFFFL
    }

    fun standardAdler32(data: ByteArray): Long {
        val adler = Adler32()
        adler.update(data)
        return adler.value
    }

    fun standardCrc32(data: ByteArray): Long {
        val crc = CRC32()
        crc.update(data)
        return crc.value
    }

    fun sha1(data: ByteArray): String {
        val md = MessageDigest.getInstance("SHA-1")
        return bytesToHex(md.digest(data))
    }

    fun verifyChunkData(data: ByteArray, chunk: ManifestChunk, fromDisk: Boolean = false): Pair<Boolean, String> {
        val expectedLen = chunk.uncompressedSize
        if (expectedLen > 0 && data.size != expectedLen) {
            return false to "Size mismatch (${data.size} != $expectedLen)"
        }
        if (data.isEmpty()) {
            return false to "Empty chunk"
        }

        val crc = chunk.crc
        if (crc != 0L) {
            if (steamAdler32(data) == crc) return true to "steam-adler32"
            if (standardAdler32(data) == crc) return true to "standard-adler32"
            if (standardCrc32(data) == crc) return true to "crc32"
        }

        val sha = chunk.sha.lowercase()
        if (sha.length == 40 && sha1(data) == sha) {
            return true to "sha1"
        }

        if (crc != 0L) {
            return false to "Checksum mismatch"
        }

        // If from disk and manifest had no checksum, ensure it's not an unwritten zero-filled block
        if (fromDisk) {
            var allZeros = true
            for (b in data) {
                if (b != 0.toByte()) {
                    allZeros = false
                    break
                }
            }
            if (allZeros) return false to "Unallocated zero region"
        }

        return true to "size-only"
    }
}
