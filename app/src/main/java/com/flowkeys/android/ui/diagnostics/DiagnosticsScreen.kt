package com.flowkeys.android.ui.diagnostics

import android.content.Context
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flowkeys.android.core.coordinator.FlowKeysCoordinator
import com.flowkeys.android.core.memory.MemoryCoordinator
import com.flowkeys.android.core.model.Language
import com.flowkeys.android.models.ModelManager
import com.flowkeys.android.models.ModelManifest
import com.flowkeys.android.oem.OemDefenseManager
import com.flowkeys.android.polish.MicroPolisher
import com.flowkeys.android.recovery.ServiceHealthWatchdog
import com.flowkeys.android.ui.theme.StitchAccentCoral
import com.flowkeys.android.ui.theme.StitchAccentPeach
import com.flowkeys.android.ui.theme.StitchBorderHighlight
import com.flowkeys.android.ui.theme.StitchBorderSubtle
import com.flowkeys.android.ui.theme.StitchCanvas
import com.flowkeys.android.ui.theme.StitchError
import com.flowkeys.android.ui.theme.StitchSuccess
import com.flowkeys.android.ui.theme.StitchSurface1
import com.flowkeys.android.ui.theme.StitchSurface2
import com.flowkeys.android.ui.theme.StitchTextMuted
import com.flowkeys.android.ui.theme.StitchTextPrimary
import com.flowkeys.android.ui.theme.StitchTextSecondary
import com.flowkeys.android.ui.theme.StitchWarning

@Composable
fun DiagnosticsScreen() {
    val context = LocalContext.current
    val modelManager = remember { ModelManager(context) }
    val deviceTier = FlowKeysCoordinator.deviceTier
    val freeRamMb = remember { MemoryCoordinator.getAvailableMemoryMb(context) }

    var testResult by remember { mutableStateOf<String?>(null) }
    var testLatency by remember { mutableStateOf(0L) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(StitchCanvas)
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "System Diagnostics",
                color = StitchTextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Real-time telemetry and engine verification",
                color = StitchTextMuted,
                fontSize = 13.sp
            )
        }

        // Hardware Profile Card
        Card(
            colors = CardDefaults.cardColors(containerColor = StitchSurface1),
            border = BorderStroke(1.dp, StitchBorderSubtle),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "HARDWARE & MEMORY",
                    color = StitchAccentCoral,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 0.5.sp
                )
                DiagnosticRow("Device Tier", deviceTier.name.replace('_', ' '))
                DiagnosticRow("Available RAM", "$freeRamMb MB")
                
                val oemFamilyLabel = when (OemDefenseManager.currentOem) {
                    OemDefenseManager.OemType.BBK_OPPO_REALME_ONEPLUS -> "OnePlus / BBK"
                    OemDefenseManager.OemType.XIAOMI -> "Xiaomi / MIUI"
                    OemDefenseManager.OemType.SAMSUNG -> "Samsung OneUI"
                    OemDefenseManager.OemType.VIVO -> "Vivo / Funtouch"
                    OemDefenseManager.OemType.GENERIC_STOCK -> "Standard AOSP"
                }
                DiagnosticRow(
                    "OEM Vendor",
                    "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ($oemFamilyLabel)"
                )
                DiagnosticRow("Android Version", "API ${Build.VERSION.SDK_INT} (Android ${Build.VERSION.RELEASE})")
                DiagnosticRow("Compute Engine", "${deviceTier.maxThreads} Threads / NNAPI")
            }
        }

        // Service & Permission Health Card
        Card(
            colors = CardDefaults.cardColors(containerColor = StitchSurface1),
            border = BorderStroke(1.dp, StitchBorderSubtle),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "SERVICE INTEGRITY",
                    color = StitchAccentCoral,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 0.5.sp
                )
                val isServiceActive = ServiceHealthWatchdog.isServiceHealthy(context)
                DiagnosticRow(
                    label = "Accessibility Service",
                    value = if (isServiceActive) "ACTIVE" else "DISCONNECTED",
                    statusColor = if (isServiceActive) StitchSuccess else StitchError
                )

                val hasOverlay = Settings.canDrawOverlays(context)
                DiagnosticRow(
                    label = "Overlay Window",
                    value = if (hasOverlay) "GRANTED" else "MISSING",
                    statusColor = if (hasOverlay) StitchSuccess else StitchError
                )

                val hasDisk = modelManager.hasSufficientDiskSpace()
                DiagnosticRow(
                    label = "Storage Free Space",
                    value = if (hasDisk) "HEALTHY (>500MB)" else "LOW STORAGE",
                    statusColor = if (hasDisk) StitchSuccess else StitchWarning
                )
            }
        }

        // Model Packs Status Card
        Card(
            colors = CardDefaults.cardColors(containerColor = StitchSurface1),
            border = BorderStroke(1.dp, StitchBorderSubtle),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "ON-DEVICE SPEECH MODELS",
                    color = StitchAccentCoral,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 0.5.sp
                )
                ModelManifest.ALL_MODELS.forEach { model ->
                    val isInstalled = modelManager.isModelInstalled(model.languages.first())
                    DiagnosticRow(
                        label = model.name,
                        value = if (isInstalled) "INSTALLED (${model.sizeBytes / (1024 * 1024)} MB)" else "STANDBY",
                        statusColor = if (isInstalled) StitchSuccess else StitchTextMuted
                    )
                }
            }
        }

        // OEM Reliability Deep Link Button
        Button(
            onClick = {
                val intent = OemDefenseManager.getOemBatteryOptimizationIntent(context)
                context.startActivity(intent)
            },
            colors = ButtonDefaults.buttonColors(containerColor = StitchSurface2),
            border = BorderStroke(1.dp, StitchBorderSubtle),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text(
                text = "Configure OEM Battery Whitelist",
                color = StitchTextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }

        // Live Benchmark Test
        Button(
            onClick = {
                val start = System.currentTimeMillis()
                val rawInput = "কালকে amake mane meeting e jete hobe na na train e jabo"
                val cleaned = MicroPolisher.polish(rawInput, Language.BENGALI)
                testLatency = System.currentTimeMillis() - start
                testResult = cleaned
            },
            colors = ButtonDefaults.buttonColors(containerColor = StitchAccentCoral),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text(
                text = "Run In-App Benchmark Test",
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
        }

        testResult?.let { result ->
            Card(
                colors = CardDefaults.cardColors(containerColor = StitchSurface1),
                border = BorderStroke(1.dp, StitchBorderHighlight),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Benchmark Result ($testLatency ms)",
                        color = StitchAccentPeach,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = result,
                        color = StitchTextPrimary,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun DiagnosticRow(
    label: String,
    value: String,
    statusColor: Color = StitchTextPrimary
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = StitchTextSecondary,
            fontSize = 13.sp,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = value,
            color = statusColor,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            textAlign = TextAlign.End
        )
    }
}
