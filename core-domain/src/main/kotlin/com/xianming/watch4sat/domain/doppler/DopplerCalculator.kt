package com.xianming.watch4sat.domain.doppler

import com.xianming.watch4sat.domain.model.DopplerReading
import com.xianming.watch4sat.domain.model.OrbitalPosition

object DopplerCalculator {
    private const val SPEED_OF_LIGHT_METERS_PER_SECOND = 299_792_458.0

    fun calculate(
        baseDownlinkHz: Long?,
        baseUplinkHz: Long?,
        position: OrbitalPosition
    ): DopplerReading {
        val correctedDownlinkHz = baseDownlinkHz?.let {
            (it.toDouble() * (SPEED_OF_LIGHT_METERS_PER_SECOND - position.rangeRateKmPerSecond * 1000.0) /
                SPEED_OF_LIGHT_METERS_PER_SECOND).toLong()
        }
        val correctedUplinkHz = baseUplinkHz?.let {
            (it.toDouble() * (SPEED_OF_LIGHT_METERS_PER_SECOND + position.rangeRateKmPerSecond * 1000.0) /
                SPEED_OF_LIGHT_METERS_PER_SECOND).toLong()
        }
        val downlinkOffsetHz = offsetHz(baseDownlinkHz, correctedDownlinkHz)
        val uplinkOffsetHz = offsetHz(baseUplinkHz, correctedUplinkHz)
        return DopplerReading(
            baseDownlinkHz = baseDownlinkHz,
            correctedDownlinkHz = correctedDownlinkHz,
            downlinkOffsetHz = downlinkOffsetHz,
            downlinkOffsetKhz = downlinkOffsetHz?.toKhz(),
            baseUplinkHz = baseUplinkHz,
            correctedUplinkHz = correctedUplinkHz,
            uplinkOffsetHz = uplinkOffsetHz,
            uplinkOffsetKhz = uplinkOffsetHz?.toKhz()
        )
    }

    private fun offsetHz(baseHz: Long?, correctedHz: Long?): Long? {
        return if (baseHz == null || correctedHz == null) null else correctedHz - baseHz
    }

    private fun Long.toKhz(): Double = this / 1000.0
}
