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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Stop
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

/**
 * Human-Friendly Dictation Playground.
 * Designed for effortless, warm, everyday use by any person without technical jargon.
 */
@Composable
fun PlaygroundScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var textValue by remember { mutableStateOf("") }
    val selectedLang by FlowKeysCoordinator.selectedLanguage.collectAsState()
    val scriptMode by FlowKeysCoordinator.scriptMode.collectAsState()
    val dictationState by FlowKeysCoordinator.dictationState.collectAsState()
    val currentTargetLang by FlowKeysCoordinator.targetTranslationLanguage.collectAsState()

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

    // Infinite breathing animation for center mic trigger
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isRecording) 1.15f else 1.04f,
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
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Privacy and Status Pill
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(9999.dp))
                .background(StitchSurface2)
                .border(1.dp, StitchBorderSubtle, RoundedCornerShape(9999.dp))
                .padding(horizontal = 14.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(StitchSuccess)
            )
            Text(
                text = "100% PRIVATE",
                color = StitchTextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 0.5.sp
            )
            Text(text = "•", color = StitchTextMuted, fontSize = 10.sp)
            Text(
                text = "Voice Stays On Your Phone",
                color = StitchSuccess,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp
            )
        }

        // Hero Microphone Button
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(136.dp)
                .padding(top = 2.dp)
        ) {
            // Outer breathing ring
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(if (isRecording) StitchAccentCoral.copy(alpha = 0.18f) else StitchSurface1.copy(alpha = 0.5f))
            )
            // Mid ring
            Box(
                modifier = Modifier
                    .size(98.dp)
                    .clip(CircleShape)
                    .background(StitchSurface2)
                    .border(
                        width = 1.dp,
                        color = if (isRecording) StitchAccentCoral.copy(alpha = 0.45f) else StitchBorderSubtle,
                        shape = CircleShape
                    )
            )
            // Core tactile button
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(68.dp)
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
                    modifier = Modifier.size(30.dp)
                )
            }
        }

        // Action Status Title & Subtitle
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = if (isRecording) "Listening in ${selectedLang.displayName.split(" ").first()}…" else "Tap to Speak",
                color = StitchTextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 17.sp
            )
            Text(
                text = if (isRecording) "Tap the button when you're done speaking" else "FlowKeys types and polishes your words instantly",
                color = if (isRecording) StitchAccentPeach else StitchTextMuted,
                fontSize = 12.sp
            )
        }

        // Section 1: "I am speaking" Language Selector
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
                Text(
                    text = "I AM SPEAKING",
                    color = StitchAccentCoral,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 0.6.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Language.entries.forEach { lang ->
                        val isSelected = lang == selectedLang
                        val (glyph, label, sublabel) = when (lang) {
                            Language.BENGALI -> Triple("অ", "বাংলা", "Bengali")
                            Language.HINDI -> Triple("अ", "हिन्दी", "Hindi")
                            Language.ENGLISH -> Triple("Aa", "English", "Global")
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) StitchSurface3 else StitchSurface2)
                                .border(
                                    width = 1.5.dp,
                                    color = if (isSelected) StitchAccentCoral else Color.Transparent,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    FlowKeysCoordinator.setLanguage(lang)
                                    // Reset target translation if same language
                                    if (currentTargetLang == lang) {
                                        FlowKeysCoordinator.setTargetTranslation(null)
                                    }
                                }
                                .padding(vertical = 10.dp, horizontal = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = glyph,
                                    color = if (isSelected) StitchAccentCoral else StitchTextSecondary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = label,
                                    color = if (isSelected) StitchTextPrimary else StitchTextSecondary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = sublabel,
                                    color = if (isSelected) StitchAccentPeach else StitchTextMuted,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section 2: "Type as" Output Style Selector
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
                Text(
                    text = "TYPE MY WORDS AS",
                    color = StitchAccentPeach,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 0.6.sp
                )

                if (selectedLang == Language.ENGLISH) {
                    // When speaking English
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        HumanModeOptionCard(
                            title = "English",
                            subtitle = "Standard text",
                            isSelected = currentTargetLang == null,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                FlowKeysCoordinator.setScriptMode(ScriptMode.NATIVE_SCRIPT)
                                FlowKeysCoordinator.setTargetTranslation(null)
                            }
                        )
                        HumanModeOptionCard(
                            title = "→ বাংলা",
                            subtitle = "Bengali",
                            isSelected = currentTargetLang == Language.BENGALI,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                FlowKeysCoordinator.setScriptMode(ScriptMode.TRANSLATED_ENGLISH)
                                FlowKeysCoordinator.setTargetTranslation(Language.BENGALI)
                                if (textValue.isNotBlank()) {
                                    scope.launch {
                                        textValue = TranslationEngine.translate(
                                            textValue,
                                            selectedLang,
                                            Language.BENGALI,
                                            apiKey = groqKey.ifBlank { null },
                                            geminiKey = geminiKey.ifBlank { null }
                                        )
                                    }
                                }
                            }
                        )
                        HumanModeOptionCard(
                            title = "→ हिन्दी",
                            subtitle = "Hindi",
                            isSelected = currentTargetLang == Language.HINDI,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                FlowKeysCoordinator.setScriptMode(ScriptMode.TRANSLATED_ENGLISH)
                                FlowKeysCoordinator.setTargetTranslation(Language.HINDI)
                                if (textValue.isNotBlank()) {
                                    scope.launch {
                                        textValue = TranslationEngine.translate(
                                            textValue,
                                            selectedLang,
                                            Language.HINDI,
                                            apiKey = groqKey.ifBlank { null },
                                            geminiKey = geminiKey.ifBlank { null }
                                        )
                                    }
                                }
                            }
                        )
                    }
                } else {
                    // When speaking Bengali or Hindi
                    val isNative = scriptMode == ScriptMode.NATIVE_SCRIPT && currentTargetLang == null
                    val isTranslateEn = currentTargetLang == Language.ENGLISH || scriptMode == ScriptMode.TRANSLATED_ENGLISH
                    val isCasualLatin = scriptMode == ScriptMode.PHONETIC_LATIN

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        HumanModeOptionCard(
                            title = if (selectedLang == Language.BENGALI) "বাংলা Script" else "हिन्दी Script",
                            subtitle = "Original native",
                            isSelected = isNative,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                FlowKeysCoordinator.setScriptMode(ScriptMode.NATIVE_SCRIPT)
                                FlowKeysCoordinator.setTargetTranslation(null)
                            }
                        )
                        HumanModeOptionCard(
                            title = "→ English",
                            subtitle = "Auto translate",
                            isSelected = isTranslateEn,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                FlowKeysCoordinator.setScriptMode(ScriptMode.TRANSLATED_ENGLISH)
                                FlowKeysCoordinator.setTargetTranslation(Language.ENGLISH)
                                if (textValue.isNotBlank()) {
                                    scope.launch {
                                        textValue = TranslationEngine.translate(
                                            textValue,
                                            selectedLang,
                                            Language.ENGLISH,
                                            apiKey = groqKey.ifBlank { null },
                                            geminiKey = geminiKey.ifBlank { null }
                                        )
                                    }
                                }
                            }
                        )
                        HumanModeOptionCard(
                            title = if (selectedLang == Language.BENGALI) "Benglish" else "Hinglish",
                            subtitle = "English letters",
                            isSelected = isCasualLatin,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                FlowKeysCoordinator.setScriptMode(ScriptMode.PHONETIC_LATIN)
                                FlowKeysCoordinator.setTargetTranslation(null)
                            }
                        )
                    }
                }
            }
        }

        // Section 3: Live Spoken Text (Interactive Sandbox)
        Card(
            colors = CardDefaults.cardColors(containerColor = StitchSurface1),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, StitchBorderSubtle, RoundedCornerShape(14.dp))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val wordCount = if (textValue.isBlank()) 0 else textValue.trim().split(Regex("\\s+")).size
                    Text(
                        text = "LIVE DICTATION • $wordCount words",
                        color = StitchTextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (textValue.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(StitchSurface2)
                                    .clickable {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("FlowKeys", textValue)
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

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = textValue,
                    onValueChange = { textValue = it },
                    placeholder = {
                        Text(
                            text = "Tap the mic above and speak in ${selectedLang.displayName.split(" ").first()}. Your polished words appear right here…",
                            color = StitchTextMuted,
                            fontSize = 13.sp,
                            lineHeight = 19.sp
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
                        .height(120.dp)
                )
            }
        }

        // Section 4: Friendly Everyday Usage Tip
        Card(
            colors = CardDefaults.cardColors(containerColor = StitchSurface1),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, StitchBorderSubtle, RoundedCornerShape(14.dp))
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
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
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = StitchAccentCoral,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Use FlowKeys Anywhere",
                        color = StitchTextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "Open WhatsApp, Gmail, or Notes and tap any text box. The FlowKeys floating mic appears right above your keyboard so you can speak and auto-type directly into your chats!",
                        color = StitchTextMuted,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
    }
}

@Composable
private fun HumanModeOptionCard(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) StitchSurface3 else StitchSurface2)
            .border(
                width = 1.5.dp,
                color = if (isSelected) StitchAccentCoral else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                color = if (isSelected) StitchAccentCoral else StitchTextPrimary,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                color = if (isSelected) StitchAccentPeach else StitchTextMuted,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
