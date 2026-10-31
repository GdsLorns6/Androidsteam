package com.example.ui

import android.os.Environment
import android.os.StatFs
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
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
import com.example.model.JobStatus
import com.example.ui.screens.DownloadsScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.UploadScreen
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
import java.io.File
import java.util.Locale

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val currentTab by viewModel.currentTab.collectAsState()
    val currentJob by viewModel.currentJob.collectAsState()
    val selectedApp by viewModel.selectedApp.collectAsState()

    BackHandler(enabled = (currentTab != NavTab.SEARCH || selectedApp != null)) {
        if (selectedApp != null) {
            viewModel.clearSelectedApp()
        } else {
            viewModel.setTab(NavTab.SEARCH)
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(SteamBackground),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopHeaderBar(
                isDownloading = currentJob?.status == JobStatus.DOWNLOADING
            )
        },
        bottomBar = {
            SteamBottomNav(
                currentTab = currentTab,
                onTabSelected = { viewModel.setTab(it) },
                hasActiveDownload = currentJob?.status == JobStatus.DOWNLOADING
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = currentTab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "screen_transition"
            ) { tab ->
                when (tab) {
                    NavTab.SEARCH -> SearchScreen(viewModel = viewModel)
                    NavTab.UPLOAD -> UploadScreen(viewModel = viewModel)
                    NavTab.DOWNLOADS -> DownloadsScreen(viewModel = viewModel)
                    NavTab.HISTORY -> HistoryScreen(viewModel = viewModel)
                    NavTab.SETTINGS -> SettingsScreen(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
private fun TopHeaderBar(
    isDownloading: Boolean,
    modifier: Modifier = Modifier
) {
    val freeSpaceGb = try {
        val stat = StatFs(Environment.getDataDirectory().path)
        val bytesAvailable = stat.availableBlocksLong * stat.blockSizeLong
        bytesAvailable / (1024L * 1024L * 1024L)
    } catch (e: Exception) {
        64L
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .background(SteamCard)
            .statusBarsPadding(),
        color = SteamCard
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(SteamCardElevated)
                    .border(1.5.dp, SteamCyan, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CloudDownload,
                    contentDescription = null,
                    tint = SteamCyan,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "STEAM MANIFEST",
                        color = SteamTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "DOWNLOADER",
                        color = SteamCyan,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    )
                }
                Text(
                    text = "High-speed depot chunk engine",
                    color = SteamTextMuted,
                    fontSize = 11.sp
                )
            }

            // Storage pill
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(SteamCardElevated)
                    .border(1.dp, SteamBorder, RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Storage,
                    contentDescription = null,
                    tint = if (freeSpaceGb > 10) SteamSuccess else SteamAccentGlow,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "$freeSpaceGb GB Free",
                    color = SteamTextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun SteamBottomNav(
    currentTab: NavTab,
    onTabSelected: (NavTab) -> Unit,
    hasActiveDownload: Boolean
) {
    NavigationBar(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        containerColor = SteamCard,
        contentColor = SteamTextPrimary,
        tonalElevation = 6.dp
    ) {
        NavigationBarItem(
            selected = currentTab == NavTab.SEARCH,
            onClick = { onTabSelected(NavTab.SEARCH) },
            icon = {
                Icon(
                    imageVector = if (currentTab == NavTab.SEARCH) Icons.Filled.Search else Icons.Outlined.Search,
                    contentDescription = "Search"
                )
            },
            label = { Text("Search", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
            colors = navItemColors(),
            modifier = Modifier.testTag("nav_tab_search")
        )

        NavigationBarItem(
            selected = currentTab == NavTab.UPLOAD,
            onClick = { onTabSelected(NavTab.UPLOAD) },
            icon = {
                Icon(
                    imageVector = if (currentTab == NavTab.UPLOAD) Icons.Filled.UploadFile else Icons.Outlined.UploadFile,
                    contentDescription = "Upload"
                )
            },
            label = { Text("Lua / ST", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
            colors = navItemColors(),
            modifier = Modifier.testTag("nav_tab_upload")
        )

        NavigationBarItem(
            selected = currentTab == NavTab.DOWNLOADS,
            onClick = { onTabSelected(NavTab.DOWNLOADS) },
            icon = {
                BadgedBox(
                    badge = {
                        if (hasActiveDownload) {
                            Badge(
                                containerColor = SteamCyan,
                                contentColor = SteamBackground
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(SteamBackground)
                                )
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = if (currentTab == NavTab.DOWNLOADS) Icons.Filled.CloudDownload else Icons.Outlined.CloudDownload,
                        contentDescription = "Downloads"
                    )
                }
            },
            label = { Text("Downloads", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
            colors = navItemColors(),
            modifier = Modifier.testTag("nav_tab_downloads")
        )

        NavigationBarItem(
            selected = currentTab == NavTab.HISTORY,
            onClick = { onTabSelected(NavTab.HISTORY) },
            icon = {
                Icon(
                    imageVector = if (currentTab == NavTab.HISTORY) Icons.Filled.History else Icons.Outlined.History,
                    contentDescription = "History"
                )
            },
            label = { Text("History", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
            colors = navItemColors(),
            modifier = Modifier.testTag("nav_tab_history")
        )

        NavigationBarItem(
            selected = currentTab == NavTab.SETTINGS,
            onClick = { onTabSelected(NavTab.SETTINGS) },
            icon = {
                Icon(
                    imageVector = if (currentTab == NavTab.SETTINGS) Icons.Filled.Settings else Icons.Outlined.Settings,
                    contentDescription = "Settings"
                )
            },
            label = { Text("Settings", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
            colors = navItemColors(),
            modifier = Modifier.testTag("nav_tab_settings")
        )
    }
}

@Composable
private fun navItemColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = SteamCyan,
    selectedTextColor = SteamCyan,
    unselectedIconColor = SteamTextSecondary,
    unselectedTextColor = SteamTextSecondary,
    indicatorColor = SteamBlue
)
