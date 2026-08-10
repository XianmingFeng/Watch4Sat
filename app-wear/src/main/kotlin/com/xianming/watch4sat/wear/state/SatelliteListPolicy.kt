package com.xianming.watch4sat.wear.state

import com.xianming.watch4sat.domain.model.SatelliteRecord
import com.xianming.watch4sat.domain.model.TransmitterRecord
import com.xianming.watch4sat.wear.WatchRoute
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.roundToInt

object SatelliteListPolicy {
    fun selectedOnly(
        satellites: List<SatelliteRecord>,
        selectedSatelliteIds: Set<Int>
    ): List<SatelliteRecord> {
        return satellites
            .filter { selectedSatelliteIds.contains(it.catalogNumber) }
            .sortedBy { it.displayName }
    }
}

enum class SatellitePagerPage {
    ALL,
    SELECTED
}

enum class SatellitePagerIndicatorPlacement {
    FixedBottom
}

enum class SatelliteStarterActionPlacement {
    AllPageListAction
}

enum class SatelliteClearActionPlacement {
    SelectedPageEdgeButton
}

enum class SatelliteClearConfirmationComponent {
    AlertDialog
}

enum class SatelliteClearConfirmationActionStyle {
    OfficialConfirmDismissIconButtons
}

enum class SatelliteClearConfirmationHeaderStyle {
    WarningIconOnly
}

enum class SatelliteClearConfirmationAction {
    Dismiss,
    Confirm
}

enum class SatelliteRowSelectionComponent {
    SplitCheckboxButton
}

enum class SatelliteRowInteractionStyle {
    SplitCheckboxToggleDetailOnRowClick
}

enum class SatelliteSplitCheckboxTone {
    Surface,
    SurfaceVariant
}

data class SatelliteSplitCheckboxSegmentTones(
    val main: SatelliteSplitCheckboxTone,
    val split: SatelliteSplitCheckboxTone
)

object SatelliteSplitCheckboxColorPolicy {
    const val usesUnifiedMainAndSplitTone: Boolean = true

    fun segmentTones(selected: Boolean): SatelliteSplitCheckboxSegmentTones {
        val tone = if (selected) {
            SatelliteSplitCheckboxTone.SurfaceVariant
        } else {
            SatelliteSplitCheckboxTone.Surface
        }
        return SatelliteSplitCheckboxSegmentTones(main = tone, split = tone)
    }
}

data class SatelliteOrbitDetailUi(
    val kind: SatelliteOrbitDetailKind,
    val value: SatelliteOrbitDetailValue
)

enum class SatelliteOrbitDetailKind {
    Altitude,
    Period,
    MeanMotion,
    Inclination,
    Eccentricity,
    Raan,
    ArgumentOfPerigee
}

sealed interface SatelliteOrbitDetailValue {
    data class Altitude(
        val meanKm: Int,
        val perigeeKm: Int,
        val apogeeKm: Int
    ) : SatelliteOrbitDetailValue

    data class PeriodMinutes(val value: Double) : SatelliteOrbitDetailValue
    data class MeanMotionRevolutionsPerDay(val value: Double) : SatelliteOrbitDetailValue
    data class Degrees(val value: Double) : SatelliteOrbitDetailValue
    data class Eccentricity(val value: Double) : SatelliteOrbitDetailValue
}

data class SatelliteTransmitterDetailUi(
    val title: String,
    val status: String?,
    val isAlive: Boolean,
    val downlink: SatelliteFrequencyDetail?,
    val uplink: SatelliteFrequencyDetail?,
    val isInverted: Boolean
)

data class SatelliteFrequencyDetail(
    val lowMhz: Double?,
    val highMhz: Double?,
    val mode: String?
)

data class SatelliteDetailUi(
    val catalogNumber: Int,
    val displayName: String,
    val selected: Boolean,
    val orbitRows: List<SatelliteOrbitDetailUi> = emptyList(),
    val transmitters: List<SatelliteTransmitterDetailUi> = emptyList()
)

data class AnimatedSatelliteRow(
    val catalogNumber: Int,
    val isExiting: Boolean
)

object SatellitePagerPolicy {
    val pages: List<SatellitePagerPage> = listOf(
        SatellitePagerPage.ALL,
        SatellitePagerPage.SELECTED
    )
    const val usesHorizontalPagerScaffold: Boolean = true
    const val usesPagerScaffoldAsOuterShell: Boolean = true
    const val usesPageLevelScreenScaffold: Boolean = true
    val indicatorPlacement: SatellitePagerIndicatorPlacement = SatellitePagerIndicatorPlacement.FixedBottom
    const val pageIndicatorMovesForEdgeButton: Boolean = false
    const val clearEdgeButtonAffectsPageIndicatorPlacement: Boolean = false
    val starterActionPlacement: SatelliteStarterActionPlacement = SatelliteStarterActionPlacement.AllPageListAction
    const val usesEdgeButton: Boolean = false
    const val usesSharedRoundListTransformationSpec: Boolean = true
    const val usesSameTransformationProviderAsRoundListPage: Boolean = true
    const val settledPageScrollStateDrivesTimeText: Boolean = true
    const val usesSnapFling: Boolean = false
    const val usesStableRotaryScroll: Boolean = true
    const val avoidsRotarySnapAtTimeTextBoundary: Boolean = true
    const val usesPagerScaffoldSpringFling: Boolean = true
    const val usesAnimatedPage: Boolean = true
    const val clearEdgeButtonUsesSettledPage: Boolean = true
    const val edgeButtonChangesDuringPagerDrag: Boolean = false
    const val reservesLeftEdgeForSwipeBack: Boolean = true
    val rowSelectionComponent: SatelliteRowSelectionComponent = SatelliteRowSelectionComponent.SplitCheckboxButton
    val rowInteractionStyle: SatelliteRowInteractionStyle = SatelliteRowInteractionStyle.SplitCheckboxToggleDetailOnRowClick
    const val usesStableItemKeys: Boolean = true
    const val animatesSelectedRemoval: Boolean = true
    val detailRoute: WatchRoute = WatchRoute.SatelliteDetail
    val clearActionPlacement: SatelliteClearActionPlacement = SatelliteClearActionPlacement.SelectedPageEdgeButton
    val clearConfirmationComponent: SatelliteClearConfirmationComponent = SatelliteClearConfirmationComponent.AlertDialog
    val clearConfirmationActionStyle: SatelliteClearConfirmationActionStyle =
        SatelliteClearConfirmationActionStyle.OfficialConfirmDismissIconButtons
    val clearConfirmationHeaderStyle: SatelliteClearConfirmationHeaderStyle =
        SatelliteClearConfirmationHeaderStyle.WarningIconOnly
    val clearConfirmationDismissAction: SatelliteClearConfirmationAction =
        SatelliteClearConfirmationAction.Dismiss
    val clearConfirmationConfirmAction: SatelliteClearConfirmationAction =
        SatelliteClearConfirmationAction.Confirm
    const val clearConfirmationUsesEdgeButton: Boolean = false
    const val clearConfirmationUsesTextButtons: Boolean = false
    const val clearConfirmationUsesThemeColors: Boolean = true
    const val clearConfirmationUsesIcon: Boolean = true
    val clearConfirmRoute: WatchRoute = WatchRoute.SatellitesClearConfirm
    const val clearRequiresConfirmation: Boolean = true
    const val usesLongPressDetail: Boolean = false

    fun shouldShowClearEdgeButton(
        settledPage: SatellitePagerPage,
        selectedCount: Int
    ): Boolean {
        return settledPage == SatellitePagerPage.SELECTED && selectedCount > 0
    }

    fun visibleSatellites(
        page: SatellitePagerPage,
        satellites: List<SatelliteRecord>,
        selectedSatelliteIds: Set<Int>,
        query: String
    ): List<SatelliteRecord> {
        val normalizedQuery = query.trim()
        val base = when (page) {
            SatellitePagerPage.ALL -> satellites
            SatellitePagerPage.SELECTED -> SatelliteListPolicy.selectedOnly(satellites, selectedSatelliteIds)
        }
        val filtered = base.filter { satellite ->
            normalizedQuery.isBlank() ||
                satellite.displayName.contains(normalizedQuery, ignoreCase = true) ||
                satellite.catalogNumber.toString().contains(normalizedQuery)
        }
        return when (page) {
            SatellitePagerPage.ALL -> filtered
            SatellitePagerPage.SELECTED -> filtered.sortedBy { it.displayName }
        }
    }

    fun detailFor(
        satellite: SatelliteRecord,
        selectedSatelliteIds: Set<Int>,
        transmitters: List<TransmitterRecord> = emptyList()
    ): SatelliteDetailUi {
        return SatelliteDetailMapper.map(satellite, selectedSatelliteIds, transmitters)
    }
}

object SatelliteDetailMapper {
    private const val EarthMuKm3PerSecond2 = 398600.4418
    private const val EarthRadiusKm = 6378.137

    fun map(
        satellite: SatelliteRecord,
        selectedSatelliteIds: Set<Int>,
        transmitters: List<TransmitterRecord>
    ): SatelliteDetailUi {
        val orbitalData = satellite.orbitalData
        val matchingTransmitters = transmitters
            .filter { it.catalogNumber == satellite.catalogNumber }
            .map { it.toUi() }

        return SatelliteDetailUi(
            catalogNumber = satellite.catalogNumber,
            displayName = satellite.displayName,
            selected = selectedSatelliteIds.contains(satellite.catalogNumber),
            orbitRows = buildList {
                if (orbitalData.meanMotion > 0.0) {
                    add(
                        SatelliteOrbitDetailUi(
                            SatelliteOrbitDetailKind.Altitude,
                            altitude(orbitalData.meanMotion, orbitalData.eccentricity)
                        )
                    )
                    add(
                        SatelliteOrbitDetailUi(
                            SatelliteOrbitDetailKind.Period,
                            SatelliteOrbitDetailValue.PeriodMinutes(
                                1440.0 / orbitalData.meanMotion
                            )
                        )
                    )
                    add(
                        SatelliteOrbitDetailUi(
                            SatelliteOrbitDetailKind.MeanMotion,
                            SatelliteOrbitDetailValue.MeanMotionRevolutionsPerDay(
                                orbitalData.meanMotion
                            )
                        )
                    )
                }
                add(
                    SatelliteOrbitDetailUi(
                        SatelliteOrbitDetailKind.Inclination,
                        SatelliteOrbitDetailValue.Degrees(orbitalData.inclinationDegrees)
                    )
                )
                add(
                    SatelliteOrbitDetailUi(
                        SatelliteOrbitDetailKind.Eccentricity,
                        SatelliteOrbitDetailValue.Eccentricity(orbitalData.eccentricity)
                    )
                )
                add(
                    SatelliteOrbitDetailUi(
                        SatelliteOrbitDetailKind.Raan,
                        SatelliteOrbitDetailValue.Degrees(
                            orbitalData.rightAscensionAscendingNodeDegrees
                        )
                    )
                )
                add(
                    SatelliteOrbitDetailUi(
                        SatelliteOrbitDetailKind.ArgumentOfPerigee,
                        SatelliteOrbitDetailValue.Degrees(
                            orbitalData.argumentOfPerigeeDegrees
                        )
                    )
                )
            },
            transmitters = matchingTransmitters
        )
    }

    private fun TransmitterRecord.toUi(): SatelliteTransmitterDetailUi {
        return SatelliteTransmitterDetailUi(
            title = description.ifBlank { uuid },
            status = status.ifBlank { null },
            isAlive = isAlive,
            downlink = frequencyDetail(downlinkLowHz, downlinkHighHz, downlinkMode),
            uplink = frequencyDetail(uplinkLowHz, uplinkHighHz, uplinkMode),
            isInverted = isInverted
        )
    }

    private fun altitude(
        meanMotionRevPerDay: Double,
        eccentricity: Double
    ): SatelliteOrbitDetailValue.Altitude {
        val periodSeconds = 86400.0 / meanMotionRevPerDay
        val semiMajorAxisKm = (EarthMuKm3PerSecond2 * (periodSeconds / (2.0 * PI)).pow(2.0)).pow(1.0 / 3.0)
        val clampedEccentricity = eccentricity.coerceIn(0.0, 0.999999)
        val meanAltitudeKm = semiMajorAxisKm - EarthRadiusKm
        val perigeeKm = semiMajorAxisKm * (1.0 - clampedEccentricity) - EarthRadiusKm
        val apogeeKm = semiMajorAxisKm * (1.0 + clampedEccentricity) - EarthRadiusKm
        return SatelliteOrbitDetailValue.Altitude(
            meanKm = meanAltitudeKm.roundToInt(),
            perigeeKm = perigeeKm.roundToInt(),
            apogeeKm = apogeeKm.roundToInt()
        )
    }

    private fun frequencyDetail(
        lowHz: Long?,
        highHz: Long?,
        mode: String?
    ): SatelliteFrequencyDetail? {
        if (lowHz == null && highHz == null) return null
        return SatelliteFrequencyDetail(
            lowMhz = lowHz?.div(1_000_000.0),
            highMhz = highHz?.div(1_000_000.0),
            mode = mode?.trim()?.takeIf { it.isNotEmpty() }
        )
    }
}

object SatelliteRemovalAnimationPolicy {
    const val usesPendingRemovalState: Boolean = true
    const val usesAnimatedVisibilityExit: Boolean = true
    const val usesStableItemKeys: Boolean = true
    const val usesAnimateItemPlacement: Boolean = true
    const val exitDurationMillis: Int = 180

    fun rowsForDisplay(
        visibleCatalogNumbers: List<Int>,
        pendingRemovalIds: Set<Int>
    ): List<AnimatedSatelliteRow> {
        return visibleCatalogNumbers.map { catalogNumber ->
            AnimatedSatelliteRow(
                catalogNumber = catalogNumber,
                isExiting = pendingRemovalIds.contains(catalogNumber)
            )
        }
    }
}
