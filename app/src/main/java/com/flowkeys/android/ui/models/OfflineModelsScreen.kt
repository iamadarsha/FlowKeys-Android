package com.flowkeys.android.ui.models

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.OfflineBolt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flowkeys.android.ui.theme.StitchAccentCoral
import com.flowkeys.android.ui.theme.StitchAccentPeach
import com.flowkeys.android.ui.theme.StitchBorderSubtle
import com.flowkeys.android.ui.theme.StitchCanvas
import com.flowkeys.android.ui.theme.StitchSuccess
import com.flowkeys.android.ui.theme.StitchSurface1
import com.flowkeys.android.ui.theme.StitchSurface2
import com.flowkeys.android.ui.theme.StitchSurface3
import com.flowkeys.android.ui.theme.StitchTextMuted
import com.flowkeys.android.ui.theme.StitchTextPrimary
import com.flowkeys.android.ui.theme.StitchTextSecondary

@Composable
fun OfflineModelsScreen() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(StitchCanvas)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Speech Recognition",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = StitchTextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Active: Android Speech Services · Cloud: Groq Whisper (optional)",
                        fontSize = 12.sp,
                        color = StitchTextMuted,
                        maxLines = 2
                    )
                }
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(StitchSurface2)
                        .border(1.dp, StitchBorderSubtle, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.OfflineBolt,
                        contentDescription = null,
                        tint = StitchAccentCoral,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Bento Card: Total Local Footprint & Status
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = StitchSurface1),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, StitchBorderSubtle, RoundedCornerShape(14.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(StitchSuccess)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "ON-DEVICE NEURAL RECOGNITION",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = StitchSuccess,
                                letterSpacing = 0.5.sp
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(StitchSuccess.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "ACTIVE & READY",
                                fontSize = 10.sp,
                                color = StitchSuccess,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "245",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = StitchTextPrimary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "MB",
                                fontSize = 16.sp,
                                color = StitchTextSecondary,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                        Text(
                            text = "Hardware DSP Active",
                            fontSize = 12.sp,
                            color = StitchTextMuted
                        )
                    }

                    // Progress Bar Meter
                    LinearProgressIndicator(
                        progress = { 0.24f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = StitchAccentCoral,
                        trackColor = StitchSurface2
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "• Bengali + Hindi (188 MB)",
                            fontSize = 11.sp,
                            color = StitchTextSecondary
                        )
                        Text(
                            text = "• English Edge (57 MB)",
                            fontSize = 11.sp,
                            color = StitchTextSecondary
                        )
                    }
                }
            }
        }

        // Section: Language Core Packs
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Layers,
                        contentDescription = null,
                        tint = StitchAccentCoral,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Language Core Packs",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = StitchTextPrimary
                    )
                }
                Text(
                    text = "3 INSTALLED",
                    fontSize = 11.sp,
                    color = StitchSuccess,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Model 1: English Installed
        item {
            ModelCard(
                glyph = "Aa",
                title = "English (Global / IN)",
                engineTag = "WHISPER-EDGE ARCHITECTURE",
                size = "57 MB",
                latency = "18 ms",
                accuracy = "99.4%",
                isInstalled = true
            )
        }

        // Model 2: Bengali
        item {
            ModelCard(
                glyph = "অ",
                title = "Bengali (বাংলা)",
                engineTag = "KOLKATA CONFORMER v4.2",
                size = "94 MB",
                latency = "21 ms",
                accuracy = "98.9%",
                isInstalled = true
            )
        }

        // Model 3: Hindi
        item {
            ModelCard(
                glyph = "अ",
                title = "Hindi (हिन्दी)",
                engineTag = "INDIC CONFORMER v4.2",
                size = "94 MB",
                latency = "20 ms",
                accuracy = "99.1%",
                isInstalled = true
            )
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun ModelCard(
    glyph: String,
    title: String,
    engineTag: String,
    size: String,
    latency: String,
    accuracy: String,
    isInstalled: Boolean
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = StitchSurface1),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, StitchBorderSubtle, RoundedCornerShape(14.dp))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row: Glyph + Title + Installed Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(StitchSurface2)
                            .border(1.dp, StitchBorderSubtle, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = glyph,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = StitchAccentCoral
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = title,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = StitchTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = engineTag,
                            fontSize = 10.sp,
                            color = StitchAccentPeach,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.4.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (isInstalled) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(StitchSuccess.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = StitchSuccess,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "READY",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = StitchSuccess
                            )
                        }
                    }
                }
            }

            // Specs Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(StitchSurface2)
                    .padding(vertical = 8.dp, horizontal = 10.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "FOOTPRINT", fontSize = 9.sp, color = StitchTextMuted, fontWeight = FontWeight.Medium)
                    Text(text = size, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = StitchTextPrimary)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "LATENCY", fontSize = 9.sp, color = StitchTextMuted, fontWeight = FontWeight.Medium)
                    Text(text = latency, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = StitchSuccess)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "ACCURACY", fontSize = 9.sp, color = StitchTextMuted, fontWeight = FontWeight.Medium)
                    Text(text = accuracy, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = StitchTextPrimary)
                }
            }
        }
    }
}
