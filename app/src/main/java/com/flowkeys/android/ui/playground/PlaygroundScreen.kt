package com.flowkeys.android.ui.playground

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flowkeys.android.asr.SpeechRecognitionManager
import com.flowkeys.android.core.coordinator.FlowKeysCoordinator
import com.flowkeys.android.core.model.DictationState
import com.flowkeys.android.core.model.Language
import com.flowkeys.android.core.model.ScriptMode
import com.flowkeys.android.polish.translation.TranslationEngine
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
import kotlinx.coroutines.launch

@Composable
fun PlaygroundScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var textValue by remember { mutableStateOf("") }
    val selectedLang by FlowKeysCoordinator.selectedLanguage.collectAsState()
    val scriptMode by FlowKeysCoordinator.scriptMode.collectAsState()
    val dictationState by FlowKeysCoordinator.dictationState.collectAsState()
    val currentTargetLang by FlowKeysCoordinator.targetTranslationLanguage.collectAsState()
    val translationMode = when (currentTargetLang) {
        Language.ENGLISH -> TranslationEngine.TranslationMode.TO_ENGLISH
        Language.BENGALI -> TranslationEngine.TranslationMode.TO_BENGALI
        Language.HINDI -> TranslationEngine.TranslationMode.TO_HINDI
        null -> TranslationEngine.TranslationMode.DIRECT_DICTATION
    }

    val dataStoreManager = remember { com.flowkeys.android.data.DataStoreManager(context) }
    val groqKey by dataStoreManager.groqApiKey.collectAsState(initial = "")
    val geminiKey by dataStoreManager.geminiApiKey.collectAsState(initial = "")

    val speechRecognitionManager = remember {
        SpeechRecognitionManager(context).apply {
            listener = object : SpeechRecognitionManager.Listener {
                override fun onSpeechStart() {}
                override fun onRmsChanged(audioLevel: Float) {}
                override fun onSpeechResult(polishedText: String) {
                    if (polishedText.isNotBlank()) {
                        textValue = if (textValue.isBlank()) polishedText else "$textValue $polishedText"
                    }
                }
                override fun onSpeechError(errorMessage: String) {}
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            speechRecognitionManager.cancel()
        }
    }

    val isRecording = dictationState is DictationState.Recording

    // Infinite breathing animation for Stitch tactile centerpiece
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isRecording) 1.14f else 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(StitchCanvas)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Stitch Top Telemetry Anchor
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(9999.dp))
                .background(StitchSurface2)
                .border(1.dp, StitchBorderSubtle, RoundedCornerShape(9999.dp))
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(StitchSuccess)
            )
            Text(
                text = "ENGINE READY",
                color = StitchTextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 0.5.sp
            )
            Text(text = "•", color = StitchTextMuted, fontSize = 10.sp)
            Text(
                text = "Dynamic Streaming",
                color = StitchSuccess,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp
            )
        }

        // Stitch Concentric Tactile Hero Mic Trigger
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(180.dp)
                .padding(top = 2.dp)
        ) {
            // Outer breathing wave ring
            Box(
                modifier = Modifier
                    .size(170.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(if (isRecording) StitchAccentCoral.copy(alpha = 0.15f) else StitchSurface1.copy(alpha = 0.5f))
            )
            // Mid structural ring
            Box(
                modifier = Modifier
                    .size(126.dp)
                    .clip(CircleShape)
                    .background(StitchSurface2)
                    .border(
                        width = 1.dp,
                        color = if (isRecording) StitchAccentCoral.copy(alpha = 0.4f) else StitchBorderSubtle,
                        shape = CircleShape
                    )
            )
            // Core tactile button (84dp touch target)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(84.dp)
                    .clip(CircleShape)
                    .background(if (isRecording) StitchAccentCoral else StitchSurface3)
                    .border(
                        width = 2.dp,
                        color = if (isRecording) StitchAccentPeach else StitchAccentCoral.copy(alpha = 0.4f),
                        shape = CircleShape
                    )
                    .clickable {
                        if (androidx.core.content.ContextCompat.checkSelfPermission(
                                context,
                                android.Manifest.permission.RECORD_AUDIO
                            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
                        ) {
                            FlowKeysCoordinator.onError("Microphone permission required")
                            return@clickable
                        }
                        if (isRecording) {
                            speechRecognitionManager.stopListening()
                        } else {
                            speechRecognitionManager.startListening(selectedLang)
                        }
                    }
            ) {
                Icon(
                    imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                    contentDescription = if (isRecording) "Stop Dictation" else "Start Dictation",
                    tint = Color.White,
                    modifier = Modifier.size(34.dp)
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = if (isRecording) "Listening in ${selectedLang.displayName}…" else "Tap to Speak Anywhere",
                color = StitchTextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )
            Text(
                text = if (isRecording) "Live preview streaming active" else "Sub-200ms Indic Polish & Transliterator",
                color = if (isRecording) StitchAccentPeach else StitchTextMuted,
                fontSize = 12.sp
            )
        }

        // Native Language Quick Selector Pills
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Language.entries.forEach { lang ->
                val isSelected = lang == selectedLang
                val glyph = when (lang) {
                    Language.BENGALI -> "অ"
                    Language.HINDI -> "अ"
                    Language.ENGLISH -> "Aa"
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) StitchSurface3 else StitchSurface1)
                        .border(
                            width = 1.5.dp,
                            color = if (isSelected) StitchAccentCoral else StitchBorderSubtle,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { FlowKeysCoordinator.setLanguage(lang) }
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = glyph,
                            color = if (isSelected) StitchAccentCoral else StitchTextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = lang.displayName.split(" ").first(),
                            color = if (isSelected) StitchTextPrimary else StitchTextSecondary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Tri-Mode Script Output Engine (Translated English / Native Script / Benglish-Hinglish Latin)
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FontDownload,
                        contentDescription = null,
                        tint = StitchAccentCoral,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "SCRIPT OUTPUT MODE",
                        color = StitchAccentCoral,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 0.5.sp
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ScriptModeButton(
                        label = "→ English",
                        sublabel = "Translated",
                        isSelected = scriptMode == ScriptMode.TRANSLATED_ENGLISH,
                        modifier = Modifier.weight(1f),
                        onClick = { FlowKeysCoordinator.setScriptMode(ScriptMode.TRANSLATED_ENGLISH) }
                    )
                    ScriptModeButton(
                        label = "Native Script",
                        sublabel = if (selectedLang == Language.BENGALI) "বাংলা" else if (selectedLang == Language.HINDI) "हिन्दी" else "English",
                        isSelected = scriptMode == ScriptMode.NATIVE_SCRIPT,
                        modifier = Modifier.weight(1f),
                        onClick = { FlowKeysCoordinator.setScriptMode(ScriptMode.NATIVE_SCRIPT) }
                    )
                    ScriptModeButton(
                        label = "Latin Script",
                        sublabel = if (selectedLang == Language.BENGALI) "Benglish" else "Hinglish",
                        isSelected = scriptMode == ScriptMode.PHONETIC_LATIN,
                        modifier = Modifier.weight(1f),
                        onClick = { FlowKeysCoordinator.setScriptMode(ScriptMode.PHONETIC_LATIN) }
                    )
                }
            }
        }

        // Tri-Directional Translation Bar
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Translate,
                        contentDescription = null,
                        tint = StitchAccentPeach,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "EXPLICIT TRANSLATION OVERRIDE",
                        color = StitchAccentPeach,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 0.5.sp
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TranslationModeButton(
                        label = "Auto (Script Mode)",
                        isSelected = translationMode == TranslationEngine.TranslationMode.DIRECT_DICTATION,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            FlowKeysCoordinator.setTargetTranslation(null)
                        }
                    )
                    TranslationModeButton(
                        label = "→ English",
                        isSelected = translationMode == TranslationEngine.TranslationMode.TO_ENGLISH,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            val mode = TranslationEngine.TranslationMode.TO_ENGLISH
                            FlowKeysCoordinator.setTargetTranslation(mode.targetLanguage)
                            if (textValue.isNotBlank()) {
                                scope.launch {
                                    textValue = TranslationEngine.translate(
                                        textValue,
                                        selectedLang,
                                        mode.targetLanguage!!,
                                        apiKey = groqKey.ifBlank { null },
                                        geminiKey = geminiKey.ifBlank { null }
                                    )
                                }
                            }
                        }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TranslationModeButton(
                        label = "→ বাংলা (BN)",
                        isSelected = translationMode == TranslationEngine.TranslationMode.TO_BENGALI,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            val mode = TranslationEngine.TranslationMode.TO_BENGALI
                            FlowKeysCoordinator.setTargetTranslation(mode.targetLanguage)
                            if (textValue.isNotBlank()) {
                                scope.launch {
                                    textValue = TranslationEngine.translate(
                                        textValue,
                                        selectedLang,
                                        mode.targetLanguage!!,
                                        apiKey = groqKey.ifBlank { null },
                                        geminiKey = geminiKey.ifBlank { null }
                                    )
                                }
                            }
                        }
                    )
                    TranslationModeButton(
                        label = "→ हिन्दी (HI)",
                        isSelected = translationMode == TranslationEngine.TranslationMode.TO_HINDI,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            val mode = TranslationEngine.TranslationMode.TO_HINDI
                            FlowKeysCoordinator.setTargetTranslation(mode.targetLanguage)
                            if (textValue.isNotBlank()) {
                                scope.launch {
                                    textValue = TranslationEngine.translate(
                                        textValue,
                                        selectedLang,
                                        mode.targetLanguage!!,
                                        apiKey = groqKey.ifBlank { null },
                                        geminiKey = geminiKey.ifBlank { null }
                                    )
                                }
                            }
                        }
                    )
                }
            }
        }

        // Interactive Editable Sandbox Card with Action Bar
        Card(
            colors = CardDefaults.cardColors(containerColor = StitchSurface1),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, StitchBorderSubtle, RoundedCornerShape(14.dp))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val wordCount = if (textValue.isBlank()) 0 else textValue.trim().split(Regex("\\s+")).size
                    Text(
                        text = "SANDBOX • $wordCount words",
                        color = StitchTextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (textValue.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(StitchSurface2)
                                    .clickable {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("FlowKeys Sandbox", textValue)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
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
                                        color = StitchTextPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(StitchSurface2)
                                    .clickable { textValue = "" }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear",
                                        tint = StitchTextMuted,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = "Clear",
                                        color = StitchTextMuted,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = textValue,
                    onValueChange = { textValue = it },
                    placeholder = {
                        Text(
                            text = "Tap here to focus. Speak in ${selectedLang.displayName} (${scriptMode.displayName}) and watch FlowKeys stream and insert polished text…",
                            color = StitchTextMuted,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = StitchSurface2,
                        unfocusedContainerColor = StitchSurface2,
                        focusedTextColor = StitchTextPrimary,
                        unfocusedTextColor = StitchTextPrimary,
                        focusedBorderColor = StitchAccentCoral,
                        unfocusedBorderColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                )
            }
        }

        // Quick Telemetry Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TelemetryMiniCard(
                icon = Icons.Default.WifiOff,
                title = "Offline",
                status = "Zero Cloud",
                modifier = Modifier.weight(1f)
            )
            TelemetryMiniCard(
                icon = Icons.Default.Bolt,
                title = "Engine",
                status = "< 3ms Polish",
                modifier = Modifier.weight(1f)
            )
            TelemetryMiniCard(
                icon = Icons.Default.Mic,
                title = "VAD",
                status = "Auto-Stop",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun ScriptModeButton(
    label: String,
    sublabel: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) StitchAccentCoral.copy(alpha = 0.18f) else StitchSurface2)
            .border(
                width = 1.dp,
                color = if (isSelected) StitchAccentCoral else Color.Transparent,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                color = if (isSelected) StitchAccentCoral else StitchTextPrimary,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = sublabel,
                color = if (isSelected) StitchAccentPeach else StitchTextMuted,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun TranslationModeButton(
    label: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) StitchAccentCoral.copy(alpha = 0.18f) else StitchSurface2)
            .border(
                width = 1.dp,
                color = if (isSelected) StitchAccentCoral else Color.Transparent,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isSelected) StitchAccentCoral else StitchTextSecondary,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun TelemetryMiniCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    status: String,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = StitchSurface1),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.border(1.dp, StitchBorderSubtle, RoundedCornerShape(12.dp))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = StitchSuccess,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = title, color = StitchTextMuted, fontSize = 11.sp)
            Text(text = status, color = StitchTextPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
        }
    }
}
