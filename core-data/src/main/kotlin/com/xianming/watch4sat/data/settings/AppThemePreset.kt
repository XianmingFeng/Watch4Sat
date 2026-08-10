package com.xianming.watch4sat.data.settings

enum class AppThemePreset {
    SYSTEM,
    PIXEL_MINT,
    SKY_BLUE,
    AURORA_GREEN,
    SOLAR_YELLOW,
    ROSE_CORAL;

    companion object {
        fun fromStoredName(value: String?): AppThemePreset {
            return entries.firstOrNull { it.name == value } ?: SKY_BLUE
        }
    }
}
