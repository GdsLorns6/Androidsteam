package com.example.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.DownloadManager
import com.example.model.HistoryEntry
import com.example.ui.MainViewModel
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
import com.example.ui.theme.SteamWarning
import java.io.File

@Composable
fun HistoryScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val history by viewModel.history.collectAsState()
    var selectedFolderToInspect by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SteamBackground)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "DOWNLOAD HISTORY",
                    color = SteamTextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "${history.size} total entries",
                    color = SteamTextMuted,
                    fontSize = 11.sp
                )
            }

            if (history.isNotEmpty()) {
                OutlinedButton(
                    onClick = { viewModel.clearHistory() },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SteamError),
                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(SteamError)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("clear_history_button")
                ) {
                    Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Clear All", fontSize = 11.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (history.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = SteamTextMuted,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "History is empty",
                        color = SteamTextSecondary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Downloaded games and cancelled jobs will be recorded here.",
                        color = SteamTextMuted,
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(history, key = { it.id }) { item ->
                    HistoryCard(
                        entry = item,
                        onResume = { viewModel.resumeDownloadFromHistory(item) },
                        onExplore = { selectedFolderToInspect = item.downloadDir },
                        onDelete = { viewModel.removeHistoryItem(item.id) }
                    )
                }
            }
        }
    }

    // File Explorer Dialog
    if (selectedFolderToInspect != null) {
        val folder = File(selectedFolderToInspect!!)
        val fileList = if (folder.exists()) folder.listFiles()?.toList() ?: emptyList() else emptyList()

        AlertDialog(
            onDismissRequest = { selectedFolderToInspect = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.FolderOpen, contentDescription = null, tint = SteamCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Game Files (${fileList.size} items)",
                        color = SteamTextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = folder.absolutePath,
                        color = SteamTextMuted,
                        fontSize = 11.sp,
                        maxLines = 2
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (fileList.isEmpty()) {
                        Text(
                            text = "No files found in directory or directory has been cleared.",
                            color = SteamTextSecondary,
                            fontSize = 12.sp
                        )
                    } else {
                        LazyColumn(modifier = Modifier.height(240.dp)) {
                            items(fileList.sortedBy { !it.isDirectory }) { f ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = if (f.isDirectory) Icons.Default.Folder else Icons.Default.InsertDriveFile,
                                            contentDescription = null,
                                            tint = if (f.isDirectory) SteamCyan else SteamTextSecondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = f.name,
                                            color = SteamTextPrimary,
                                            fontSize = 12.sp,
                                            maxLines = 1
                                        )
                                    }
                                    if (!f.isDirectory) {
                                        Text(
                                            text = DownloadManager.formatBytes(f.length()),
                                            color = SteamTextMuted,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { selectedFolderToInspect = null },
                    colors = ButtonDefaults.buttonColors(containerColor = SteamCyan, contentColor = SteamBackground)
                ) {
                    Text("Close")
                }
            },
            containerColor = SteamCard,
            tonalElevation = 6.dp
        )
    }
}

@Composable
private fun HistoryCard(
    entry: HistoryEntry,
    onResume: () -> Unit,
    onExplore: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, SteamBorder, RoundedCornerShape(12.dp))
            .testTag("history_item_${entry.appId}"),
        color = SteamCard
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = entry.gameName,
                        color = SteamTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "App ID ${entry.appId} • ${entry.depotCount} depots • ${entry.sizeFormatted}",
                        color = SteamTextSecondary,
                        fontSize = 11.sp
                    )
                }

                val statusColor = if (entry.status == "complete") SteamSuccess else SteamWarning
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(statusColor.copy(alpha = 0.2f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = entry.status.uppercase(),
                        color = statusColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = entry.completedAt.ifEmpty { entry.startedAt },
                    color = SteamTextMuted,
                    fontSize = 10.sp
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (entry.status != "complete") {
                        OutlinedButton(
                            onClick = onResume,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = SteamCyan),
                            border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(SteamCyan)),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Resume", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    OutlinedButton(
                        onClick = onExplore,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SteamTextSecondary),
                        border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(SteamBorder)),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Files", fontSize = 11.sp)
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = SteamTextMuted, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}
