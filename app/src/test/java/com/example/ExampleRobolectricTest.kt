package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.engine.CryptoEngine
import com.example.engine.LuaParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Steam Manifest Downloader", appName)
    }

    @Test
    fun `test lua parser addappid and setmanifestid`() {
        val script = """
            addappid(1030300)
            addappid(1030301, 1, "e8d9c0b1a234567890abcdef1234567890abcdef1234567890abcdef12345678")
            setManifestid(1030301, "794285194029410294", 8945201948)
        """.trimIndent()

        val parsed = LuaParser.parseLuaContent(script)
        assertEquals(1030300L, parsed.mainAppId)
        assertEquals(1, parsed.depots.size)
        val depot = parsed.depots.first()
        assertEquals(1030301L, depot.depotId)
        assertEquals("794285194029410294", depot.manifestId)
        assertEquals("e8d9c0b1a234567890abcdef1234567890abcdef1234567890abcdef12345678", depot.depotKey)
        assertEquals(8945201948L, depot.sizeBytes)
    }

    @Test
    fun `test steam adler32 calculation`() {
        val data = "SteamManifestDownloader".toByteArray(Charsets.UTF_8)
        val adler = CryptoEngine.steamAdler32(data)
        assertTrue(adler != 0L)
    }
}
