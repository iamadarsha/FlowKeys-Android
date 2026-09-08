package com.flowkeys.android.ui.dictionary

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flowkeys.android.data.DataStoreManager
import com.flowkeys.android.dictionary.LearnedVocabularyEntry
import com.flowkeys.android.dictionary.LearnedVocabularyStore
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
import kotlinx.coroutines.launch

@Composable
fun LearnedVocabularyScreen() {
    val context = LocalContext.current
    val dataStore = DataStoreManager(context)
    val scope = rememberCoroutineScope()

    val isEnabled by dataStore.isLearnedVocabEnabled.collectAsState(initial = true)
    val vocabularyList by LearnedVocabularyStore.vocabularyList.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(StitchCanvas)
            .padding(horizontal = 20.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))

            // Enable/Disable Card
            Card(
                colors = CardDefaults.cardColors(containerColor = StitchSurface1),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, StitchBorderSubtle, RoundedCornerShape(14.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(StitchAccentCoral.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Psychology, contentDescription = null, tint = StitchAccentCoral, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Auto-Learn Vocabulary", color = StitchTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            Text("Adapts locally to repeated terms & corrections", color = StitchTextSecondary, fontSize = 12.sp)
                        }
                    }

                    Switch(
                        checked = isEnabled,
                        onCheckedChange = { checked ->
                            scope.launch {
                                dataStore.setLearnedVocabEnabled(checked)
                                LearnedVocabularyStore.setEnabled(checked)
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = StitchAccentCoral,
                            uncheckedThumbColor = StitchTextMuted,
                            uncheckedTrackColor = StitchSurface3
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Privacy Disclosure Banner
            Card(
                colors = CardDefaults.cardColors(containerColor = StitchSurface2),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, StitchBorderSubtle, RoundedCornerShape(12.dp))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Shield, contentDescription = null, tint = StitchSuccess, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "100% on-device learning. No contact permissions required. Compact context hints are used locally to personalize speech recognition.",
                        color = StitchTextMuted,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Learned Terms (${vocabularyList.size})",
                    color = StitchTextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                if (vocabularyList.isNotEmpty()) {
                    OutlinedButton(
                        onClick = { LearnedVocabularyStore.clearAll() },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = StitchError),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = StitchError, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clear All", color = StitchError, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (vocabularyList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(StitchSurface1)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No learned vocabulary yet", color = StitchTextSecondary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "As you dictate and correct terms, FlowKeys will automatically learn them here.",
                            color = StitchTextMuted,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            modifier = Modifier.padding(horizontal = 16.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }

        items(vocabularyList, key = { it.term }) { entry ->
            LearnedWordItem(entry = entry, onDelete = { LearnedVocabularyStore.deleteEntry(entry.term) })
            Spacer(modifier = Modifier.height(8.dp))
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun LearnedWordItem(
    entry: LearnedVocabularyEntry,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = StitchSurface1),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, StitchBorderSubtle, RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = entry.canonicalForm,
                        color = StitchTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (entry.term.lowercase() != entry.canonicalForm.lowercase()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "(from \"${entry.term}\")",
                            color = StitchTextMuted,
                            fontSize = 12.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                when (entry.source) {
                                    LearnedVocabularyEntry.SOURCE_CORRECTION -> StitchSuccess.copy(alpha = 0.2f)
                                    LearnedVocabularyEntry.SOURCE_PROPER_NOUN -> StitchAccentCoral.copy(alpha = 0.2f)
                                    else -> StitchSurface3
                                }
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = when (entry.source) {
                                LearnedVocabularyEntry.SOURCE_CORRECTION -> "CORRECTION"
                                LearnedVocabularyEntry.SOURCE_PROPER_NOUN -> "PROPER NOUN"
                                else -> "FREQUENCY"
                            },
                            color = when (entry.source) {
                                LearnedVocabularyEntry.SOURCE_CORRECTION -> StitchSuccess
                                LearnedVocabularyEntry.SOURCE_PROPER_NOUN -> StitchAccentPeach
                                else -> StitchTextMuted
                            },
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "Used ${entry.frequency} time${if (entry.frequency != 1) "s" else ""}" +
                            if (entry.correctionCount > 0) " • ${entry.correctionCount} corrections" else "",
                    color = StitchTextSecondary,
                    fontSize = 11.sp
                )
            }

            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = StitchTextMuted, modifier = Modifier.size(18.dp))
            }
        }
    }
}
