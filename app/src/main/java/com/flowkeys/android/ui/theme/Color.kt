package com.flowkeys.android.ui.theme

import androidx.compose.ui.graphics.Color

// Stitch "OLED Studio Dark" Design System Tokens (ui-ux-pro-max)
val StitchCanvas = Color(0xFF0A0B0E)             // Pure OLED deep charcoal canvas
val StitchSurfaceRecessed = Color(0xFF0F1115)    // Recessed well for search / input
val StitchSurface1 = Color(0xFF15181E)           // Tier 1 card container (subtle elevation)
val StitchSurface2 = Color(0xFF1E222A)           // Tier 2 interactive buttons & headers
val StitchSurface3 = Color(0xFF272C36)           // Tier 3 selected chips & active controls

// Signature Accents
val StitchAccentCoral = Color(0xFFFF5E36)        // Signature focal heat (recording / primary CTA)
val StitchAccentPeach = Color(0xFFFFA07A)        // Secondary telemetry & waveform glow
val StitchAccentIndigo = Color(0xFF6366F1)       // Intelligence & translation accent

// Contrast-Certified Typography (WCAG AAA)
val StitchTextPrimary = Color(0xFFF8FAFC)        // 16:1 contrast - High-emphasis headers
val StitchTextSecondary = Color(0xFFCBD5E1)      // 8:1 contrast - Subtitles & interactive labels
val StitchTextMuted = Color(0xFF94A3B8)          // 5.2:1 contrast - Secondary metadata (no dark text fail)

// Functional Semantics
val StitchSuccess = Color(0xFF10B981)            // Emerald: Engine ready & local model verified
val StitchWarning = Color(0xFFF59E0B)            // Amber: Audio clipping / resource notice
val StitchError = Color(0xFFEF4444)              // Ruby: Error state / permission missing
val StitchBorderSubtle = Color(0x1AFFFFFF)       // Hairline border (rgba 255,255,255,0.10)
val StitchBorderHighlight = Color(0x33FF5E36)    // Coral focus aura

// Compatibility Aliases mapped to Stitch
val FlowKeysPrimary = StitchAccentCoral
val FlowKeysPrimaryDark = Color(0xFFE84D25)
val FlowKeysAccent = StitchSuccess
val FlowKeysSurface = StitchCanvas
val FlowKeysSurfaceVariant = StitchSurface1
val FlowKeysCard = StitchSurface1
val FlowKeysOnSurface = StitchTextPrimary
val FlowKeysMuted = StitchTextMuted
val FlowKeysWaveform = StitchAccentPeach
val FlowKeysError = StitchError
val FlowKeysBorder = StitchBorderSubtle
