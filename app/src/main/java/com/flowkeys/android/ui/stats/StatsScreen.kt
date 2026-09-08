package com.flowkeys.android.ui.stats

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flowkeys.android.dictionary.LearnedVocabularyStore
import com.flowkeys.android.stats.LocalStatsRepository
import com.flowkeys.android.ui.theme.StitchAccentCoral
import com.flowkeys.android.ui.theme.StitchAccentPeach
import com.flowkeys.android.ui.theme.StitchBorderSubtle
import com.flowkeys.android.ui.theme.StitchCanvas
import com.flowkeys.android.ui.theme.StitchError
import com.flowkeys.android.ui.theme.StitchSuccess
import com.flowkeys.android.ui.theme.StitchSurface1
import com.flowkeys.android.ui.theme.StitchSurface2
import com.flowkeys.android.ui.theme.StitchSurface3
import com.flowkeys.android.ui.theme.StitchTextMuted
import com.flowkeys.android.ui.theme.StitchTextPrimary
import com.flowkeys.android.ui.theme.StitchTextSecondary

@Composable
fun StatsScreen() {
    val stats by LocalStatsRepository.stats.collectAsState()
    val vocabCount = LearnedVocabularyStore.getEntryCount()
    val correctionCount = LearnedVocabularyStore.getCorrectionCount()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(StitchCanvas)
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Hero Metric: Words & Time Saved
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                modifier = Modifier.weight(1f),
                title = "WORDS SPOKEN",
                value = stats.totalWordsSpoken.toString(),
                subtitle = "${stats.totalSessions} dictations",
                icon = Icons.Default.Bolt,
                accentColor = StitchAccentCoral
            )

            MetricCard(
                modifier = Modifier.weight(1f),
                title = "EST. TIME SAVED",
                value = stats.formattedTimeSaved,
                subtitle = "vs 35 WPM typing",
                icon = Icons.Default.Schedule,
                accentColor = StitchSuccess
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Streaks Card
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                modifier = Modifier.weight(1f),
                title = "CURRENT STREAK",
                value = "${stats.currentStreakDays} days",
                subtitle = "Active daily practice",
                icon = Icons.Default.LocalFireDepartment,
                accentColor = StitchAccentPeach
            )

            MetricCard(
                modifier = Modifier.weight(1f),
                title = "BEST STREAK",
                value = "${stats.longestStreakDays} days",
                subtitle = "Personal record",
                icon = Icons.Default.AutoAwesome,
                accentColor = StitchAccentCoral
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Language Distribution
        Text(
            text = "Language Distribution",
            color = StitchTextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(10.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = StitchSurface1),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, StitchBorderSubtle, RoundedCornerShape(14.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                val totalWords = stats.totalWordsSpoken.coerceAtLeast(1L)
                val enPercent = (stats.englishWords * 100 / totalWords).toInt()
                val hiPercent = (stats.hindiWords * 100 / totalWords).toInt()
                val bnPercent = (stats.bengaliWords * 100 / totalWords).toInt()

                LanguageBar(name = "English", count = stats.englishWords, percent = enPercent, color = StitchAccentCoral)
                Spacer(modifier = Modifier.height(12.dp))
                LanguageBar(name = "বাংলা (Bengali)", count = stats.bengaliWords, percent = bnPercent, color = StitchSuccess)
                Spacer(modifier = Modifier.height(12.dp))
                LanguageBar(name = "हिंदी (Hindi)", count = stats.hindiWords, percent = hiPercent, color = StitchAccentPeach)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Personalization Stats
        Text(
            text = "Personalization & Learning",
            color = StitchTextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(10.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = StitchSurface1),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, StitchBorderSubtle, RoundedCornerShape(14.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Psychology, contentDescription = null, tint = StitchAccentCoral, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(text = "Learned Vocabulary", color = StitchTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text(text = "$vocabCount custom terms & proper nouns recognized", color = StitchTextSecondary, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Translate, contentDescription = null, tint = StitchSuccess, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(text = "Corrections Remembered", color = StitchTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text(text = "$correctionCount learned speech corrections active", color = StitchTextSecondary, fontSize = 12.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Privacy Guarantee Card
        Card(
            colors = CardDefaults.cardColors(containerColor = StitchSurface2),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, StitchBorderSubtle, RoundedCornerShape(12.dp))
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Shield, contentDescription = null, tint = StitchSuccess, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "100% on-device statistics. No telemetry or speech metadata is ever transmitted to any external server.",
                    color = StitchTextMuted,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Reset Button
        OutlinedButton(
            onClick = { LocalStatsRepository.clearStats() },
            colors = ButtonDefaults.outlinedButtonColors(contentColor = StitchError),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
        ) {
            Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = StitchError, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Reset Local Stats", color = StitchError, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun MetricCard(
    modifier: Modifier,
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = StitchSurface1),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier.border(1.dp, StitchBorderSubtle, RoundedCornerShape(14.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = StitchTextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                color = StitchTextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = StitchTextSecondary,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun LanguageBar(name: String, count: Long, percent: Int, color: Color) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = name, color = StitchTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text(text = "$count words ($percent%)", color = StitchTextSecondary, fontSize = 12.sp)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(StitchSurface3)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = (percent / 100f).coerceIn(0f, 1f))
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(color)
            )
        }
    }
}
