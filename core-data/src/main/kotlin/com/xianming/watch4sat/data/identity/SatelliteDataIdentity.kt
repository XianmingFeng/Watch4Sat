package com.xianming.watch4sat.data.identity

import com.xianming.watch4sat.domain.model.SatelliteRecord
import java.security.MessageDigest

object SatelliteDataIdentity {

    fun sha256(records: List<SatelliteRecord>): String {
        return MessageDigest.getInstance("SHA-256")
            .digest(normalizedContent(records).encodeToByteArray())
            .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
    }

    fun normalizedContent(records: List<SatelliteRecord>): String {
        return buildString {
            records.sortedBy(SatelliteRecord::catalogNumber).forEach { record ->
                appendField(record.catalogNumber.toString())
                appendField(record.displayName)
                appendField(record.objectId.orEmpty())
                val orbital = record.orbitalData
                appendField(orbital.name)
                appendField(orbital.epoch.toHexString())
                appendField(orbital.meanMotion.toHexString())
                appendField(orbital.eccentricity.toHexString())
                appendField(orbital.inclinationDegrees.toHexString())
                appendField(orbital.rightAscensionAscendingNodeDegrees.toHexString())
                appendField(orbital.argumentOfPerigeeDegrees.toHexString())
                appendField(orbital.meanAnomalyDegrees.toHexString())
                appendField(orbital.bstar.toHexString())
                appendField(orbital.meanMotionDot.toHexString())
                append('\n')
            }
        }
    }

    private fun StringBuilder.appendField(value: String) {
        append(value.length)
        append(':')
        append(value)
        append('|')
    }

    private fun Double.toHexString(): String {
        return java.lang.Double.toHexString(if (this == 0.0) 0.0 else this)
    }
}
