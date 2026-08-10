package com.xianming.watch4sat.data.settings

enum class RadarForwardAxis {
    SCREEN_TOP,
    TOWARD_HAND;

    companion object {
        fun fromStoredName(value: String?): RadarForwardAxis {
            return when (value) {
                "SCREEN_RIGHT" -> TOWARD_HAND
                else -> entries.firstOrNull { it.name == value } ?: SCREEN_TOP
            }
        }
    }
}

enum class RadarWristSide {
    LEFT,
    RIGHT;

    companion object {
        fun fromStoredName(value: String?): RadarWristSide {
            return entries.firstOrNull { it.name == value } ?: LEFT
        }
    }
}
