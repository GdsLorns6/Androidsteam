package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.DownloadManager
import com.example.model.SteamDepot
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
fun DepotItem(
    depot: SteamDepot,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .border(
                1.dp,
                if (depot.isSelected) SteamCyan.copy(alpha = 0.5f) else SteamBorder,
                RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onToggle)
            .testTag("depot_item_${depot.depotId}"),
        color = if (depot.isSelected) SteamCardElevated else SteamCard
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = depot.isSelected,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = SteamCyan,
                    uncheckedColor = SteamTextSecondary,
                    checkmarkColor = SteamCard
                ),
                modifier = Modifier.size(36.dp)
            )

            Spacer(modifier = Modifier.width(6.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Depot ${depot.depotId}",
                        color = SteamTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    if (!depot.depotKey.isNullOrBlank()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = "Key available",
                            tint = SteamSuccess,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    val os = depot.oslist ?: "All OS"
                    Box(
                        modifier = Modifier
                            .background(SteamBlue, RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = os.uppercase(),
                            color = SteamCyan,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (!depot.language.isNullOrBlank() && depot.language != "all") {
                        Spacer(modifier = Modifier.width(4.dp))
                        Box(
                            modifier = Modifier
                                .background(SteamBorder, RoundedCornerShape(4.dp))
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = depot.language.uppercase(),
                                color = SteamTextSecondary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    if (!depot.manifestId.isNullOrBlank()) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "GID: ${depot.manifestId.take(10)}...",
                            color = SteamTextMuted,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            if (depot.sizeBytes != null && depot.sizeBytes > 0) {
                Text(
                    text = DownloadManager.formatBytes(depot.sizeBytes),
                    color = SteamTextSecondary,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp
                )
            }
        }
    }
}
