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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.model.SteamAppSummary
import com.example.ui.theme.SteamAccentGlow
import com.example.ui.theme.SteamBorder
import com.example.ui.theme.SteamCard
import com.example.ui.theme.SteamCardElevated
import com.example.ui.theme.SteamCyan
import com.example.ui.theme.SteamTextMuted
import com.example.ui.theme.SteamTextPrimary
import com.example.ui.theme.SteamTextSecondary

@Composable
fun GameCard(
    game: SteamAppSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, SteamBorder, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag("game_card_${game.appId}"),
        color = SteamCard,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(width = 90.dp, height = 48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(SteamCardElevated),
                contentAlignment = Alignment.Center
            ) {
                if (game.tinyImage.isNotBlank()) {
                    SubcomposeAsyncImage(
                        model = game.tinyImage,
                        contentDescription = game.name,
                        contentScale = ContentScale.Crop,
                        loading = {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = SteamCyan,
                                strokeWidth = 2.dp
                            )
                        },
                        error = {
                            Icon(
                                imageVector = Icons.Default.Gamepad,
                                contentDescription = null,
                                tint = SteamTextMuted,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Gamepad,
                        contentDescription = null,
                        tint = SteamAccentGlow,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = game.name,
                    color = SteamTextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(SteamBorder, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "APP ID ${game.appId}",
                            color = SteamCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Select",
                tint = SteamTextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
