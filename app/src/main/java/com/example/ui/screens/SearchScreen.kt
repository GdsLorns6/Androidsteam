package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.example.engine.DownloadManager
import com.example.ui.MainViewModel
import com.example.ui.components.DepotItem
import com.example.ui.components.GameCard
import com.example.ui.theme.SteamAccentGlow
import com.example.ui.theme.SteamBackground
import com.example.ui.theme.SteamBlue
import com.example.ui.theme.SteamBorder
import com.example.ui.theme.SteamCard
import com.example.ui.theme.SteamCardElevated
import com.example.ui.theme.SteamCyan
import com.example.ui.theme.SteamTextMuted
import com.example.ui.theme.SteamTextPrimary
import com.example.ui.theme.SteamTextSecondary

@Composable
fun SearchScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val selectedApp by viewModel.selectedApp.collectAsState()
    val depots by viewModel.depots.collectAsState()
    val isLoadingDepots by viewModel.isLoadingDepots.collectAsState()
    val settings by viewModel.settings.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SteamBackground)
            .padding(16.dp)
    ) {
        if (selectedApp == null) {
            // Search Input Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_game_input"),
                placeholder = {
                    Text(
                        text = "Search by Game Name or App ID (e.g. 1030300)...",
                        color = SteamTextMuted,
                        fontSize = 13.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = SteamCyan
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { viewModel.onSearchQueryChanged("") },
                            modifier = Modifier.testTag("clear_search_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = SteamTextSecondary
                            )
                        }
                    }
                },
                singleLine = true,
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

            Spacer(modifier = Modifier.height(12.dp))

            // Quick Featured Pills
            Text(
                text = "POPULAR STEAM TITLES",
                color = SteamTextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(6.dp))

            val quickApps = listOf(
                "Silksong (1030300)" to "1030300",
                "Portal 2 (620)" to "620",
                "Half-Life 2 (220)" to "220",
                "Balatro (2379780)" to "2379780",
                "Elden Ring" to "1245620"
            )

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(quickApps) { (label, id) ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(SteamCardElevated)
                            .border(1.dp, SteamBorder, RoundedCornerShape(8.dp))
                            .clickable { viewModel.onSearchQueryChanged(id) }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = label,
                            color = SteamAccentGlow,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isSearching) {
                Box(modifier = Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = SteamCyan)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(searchResults, key = { it.appId }) { app ->
                        GameCard(
                            game = app,
                            onClick = { viewModel.selectApp(app) }
                        )
                    }
                }
            }
        } else {
            // Selected Game Detail & Depots
            val app = selectedApp!!
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(
                    onClick = { viewModel.clearSelectedApp() },
                    modifier = Modifier.testTag("back_to_search_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = SteamCyan
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "App Details & Depots",
                    color = SteamTextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Game Header Banner
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, SteamBorder, RoundedCornerShape(12.dp)),
                color = SteamCard
            ) {
                Column {
                    if (!app.headerImage.isNullOrBlank()) {
                        SubcomposeAsyncImage(
                            model = app.headerImage,
                            contentDescription = app.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                        )
                    }
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = app.name,
                                color = SteamTextPrimary,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            Box(
                                modifier = Modifier
                                    .background(SteamBlue, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "ID ${app.appId}",
                                    color = SteamCyan,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        if (!app.shortDescription.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = app.shortDescription,
                                color = SteamTextSecondary,
                                fontSize = 12.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Depot Controls Bar
            val selectedCount = depots.count { it.isSelected }
            val selectedBytes = depots.filter { it.isSelected }.sumOf { it.sizeBytes ?: 0L }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "DEPOTS (${selectedCount}/${depots.size} selected)",
                    color = SteamTextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = { viewModel.autoSelectLikeSteam() },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SteamCyan),
                        border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(SteamCyan)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("auto_select_depots_button")
                    ) {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Like Steam", fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            val allSelected = depots.all { it.isSelected }
                            viewModel.selectAllDepots(!allSelected)
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SteamTextSecondary),
                        border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(SteamBorder)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("select_all_depots_button")
                    ) {
                        Icon(imageVector = Icons.Default.SelectAll, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (depots.all { it.isSelected }) "Deselect" else "All",
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (isLoadingDepots) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = SteamCyan)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(text = "Querying Steam depots & manifests...", color = SteamTextSecondary, fontSize = 12.sp)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(depots, key = { it.depotId }) { depot ->
                        DepotItem(
                            depot = depot,
                            onToggle = { viewModel.toggleDepotSelection(depot.depotId) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action / Download Footer Bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, SteamBorder, RoundedCornerShape(12.dp)),
                color = SteamCardElevated
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                tint = SteamTextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Save: SteamDownloads/${app.appId} - ...",
                                color = SteamTextSecondary,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (selectedBytes > 0) {
                            Text(
                                text = DownloadManager.formatBytes(selectedBytes),
                                color = SteamCyan,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = { viewModel.startDownloadCurrentApp() },
                        enabled = selectedCount > 0,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("start_download_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SteamCyan,
                            contentColor = SteamBackground,
                            disabledContainerColor = SteamBorder,
                            disabledContentColor = SteamTextMuted
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (selectedCount > 0) "Download Selected ($selectedCount Depots)" else "Select at least 1 Depot",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}
