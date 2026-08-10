package com.xianming.watch4sat.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "satellites")
data class SatelliteEntity(
    @PrimaryKey val catalogNumber: Int,
    val displayName: String,
    val objectId: String?,
    val orbitalName: String,
    val epoch: Double,
    val meanMotion: Double,
    val eccentricity: Double,
    val inclinationDegrees: Double,
    val rightAscensionAscendingNodeDegrees: Double,
    val argumentOfPerigeeDegrees: Double,
    val meanAnomalyDegrees: Double,
    val bstar: Double,
    val meanMotionDot: Double
)
