package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.SteamBorder
import com.example.ui.theme.SteamConsoleBg
import com.example.ui.theme.SteamCyan
import com.example.ui.theme.SteamError
import com.example.ui.theme.SteamSuccess
import com.example.ui.theme.SteamTextMuted
import com.example.ui.theme.SteamTextSecondary
import com.example.ui.theme.SteamWarning

@Composable
fun TerminalLogView(
    logs: List<String>,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SteamConsoleBg)
            .border(1.dp, SteamBorder, RoundedCornerShape(12.dp))
            .testTag("terminal_log_view")
    ) {
        // Terminal Title Bar with Dots
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF131A24))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(SteamError))
                Spacer(modifier = Modifier.width(6.dp))
                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(SteamWarning))
                Spacer(modifier = Modifier.width(6.dp))
                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(SteamSuccess))
                Spacer(modifier = Modifier.width(10.dp))
                Icon(
                    imageVector = Icons.Default.Terminal,
                    contentDescription = null,
                    tint = SteamCyan,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "DOWNLOAD CONSOLE",
                    color = SteamTextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            IconButton(
                onClick = {
                    val fullLog = logs.joinToString("\n")
                    clipboardManager.setText(AnnotatedString(fullLog))
                    Toast.makeText(context, "Console logs copied to clipboard", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy logs",
                    tint = SteamTextMuted,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        // Terminal Log List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .padding(8.dp)
        ) {
            if (logs.isEmpty()) {
                item {
                    Text(
                        text = "Awaiting engine events...",
                        color = SteamTextMuted,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            } else {
                items(logs) { line ->
                    val color = when {
                        line.contains("error", ignoreCase = true) || line.contains("failed", ignoreCase = true) -> SteamError
                        line.contains("completed", ignoreCase = true) || line.contains("success", ignoreCase = true) -> SteamSuccess
                        line.contains("warning", ignoreCase = true) || line.contains("resuming", ignoreCase = true) -> SteamWarning
                        line.contains("%") || line.contains("depot", ignoreCase = true) -> SteamCyan
                        else -> SteamTextSecondary
                    }
                    Text(
                        text = line,
                        color = color,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(vertical = 1.dp)
                    )
                }
            }
        }
    }
}
