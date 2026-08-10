package com.xianming.watch4sat.data.settings

enum class MapTileMode {
    AUTO,
    OSM_ONLY,
    OFFLINE_WORLD;

    companion object {
        fun fromStoredName(value: String?): MapTileMode {
            return entries.firstOrNull { it.name == value } ?: AUTO
        }
    }
}
