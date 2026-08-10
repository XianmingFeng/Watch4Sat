package com.xianming.watch4sat.tile

import com.xianming.watch4sat.domain.model.SatellitePass
import com.xianming.watch4sat.domain.time.ClockTimeFormatter
import com.xianming.watch4sat.wear.state.TleFreshnessUiKind
import com.xianming.watch4sat.wear.state.TleFreshnessUiModel
import com.xianming.watch4sat.wear.state.TleFreshnessUiPolicy
import com.xianming.watch4sat.wear.state.TleRelativeAge
import kotlin.math.roundToInt

internal object NextPassTileDisplayPolicy {
    fun modelFor(
        state: TileState,
        nowMillis: Long,
        clockTimeFormatter: ClockTimeFormatter = ClockTimeFormatter.twentyFourHour(),
        text: NextPassTileText = NextPassTileText.English
    ): NextPassTileDisplayModel {
        if (state.station == null || state.passResolutionStatus == TilePassResolutionStatus.NeedsQth) {
            return emptyModel(
                kind = NextPassTileKind.NoQth,
                header = text.tleFreshness.tileHeaderDefault,
                title = text.noQthTitle,
                meta = text.setLocationFirst,
                ctaLabel = text.setQthAction,
                tone = NextPassTileTone.Warning
            )
        }
        if (state.cachedSatelliteCount == 0) {
            return emptyModel(
                kind = NextPassTileKind.NoTle,
                header = text.tleFreshness.tileHeaderDefault,
                title = text.noTleTitle,
                meta = text.orbitalDataMissing,
                ctaLabel = text.updateAction,
                tone = NextPassTileTone.Warning
            )
        }
        if (state.selectedSatelliteCount == 0) {
            return emptyModel(
                kind = NextPassTileKind.NoSatellites,
                header = text.tleFreshness.tileHeaderDefault,
                title = text.noSatellitesTitle,
                meta = text.selectSatellitesFirst,
                ctaLabel = text.selectAction,
                tone = NextPassTileTone.Info
            ).withFreshness(state, text)
        }
        val pass = state.nextPass ?: return emptyModel(
            kind = NextPassTileKind.NoPassSoon,
            header = text.tleFreshness.tileHeaderDefault,
            title = text.noPassSoonTitle,
            meta = text.openForPassList,
            ctaLabel = text.openAction,
            tone = NextPassTileTone.Info
        ).withFreshness(state, text)

        if (nowMillis >= pass.losMillis) {
            return endedPassModel(
                model = passModel(
                    kind = NextPassTileKind.ActivePass,
                    pass = pass,
                    nowMillis = nowMillis,
                    eventMillis = pass.losMillis,
                    metaPrefix = text.losLabel,
                    showProgress = false,
                    progress = null,
                    clockTimeFormatter = clockTimeFormatter,
                    text = text
                ),
                text = text
            ).withFreshness(state, text)
        }
        val model = if (pass.isActiveAt(nowMillis)) {
            passModel(
                kind = NextPassTileKind.ActivePass,
                pass = pass,
                nowMillis = nowMillis,
                eventMillis = pass.losMillis,
                metaPrefix = text.losLabel,
                showProgress = true,
                progress = pass.remainingProgress(nowMillis),
                clockTimeFormatter = clockTimeFormatter,
                text = text
            )
        } else {
            passModel(
                kind = NextPassTileKind.UpcomingPass,
                pass = pass,
                nowMillis = nowMillis,
                eventMillis = pass.aosMillis,
                metaPrefix = text.aosLabel,
                showProgress = false,
                progress = null,
                clockTimeFormatter = clockTimeFormatter,
                text = text
            )
        }
        return model.withFreshness(state, text)
    }

    fun offline(
        text: NextPassTileText = NextPassTileText.English
    ): NextPassTileDisplayModel {
        return emptyModel(
            kind = NextPassTileKind.TileOffline,
            header = text.tleFreshness.tileHeaderDefault,
            title = text.tileOfflineTitle,
            meta = text.openAppToRetry,
            ctaLabel = text.openAction,
            tone = NextPassTileTone.Error
        )
    }

    private fun passModel(
        kind: NextPassTileKind,
        pass: SatellitePass,
        nowMillis: Long,
        eventMillis: Long,
        metaPrefix: String,
        showProgress: Boolean,
        progress: Float?,
        clockTimeFormatter: ClockTimeFormatter,
        text: NextPassTileText
    ): NextPassTileDisplayModel {
        return NextPassTileDisplayModel(
            kind = kind,
            header = text.tleFreshness.tileHeaderDefault,
            title = pass.satelliteName,
            countdown = countdownLabel(eventMillis - nowMillis, text),
            meta = "$metaPrefix ${clockTimeFormatter.formatMinutes(eventMillis)} · " +
                "${text.maxLabel} ${pass.maxElevationDegrees.roundDeg()}",
            ctaLabel = text.radarAction,
            tone = NextPassTileTone.Primary,
            showProgress = showProgress,
            progress = progress,
            countdownTargetMillis = eventMillis,
            progressStartMillis = pass.aosMillis.takeIf { showProgress },
            progressEndMillis = pass.losMillis.takeIf { showProgress },
            nextTransitionMillis = eventMillis,
            maxElevationDegrees = pass.maxElevationDegrees,
            launchAction = TileLaunchPolicy.actionFor(
                kind = kind,
                catalogNumber = pass.catalogNumber,
                aosMillis = pass.aosMillis,
                losMillis = pass.losMillis
            )
        )
    }

    private fun emptyModel(
        kind: NextPassTileKind,
        header: String,
        title: String,
        meta: String,
        ctaLabel: String,
        tone: NextPassTileTone
    ): NextPassTileDisplayModel {
        return NextPassTileDisplayModel(
            kind = kind,
            header = header,
            title = title,
            meta = meta,
            ctaLabel = ctaLabel,
            tone = tone,
            launchAction = TileLaunchPolicy.actionFor(kind)
        )
    }

    private fun NextPassTileDisplayModel.withFreshness(
        state: TileState,
        text: NextPassTileText
    ): NextPassTileDisplayModel {
        val freshness = state.tleFreshness
        val freshnessUi = TleFreshnessUiPolicy.model(freshness)
        val freshnessText = freshnessUi.resolveText(text.tleFreshness)
        val transition = listOfNotNull(
            nextTransitionMillis,
            freshness.nextBoundaryMillis
        ).minOrNull()
        val freshnessDescription = listOf(
            freshnessText.statusLabel,
            freshnessText.detail,
            freshnessText.guidance
        ).joinToString(", ")
        return copy(
            header = freshnessText.tileHeader,
            tleFreshnessSeverity = freshness.severity,
            nextTransitionMillis = transition,
            accessibilityFreshnessDescription = freshnessDescription,
            accessibilityDescription =
                "$title, $freshnessDescription, ${text.opens} $ctaLabel"
        )
    }

    private fun TleFreshnessUiModel.resolveText(
        text: TleFreshnessText
    ): TileTleFreshnessText {
        val retrieval = retrievalAge.resolveText(text)
        val epoch = oldestEpochAge.resolveText(text)
        return when (kind) {
            TleFreshnessUiKind.ClockSkew -> TileTleFreshnessText(
                statusLabel = text.deviceTimeChanged,
                tileHeader = text.tileHeaderCheckTime,
                detail = text.detailClockSkew,
                guidance = text.cachedOfflineGuidance
            )
            TleFreshnessUiKind.Unknown -> TileTleFreshnessText(
                statusLabel = text.ageUnknown,
                tileHeader = text.tileHeaderAgeUnknown,
                detail = text.format(text.detailOldestEpochFormat, epoch),
                guidance = text.format(
                    text.refreshGuidanceFormat,
                    text.cachedOfflineGuidance
                )
            )
            TleFreshnessUiKind.Fresh -> TileTleFreshnessText(
                statusLabel = text.fresh,
                tileHeader = text.tileHeaderDefault,
                detail = text.format(text.detailDownloadedFormat, retrieval, epoch),
                guidance = text.currentGuidance
            )
            TleFreshnessUiKind.Stale -> TileTleFreshnessText(
                statusLabel = text.stale,
                tileHeader = text.tileHeaderStale,
                detail = text.format(text.detailDownloadedFormat, retrieval, epoch),
                guidance = text.format(
                    text.refreshGuidanceFormat,
                    text.cachedOfflineGuidance
                )
            )
            TleFreshnessUiKind.VeryStale -> TileTleFreshnessText(
                statusLabel = text.veryStale,
                tileHeader = text.tileHeaderVeryStale,
                detail = text.format(text.detailDownloadedFormat, retrieval, epoch),
                guidance = text.format(
                    text.accuracyGuidanceFormat,
                    text.cachedOfflineGuidance
                )
            )
        }
    }

    private fun TleRelativeAge?.resolveText(text: TleFreshnessText): String =
        when (this) {
            null,
            TleRelativeAge.Unknown -> text.ageUnknown
            TleRelativeAge.JustNow -> text.relativeJustNow
            is TleRelativeAge.Minutes -> text.format(text.relativeMinutesFormat, value)
            is TleRelativeAge.Hours -> text.format(text.relativeHoursFormat, value)
            is TleRelativeAge.Days -> text.format(text.relativeDaysFormat, value)
        }

    private fun countdownLabel(
        remainingMillis: Long,
        text: NextPassTileText
    ): String {
        val seconds = ((remainingMillis.coerceAtLeast(0L) + 999L) / 1_000L).coerceAtLeast(0L)
        return if (seconds < 60L) {
            "$seconds ${text.secondsShort}"
        } else {
            "${(seconds + 59L) / 60L} ${text.minutesShort}"
        }
    }

    private fun SatellitePass.remainingProgress(nowMillis: Long): Float {
        val duration = (losMillis - aosMillis).coerceAtLeast(1L)
        return ((losMillis - nowMillis).toFloat() / duration.toFloat()).coerceIn(0f, 1f)
    }

    private fun Double.roundDeg(): String = "${roundToInt()}°"

    internal fun activePassModel(
        model: NextPassTileDisplayModel,
        clockTimeFormatter: ClockTimeFormatter = ClockTimeFormatter.twentyFourHour(),
        text: NextPassTileText = NextPassTileText.English
    ): NextPassTileDisplayModel? {
        val identity = model.launchAction.exactPassIdentity ?: return null
        val maxElevationDegrees = model.maxElevationDegrees ?: return null
        return model.copy(
            kind = NextPassTileKind.ActivePass,
            countdown = countdownLabel(identity.losMillis - identity.aosMillis, text),
            meta = "${text.losLabel} ${clockTimeFormatter.formatMinutes(identity.losMillis)} · " +
                "${text.maxLabel} ${maxElevationDegrees.roundDeg()}",
            ctaLabel = text.radarAction,
            tone = NextPassTileTone.Primary,
            showProgress = true,
            progress = 1f,
            countdownTargetMillis = identity.losMillis,
            progressStartMillis = identity.aosMillis,
            progressEndMillis = identity.losMillis,
            nextTransitionMillis = identity.losMillis,
            accessibilityDescription =
                "${model.title}, " +
                    "${model.accessibilityFreshnessDescription.orUnknownFreshness(text)}, " +
                    "${text.opens} ${text.radarAction}"
        )
    }

    internal fun endedPassModel(
        model: NextPassTileDisplayModel,
        text: NextPassTileText = NextPassTileText.English
    ): NextPassTileDisplayModel {
        return model.copy(
            kind = NextPassTileKind.NoPassSoon,
            countdown = null,
            meta = text.passEnded,
            ctaLabel = text.passesAction,
            tone = NextPassTileTone.Info,
            showProgress = false,
            progress = null,
            countdownTargetMillis = null,
            progressStartMillis = null,
            progressEndMillis = null,
            nextTransitionMillis = null,
            accessibilityDescription =
                "${model.title}, ${text.passEnded.lowercase()}, " +
                    "${model.accessibilityFreshnessDescription.orUnknownFreshness(text)}, " +
                    "${text.opens} ${text.passesAction}"
        )
    }

    private fun String?.orUnknownFreshness(text: NextPassTileText): String {
        return this ?: text.tleAgeUnknown
    }
}

private data class TileTleFreshnessText(
    val statusLabel: String,
    val tileHeader: String,
    val detail: String,
    val guidance: String
)

internal data class NextPassTileTimelineEntryModel(
    val model: NextPassTileDisplayModel,
    val startMillis: Long? = null,
    val endMillis: Long? = null
)

internal object NextPassTileTimelinePolicy {
    fun entriesFor(
        model: NextPassTileDisplayModel,
        clockTimeFormatter: ClockTimeFormatter = ClockTimeFormatter.twentyFourHour(),
        text: NextPassTileText = NextPassTileText.English
    ): List<NextPassTileTimelineEntryModel> {
        val identity = model.launchAction.exactPassIdentity
            ?: return listOf(NextPassTileTimelineEntryModel(model))
        val ended = NextPassTileDisplayPolicy.endedPassModel(model, text)
        return when (model.kind) {
            NextPassTileKind.UpcomingPass -> {
                val active = NextPassTileDisplayPolicy.activePassModel(
                    model = model,
                    clockTimeFormatter = clockTimeFormatter,
                    text = text
                )
                    ?: return listOf(NextPassTileTimelineEntryModel(model))
                listOf(
                    NextPassTileTimelineEntryModel(
                        model = model,
                        endMillis = identity.aosMillis
                    ),
                    NextPassTileTimelineEntryModel(
                        model = active,
                        startMillis = identity.aosMillis,
                        endMillis = identity.losMillis
                    ),
                    NextPassTileTimelineEntryModel(
                        model = ended
                    )
                )
            }
            NextPassTileKind.ActivePass -> listOf(
                NextPassTileTimelineEntryModel(
                    model = model,
                    endMillis = identity.losMillis
                ),
                NextPassTileTimelineEntryModel(
                    model = ended
                )
            )
            else -> listOf(NextPassTileTimelineEntryModel(model))
        }
    }
}
