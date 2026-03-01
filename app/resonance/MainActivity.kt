// @path: app/resonance/MainActivity.kt
package com.radwrld.resonance

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface {
                    MusicAppUI()
                }
            }
        }
    }
}

private val BgColor = Color(0xFF07090D)
private val SearchBgColor = Color(0xFF15181E)
private val TextPrimary = Color(0xFFFFFFFF)
private val TextSecondary = Color(0xFF8E929B)
private val AccentCyan = Color(0xFF38B2CE)
private val CardLateNight = Color(0xFF111A29)
private val CardEnergyTop = Color(0xFF3A2424)
private val CardEnergyBottom = Color(0xFF161214)
private val CardFocusTop = Color(0xFF162B35)
private val CardFocusBottom = Color(0xFF0C141A)

@Composable
fun MusicAppUI() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 40.dp, bottom = 40.dp)
    ) {
        item { HeaderSection() }
        item { Spacer(modifier = Modifier.height(24.dp)) }
        item { SearchSection() }
        item { Spacer(modifier = Modifier.height(32.dp)) }
        item { MoodClustersSection() }
        item { Spacer(modifier = Modifier.height(32.dp)) }
        item { EchoesSection() }
    }
}

@Composable
private fun HeaderSection() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Good Evening, Alex",
                color = TextPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Canvas(modifier = Modifier.size(8.dp)) {
                    drawCircle(AccentCyan)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Current Vibe: ",
                    color = TextSecondary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Melancholy",
                    color = AccentCyan,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color(0xFF121B24)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.SmartToy,
                contentDescription = "Profile",
                tint = AccentCyan,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun SearchSection() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(SearchBgColor)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = "Search",
                tint = TextSecondary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "Search moods, artists, or tracks.",
                color = TextSecondary,
                fontSize = 15.sp,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Rounded.Tune,
                contentDescription = "Filter",
                tint = TextSecondary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun MoodClustersSection() {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Mood Clusters",
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "View All",
                color = AccentCyan,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(CardLateNight)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = Color(0xFF1B2E4B).copy(alpha = 0.5f),
                    radius = 120f,
                    center = Offset(size.width * 0.8f, size.height * 0.2f)
                )
                drawCircle(
                    color = Color(0xFF1A2840).copy(alpha = 0.6f),
                    radius = 80f,
                    center = Offset(size.width * 0.6f, size.height * 0.5f)
                )
                drawCircle(
                    color = Color(0xFF131F33).copy(alpha = 0.8f),
                    radius = 150f,
                    center = Offset(size.width * 0.3f, size.height * 0.8f)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF13283D)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.NightsStay,
                        contentDescription = "Late Night",
                        tint = AccentCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = "Late Night Chill",
                            color = TextPrimary,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Slow tempo • Deep bass • 42 tracks",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = "Play",
                            tint = Color.Black,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            SmallMoodCard(
                modifier = Modifier.weight(1f),
                title = "Energy Boost",
                icon = Icons.Rounded.Bolt,
                iconTint = Color(0xFFE58740),
                bgBrush = Brush.verticalGradient(
                    colors = listOf(CardEnergyTop, CardEnergyBottom)
                )
            )

            SmallMoodCard(
                modifier = Modifier.weight(1f),
                title = "Deep Focus",
                icon = Icons.Rounded.FilterCenterFocus,
                iconTint = AccentCyan,
                bgBrush = Brush.verticalGradient(
                    colors = listOf(CardFocusTop, CardFocusBottom)
                )
            )
        }
    }
}

@Composable
private fun SmallMoodCard(
    modifier: Modifier,
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    bgBrush: Brush
) {
    Box(
        modifier = modifier
            .height(140.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(bgBrush)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.05f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun EchoesSection() {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Echoes",
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Canvas(modifier = Modifier.size(6.dp)) { drawCircle(AccentCyan) }
                Canvas(modifier = Modifier.size(6.dp)) { drawCircle(Color.DarkGray) }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        EchoItem(
            title = "Midnight City",
            artist = "M83",
            tagText = "DREAMY",
            tagColor = Color(0xFF9462A5),
            imageContent = {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRect(Color(0xFF0F0E13))
                    drawCircle(Color(0xFF8C9FB5).copy(alpha = 0.5f), radius = 30f, center = Offset(20f, 60f))
                    drawCircle(Color(0xFF4C5D70).copy(alpha = 0.6f), radius = 25f, center = Offset(50f, 30f))
                }
            }
        )
        Spacer(modifier = Modifier.height(16.dp))
        EchoItem(
            title = "Space Song",
            artist = "Beach House",
            tagText = "NOSTALGIA",
            tagColor = Color(0xFF38B2CE),
            imageContent = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF3B2E63), Color(0xFFB52B65))
                            )
                        )
                )
            }
        )
        Spacer(modifier = Modifier.height(16.dp))
        EchoItem(
            title = "Nightcall",
            artist = "Kavinsky",
            tagText = "DRIVING",
            tagColor = Color(0xFF1B8A88),
            imageContent = {
                Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0A0C13)), contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.size(30.dp)) {
                        drawLine(AccentCyan, Offset(0f, 0f), Offset(size.width, size.height), strokeWidth = 4f)
                        drawLine(AccentCyan, Offset(size.width, 0f), Offset(0f, size.height), strokeWidth = 4f)
                    }
                }
            }
        )
    }
}

@Composable
private fun EchoItem(
    title: String,
    artist: String,
    tagText: String,
    tagColor: Color,
    imageContent: @Composable () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
        ) {
            imageContent()
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = artist,
                color = TextSecondary,
                fontSize = 14.sp
            )
        }
        Box(
            modifier = Modifier
                .border(1.dp, tagColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                text = tagText,
                color = tagColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Icon(
            imageVector = Icons.Rounded.MoreVert,
            contentDescription = "Options",
            tint = TextSecondary,
            modifier = Modifier.size(24.dp)
        )
    }
}
