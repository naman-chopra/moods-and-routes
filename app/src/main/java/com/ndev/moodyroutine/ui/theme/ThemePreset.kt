package com.ndev.moodyroutine.ui.theme

enum class AppThemePreset(
    val id: String,
    val title: String,
    val description: String,
    val primaryColor: Long,
    val primaryLightColor: Long,
    val appBgColor: Long,
    val cardBgColor: Long,
    val cardBorderColor: Long,
    val popupBgColor: Long,
    val contourGreyColor: Long
) {
    CRIMSON_OBSIDIAN(
        id = "crimson_obsidian",
        title = "Crimson Obsidian",
        description = "Deep obsidian black with vivid crimson-orange",
        primaryColor = 0xFFFF3B00,
        primaryLightColor = 0xFFFF6838,
        appBgColor = 0xFF08080A,
        cardBgColor = 0xFF18181B,
        cardBorderColor = 0xFF2A2A32,
        popupBgColor = 0xFF0C0C0E,
        contourGreyColor = 0xFF71717A
    ),
    BLOOD_ORANGE_AMOLED(
        id = "blood_orange_amoled",
        title = "Blood Orange AMOLED",
        description = "Pure AMOLED true black with blood orange",
        primaryColor = 0xFFE62E00,
        primaryLightColor = 0xFFFF5722,
        appBgColor = 0xFF000000,
        cardBgColor = 0xFF141417,
        cardBorderColor = 0xFF222228,
        popupBgColor = 0xFF070709,
        contourGreyColor = 0xFF6B7280
    ),
    SUNSET_FLAME_TITANIUM(
        id = "sunset_flame_titanium",
        title = "Sunset Flame Titanium",
        description = "Dark titanium graphite with radiant flame",
        primaryColor = 0xFFF93800,
        primaryLightColor = 0xFFFF7043,
        appBgColor = 0xFF0A0A0D,
        cardBgColor = 0xFF1E1E24,
        cardBorderColor = 0xFF32323C,
        popupBgColor = 0xFF0F0F13,
        contourGreyColor = 0xFF78716C
    ),
    TERRACOTTA_CHARCOAL(
        id = "terracotta_charcoal",
        title = "Terracotta Charcoal",
        description = "Deep charcoal with rich terracotta red-orange",
        primaryColor = 0xFFEA380C,
        primaryLightColor = 0xFFF87171,
        appBgColor = 0xFF09090C,
        cardBgColor = 0xFF1A1A1E,
        cardBorderColor = 0xFF2F2F38,
        popupBgColor = 0xFF0D0D11,
        contourGreyColor = 0xFF64748B
    );

    companion object {
        fun fromId(id: String?): AppThemePreset {
            return entries.find { it.id == id } ?: CRIMSON_OBSIDIAN
        }
    }
}
