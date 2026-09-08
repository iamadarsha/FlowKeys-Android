package com.flowkeys.android.ui.onboarding

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.BubbleChart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.flowkeys.android.core.coordinator.FlowKeysCoordinator
import com.flowkeys.android.core.model.Language
import com.flowkeys.android.ui.theme.StitchAccentCoral
import com.flowkeys.android.ui.theme.StitchAccentPeach
import com.flowkeys.android.ui.theme.StitchBorderSubtle
import com.flowkeys.android.ui.theme.StitchCanvas
import com.flowkeys.android.ui.theme.StitchSuccess
import com.flowkeys.android.ui.theme.StitchSurface1
import com.flowkeys.android.ui.theme.StitchSurface2
import com.flowkeys.android.ui.theme.StitchSurface3
import com.flowkeys.android.ui.theme.StitchSurfaceRecessed
import com.flowkeys.android.ui.theme.StitchTextMuted
import com.flowkeys.android.ui.theme.StitchTextPrimary
import com.flowkeys.android.ui.theme.StitchTextSecondary

/**
 * High-fidelity 4-Screen Onboarding Flow faithfully reproduced from the connected Google Stitch project
 * ("Phase-Based App Screen Design" - projects/2034639286179709771):
 *
 * Screen 1: Onboarding_Welcome (Value Promise)
 * Screen 2: Onboarding_Language (Language & Local Personalization)
 * Screen 3: Onboarding_PrivacyPermissions (Privacy & Android System Permissions)
 * Screen 4: Onboarding_FirstSuccess (First Success / Magic Moment Simulator)
 */
@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var currentStep by remember { mutableIntStateOf(1) } // 1, 2, 3, 4
    val selectedLang by FlowKeysCoordinator.selectedLanguage.collectAsState()

    // Dynamic permission states refreshed on lifecycle resume
    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }
    var hasOverlayPermission by remember {
        mutableStateOf(Settings.canDrawOverlays(context))
    }
    var hasAccessibilityPermission by remember {
        mutableStateOf(isAccessibilityEnabled(context))
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasMicPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                hasOverlayPermission = Settings.canDrawOverlays(context)
                hasAccessibilityPermission = isAccessibilityEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val requestMicLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMicPermission = granted
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(StitchCanvas)
    ) {
        AnimatedContent(
            targetState = currentStep,
            transitionSpec = {
                if (targetState > initialState) {
                    (slideInHorizontally { it } + fadeIn()).togetherWith(slideOutHorizontally { -it } + fadeOut())
                } else {
                    (slideInHorizontally { -it } + fadeIn()).togetherWith(slideOutHorizontally { it } + fadeOut())
                }
            },
            label = "OnboardingStepTransition"
        ) { step ->
            when (step) {
                1 -> Screen1Welcome(
                    onNext = { currentStep = 2 }
                )
                2 -> Screen2Language(
                    selectedLanguage = selectedLang,
                    onSelectLanguage = { FlowKeysCoordinator.setLanguage(it) },
                    onBack = { currentStep = 1 },
                    onNext = { currentStep = 3 }
                )
                3 -> Screen3PrivacyPermissions(
                    context = context,
                    hasMic = hasMicPermission,
                    hasOverlay = hasOverlayPermission,
                    hasAccessibility = hasAccessibilityPermission,
                    onRequestMic = { requestMicLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                    onRequestOverlay = {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        )
                        context.startActivity(intent)
                    },
                    onRequestAccessibility = {
                        android.widget.Toast.makeText(
                            context,
                            "Tap 'FlowKeys Dictation Service' and turn the switch ON",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                        com.flowkeys.android.accessibility.FlowKeysAccessibilityService.openSettings(context)
                    },
                    onRequestAppInfo = {
                        com.flowkeys.android.accessibility.FlowKeysAccessibilityService.openAppInfo(context)
                    },
                    onBack = { currentStep = 2 },
                    onNext = { currentStep = 4 }
                )
                4 -> Screen4FirstSuccess(
                    onBack = { currentStep = 3 },
                    onComplete = onFinished
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SCREEN 1: Onboarding_Welcome (The Value Promise)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun Screen1Welcome(onNext: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(6.dp))

        // Top Brand Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(StitchAccentCoral),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "FlowKeys",
                    color = StitchTextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.2).sp
                )
            }

            // Step Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(StitchSurface2)
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "01 / 04",
                    color = StitchAccentCoral,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Editorial Headline
        Text(
            text = "Speak naturally.\nFlowKeys writes it.",
            color = StitchTextPrimary,
            fontSize = 21.sp,
            lineHeight = 26.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.3).sp
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Turn your voice into clean, ready-to-send text in any app — without replacing your existing keyboard.",
            color = StitchTextSecondary,
            fontSize = 12.sp,
            lineHeight = 16.sp
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Language Support Micro-Pill
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(StitchSurface1)
                .border(1.dp, StitchBorderSubtle, RoundedCornerShape(14.dp))
                .padding(horizontal = 9.dp, vertical = 3.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = null,
                    tint = StitchSuccess,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "English  •  हिंदी  •  বাংলা",
                    color = StitchTextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "On-Device AI",
                    color = StitchTextMuted,
                    fontSize = 10.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Hero Demonstration Phone Card Mockup (Compact High-Fidelity)
        Card(
            colors = CardDefaults.cardColors(containerColor = StitchSurfaceRecessed),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, StitchBorderSubtle, RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                // Chat header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(7.dp))
                        .background(StitchSurface1)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(StitchAccentCoral.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("A", color = StitchAccentCoral, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                        }
                        Spacer(modifier = Modifier.width(5.dp))
                        Column {
                            Text("Aarav Sharma", color = StitchTextPrimary, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
                            Text("online", color = StitchSuccess, fontSize = 8.5.sp)
                        }
                    }
                    Text("WhatsApp", color = StitchTextMuted, fontSize = 9.sp)
                }

                Spacer(modifier = Modifier.height(5.dp))

                // Chat bubble incoming
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(StitchSurface2)
                        .padding(horizontal = 7.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "Can you send the project summary before sync?",
                        color = StitchTextSecondary,
                        fontSize = 10.5.sp,
                        lineHeight = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(5.dp))

                // Outgoing bubble with voice draft
                Box(
                    modifier = Modifier
                        .align(Alignment.End)
                        .fillMaxWidth(0.85f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(StitchAccentCoral.copy(alpha = 0.15f))
                        .border(1.dp, StitchAccentCoral.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 7.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "Drafting it now via voice, give me two minutes!",
                        color = StitchAccentPeach,
                        fontSize = 10.5.sp,
                        lineHeight = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // FLOATING FLOWKEYS PILL ABOVE KEYBOARD MOCKUP
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(StitchSurface3)
                        .border(1.dp, StitchAccentCoral.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(StitchAccentCoral),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Mic, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                            }
                            Spacer(modifier = Modifier.width(5.dp))
                            // Waveform bars
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val heights = listOf(6, 12, 16, 10, 14, 11, 8)
                                heights.forEach { h ->
                                    Box(
                                        modifier = Modifier
                                            .width(2.dp)
                                            .height(h.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(StitchAccentCoral)
                                    )
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(9.dp))
                                .background(StitchSurface1)
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "LISTENING…",
                                color = StitchSuccess,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(5.dp))

                // Simulated keyboard bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(StitchSurface1)
                        .padding(vertical = 4.dp, horizontal = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "⌨  Gboard / System Keyboard Stays Active",
                        color = StitchTextMuted,
                        fontSize = 9.5.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Equation Badge
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(StitchSurface1)
                .border(1.dp, StitchBorderSubtle, RoundedCornerShape(8.dp))
                .padding(vertical = 5.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Keyboard, contentDescription = null, tint = StitchTextMuted, modifier = Modifier.size(13.dp))
                Spacer(modifier = Modifier.height(1.dp))
                Text("Your Keyboard", color = StitchTextSecondary, fontSize = 9.5.sp)
            }
            Text("+", color = StitchTextMuted, fontSize = 11.sp)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Mic, contentDescription = null, tint = StitchAccentCoral, modifier = Modifier.size(13.dp))
                Spacer(modifier = Modifier.height(1.dp))
                Text("FlowKeys AI", color = StitchAccentCoral, fontSize = 9.5.sp, fontWeight = FontWeight.SemiBold)
            }
            Text("=", color = StitchTextMuted, fontSize = 11.sp)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = StitchSuccess, modifier = Modifier.size(13.dp))
                Spacer(modifier = Modifier.height(1.dp))
                Text("Clean Text", color = StitchSuccess, fontSize = 9.5.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Primary CTA Button (Always visible without scrolling)
        Button(
            onClick = onNext,
            colors = ButtonDefaults.buttonColors(containerColor = StitchAccentCoral),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Get started",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Lock, contentDescription = null, tint = StitchSuccess, modifier = Modifier.size(12.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "On-device by default. Optional cloud AI is user-enabled and user-keyed.",
                color = StitchTextMuted,
                fontSize = 10.sp
            )
        }

        Spacer(modifier = Modifier.height(10.dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SCREEN 2: Onboarding_Language (Language & Local Personalization)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun Screen2Language(
    selectedLanguage: Language,
    onSelectLanguage: (Language) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        // Header with Back and Indicator
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(32.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = StitchTextPrimary, modifier = Modifier.size(18.dp))
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(StitchSurface2)
                    .padding(horizontal = 9.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "02 / 04",
                    color = StitchAccentCoral,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.GraphicEq, contentDescription = null, tint = StitchSuccess, modifier = Modifier.size(13.dp))
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = "ACOUSTIC TUNING",
                color = StitchSuccess,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Make FlowKeys\nsound like you.",
            color = StitchTextPrimary,
            fontSize = 22.sp,
            lineHeight = 27.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.4).sp
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Choose your primary language. FlowKeys learns your frequently used words and names privately on your phone.",
            color = StitchTextSecondary,
            fontSize = 12.sp,
            lineHeight = 17.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Language Cards (Compact 48-50dp height)
        LanguageOptionCard(
            title = "English",
            scriptBadge = "Global",
            samplePhrase = "“I'll send it tomorrow.”",
            symbol = "Aa",
            isSelected = selectedLanguage == Language.ENGLISH,
            onSelect = { onSelectLanguage(Language.ENGLISH) }
        )

        Spacer(modifier = Modifier.height(6.dp))

        LanguageOptionCard(
            title = "हिंदी",
            subtitle = "(Hindi)",
            scriptBadge = "On-Device",
            samplePhrase = "“मैं इसे कल भेज दूँगा।”",
            symbol = "अ",
            isSelected = selectedLanguage == Language.HINDI,
            onSelect = { onSelectLanguage(Language.HINDI) }
        )

        Spacer(modifier = Modifier.height(6.dp))

        LanguageOptionCard(
            title = "বাংলা",
            subtitle = "(Bengali)",
            scriptBadge = "On-Device",
            samplePhrase = "“আমি এটা কাল পাঠিয়ে দেব।”",
            symbol = "অ",
            isSelected = selectedLanguage == Language.BENGALI,
            onSelect = { onSelectLanguage(Language.BENGALI) }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Privacy & Local Learning Architecture Node (Compact)
        Card(
            colors = CardDefaults.cardColors(containerColor = StitchSurface1),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, StitchBorderSubtle, RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(StitchSuccess.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Shield, contentDescription = null, tint = StitchSuccess, modifier = Modifier.size(15.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Learns locally",
                                color = StitchTextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(StitchSuccess)
                            )
                        }
                        Text(
                            text = "Remembers frequent words & corrections without sending data off your phone.",
                            color = StitchTextSecondary,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(StitchSurface2)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Memory, contentDescription = null, tint = StitchTextMuted, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Private Neural Core", color = StitchTextMuted, fontSize = 10.sp)
                    }
                    Text("0 kB sent to cloud", color = StitchSuccess, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onNext,
            colors = ButtonDefaults.buttonColors(containerColor = StitchAccentCoral),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Continue",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "YOU CAN SWITCH SCRIPTS & LANGUAGES ANYTIME",
            color = StitchTextMuted,
            fontSize = 9.sp,
            letterSpacing = 0.5.sp,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(10.dp))
    }
}

@Composable
private fun LanguageOptionCard(
    title: String,
    subtitle: String? = null,
    scriptBadge: String,
    samplePhrase: String,
    symbol: String,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) StitchSurface2 else StitchSurface1
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) StitchAccentCoral else StitchBorderSubtle,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onSelect() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) StitchAccentCoral.copy(alpha = 0.18f) else StitchSurface3),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = symbol,
                        color = if (isSelected) StitchAccentCoral else StitchTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = title,
                            color = StitchTextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (subtitle != null) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = subtitle,
                                color = StitchTextMuted,
                                fontSize = 12.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isSelected) StitchAccentCoral.copy(alpha = 0.2f) else StitchSurface3)
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = scriptBadge.uppercase(),
                                color = if (isSelected) StitchAccentCoral else StitchTextMuted,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Text(
                        text = samplePhrase,
                        color = StitchTextSecondary,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) StitchAccentCoral else StitchSurface3)
                    .border(1.dp, if (isSelected) StitchAccentCoral else StitchBorderSubtle, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SCREEN 3: Onboarding_PrivacyPermissions (Privacy & Android Permissions)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun Screen3PrivacyPermissions(
    context: Context,
    hasMic: Boolean,
    hasOverlay: Boolean,
    hasAccessibility: Boolean,
    onRequestMic: () -> Unit,
    onRequestOverlay: () -> Unit,
    onRequestAccessibility: () -> Unit,
    onRequestAppInfo: () -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        // Progress Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(32.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = StitchTextPrimary, modifier = Modifier.size(18.dp))
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(StitchSurface2)
                    .padding(horizontal = 9.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "03 / 04",
                    color = StitchAccentCoral,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Private by default.\nReady anywhere.",
            color = StitchTextPrimary,
            fontSize = 22.sp,
            lineHeight = 27.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.4).sp
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "FlowKeys needs permissions so the floating bubble can appear above your keyboard and insert dictated text.",
            color = StitchTextSecondary,
            fontSize = 12.sp,
            lineHeight = 17.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Permission Stack (Compact 48dp min touch targets)
        PermissionCard(
            title = "Microphone",
            subtitle = "Records audio for instant voice dictation.",
            icon = Icons.Default.Mic,
            isGranted = hasMic,
            actionLabel = "Grant",
            onAction = onRequestMic
        )

        Spacer(modifier = Modifier.height(6.dp))

        PermissionCard(
            title = "Accessibility",
            subtitle = "Required: Detects keyboard & types text into WhatsApp/apps.",
            icon = Icons.Default.TextFields,
            isGranted = hasAccessibility,
            actionLabel = "Enable",
            onAction = onRequestAccessibility
        )

        Spacer(modifier = Modifier.height(6.dp))

        PermissionCard(
            title = "Floating Bubble",
            subtitle = "Required: Shows sleek mic pill docked over keyboard.",
            icon = Icons.Default.BubbleChart,
            isGranted = hasOverlay,
            actionLabel = "Allow",
            onAction = onRequestOverlay
        )

        if (android.os.Build.VERSION.SDK_INT >= 33 && !hasAccessibility) {
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = StitchSurface2),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, StitchAccentCoral.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🔒", fontSize = 13.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Android 13+ Restricted Setting Notice",
                            color = StitchAccentPeach,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "If FlowKeys switch is grayed out in Accessibility Settings:\n1. Tap 'Open App Info' below\n2. Tap the ⋮ (three dots) in top-right corner\n3. Tap 'Allow restricted settings'\n4. Return and turn FlowKeys ON",
                        color = StitchTextSecondary,
                        fontSize = 10.5.sp,
                        lineHeight = 15.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Button(
                        onClick = onRequestAppInfo,
                        colors = ButtonDefaults.buttonColors(containerColor = StitchSurface3),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().height(32.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("Open App Info (Allow Restricted)", color = StitchTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Architecture & Data Privacy Card (Compact)
        Card(
            colors = CardDefaults.cardColors(containerColor = StitchSurface1),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, StitchBorderSubtle, RoundedCornerShape(12.dp))
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.VerifiedUser,
                    contentDescription = null,
                    tint = StitchSuccess,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "ZERO TELEMETRY GUARANTEE",
                        color = StitchTextPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "• Passwords/PINs never read  • No typing tracked outside mic\n• 100% on-device processing by default",
                        color = StitchTextSecondary,
                        fontSize = 10.sp,
                        lineHeight = 14.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        val allGranted = hasMic && hasOverlay && hasAccessibility

        val (btnText, btnAction) = when {
            !hasAccessibility -> "Enable Accessibility in Settings" to onRequestAccessibility
            !hasOverlay -> "Enable Floating Bubble" to onRequestOverlay
            !hasMic -> "Grant Microphone Permission" to onRequestMic
            else -> "Set up FlowKeys" to onNext
        }

        Button(
            onClick = btnAction,
            colors = ButtonDefaults.buttonColors(
                containerColor = StitchAccentCoral
            ),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = btnText,
                    color = Color.White,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "CONFIGURABLE ANYTIME IN APP SETTINGS",
            color = StitchTextMuted,
            fontSize = 9.sp,
            letterSpacing = 0.5.sp,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(10.dp))
    }
}

@Composable
private fun PermissionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isGranted: Boolean,
    actionLabel: String,
    onAction: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = StitchSurface1),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (isGranted) StitchSuccess.copy(alpha = 0.4f) else StitchBorderSubtle,
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isGranted) StitchSuccess.copy(alpha = 0.15f) else StitchSurface2),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isGranted) StitchSuccess else StitchAccentCoral,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = title,
                            color = StitchTextPrimary,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isGranted) StitchSuccess.copy(alpha = 0.2f) else StitchSurface3)
                                .padding(horizontal = 5.dp, vertical = 1.5.dp)
                        ) {
                            Text(
                                text = if (isGranted) "ACTIVE" else "REQUIRED",
                                color = if (isGranted) StitchSuccess else StitchTextMuted,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    Text(
                        text = subtitle,
                        color = StitchTextSecondary,
                        fontSize = 10.sp,
                        lineHeight = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (isGranted) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Active",
                    tint = StitchSuccess,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Button(
                    onClick = onAction,
                    colors = ButtonDefaults.buttonColors(containerColor = StitchAccentCoral),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Text(
                        text = actionLabel,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SCREEN 4: Onboarding_FirstSuccess (First Success / Magic Moment Simulator)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun Screen4FirstSuccess(
    onBack: () -> Unit,
    onComplete: () -> Unit
) {
    var demoLang by remember { mutableStateOf("bn") } // "bn", "hi", "en"

    val sampleText = when (demoLang) {
        "bn" -> "কাল আমাকে অফিসে একটু আগে যেতে হবে।"
        "hi" -> "कल मुझे ऑफिस से थोड़ा जल्दी निकलना होगा।"
        else -> "I need to leave the office a little early tomorrow."
    }

    val sampleMeta = when (demoLang) {
        "bn" -> "৪০ অক্ষর • ১০০% নিশ্চিত • BN"
        "hi" -> "४२ अक्षर • ১০০% सटीक • HI"
        else -> "52 chars • 100% accurate • EN"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        // Progress Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = StitchTextPrimary)
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(StitchSurface2)
                    .padding(horizontal = 10.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "04 / 04",
                    color = StitchAccentCoral,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = StitchAccentCoral, modifier = Modifier.size(13.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "EXPERIENCE MAGIC",
                color = StitchAccentCoral,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Now, let's try it.",
            color = StitchTextPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.3).sp
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Open any text field, tap the FlowKeys bubble, and speak normally.",
            color = StitchTextSecondary,
            fontSize = 12.sp,
            lineHeight = 17.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Live Multilingual Simulation Card
        Card(
            colors = CardDefaults.cardColors(containerColor = StitchSurfaceRecessed),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, StitchBorderSubtle, RoundedCornerShape(14.dp))
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("TARGET LANGUAGE", color = StitchTextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Bolt, contentDescription = null, tint = StitchSuccess, modifier = Modifier.size(11.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("Zero Cloud Delay", color = StitchSuccess, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Selector tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(StitchSurface1)
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(
                        Triple("bn", "বাংলা", "Bengali"),
                        Triple("hi", "हिंदी", "Hindi"),
                        Triple("en", "English", "Global")
                    ).forEach { (code, title, sub) ->
                        val isSelected = demoLang == code
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) StitchAccentCoral else Color.Transparent)
                                .clickable { demoLang = code }
                                .padding(vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = title,
                                    color = if (isSelected) Color.White else StitchTextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = sub,
                                    color = if (isSelected) Color.White.copy(alpha = 0.85f) else StitchTextMuted,
                                    fontSize = 9.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Real Input Box Area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(StitchSurface1)
                        .border(1.dp, StitchBorderSubtle, RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = sampleText,
                                color = StitchTextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Box(
                                modifier = Modifier
                                    .width(2.dp)
                                    .height(14.dp)
                                    .background(StitchAccentCoral)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = sampleMeta,
                            color = StitchTextMuted,
                            fontSize = 9.sp,
                            modifier = Modifier.align(Alignment.End)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Floating FlowKeys Dynamic Micro-Interaction Pill
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(StitchSurface3)
                        .border(1.dp, StitchAccentCoral.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(StitchAccentCoral),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Mic, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(2.5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val heights = listOf(8, 14, 18, 10, 16, 12, 6)
                                heights.forEach { h ->
                                    Box(
                                        modifier = Modifier
                                            .width(2.5.dp)
                                            .height(h.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(StitchAccentCoral)
                                    )
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(StitchSurface1)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${demoLang.uppercase()} • STREAMING",
                                color = StitchSuccess,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Realistic keyboard mockup bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(StitchSurface1)
                        .padding(vertical = 5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Q  W  E  R  T  Y  U  I  O  P",
                        color = StitchTextMuted,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.5.sp
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Engine ready card
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(StitchSurface1)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StitchSuccess, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Whisper-Engine Ready", color = StitchTextSecondary, fontSize = 10.sp)
                    }
                    Text("Model: 42 MB • On-Device", color = StitchTextMuted, fontSize = 9.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Bottom CTA Button
        Button(
            onClick = onComplete,
            colors = ButtonDefaults.buttonColors(containerColor = StitchAccentCoral),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Start using FlowKeys",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "You can change everything later in Settings.",
            color = StitchTextMuted,
            fontSize = 11.sp,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))
    }
}

private fun isAccessibilityEnabled(context: Context): Boolean {
    return com.flowkeys.android.accessibility.FlowKeysAccessibilityService.isEnabled(context)
}

