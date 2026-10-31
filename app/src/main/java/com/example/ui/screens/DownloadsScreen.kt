package com.example.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.DownloadManager
import com.example.model.JobStatus
import com.example.ui.MainViewModel
import com.example.ui.NavTab
import com.example.ui.components.TerminalLogView
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
import com.example.ui.theme.SteamWarning

@Composable
fun DownloadsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val currentJob by viewModel.currentJob.collectAsState()
    val settings by viewModel.settings.collectAsState()

    val rawProgress = currentJob?.let { job ->
        if (job.totalBytes > 0) {
            (job.completedBytes.toFloat() / job.totalBytes.toFloat()).coerceIn(0f, 1f)
        } else 0f
    } ?: 0f

    val animatedProgress by animateFloatAsState(
        targetValue = rawProgress,
        animationSpec = tween(durationMillis = 300, easing = LinearEasing),
        label = "download_progress"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(SteamBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (currentJob == null) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, SteamBorder, RoundedCornerShape(16.dp)),
                    color = SteamCard
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = null,
                            tint = SteamCyan,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No Active Downloads",
                            color = SteamTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Select a game from Store Search or upload a .lua file to begin downloading Steam depots.",
                            color = SteamTextMuted,
                            fontSize = 13.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = { viewModel.setTab(NavTab.SEARCH) },
                            colors = ButtonDefaults.buttonColors(containerColor = SteamCyan, contentColor = SteamBackground),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("browse_games_empty_button")
                        ) {
                            Text(text = "Search Steam Games", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            val job = currentJob!!

            // Progress Header Card
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, SteamBorder, RoundedCornerShape(14.dp)),
                    color = SteamCardElevated
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = job.gameName,
                                    color = SteamTextPrimary,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "App ID ${job.appId}",
                                        color = SteamCyan,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    if (job.currentDepotId > 0) {
                                        Text(
                                            text = " • Depot ${job.currentDepotId} (${job.currentDepotIndex}/${job.depots.size})",
                                            color = SteamTextSecondary,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }

                            val (statusColor, statusText) = when (job.status) {
                                JobStatus.DOWNLOADING -> SteamCyan to "DOWNLOADING"
                                JobStatus.COMPLETED -> SteamSuccess to "COMPLETED"
                                JobStatus.PAUSED -> SteamWarning to "PAUSED"
                                JobStatus.CANCELLED_RESUMABLE -> SteamWarning to "RESUMABLE"
                                JobStatus.ERROR -> SteamError to "ERROR"
                                JobStatus.PENDING -> SteamTextMuted to "PENDING"
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(statusColor.copy(alpha = 0.2f))
                                    .border(1.dp, statusColor, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = statusText,
                                    color = statusColor,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Progress Bar
                        LinearProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp)),
                            color = SteamCyan,
                            trackColor = SteamBackground,
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Stats Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${(animatedProgress * 100).toInt()}%",
                                color = SteamTextPrimary,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${DownloadManager.formatBytes(job.completedBytes)} / ${DownloadManager.formatBytes(job.totalBytes)}",
                                    color = SteamTextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${job.doneChunks} / ${job.totalChunks} chunks" + if (job.skippedChunks > 0) " (${job.skippedChunks} verified on disk)" else "",
                                    color = SteamTextMuted,
                                    fontSize = 10.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Telemetry Grid
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SteamBackground, RoundedCornerShape(8.dp))
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = null,
                                    tint = SteamCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(text = "SPEED", color = SteamTextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    val mbSpeed = job.speedBytesPerSec.toDouble() / (1024.0 * 1024.0)
                                    Text(
                                        text = String.format("%.2f MB/s", mbSpeed),
                                        color = SteamTextPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = null,
                                    tint = SteamAccentGlow,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(text = "ESTIMATED TIME", color = SteamTextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = DownloadManager.formatEta(job.etaSeconds),
                                        color = SteamTextPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (job.status == JobStatus.DOWNLOADING) {
                                OutlinedButton(
                                    onClick = { viewModel.downloadManager.pauseDownload() },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp)
                                        .testTag("pause_download_button"),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SteamWarning),
                                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(SteamWarning)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Pause, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Pause", fontWeight = FontWeight.Bold)
                                }
                            } else if (job.status == JobStatus.PAUSED) {
                                Button(
                                    onClick = { viewModel.downloadManager.resumePausedDownload(settings) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp)
                                        .testTag("resume_download_button"),
                                    colors = ButtonDefaults.buttonColors(containerColor = SteamCyan, contentColor = SteamBackground),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Resume", fontWeight = FontWeight.Bold)
                                }
                            }

                            if (job.status == JobStatus.DOWNLOADING || job.status == JobStatus.PAUSED) {
                                OutlinedButton(
                                    onClick = { viewModel.downloadManager.cancelDownload() },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp)
                                        .testTag("cancel_download_button"),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SteamError),
                                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(SteamError)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Cancel & Save", fontWeight = FontWeight.Bold)
                                }
                            }

                            if (job.status == JobStatus.COMPLETED) {
                                Button(
                                    onClick = { viewModel.setTab(NavTab.HISTORY) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(42.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = SteamSuccess, contentColor = SteamBackground),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("View in Downloaded Library", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // Real-Time Terminal View
            item {
                TerminalLogView(logs = job.logs)
            }
        }
    }
}
