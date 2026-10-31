package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppSettings
import com.example.ui.MainViewModel
import com.example.ui.theme.SteamAccentGlow
import com.example.ui.theme.SteamBackground
import com.example.ui.theme.SteamBlue
import com.example.ui.theme.SteamBorder
import com.example.ui.theme.SteamCard
import com.example.ui.theme.SteamCardElevated
import com.example.ui.theme.SteamCyan
import com.example.ui.theme.SteamError
import com.example.ui.theme.SteamSuccess
import com.example.ui.theme.SteamTextMuted
import com.example.ui.theme.SteamTextPrimary
import com.example.ui.theme.SteamTextSecondary

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentSettings by viewModel.settings.collectAsState()

    var downloadLocation by remember(currentSettings) { mutableStateOf(currentSettings.downloadLocation) }
    var concurrency by remember(currentSettings) { mutableFloatStateOf(currentSettings.concurrency.toFloat()) }
    var targetPlatform by remember(currentSettings) { mutableStateOf(currentSettings.targetPlatform) }
    var gameLanguage by remember(currentSettings) { mutableStateOf(currentSettings.gameLanguage) }
    var autoSelectDepots by remember(currentSettings) { mutableStateOf(currentSettings.autoSelectDepots) }
    var includeDlc by remember(currentSettings) { mutableStateOf(currentSettings.includeDlc) }
    var verifyChunks by remember(currentSettings) { mutableStateOf(currentSettings.verifyChunks) }
    var newSourceUrl by remember { mutableStateOf("") }
    var sourcesList by remember(currentSettings) { mutableStateOf(currentSettings.depotSources) }
    var hubcapKey by remember(currentSettings) { mutableStateOf(currentSettings.hubcapApiKey) }
    var ryuuKey by remember(currentSettings) { mutableStateOf(currentSettings.ryuuApiKey) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(SteamBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Settings, contentDescription = null, tint = SteamCyan)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "SETTINGS & PREFERENCES",
                    color = SteamTextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Section: Storage
        item {
            SettingsCard(title = "Download Location") {
                OutlinedTextField(
                    value = downloadLocation,
                    onValueChange = { downloadLocation = it },
                    modifier = Modifier.fillMaxWidth().testTag("download_location_input"),
                    placeholder = { Text("Storage path for downloads...", color = SteamTextMuted, fontSize = 12.sp) },
                    singleLine = true,
                    colors = outlinedFieldColors(),
                    shape = RoundedCornerShape(8.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        downloadLocation = viewModel.downloadManager.getDefaultDownloadDir().absolutePath
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SteamCyan),
                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(SteamBorder)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(imageVector = Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Reset to Default: Downloads/SteamDownloads", fontSize = 11.sp)
                }
            }
        }

        // Section: Network & Performance
        item {
            SettingsCard(title = "Performance & Concurrency") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Max Concurrent Chunks", color = SteamTextPrimary, fontSize = 13.sp)
                    Text(
                        text = "${concurrency.toInt()} streams",
                        color = SteamCyan,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Slider(
                    value = concurrency,
                    onValueChange = { concurrency = it },
                    valueRange = 2f..32f,
                    steps = 14,
                    colors = SliderDefaults.colors(
                        thumbColor = SteamCyan,
                        activeTrackColor = SteamCyan,
                        inactiveTrackColor = SteamBorder
                    ),
                    modifier = Modifier.testTag("concurrency_slider")
                )
                Text(
                    text = "Parallel connection worker threads downloading and decrypting simultaneously.",
                    color = SteamTextMuted,
                    fontSize = 11.sp
                )
            }
        }

        // Section: Platform & Filters
        item {
            SettingsCard(title = "Depot Auto-Selection") {
                Text(text = "Target OS Platform", color = SteamTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))

                val platforms = listOf("windows", "linux", "macos", "android", "all")
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    platforms.forEach { p ->
                        FilterChip(
                            selected = targetPlatform.equals(p, ignoreCase = true),
                            onClick = { targetPlatform = p },
                            label = { Text(p.uppercase(), fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SteamCyan,
                                selectedLabelColor = SteamBackground,
                                containerColor = SteamBorder,
                                labelColor = SteamTextSecondary
                            ),
                            border = null
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                SettingsToggleRow(
                    title = "Pick Depots Like Steam",
                    subtitle = "Automatically select only depots for this OS and preferred language",
                    checked = autoSelectDepots,
                    onCheckedChange = { autoSelectDepots = it }
                )

                Spacer(modifier = Modifier.height(8.dp))

                SettingsToggleRow(
                    title = "Include DLC Depots",
                    subtitle = "Mark DLC and expansion depots for downloading",
                    checked = includeDlc,
                    onCheckedChange = { includeDlc = it }
                )

                Spacer(modifier = Modifier.height(8.dp))

                SettingsToggleRow(
                    title = "Verify Chunks on Download",
                    subtitle = "Calculate Adler-32, CRC32 and SHA-1 checksums before writing to disk",
                    checked = verifyChunks,
                    onCheckedChange = { verifyChunks = it }
                )
            }
        }

        // Section: Remote Sources
        item {
            SettingsCard(title = "Depot & Manifest Sources") {
                Text(
                    text = "Remote source endpoints (archive.org / GitHub) containing branches and depot keys:",
                    color = SteamTextMuted,
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                sourcesList.forEachIndexed { idx, s ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SteamBackground, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = s,
                            color = SteamTextSecondary,
                            fontSize = 11.sp,
                            maxLines = 1,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                sourcesList = sourcesList.filterIndexed { i, _ -> i != idx }
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = SteamError, modifier = Modifier.size(14.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newSourceUrl,
                        onValueChange = { newSourceUrl = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("https://...", color = SteamTextMuted, fontSize = 11.sp) },
                        singleLine = true,
                        colors = outlinedFieldColors(),
                        shape = RoundedCornerShape(8.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (newSourceUrl.isNotBlank() && !sourcesList.contains(newSourceUrl.trim())) {
                                sourcesList = sourcesList + newSourceUrl.trim()
                                newSourceUrl = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SteamCyan, contentColor = SteamBackground),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add", fontSize = 11.sp)
                    }
                }
            }
        }

        // Save Button
        item {
            Button(
                onClick = {
                    val updated = currentSettings.copy(
                        downloadLocation = downloadLocation.trim(),
                        concurrency = concurrency.toInt(),
                        targetPlatform = targetPlatform,
                        gameLanguage = gameLanguage,
                        autoSelectDepots = autoSelectDepots,
                        includeDlc = includeDlc,
                        verifyChunks = verifyChunks,
                        depotSources = sourcesList,
                        hubcapApiKey = hubcapKey.trim(),
                        ryuuApiKey = ryuuKey.trim()
                    )
                    viewModel.updateSettings(updated)
                    Toast.makeText(context, "Settings saved successfully", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("save_settings_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SteamCyan,
                    contentColor = SteamBackground
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Settings", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }

        // About Box
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, SteamBorder, RoundedCornerShape(12.dp)),
                color = SteamCard
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = SteamAccentGlow, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Steam Manifest Downloader", color = SteamTextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Android Edition v1.5.0 • Native High-Speed Engine • LZMA/AES/ZLIB/Adler32 Verification • Resumable Journaling",
                        color = SteamTextMuted,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, SteamBorder, RoundedCornerShape(12.dp)),
        color = SteamCard
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = title,
                color = SteamTextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = SteamTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(text = subtitle, color = SteamTextMuted, fontSize = 11.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = SteamCyan,
                checkedTrackColor = SteamBlue,
                uncheckedThumbColor = SteamTextSecondary,
                uncheckedTrackColor = SteamBorder
            )
        )
    }
}

@Composable
private fun outlinedFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = SteamCyan,
    unfocusedBorderColor = SteamBorder,
    focusedContainerColor = SteamBackground,
    unfocusedContainerColor = SteamBackground,
    cursorColor = SteamCyan,
    focusedTextColor = SteamTextPrimary,
    unfocusedTextColor = SteamTextPrimary
)
