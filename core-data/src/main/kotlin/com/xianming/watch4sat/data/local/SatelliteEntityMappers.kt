package com.xianming.watch4sat.data.local

import com.xianming.watch4sat.domain.model.OrbitalData
import com.xianming.watch4sat.domain.model.SatelliteRecord

fun SatelliteRecord.toEntity(): SatelliteEntity {
    return SatelliteEntity(
        catalogNumber = catalogNumber,
        displayName = displayName,
        objectId = objectId,
        orbitalName = orbitalData.name,
        epoch = orbitalData.epoch,
        meanMotion = orbitalData.meanMotion,
        eccentricity = orbitalData.eccentricity,
        inclinationDegrees = orbitalData.inclinationDegrees,
        rightAscensionAscendingNodeDegrees = orbitalData.rightAscensionAscendingNodeDegrees,
        argumentOfPerigeeDegrees = orbitalData.argumentOfPerigeeDegrees,
        meanAnomalyDegrees = orbitalData.meanAnomalyDegrees,
        bstar = orbitalData.bstar,
        meanMotionDot = orbitalData.meanMotionDot
    )
}

fun SatelliteEntity.toDomain(selectedCatalogNumbers: Set<Int> = emptySet()): SatelliteRecord {
    val orbitalData = OrbitalData(
        name = orbitalName,
        catalogNumber = catalogNumber,
        epoch = epoch,
        meanMotion = meanMotion,
        eccentricity = eccentricity,
        inclinationDegrees = inclinationDegrees,
        rightAscensionAscendingNodeDegrees = rightAscensionAscendingNodeDegrees,
        argumentOfPerigeeDegrees = argumentOfPerigeeDegrees,
        meanAnomalyDegrees = meanAnomalyDegrees,
        bstar = bstar,
        meanMotionDot = meanMotionDot
    )
    return SatelliteRecord(
        catalogNumber = catalogNumber,
        displayName = displayName,
        orbitalData = orbitalData,
        objectId = objectId,
        selected = catalogNumber in selectedCatalogNumbers
    )
}
