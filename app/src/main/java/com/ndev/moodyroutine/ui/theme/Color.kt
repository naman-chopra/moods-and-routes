package com.ndev.moodyroutine.ui.theme

import androidx.compose.ui.graphics.Color

// Topographic "M" Icon Themed Palette (Obsidian Black, Glowing Amber/Orange, Slate & Contour Greys)
val MoodyDarkBackground = Color(0xFF0C0C0E)     // Deep obsidian background
val MoodyDarkSurface = Color(0xFF18181B)        // Matches icon background (Zinc-900)
val MoodyDarkSurfaceVariant = Color(0xFF27272A) // Elevated card/container (Zinc-800)
val MoodyDarkCard = Color(0xFF18181B)
val MoodyDarkOutline = Color(0xFF3F3F46)        // Contour outline (Zinc-700)

val MoodyLightBackground = Color(0xFFF9FAFB)
val MoodyLightSurface = Color(0xFFFFFFFF)
val MoodyLightSurfaceVariant = Color(0xFFF4F4F5)
val MoodyLightCard = Color(0xFFFFFFFF)
val MoodyLightOutline = Color(0xFFE4E4E7)

// Moody Vibrant Orange Palette (Topographic Summit Beacon & Glow)
val MoodyOrange = Color(0xFFEA580C)             // Terracotta / Sunset Orange
val MoodyOrangeLight = Color(0xFFFB923C)        // Glowing Ember Orange
val MoodyOrangeDark = Color(0xFFC2410C)         // Deep Burnt Orange

// Contour & Slate Greys (Elevation Lines)
val MoodyContourGrey = Color(0xFF71717A)        // Mid elevation contour line
val MoodySlateGrey = Color(0xFFA1A1AA)          // Muted text & subtle highlights
val MoodyCharcoalGrey = Color(0xFF3F3F46)       // Outer contour line

// Backward-compatible aliases
val OneUiDarkBackground = MoodyDarkBackground
val OneUiDarkSurface = MoodyDarkSurface
val OneUiDarkSurfaceVariant = MoodyDarkSurfaceVariant
val OneUiDarkCard = MoodyDarkCard
val OneUiDarkOutline = MoodyDarkOutline

val OneUiLightBackground = MoodyLightBackground
val OneUiLightSurface = MoodyLightSurface
val OneUiLightSurfaceVariant = MoodyLightSurfaceVariant
val OneUiLightCard = MoodyLightCard
val OneUiLightOutline = MoodyLightOutline

val SamsungBlue = MoodyOrange
val SamsungBlueLight = MoodyOrangeLight
val SamsungBlueDark = MoodyOrangeDark

// Accent Colors
val AccentPurple = Color(0xFF7F56D9)
val AccentGreen = Color(0xFF12B76A)
val AccentOrange = MoodyOrangeLight
val AccentRed = Color(0xFFFF1744)
val AccentTeal = Color(0xFF06AED4)
val AccentPink = Color(0xFFEE46BC)
val AccentIndigo = Color(0xFF4E5BA6)

// Mode swatch colors (Leading with iconic Orange & Contour Grey)
val ModeColors = listOf(
    MoodyOrange,        // Moody Orange (Hero)
    Color(0xFFFB923C),  // Ember Orange
    MoodyContourGrey,   // Contour Grey
    Color(0xFF3F3F46),  // Charcoal
    Color(0xFF7F56D9),  // Purple
    Color(0xFF12B76A),  // Green
    Color(0xFFFF1744),  // Crimson Red
    Color(0xFF06AED4),  // Teal
    Color(0xFFEE46BC),  // Pink
    Color(0xFF4E5BA6),  // Indigo
    Color(0xFF10B981),  // Emerald
    Color(0xFFF59E0B)   // Amber
)
