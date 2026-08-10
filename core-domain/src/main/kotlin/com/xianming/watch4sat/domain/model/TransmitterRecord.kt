package com.xianming.watch4sat.domain.model

data class TransmitterRecord(
    val uuid: String,
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
