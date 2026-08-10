package com.xianming.watch4sat.domain.model

data class DopplerReading(
    val baseDownlinkHz: Long?,
    val correctedDownlinkHz: Long?,
    val downlinkOffsetHz: Long?,
    val downlinkOffsetKhz: Double?,
    val baseUplinkHz: Long?,
    val correctedUplinkHz: Long?,
    val uplinkOffsetHz: Long?,
    val uplinkOffsetKhz: Double?
)
