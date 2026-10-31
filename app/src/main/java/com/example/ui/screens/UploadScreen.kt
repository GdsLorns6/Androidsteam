package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.SteamDepot
import com.example.ui.MainViewModel
import com.example.ui.NavTab
import com.example.ui.theme.SteamAccentGlow
import com.example.ui.theme.SteamBackground
import com.example.ui.theme.SteamBlue
import com.example.ui.theme.SteamBorder
import com.example.ui.theme.SteamCard
import com.example.ui.theme.SteamCardElevated
import com.example.ui.theme.SteamCyan
import com.example.ui.theme.SteamSuccess
import com.example.ui.theme.SteamTextMuted
import com.example.ui.theme.SteamTextPrimary
import com.example.ui.theme.SteamTextSecondary

@Composable
fun UploadScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var luaCodeInput by remember { mutableStateOf("") }
    val parseResult by viewModel.luaParseResult.collectAsState()

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bytes = inputStream?.readBytes()
                inputStream?.close()

                if (bytes != null) {
                    val filename = uri.lastPathSegment ?: ""
                    if (filename.endsWith(".st", ignoreCase = true) || (bytes.size >= 12 && bytes[0] != 'a'.code.toByte())) {
                        viewModel.parseStBytes(bytes)
                        Toast.makeText(context, "Parsed encrypted .st manifest package", Toast.LENGTH_SHORT).show()
                    } else {
                        val text = String(bytes, Charsets.UTF_8)
                        luaCodeInput = text
                        viewModel.parseLuaCode(text)
                        Toast.makeText(context, "Loaded .lua configuration script", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to read file: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(SteamBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "UPLOAD .LUA OR .ST FILE",
                color = SteamTextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Drop, import or paste Steam manifest configurations. Decrypts .st archives and extracts all depot IDs, manifest GIDs and decryption keys.",
                color = SteamTextMuted,
                fontSize = 12.sp
            )
        }

        // File Import Box
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, SteamCyan.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .clickable {
                        filePicker.launch(arrayOf("*/*"))
                    }
                    .testTag("file_picker_drop_zone"),
                color = SteamCardElevated
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.UploadFile,
                        contentDescription = "Upload",
                        tint = SteamCyan,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Pick .LUA or .ST File from Storage",
                        color = SteamTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Supports standard Lua scripts & encrypted binary .st files",
                        color = SteamTextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Preset Samples Pills
        item {
            Text(
                text = "QUICK PRESETS",
                color = SteamTextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(6.dp))

            val sampleSilksong = """
addappid(1030300)
addappid(1030301, 1, "e8d9c0b1a234567890abcdef1234567890abcdef1234567890abcdef12345678")
setManifestid(1030301, "794285194029410294", 8945201948)
addappid(1030302, 1, "a1b2c3d4e5f60718293a4b5c6d7e8f90a1b2c3d4e5f60718293a4b5c6d7e8f90")
setManifestid(1030302, "618492048291048291", 421098412)
            """.trimIndent()

            val samplePortal2 = """
addappid(620)
addappid(621, 1, "3489102938475610293847561029384756102938475610293847561029384756")
setManifestid(621, "491029482910482910", 6842019482)
addappid(622, 1, "7891234567890abcdef1234567890abcdef1234567890abcdef1234567890ab")
setManifestid(622, "104928104829104829", 310984120)
            """.trimIndent()

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    OutlinedButton(
                        onClick = {
                            luaCodeInput = sampleSilksong
                            viewModel.parseLuaCode(sampleSilksong)
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SteamCyan),
                        border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(SteamBorder)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("preset_silksong_button")
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Silksong Preset (1030300)", fontSize = 11.sp)
                    }
                }
                item {
                    OutlinedButton(
                        onClick = {
                            luaCodeInput = samplePortal2
                            viewModel.parseLuaCode(samplePortal2)
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SteamAccentGlow),
                        border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(SteamBorder)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("preset_portal2_button")
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Portal 2 Preset (620)", fontSize = 11.sp)
                    }
                }
            }
        }

        // Text Area for Lua code
        item {
            OutlinedTextField(
                value = luaCodeInput,
                onValueChange = {
                    luaCodeInput = it
                    if (it.isNotBlank()) viewModel.parseLuaCode(it)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .testTag("lua_code_input"),
                placeholder = {
                    Text(
                        text = "Paste Lua code here (e.g. addappid(1030300)...)",
                        color = SteamTextMuted,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SteamCyan,
                    unfocusedBorderColor = SteamBorder,
                    focusedContainerColor = SteamCard,
                    unfocusedContainerColor = SteamCard,
                    cursorColor = SteamCyan,
                    focusedTextColor = SteamTextPrimary,
                    unfocusedTextColor = SteamTextPrimary
                )
            )
        }

        // Parse Result Preview
        if (parseResult != null) {
            val res = parseResult!!
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, SteamSuccess.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                    color = SteamCard
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Code,
                                contentDescription = null,
                                tint = SteamSuccess,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Parsed Configuration",
                                color = SteamTextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Main App ID:", color = SteamTextSecondary, fontSize = 13.sp)
                            Text(
                                text = res.mainAppId?.toString() ?: "None",
                                color = SteamCyan,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Depots Found:", color = SteamTextSecondary, fontSize = 13.sp)
                            Text(
                                text = "${res.depots.size} depots",
                                color = SteamTextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        res.depots.take(4).forEach { d ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(SteamCardElevated, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Depot ${d.depotId}",
                                    color = SteamTextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (!d.depotKey.isNullOrBlank()) {
                                        Icon(
                                            imageVector = Icons.Default.Key,
                                            contentDescription = null,
                                            tint = SteamSuccess,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    Text(
                                        text = d.manifestId?.let { "Manifest: ${it.take(8)}..." } ?: "Base Container",
                                        color = SteamTextMuted,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                viewModel.setTab(NavTab.SEARCH)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("inspect_parsed_depots_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SteamCyan,
                                contentColor = SteamBackground
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Select & Download These Depots", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}
