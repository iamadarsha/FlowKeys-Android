package com.flowkeys.android.ui.history

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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

data class HistoryItem(
    val id: String,
    val text: String,
    val language: String,
    val languageCode: String,
    val app: String,
    val time: String,
    val duration: String,
    val metric: String
)

@Composable
fun HistoryScreen() {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("all") }

    val allItems = remember {
        listOf(
            HistoryItem(
                id = "1",
                text = "কাল আমাকে চারটের মিটিংয়ের জন্য বেরোতে হবে।",
                language = "বাংলা",
                languageCode = "bn",
                app = "WhatsApp",
                time = "10:42 AM",
                duration = "0:04",
                metric = "42 words/min"
            ),
            HistoryItem(
                id = "2",
                text = "Can you send the updated deck before 3 PM staging sync?",
                language = "English",
                languageCode = "en",
                app = "Gmail",
                time = "10:37 AM",
                duration = "0:06",
                metric = "Low latency · 18ms"
            ),
            HistoryItem(
                id = "3",
                text = "कल राहुल को बोल देना कि मैं पाँच बजे पहुँच जाऊँगा।",
                language = "हिंदी",
                languageCode = "hi",
                app = "WhatsApp",
                time = "9:15 AM",
                duration = "0:05",
                metric = "Punctuation auto-tuned"
            ),
            HistoryItem(
                id = "4",
                text = "আজকের ক্লায়েন্ট কলটা reschedule করা যাবে কি?",
                language = "বাংলা/EN",
                languageCode = "bn",
                app = "Slack",
                time = "Yesterday",
                duration = "0:03",
                metric = "Colloquial Bengali tuned"
            )
        )
    }

    val filteredItems = allItems.filter { item ->
        val matchesFilter = when (selectedFilter) {
            "bn" -> item.languageCode == "bn"
            "hi" -> item.languageCode == "hi"
            "en" -> item.languageCode == "en"
            else -> true
        }
        val matchesSearch = searchQuery.isBlank() ||
                item.text.contains(searchQuery, ignoreCase = true) ||
                item.app.contains(searchQuery, ignoreCase = true)
        matchesFilter && matchesSearch
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(StitchCanvas)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Privacy Banner
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = StitchSurface1),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, StitchBorderSubtle, RoundedCornerShape(12.dp))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(StitchSurface2)
                            .border(1.dp, StitchBorderSubtle, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Encrypted",
                            tint = StitchSuccess,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Stored locally in encrypted sandbox",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = StitchTextPrimary
                        )
                        Text(
                            text = "Zero telemetries · Never synced to cloud",
                            fontSize = 11.sp,
                            color = StitchTextMuted
                        )
                    }
                }
            }
        }

        // Search Bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search transcripts, apps, words...", color = StitchTextMuted, fontSize = 13.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = StitchTextMuted
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = StitchSurface1,
                    unfocusedContainerColor = StitchSurface1,
                    focusedBorderColor = StitchAccentCoral,
                    unfocusedBorderColor = StitchBorderSubtle,
                    focusedTextColor = StitchTextPrimary,
                    unfocusedTextColor = StitchTextPrimary
                ),
                singleLine = true
            )
        }

        // Filter Chips (44dp touch target height)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChipItem(
                    label = "All (${allItems.size})",
                    isSelected = selectedFilter == "all",
                    onClick = { selectedFilter = "all" }
                )
                FilterChipItem(
                    label = "বাংলা (BN)",
                    isSelected = selectedFilter == "bn",
                    onClick = { selectedFilter = "bn" }
                )
                FilterChipItem(
                    label = "हिंदी (HI)",
                    isSelected = selectedFilter == "hi",
                    onClick = { selectedFilter = "hi" }
                )
                FilterChipItem(
                    label = "English (EN)",
                    isSelected = selectedFilter == "en",
                    onClick = { selectedFilter = "en" }
                )
            }
        }

        // Items or Empty State
        if (filteredItems.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No dictation transcripts found",
                        color = StitchTextMuted,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            items(filteredItems) { item ->
                HistoryCard(
                    item = item,
                    onCopy = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("FlowKeys Dictation", item.text)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun FilterChipItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .heightIn(min = 40.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) StitchAccentCoral else StitchSurface2)
            .border(
                1.dp,
                if (isSelected) StitchAccentPeach else StitchBorderSubtle,
                RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color.White else StitchTextSecondary
        )
    }
}

@Composable
fun HistoryCard(item: HistoryItem, onCopy: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = StitchSurface1),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, StitchBorderSubtle, RoundedCornerShape(14.dp))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row: App name + Language + Duration
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (item.app == "Gmail") Icons.Default.Mail else Icons.AutoMirrored.Filled.Chat,
                        contentDescription = item.app,
                        tint = if (item.app == "Gmail") StitchAccentCoral else StitchAccentPeach,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${item.time} · ${item.app}",
                        fontSize = 12.sp,
                        color = StitchTextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(StitchSurface2)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = item.language,
                            fontSize = 11.sp,
                            color = StitchAccentCoral,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = "Duration",
                        tint = StitchTextMuted,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = item.duration,
                        fontSize = 11.sp,
                        color = StitchTextMuted
                    )
                }
            }

            // Dictated Transcript
            Text(
                text = item.text,
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal,
                color = StitchTextPrimary,
                lineHeight = 22.sp
            )

            // Footer Row: Metric & 44dp Copy Action Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(StitchSurface2)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.metric,
                    fontSize = 11.sp,
                    color = StitchTextMuted
                )

                // 44dp accessible touch target Copy Button
                Box(
                    modifier = Modifier
                        .heightIn(min = 36.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(StitchSurface3)
                        .border(1.dp, StitchBorderSubtle, RoundedCornerShape(6.dp))
                        .clickable(onClick = onCopy)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy",
                            tint = StitchTextPrimary,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "Copy",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = StitchTextPrimary
                        )
                    }
                }
            }
        }
    }
}
