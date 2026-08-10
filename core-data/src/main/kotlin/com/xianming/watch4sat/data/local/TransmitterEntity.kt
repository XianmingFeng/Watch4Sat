package com.xianming.watch4sat.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transmitters")
data class TransmitterEntity(
    @PrimaryKey val uuid: String,
    val catalogNumber: Int?,
    val description: String,
    val isAlive: Boolean,
    val status: String,
    val downlinkLowHz: Long?,
    val downlinkHighHz: Long?,
    val downlinkMode: String?,
    val uplinkLowHz: Long?,
    val uplinkHighHz: Long?,
    val uplinkMode: String?,
    val isInverted: Boolean
)
