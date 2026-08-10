package com.xianming.watch4sat.data.local

import com.xianming.watch4sat.domain.model.TransmitterRecord

fun TransmitterRecord.toEntity(): TransmitterEntity {
    return TransmitterEntity(
        uuid = uuid,
        catalogNumber = catalogNumber,
        description = description,
        isAlive = isAlive,
        status = status,
        downlinkLowHz = downlinkLowHz,
        downlinkHighHz = downlinkHighHz,
        downlinkMode = downlinkMode,
        uplinkLowHz = uplinkLowHz,
        uplinkHighHz = uplinkHighHz,
        uplinkMode = uplinkMode,
        isInverted = isInverted
    )
}

fun TransmitterEntity.toDomain(): TransmitterRecord {
    return TransmitterRecord(
        uuid = uuid,
        catalogNumber = catalogNumber,
        description = description,
        isAlive = isAlive,
        status = status,
        downlinkLowHz = downlinkLowHz,
        downlinkHighHz = downlinkHighHz,
        downlinkMode = downlinkMode,
        uplinkLowHz = uplinkLowHz,
        uplinkHighHz = uplinkHighHz,
        uplinkMode = uplinkMode,
        isInverted = isInverted
    )
}
