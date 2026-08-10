package com.xianming.watch4sat.wear.state

import com.xianming.watch4sat.domain.model.TransmitterRecord

object RadarTransmitterSelector {

    fun optionsFor(
        catalogNumber: Int,
        transmitters: List<TransmitterRecord>
    ): List<TransmitterRecord> {
        return transmitters.filter { transmitter ->
            transmitter.catalogNumber == catalogNumber && transmitter.hasAnyFrequency()
        }
    }

    fun selectedTransmitter(
        options: List<TransmitterRecord>,
        selectedUuid: String?
    ): TransmitterRecord? {
        return options.firstOrNull { it.uuid == selectedUuid } ?: options.firstOrNull()
    }

    fun nextUuid(options: List<TransmitterRecord>, selectedUuid: String?): String? {
        if (options.isEmpty()) return null
        val currentIndex = options.indexOfFirst { it.uuid == selectedUuid }
        val nextIndex = if (currentIndex < 0) 0 else (currentIndex + 1) % options.size
        return options[nextIndex].uuid
    }

    private fun TransmitterRecord.hasAnyFrequency(): Boolean {
        return downlinkLowHz != null ||
            downlinkHighHz != null ||
            uplinkLowHz != null ||
            uplinkHighHz != null
    }
}
