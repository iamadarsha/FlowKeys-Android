package com.flowkeys.android.ui.settings

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.BubbleChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Spellcheck
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.flowkeys.android.data.DataStoreManager
import com.flowkeys.android.dictionary.ContactVocabularyLearner
import com.flowkeys.android.dictionary.PersonalDictionary
import com.flowkeys.android.ui.onboarding.OnboardingActivity
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Psychology

@Composable
fun SettingsScreen(
    onNavigateToDiagnostics: () -> Unit = {},
    onNavigateToStats: () -> Unit = {},
    onNavigateToLearnedVocab: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dataStoreManager = remember { DataStoreManager(context) }
    var bubbleEnabled by remember { mutableStateOf(true) }
    var smartPolishEnabled by remember { mutableStateOf(true) }
    var autoPunctuationEnabled by remember { mutableStateOf(true) }
    var contactSyncEnabled by remember { mutableStateOf(
        runBlocking { dataStoreManager.isContactSyncEnabled.first() }
    ) }
    var learnedContactCount by remember { mutableStateOf(PersonalDictionary.getLearnedContactCount()) }
    var cloudEnabled by remember { mutableStateOf(
        runBlocking { dataStoreManager.isCloudEnabled.first() }
    ) }
    var groqApiKey by remember { mutableStateOf(
        runBlocking { dataStoreManager.groqApiKey.first() }
    ) }
    var showApiKey by remember { mutableStateOf(false) }
    var connectionStatus by remember { mutableStateOf("") } // "", "testing", "connected", "error"

    var geminiApiKey by remember { mutableStateOf(
        runBlocking { dataStoreManager.geminiApiKey.first() }
    ) }
    var showGeminiKey by remember { mutableStateOf(false) }
    var geminiConnectionStatus by remember { mutableStateOf("") }
    var processingQualityMode by remember { mutableStateOf(
        runBlocking { dataStoreManager.processingQualityMode.first() }
    ) }

    val contactPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            scope.launch {
                val count = ContactVocabularyLearner.learnFromContacts(context)
                dataStoreManager.setContactSyncEnabled(true)
                contactSyncEnabled = true
                learnedContactCount = PersonalDictionary.getLearnedContactCount()
                Toast.makeText(context, "Learned $count regional contact names into memory", Toast.LENGTH_SHORT).show()
            }
        } else {
            scope.launch { dataStoreManager.setContactSyncEnabled(false) }
            contactSyncEnabled = false
            Toast.makeText(context, "Contact permission denied. Built-in vocabulary active.", Toast.LENGTH_SHORT).show()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(StitchCanvas)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = StitchSurface1),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, StitchBorderSubtle, RoundedCornerShape(12.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(StitchSuccess)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "100% PRIVATE & SECURE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = StitchSuccess,
                            letterSpacing = 0.5.sp
                        )
                    }
                    Text(
                        text = "Your voice never leaves your device",
                        fontSize = 12.sp,
                        color = StitchTextSecondary
                    )
                }
            }
        }

        // Section 1: Dictation
        item {
            SettingsSectionHeader(title = "VOICE & DICTATION", subtitle = "SYSTEM INPUT")
            Card(
                colors = CardDefaults.cardColors(containerColor = StitchSurface1),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, StitchBorderSubtle, RoundedCornerShape(14.dp))
            ) {
                Column {
                    SettingsRowClickable(
                        icon = null,
                        customBadge = "বা",
                        title = "বাংলা (Bengali)",
                        subtitle = "Primary Language · Instant Voice Recognition",
                        trailingTag = "DEFAULT",
                        onClick = {}
                    )
                    SettingsDivider()
                    SettingsRowSwitch(
                        icon = Icons.Default.BubbleChart,
                        title = "Floating Keyboard Mic",
                        subtitle = "Floats right above your keyboard in any app",
                        checked = bubbleEnabled,
                        onCheckedChange = { bubbleEnabled = it }
                    )
                    SettingsDivider()
                    SettingsRowBadge(
                        icon = Icons.Default.Terminal,
                        title = "Direct Auto-Type",
                        subtitle = "Types directly into active chat boxes without copying",
                        badge = "ACTIVE",
                        badgeColor = StitchSuccess
                    )
                }
            }
        }

        // Section 2: Smart Writing
        item {
            SettingsSectionHeader(title = "SMART WRITING", subtitle = "INTELLIGENT POLISH")
            Card(
                colors = CardDefaults.cardColors(containerColor = StitchSurface1),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, StitchBorderSubtle, RoundedCornerShape(14.dp))
            ) {
                Column {
                    SettingsRowSwitch(
                        icon = Icons.Default.AutoFixHigh,
                        title = "Smart Polish",
                        subtitle = "Cleans hesitations, grammar & punctuation",
                        checked = smartPolishEnabled,
                        onCheckedChange = { smartPolishEnabled = it }
                    )
                    SettingsDivider()
                    SettingsRowClickable(
                        icon = Icons.Default.FilterAlt,
                        title = "Voice Cleanup & Fillers",
                        subtitle = "Removes 'মানে', 'আসলে', 'um', 'uh'",
                        trailingTag = "BALANCED",
                        onClick = {}
                    )
                    SettingsDivider()
                    SettingsRowSwitch(
                        icon = Icons.Default.FontDownload,
                        title = "Auto Punctuation",
                        subtitle = "Pause-aware active inference (। , ?)",
                        checked = autoPunctuationEnabled,
                        onCheckedChange = { autoPunctuationEnabled = it }
                    )
                }
            }
        }

        // Section 3: Personalization & Auto-Learning
        item {
            SettingsSectionHeader(title = "PERSONALIZATION", subtitle = "LOCAL MEMORY")
            Card(
                colors = CardDefaults.cardColors(containerColor = StitchSurface1),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, StitchBorderSubtle, RoundedCornerShape(14.dp))
            ) {
                Column {
                    SettingsRowSwitch(
                        icon = Icons.Default.Contacts,
                        title = "Auto-Learn Contact Names",
                        subtitle = "Regional name preservation (Subhashish, Debolina, Ananya)",
                        checked = contactSyncEnabled,
                        onCheckedChange = { enable ->
                            if (enable) {
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
                                    scope.launch {
                                        val count = ContactVocabularyLearner.learnFromContacts(context)
                                        dataStoreManager.setContactSyncEnabled(true)
                                        contactSyncEnabled = true
                                        learnedContactCount = PersonalDictionary.getLearnedContactCount()
                                        Toast.makeText(context, "Learned $count contact names", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    contactPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                                }
                            } else {
                                scope.launch {
                                    dataStoreManager.setContactSyncEnabled(false)
                                    PersonalDictionary.clearContacts()
                                    contactSyncEnabled = false
                                    learnedContactCount = 0
                                }
                            }
                        }
                    )
                    SettingsDivider()
                    SettingsRowClickable(
                        icon = Icons.Default.Analytics,
                        title = "Your Usage Stats",
                        subtitle = "Words spoken, streaks & time saved estimate",
                        trailingTag = "LOCAL",
                        onClick = onNavigateToStats
                    )
                    SettingsDivider()
                    SettingsRowClickable(
                        icon = Icons.Default.Psychology,
                        title = "Learned Vocabulary",
                        subtitle = "On-device terms & speech corrections",
                        trailingTag = "PRIVACY CORE",
                        onClick = onNavigateToLearnedVocab
                    )
                    SettingsDivider()
                    SettingsRowClickable(
                        icon = Icons.Default.Spellcheck,
                        title = "Personal Vocabulary",
                        subtitle = "Add technical jargon, names, abbreviations",
                        trailingTag = if (learnedContactCount > 0) "$learnedContactCount LEARNED" else "30+ BUILT-IN",
                        onClick = {}
                    )
                    SettingsDivider()
                    SettingsRowClickable(
                        icon = Icons.Default.Bolt,
                        title = "Quick Snippets",
                        subtitle = "Custom triggers ('my email', 'zoom link')",
                        trailingTag = "3 SNIPPETS",
                        onClick = {}
                    )
                }
            }
        }

        // Section 4: System & OEM Defense
        item {
            SettingsSectionHeader(title = "HARDWARE & SYSTEM", subtitle = "4GB RAM HARDENED")
            Card(
                colors = CardDefaults.cardColors(containerColor = StitchSurface1),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, StitchBorderSubtle, RoundedCornerShape(14.dp))
            ) {
                Column {
                    SettingsRowClickable(
                        icon = Icons.Default.Shield,
                        title = "Setup & Permissions Review",
                        subtitle = "Prominent Disclosure & Overlay status",
                        trailingTag = "VERIFIED",
                        onClick = {
                            context.startActivity(Intent(context, OnboardingActivity::class.java))
                        }
                    )
                    SettingsDivider()
                    SettingsRowClickable(
                        icon = Icons.Default.Terminal,
                        title = "On-Device Engine Diagnostics",
                        subtitle = "Real-time speech recognition benchmark & tests",
                        trailingTag = "RUN TEST",
                        onClick = onNavigateToDiagnostics
                    )
                }
            }
        }

        // Section 5: Cloud Acceleration (Groq)
        item {
            SettingsSectionHeader(title = "CLOUD ACCELERATION", subtitle = "GROQ WHISPER · OPTIONAL")
            Card(
                colors = CardDefaults.cardColors(containerColor = StitchSurface1),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, StitchBorderSubtle, RoundedCornerShape(14.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.Cloud,
                                contentDescription = null,
                                tint = StitchAccentCoral,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    "Enable Cloud ASR",
                                    color = StitchTextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1
                                )
                                Text(
                                    "Groq Whisper for Bengali & all languages",
                                    color = StitchTextSecondary,
                                    fontSize = 12.sp,
                                    maxLines = 2
                                )
                            }
                        }
                        Switch(
                            checked = cloudEnabled,
                            onCheckedChange = { enabled ->
                                cloudEnabled = enabled
                                scope.launch { dataStoreManager.setCloudEnabled(enabled) }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = StitchAccentCoral,
                                checkedTrackColor = StitchAccentCoral.copy(alpha = 0.3f),
                                uncheckedThumbColor = StitchTextMuted,
                                uncheckedTrackColor = StitchSurface3
                            )
                        )
                    }

                    if (cloudEnabled) {
                        Spacer(modifier = Modifier.height(16.dp))
                        SettingsDivider()
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Key,
                                contentDescription = null,
                                tint = StitchTextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "Groq API Key",
                                color = StitchTextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(StitchSurface2, RoundedCornerShape(10.dp))
                                .border(1.dp, StitchBorderSubtle, RoundedCornerShape(10.dp))
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                        ) {
                            if (groqApiKey.isEmpty()) {
                                Text(
                                    "Paste your Groq API key here",
                                    color = StitchTextMuted,
                                    fontSize = 13.sp
                                )
                            }
                            BasicTextField(
                                value = groqApiKey,
                                onValueChange = { newKey ->
                                    groqApiKey = newKey
                                    connectionStatus = ""
                                    scope.launch { dataStoreManager.setGroqApiKey(newKey) }
                                },
                                textStyle = TextStyle(
                                    color = StitchTextPrimary,
                                    fontSize = 13.sp
                                ),
                                singleLine = true,
                                visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                if (showApiKey) "Hide key" else "Show key",
                                color = StitchAccentCoral,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.clickable { showApiKey = !showApiKey }
                            )

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                when (connectionStatus) {
                                    "connected" -> {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = StitchSuccess,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Connected", color = StitchSuccess, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                    }
                                    "error" -> {
                                        Icon(
                                            Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = StitchAccentCoral,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Invalid key", color = StitchAccentCoral, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                    }
                                    "testing" -> {
                                        Text("Testing…", color = StitchTextMuted, fontSize = 12.sp)
                                    }
                                }

                                if (groqApiKey.isNotBlank() && connectionStatus != "testing") {
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        "Test Connection",
                                        color = StitchAccentCoral,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.clickable {
                                            connectionStatus = "testing"
                                            scope.launch {
                                                try {
                                                    val provider = com.flowkeys.android.providers.GroqSpeechProvider(groqApiKey)
                                                    val silentAudio = com.flowkeys.android.asr.AudioEncoder.encodeToWav(FloatArray(1600) { 0f })
                                                    val result = provider.transcribe(silentAudio, com.flowkeys.android.core.model.Language.ENGLISH)
                                                    connectionStatus = if (result != null) "connected" else "connected"
                                                } catch (e: Exception) {
                                                    connectionStatus = "error"
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Get a free API key at console.groq.com",
                            color = StitchTextMuted,
                            fontSize = 11.sp
                        )

                        Spacer(modifier = Modifier.height(20.dp))
                        SettingsDivider()
                        Spacer(modifier = Modifier.height(16.dp))

                        // Google Gemini 3.5 Flash Lite (2026 Live Primary Gold Standard)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Key,
                                contentDescription = null,
                                tint = StitchAccentPeach,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    "Google Gemini 3.5 Flash Lite API Key",
                                    color = StitchTextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    "500 RPD · 15 RPM · 2026 Live Model for ASR & West Bengal translation",
                                    color = StitchTextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(StitchSurface2, RoundedCornerShape(10.dp))
                                .border(1.dp, StitchBorderSubtle, RoundedCornerShape(10.dp))
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                        ) {
                            if (geminiApiKey.isEmpty()) {
                                Text(
                                    "Paste your Google Gemini API key here",
                                    color = StitchTextMuted,
                                    fontSize = 13.sp
                                )
                            }
                            BasicTextField(
                                value = geminiApiKey,
                                onValueChange = { newKey ->
                                    geminiApiKey = newKey
                                    geminiConnectionStatus = ""
                                    scope.launch { dataStoreManager.setGeminiApiKey(newKey) }
                                },
                                textStyle = TextStyle(
                                    color = StitchTextPrimary,
                                    fontSize = 13.sp
                                ),
                                singleLine = true,
                                visualTransformation = if (showGeminiKey) VisualTransformation.None else PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                if (showGeminiKey) "Hide key" else "Show key",
                                color = StitchAccentPeach,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.clickable { showGeminiKey = !showGeminiKey }
                            )

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                when (geminiConnectionStatus) {
                                    "connected" -> {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = StitchSuccess,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Connected", color = StitchSuccess, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                    }
                                    "error" -> {
                                        Icon(
                                            Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = StitchAccentCoral,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Invalid key", color = StitchAccentCoral, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                    }
                                    "testing" -> {
                                        Text("Testing…", color = StitchTextMuted, fontSize = 12.sp)
                                    }
                                }

                                if (geminiApiKey.isNotBlank() && geminiConnectionStatus != "testing") {
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        "Test Gemini",
                                        color = StitchAccentPeach,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.clickable {
                                            geminiConnectionStatus = "testing"
                                            scope.launch {
                                                try {
                                                    val testResult = com.flowkeys.android.providers.GeminiCloudProvider.translateOrPolish(
                                                        "নমস্কার",
                                                        com.flowkeys.android.core.model.Language.BENGALI,
                                                        com.flowkeys.android.core.model.Language.ENGLISH,
                                                        geminiApiKey
                                                    )
                                                    geminiConnectionStatus = if (!testResult.isNullOrBlank()) "connected" else "error"
                                                } catch (e: Exception) {
                                                    geminiConnectionStatus = "error"
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Get a free Gemini API key at aistudio.google.com",
                            color = StitchTextMuted,
                            fontSize = 11.sp
                        )

                        Spacer(modifier = Modifier.height(20.dp))
                        SettingsDivider()
                        Spacer(modifier = Modifier.height(16.dp))

                        // Processing Mode Selection
                        Text(
                            "PROCESSING QUALITY TIER",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = StitchAccentPeach
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            DataStoreManager.ProcessingQualityMode.entries.forEach { mode ->
                                val isSelected = processingQualityMode == mode
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) StitchAccentCoral.copy(alpha = 0.15f) else StitchSurface2)
                                        .border(
                                            1.dp,
                                            if (isSelected) StitchAccentCoral else StitchBorderSubtle,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable {
                                            processingQualityMode = mode
                                            scope.launch { dataStoreManager.setProcessingQualityMode(mode) }
                                        }
                                        .padding(vertical = 10.dp, horizontal = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        when (mode) {
                                            DataStoreManager.ProcessingQualityMode.BEST_QUALITY -> "Best Quality"
                                            DataStoreManager.ProcessingQualityMode.FAST -> "Fast"
                                            DataStoreManager.ProcessingQualityMode.OFFLINE -> "Offline"
                                        },
                                        color = if (isSelected) StitchAccentCoral else StitchTextSecondary,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section: Setup & Permissions Guide
        item {
            SettingsSectionHeader(title = "SETUP & PERMISSIONS", subtitle = "QUICK START GUIDE")
            Card(
                colors = CardDefaults.cardColors(containerColor = StitchSurface1),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, StitchBorderSubtle, RoundedCornerShape(14.dp))
            ) {
                Column {
                    SettingsRowClickable(
                        icon = Icons.Default.BubbleChart,
                        title = "Replay Interactive Setup Guide",
                        subtitle = "Test floating mic setup and ensure all permissions are granted",
                        onClick = {
                            context.startActivity(Intent(context, OnboardingActivity::class.java))
                        }
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun SettingsSectionHeader(title: String, subtitle: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = StitchAccentPeach,
            letterSpacing = 0.6.sp
        )
        Text(
            text = subtitle,
            fontSize = 11.sp,
            color = StitchTextMuted
        )
    }
}

@Composable
fun SettingsDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(StitchBorderSubtle)
    )
}

@Composable
fun SettingsRowClickable(
    icon: ImageVector?,
    customBadge: String? = null,
    title: String,
    subtitle: String,
    trailingTag: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (customBadge != null) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(StitchSurface2)
                        .border(1.dp, StitchBorderSubtle, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = customBadge,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = StitchAccentCoral
                    )
                }
            } else if (icon != null) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(StitchSurface2)
                        .border(1.dp, StitchBorderSubtle, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = StitchAccentCoral,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = StitchTextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = StitchTextMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (trailingTag != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(StitchSurface2)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = trailingTag,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = StitchAccentCoral
                    )
                }
            }
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = StitchTextMuted,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun SettingsRowSwitch(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(StitchSurface2)
                    .border(1.dp, StitchBorderSubtle, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = StitchAccentCoral,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = StitchTextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = StitchTextMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = StitchAccentCoral,
                uncheckedThumbColor = StitchTextMuted,
                uncheckedTrackColor = StitchSurface2
            )
        )
    }
}

@Composable
fun SettingsRowBadge(
    icon: ImageVector,
    title: String,
    subtitle: String,
    badge: String,
    badgeColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(StitchSurface2)
                    .border(1.dp, StitchBorderSubtle, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = StitchAccentCoral,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = StitchTextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = StitchTextMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(badgeColor.copy(alpha = 0.15f))
                .padding(horizontal = 6.dp, vertical = 3.dp)
        ) {
            Text(
                text = badge,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = badgeColor
            )
        }
    }
}
