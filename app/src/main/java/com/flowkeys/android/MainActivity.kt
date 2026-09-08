package com.flowkeys.android

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flowkeys.android.ui.history.HistoryScreen
import com.flowkeys.android.ui.models.OfflineModelsScreen
import com.flowkeys.android.ui.onboarding.OnboardingActivity
import com.flowkeys.android.ui.playground.PlaygroundScreen
import com.flowkeys.android.ui.settings.SettingsScreen
import com.flowkeys.android.ui.theme.FlowKeysAccent
import com.flowkeys.android.ui.theme.FlowKeysMuted
import com.flowkeys.android.ui.theme.FlowKeysPrimary
import com.flowkeys.android.ui.theme.FlowKeysSurface
import com.flowkeys.android.ui.theme.FlowKeysTheme
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.drawBehind
import com.flowkeys.android.ui.diagnostics.DiagnosticsScreen
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import com.flowkeys.android.ui.theme.StitchBorderSubtle
import com.flowkeys.android.ui.theme.StitchSurfaceRecessed
import com.flowkeys.android.ui.theme.StitchTextPrimary
import com.flowkeys.android.ui.theme.StitchTextSecondary

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        // Microphone permission handled
    }

    override fun onResume() {
        super.onResume()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            FlowKeysTheme {
                var selectedTab by remember { mutableIntStateOf(0) }
                var showDiagnostics by remember { mutableStateOf(false) }
                var showStats by remember { mutableStateOf(false) }
                var showLearnedVocab by remember { mutableStateOf(false) }

                val isSubScreen = showDiagnostics || showStats || showLearnedVocab
                val subScreenTitle = when {
                    showDiagnostics -> "System Diagnostics"
                    showStats -> "Your Usage Stats"
                    showLearnedVocab -> "Learned Vocabulary"
                    else -> "FlowKeys"
                }

                Scaffold(
                    topBar = {
                        TopAppBar(
                            navigationIcon = {
                                if (isSubScreen) {
                                    IconButton(onClick = {
                                        showDiagnostics = false
                                        showStats = false
                                        showLearnedVocab = false
                                    }) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = "Back to Settings",
                                            tint = StitchTextPrimary
                                        )
                                    }
                                }
                            },
                            title = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(start = if (isSubScreen) 0.dp else 4.dp)
                                ) {
                                    Text(
                                        text = subScreenTitle,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp,
                                        color = StitchTextPrimary,
                                        letterSpacing = (-0.3).sp
                                    )
                                    if (!isSubScreen) {
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    color = FlowKeysAccent.copy(alpha = 0.15f),
                                                    shape = RoundedCornerShape(20.dp)
                                                )
                                                .border(
                                                    width = 1.dp,
                                                    color = FlowKeysAccent.copy(alpha = 0.35f),
                                                    shape = RoundedCornerShape(20.dp)
                                                )
                                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(6.dp)
                                                        .background(FlowKeysAccent, androidx.compose.foundation.shape.CircleShape)
                                                )
                                                Spacer(modifier = Modifier.width(5.dp))
                                                Text(
                                                    text = "ON-DEVICE",
                                                    color = FlowKeysAccent,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 10.sp,
                                                    letterSpacing = 0.6.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            },
                            actions = {
                                if (!isSubScreen) {
                                    IconButton(
                                        onClick = {
                                            startActivity(Intent(this@MainActivity, OnboardingActivity::class.java))
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Settings,
                                            contentDescription = "Setup & Permissions",
                                            tint = StitchTextSecondary
                                        )
                                    }
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = FlowKeysSurface
                            )
                        )
                    },
                    bottomBar = {
                        if (!isSubScreen) {
                            NavigationBar(
                                containerColor = StitchSurfaceRecessed,
                                modifier = Modifier.drawBehind {
                                    drawLine(
                                        color = StitchBorderSubtle,
                                        start = androidx.compose.ui.geometry.Offset(0f, 0f),
                                        end = androidx.compose.ui.geometry.Offset(size.width, 0f),
                                        strokeWidth = 1f
                                    )
                                }
                            ) {
                                NavigationBarItem(
                                    selected = selectedTab == 0,
                                    onClick = { selectedTab = 0 },
                                    icon = {
                                        Icon(
                                            imageVector = Icons.Default.Mic,
                                            contentDescription = "Dictate"
                                        )
                                    },
                                    label = { Text("Dictate", fontSize = 11.sp, fontWeight = if (selectedTab == 0) FontWeight.SemiBold else FontWeight.Normal) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = FlowKeysPrimary,
                                        selectedTextColor = StitchTextPrimary,
                                        indicatorColor = FlowKeysPrimary.copy(alpha = 0.18f),
                                        unselectedIconColor = FlowKeysMuted,
                                        unselectedTextColor = FlowKeysMuted
                                    )
                                )
                                NavigationBarItem(
                                    selected = selectedTab == 1,
                                    onClick = { selectedTab = 1 },
                                    icon = {
                                        Icon(
                                            imageVector = Icons.Default.History,
                                            contentDescription = "History"
                                        )
                                    },
                                    label = { Text("History", fontSize = 11.sp, fontWeight = if (selectedTab == 1) FontWeight.SemiBold else FontWeight.Normal) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = FlowKeysPrimary,
                                        selectedTextColor = StitchTextPrimary,
                                        indicatorColor = FlowKeysPrimary.copy(alpha = 0.18f),
                                        unselectedIconColor = FlowKeysMuted,
                                        unselectedTextColor = FlowKeysMuted
                                    )
                                )
                                NavigationBarItem(
                                    selected = selectedTab == 2,
                                    onClick = { selectedTab = 2 },
                                    icon = {
                                        Icon(
                                            imageVector = Icons.Default.CloudDownload,
                                            contentDescription = "Models"
                                        )
                                    },
                                    label = { Text("Models", fontSize = 11.sp, fontWeight = if (selectedTab == 2) FontWeight.SemiBold else FontWeight.Normal) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = FlowKeysPrimary,
                                        selectedTextColor = StitchTextPrimary,
                                        indicatorColor = FlowKeysPrimary.copy(alpha = 0.18f),
                                        unselectedIconColor = FlowKeysMuted,
                                        unselectedTextColor = FlowKeysMuted
                                    )
                                )
                                NavigationBarItem(
                                    selected = selectedTab == 3,
                                    onClick = { selectedTab = 3 },
                                    icon = {
                                        Icon(
                                            imageVector = Icons.Default.Tune,
                                            contentDescription = "Settings"
                                        )
                                    },
                                    label = { Text("Settings", fontSize = 11.sp, fontWeight = if (selectedTab == 3) FontWeight.SemiBold else FontWeight.Normal) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = FlowKeysPrimary,
                                        selectedTextColor = StitchTextPrimary,
                                        indicatorColor = FlowKeysPrimary.copy(alpha = 0.18f),
                                        unselectedIconColor = FlowKeysMuted,
                                        unselectedTextColor = FlowKeysMuted
                                    )
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        when {
                            showDiagnostics -> DiagnosticsScreen()
                            showStats -> com.flowkeys.android.ui.stats.StatsScreen()
                            showLearnedVocab -> com.flowkeys.android.ui.dictionary.LearnedVocabularyScreen()
                            else -> {
                                when (selectedTab) {
                                    0 -> PlaygroundScreen()
                                    1 -> HistoryScreen()
                                    2 -> OfflineModelsScreen()
                                    3 -> SettingsScreen(
                                        onNavigateToDiagnostics = { showDiagnostics = true },
                                        onNavigateToStats = { showStats = true },
                                        onNavigateToLearnedVocab = { showLearnedVocab = true }
                                    )
                                    else -> PlaygroundScreen()
                                }
                            }
                        }
                    }
                }
            }
        }

        // Check if overlay or accessibility is missing on first launch
        checkFirstRun()
    }

    private fun checkFirstRun() {
        val prefs = getSharedPreferences("flowkeys_app_state", MODE_PRIVATE)
        val onboardingDone = prefs.getBoolean("onboarding_done", false)
        if (onboardingDone) return

        val hasOverlay = Settings.canDrawOverlays(this)
        val hasMic = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        val serviceName = "$packageName/com.flowkeys.android.accessibility.FlowKeysAccessibilityService"
        val enabledServices = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: ""
        val hasAccessibility = enabledServices.contains(serviceName)

        if (!hasOverlay || !hasMic || !hasAccessibility) {
            startActivity(Intent(this, OnboardingActivity::class.java))
        }
    }
}
