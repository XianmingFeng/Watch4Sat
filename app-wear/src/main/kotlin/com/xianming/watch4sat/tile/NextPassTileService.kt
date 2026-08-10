package com.xianming.watch4sat.tile

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.wear.protolayout.ActionBuilders.intExtra
import androidx.wear.protolayout.ActionBuilders.launchAction
import androidx.wear.protolayout.ActionBuilders.longExtra
import androidx.wear.protolayout.ActionBuilders.stringExtra
import androidx.wear.protolayout.DeviceParametersBuilders
import androidx.wear.protolayout.DimensionBuilders.dp
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.DimensionBuilders.wrap
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.protolayout.TypeBuilders.StringLayoutConstraint
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicFloat
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicInstant
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicInt32
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicString
import androidx.wear.protolayout.material3.ColorScheme
import androidx.wear.protolayout.material3.ProgressIndicatorColors
import androidx.wear.protolayout.material3.Typography
import androidx.wear.protolayout.material3.circularProgressIndicator
import androidx.wear.protolayout.material3.materialScope
import androidx.wear.protolayout.material3.primaryLayout
import androidx.wear.protolayout.material3.text
import androidx.wear.protolayout.material3.textEdgeButton
import androidx.wear.protolayout.modifiers.clickable
import androidx.wear.protolayout.types.LayoutColor
import androidx.wear.protolayout.types.LayoutString
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.ResourceBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import com.google.common.util.concurrent.ListenableFuture
import com.xianming.watch4sat.BuildConfig
import com.xianming.watch4sat.EnglishLocaleContext
import com.xianming.watch4sat.MainActivity
import com.xianming.watch4sat.R
import com.xianming.watch4sat.data.Watch4SatDataLayer
import com.xianming.watch4sat.data.network.NetworkClientIdentity
import com.xianming.watch4sat.domain.freshness.TleFreshnessSeverity
import com.xianming.watch4sat.domain.time.ClockTimeFormatter
import com.xianming.watch4sat.time.AndroidClockTimeFormatter
import com.xianming.watch4sat.wear.TileLaunchIntentPolicy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.time.Instant
import kotlin.math.min

class NextPassTileService : TileService() {
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(EnglishLocaleContext.wrap(newBase))
    }

    private val tileStateRepository: TileStateRepository by lazy {
        val dependencies = Watch4SatDataLayer.create(
            context = applicationContext,
            networkClientIdentity = NetworkClientIdentity(
                versionName = BuildConfig.VERSION_NAME,
                applicationId = BuildConfig.APPLICATION_ID
            )
        )
        TileStateRepository(
            satelliteDataRepository = dependencies.satelliteDataRepository,
            settingsStore = dependencies.settingsStore,
            passSnapshotCache = dependencies.passSnapshotCache
        )
    }

    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<TileBuilders.Tile> {
        return CallbackToFutureAdapter.getFuture { completer ->
            coroutineScope.launch {
                val nowMillis = System.currentTimeMillis()
                val clockTimeFormatter = AndroidClockTimeFormatter.create(applicationContext)
                val text = NextPassTileText.from(applicationContext)
                val tile = runCatching {
                    val state = tileStateRepository.load(nowMillis)
                    buildTile(
                        requestParams = requestParams,
                        model = NextPassTileDisplayPolicy.modelFor(
                            state = state,
                            nowMillis = nowMillis,
                            clockTimeFormatter = clockTimeFormatter,
                            text = text
                        ),
                        nowMillis = nowMillis,
                        clockTimeFormatter = clockTimeFormatter,
                        text = text
                    )
                }.getOrElse { throwable ->
                    buildTile(
                        requestParams = requestParams,
                        model = NextPassTileDisplayPolicy.offline(text),
                        nowMillis = nowMillis,
                        clockTimeFormatter = clockTimeFormatter,
                        text = text
                    )
                }
                completer.set(tile)
            }
            "NextPassTileService.onTileRequest"
        }
    }

    override fun onDestroy() {
        coroutineScope.cancel()
        super.onDestroy()
    }

    private fun buildTile(
        requestParams: RequestBuilders.TileRequest,
        model: NextPassTileDisplayModel,
        nowMillis: Long,
        clockTimeFormatter: ClockTimeFormatter,
        text: NextPassTileText
    ): TileBuilders.Tile {
        val timeline = NextPassTileTimelineRenderer.timeline(
            entries = NextPassTileTimelinePolicy.entriesFor(
                model = model,
                clockTimeFormatter = clockTimeFormatter,
                text = text
            )
        ) { entryModel ->
            val tileClick = clickable(
                action = launchAction(
                    ComponentName(applicationContext, MainActivity::class.java),
                    entryModel.launchAction.toProtoLayoutExtras()
                ),
                id = "next_pass_tile_${entryModel.kind.name}"
            )
            LayoutElementBuilders.Layout.Builder()
                .setRoot(
                    NextPassTileLayoutPolicy.layout(
                        context = applicationContext,
                        deviceConfiguration = requestParams.deviceConfiguration,
                        model = entryModel,
                        tileClick = tileClick
                    )
                )
                .build()
        }

        return TileBuilders.Tile.Builder()
            .setResourcesVersion(RESOURCES_VERSION)
            .setTileTimeline(timeline)
            .apply {
                NextPassTileLayoutPolicy.freshnessIntervalMillis(model, nowMillis)?.let {
                    setFreshnessIntervalMillis(it)
                }
            }
            .build()
    }

    override fun onResourcesRequest(requestParams: RequestBuilders.ResourcesRequest): ListenableFuture<ResourceBuilders.Resources> {
        return immediateFuture(
            ResourceBuilders.Resources.Builder()
                .setVersion(RESOURCES_VERSION)
                .build()
        )
    }

    private fun <T> immediateFuture(value: T): ListenableFuture<T> {
        return CallbackToFutureAdapter.getFuture { completer ->
            completer.set(value)
            "NextPassTileService.immediateFuture"
        }
    }

    private companion object {
        const val RESOURCES_VERSION = "1"
    }
}

internal fun TileLaunchAction.toProtoLayoutExtras() = buildMap {
    put(TileLaunchIntentPolicy.extraSource, stringExtra(TileLaunchIntentPolicy.sourceTile))
    put(TileLaunchIntentPolicy.extraDestination, stringExtra(destination.name))
    catalogNumber?.let {
        put(TileLaunchIntentPolicy.extraCatalogNumber, intExtra(it))
    }
    aosMillis?.let {
        put(TileLaunchIntentPolicy.extraAosMillis, longExtra(it))
    }
    losMillis?.let {
        put(TileLaunchIntentPolicy.extraLosMillis, longExtra(it))
    }
}

internal fun TileLaunchAction.toMainActivityIntent(context: Context): Intent {
    return Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        putExtra(TileLaunchIntentPolicy.extraSource, TileLaunchIntentPolicy.sourceTile)
        putExtra(TileLaunchIntentPolicy.extraDestination, destination.name)
        catalogNumber?.let {
            putExtra(TileLaunchIntentPolicy.extraCatalogNumber, it)
        }
        aosMillis?.let {
            putExtra(TileLaunchIntentPolicy.extraAosMillis, it)
        }
        losMillis?.let {
            putExtra(TileLaunchIntentPolicy.extraLosMillis, it)
        }
    }
}

internal data class NextPassTileProgressRingSpec(
    val diameterDp: Float,
    val startAngleDegrees: Float,
    val endAngleDegrees: Float,
    val strokeWidthDp: Float
)

internal data class NextPassTileProgressState(
    val staticProgress: Float,
    val dynamicProgress: DynamicFloat?
)

internal object NextPassTileTimelineRenderer {
    fun timeline(
        entries: List<NextPassTileTimelineEntryModel>,
        layoutFor: (NextPassTileDisplayModel) -> LayoutElementBuilders.Layout
    ): TimelineBuilders.Timeline {
        return TimelineBuilders.Timeline.Builder().apply {
            entries.forEach { entry ->
                addTimelineEntry(
                    TimelineBuilders.TimelineEntry.Builder()
                        .setLayout(layoutFor(entry.model))
                        .apply {
                            entry.validity()?.let(::setValidity)
                        }
                        .build()
                )
            }
        }.build()
    }

    private fun NextPassTileTimelineEntryModel.validity(): TimelineBuilders.TimeInterval? {
        if (startMillis == null && endMillis == null) return null
        val end = requireNotNull(endMillis) {
            "A bounded Tile timeline entry requires an end time"
        }
        val start = startMillis ?: 0L
        require(end > start) {
            "Tile timeline entry end time must be after its start time"
        }
        return TimelineBuilders.TimeInterval.Builder()
            .apply {
                startMillis?.let(::setStartMillis)
                setEndMillis(end)
            }
            .build()
    }
}

internal object NextPassTileLayoutPolicy {
    const val ScreenEdgeProgressStartAngleDegrees = 216f
    const val ScreenEdgeProgressEndAngleDegrees = 504f
    const val CountdownLayoutPattern = "9999 min"
    private const val LargeScreenBreakpointDp = 225
    private const val SmallScreenProgressStrokeWidthDp = 5f
    private const val LargeScreenProgressStrokeWidthDp = 6f
    private const val MaxTitleChars = 18
    private const val MaxMetaChars = 28
    private const val MinTransitionRefreshDelayMillis = 1L
    private val WarningTileColor = LayoutColor(0xFFFFB300.toInt())
    private val ErrorTileColor = LayoutColor(0xFFF44336.toInt())
    private val VeryStaleTileColor = LayoutColor(0xFFFF6D00.toInt())

    private val CountdownLayoutConstraint =
        StringLayoutConstraint.Builder(CountdownLayoutPattern).build()

    fun layout(
        context: Context,
        deviceConfiguration: DeviceParametersBuilders.DeviceParameters,
        model: NextPassTileDisplayModel,
        tileClick: ModifiersBuilders.Clickable,
        allowDynamicTheme: Boolean = true
    ): LayoutElementBuilders.LayoutElement {
        return materialScope(
            context = context,
            deviceConfiguration = deviceConfiguration,
            allowDynamicTheme = allowDynamicTheme,
            defaultColorScheme = ColorScheme()
        ) {
            val content = primaryLayout(
                titleSlot = {
                    tileLabel(
                        text = model.header,
                        typography = Typography.TITLE_SMALL,
                        color = model.headerColor(this)
                    )
                },
                mainSlot = {
                    tileMainContent(model)
                },
                bottomSlot = {
                    textEdgeButton(
                        onClick = tileClick,
                        labelContent = {
                            tileEdgeButtonLabel(model.ctaLabel)
                        }
                    )
                },
                onClick = tileClick
            )
            stackedRoot(
                progressIndicator = if (model.showProgress) {
                    screenEdgeProgressIndicator(model)
                } else {
                    null
                },
                content = content,
                contentDescription = model.accessibilityDescription
            )
        }
    }

    fun progressRingSpec(
        screenWidthDp: Int,
        screenHeightDp: Int
    ): NextPassTileProgressRingSpec {
        return NextPassTileProgressRingSpec(
            diameterDp = min(screenWidthDp, screenHeightDp).toFloat(),
            startAngleDegrees = ScreenEdgeProgressStartAngleDegrees,
            endAngleDegrees = ScreenEdgeProgressEndAngleDegrees,
            strokeWidthDp = if (screenWidthDp >= LargeScreenBreakpointDp) {
                LargeScreenProgressStrokeWidthDp
            } else {
                SmallScreenProgressStrokeWidthDp
            }
        )
    }

    fun stackedRoot(
        progressIndicator: LayoutElementBuilders.LayoutElement?,
        content: LayoutElementBuilders.LayoutElement,
        contentDescription: String
    ): LayoutElementBuilders.Box {
        return LayoutElementBuilders.Box.Builder()
            .setWidth(expand())
            .setHeight(expand())
            .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
            .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
            .apply {
                progressIndicator?.let(::addContent)
            }
            .setModifiers(
                ModifiersBuilders.Modifiers.Builder()
                    .setSemantics(
                        ModifiersBuilders.Semantics.Builder()
                            .setContentDescription(contentDescription)
                            .build()
                    )
                    .build()
            )
            .addContent(content)
            .build()
    }

    fun progressState(model: NextPassTileDisplayModel): NextPassTileProgressState {
        return NextPassTileProgressState(
            staticProgress = model.progress?.coerceIn(0f, 1f) ?: 0f,
            dynamicProgress = dynamicProgress(model)
        )
    }

    private fun androidx.wear.protolayout.material3.MaterialScope.tileMainContent(
        model: NextPassTileDisplayModel
    ): LayoutElementBuilders.LayoutElement {
        val materialScope = this
        val contents = buildList {
            add(
                tileLabel(
                    text = model.title.take(MaxTitleChars),
                    typography = Typography.TITLE_MEDIUM,
                    color = model.primaryColor(materialScope)
                )
            )
            model.countdown?.let { countdown ->
                add(
                    tileLabel(
                        text = countdownLayoutString(
                            model = model,
                            staticText = countdown,
                            secondsSuffix = context.getString(R.string.tile_seconds_short),
                            minutesSuffix = context.getString(R.string.tile_minutes_short)
                        ),
                        typography = Typography.NUMERAL_LARGE,
                        color = model.primaryColor(materialScope)
                    )
                )
            }
            add(
                tileLabel(
                    text = model.meta.take(MaxMetaChars),
                    typography = Typography.BODY_SMALL,
                    color = colorScheme.onSurfaceVariant
                )
            )
        }
        return centeredMainContent(contents)
    }

    fun centeredMainContent(
        contents: List<LayoutElementBuilders.LayoutElement>
    ): LayoutElementBuilders.Box {
        val column = LayoutElementBuilders.Column.Builder()
            .setWidth(expand())
            .setHeight(wrap())
            .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
            .apply {
                contents.forEach(::addContent)
            }
            .build()
        return LayoutElementBuilders.Box.Builder()
            .setWidth(expand())
            .setHeight(expand())
            .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
            .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
            .addContent(column)
            .build()
    }

    private fun androidx.wear.protolayout.material3.MaterialScope.screenEdgeProgressIndicator(
        model: NextPassTileDisplayModel
    ): LayoutElementBuilders.LayoutElement {
        val spec = progressRingSpec(
            screenWidthDp = deviceConfiguration.screenWidthDp,
            screenHeightDp = deviceConfiguration.screenHeightDp
        )
        val progressState = progressState(model)
        return circularProgressIndicator(
            staticProgress = progressState.staticProgress,
            dynamicProgress = progressState.dynamicProgress,
            startAngleDegrees = spec.startAngleDegrees,
            endAngleDegrees = spec.endAngleDegrees,
            strokeWidth = spec.strokeWidthDp,
            colors = ProgressIndicatorColors(
                indicatorColor = colorScheme.primary,
                trackColor = colorScheme.surfaceContainer,
                trackOverflowColor = colorScheme.surfaceContainer
            ),
            size = dp(spec.diameterDp)
        )
    }

    private fun NextPassTileDisplayModel.primaryColor(
        scope: androidx.wear.protolayout.material3.MaterialScope
    ): LayoutColor {
        return when (tone) {
            NextPassTileTone.Primary -> scope.colorScheme.primary
            NextPassTileTone.Info -> scope.colorScheme.secondary
            NextPassTileTone.Warning -> WarningTileColor
            NextPassTileTone.Error -> ErrorTileColor
        }
    }

    private fun NextPassTileDisplayModel.headerColor(
        scope: androidx.wear.protolayout.material3.MaterialScope
    ): LayoutColor {
        return when (tleFreshnessSeverity) {
            TleFreshnessSeverity.FRESH,
            null -> scope.colorScheme.onSurfaceVariant
            TleFreshnessSeverity.STALE -> WarningTileColor
            TleFreshnessSeverity.VERY_STALE -> VeryStaleTileColor
        }
    }

    private fun androidx.wear.protolayout.material3.MaterialScope.tileLabel(
        text: String,
        typography: Int,
        color: LayoutColor
    ): LayoutElementBuilders.LayoutElement {
        return text(
            text = LayoutString(text),
            typography = typography,
            color = color,
            maxLines = 1,
            overflow = LayoutElementBuilders.TEXT_OVERFLOW_ELLIPSIZE
        )
    }

    private fun androidx.wear.protolayout.material3.MaterialScope.tileEdgeButtonLabel(
        label: String
    ): LayoutElementBuilders.LayoutElement {
        return text(
            text = LayoutString(label),
            maxLines = 1,
            overflow = LayoutElementBuilders.TEXT_OVERFLOW_ELLIPSIZE
        )
    }

    private fun androidx.wear.protolayout.material3.MaterialScope.tileLabel(
        text: LayoutString,
        typography: Int,
        color: LayoutColor
    ): LayoutElementBuilders.LayoutElement {
        return text(
            text = text,
            typography = typography,
            color = color,
            maxLines = 1,
            overflow = LayoutElementBuilders.TEXT_OVERFLOW_ELLIPSIZE
        )
    }

    private fun dynamicProgress(model: NextPassTileDisplayModel): DynamicFloat? {
        val startMillis = model.progressStartMillis ?: return null
        val endMillis = model.progressEndMillis ?: return null
        val durationSeconds = (endMillis - startMillis) / 1_000f
        if (durationSeconds <= 0f) return null

        val remainingSeconds = DynamicInstant.platformTimeWithSecondsPrecision()
            .durationUntil(DynamicInstant.withSecondsPrecision(Instant.ofEpochMilli(endMillis)))
            .toIntSeconds()
        val rawProgress = remainingSeconds.div(durationSeconds)
        val upperBounded = DynamicFloat.onCondition(rawProgress.gt(1f))
            .use(1f)
            .elseUse(rawProgress)
        return DynamicFloat.onCondition(rawProgress.lt(0f))
            .use(0f)
            .elseUse(upperBounded)
    }

    fun countdownLayoutString(
        model: NextPassTileDisplayModel,
        staticText: String,
        secondsSuffix: String,
        minutesSuffix: String
    ): LayoutString {
        val targetMillis = model.countdownTargetMillis ?: return LayoutString(staticText)
        val remainingSeconds = DynamicInstant.platformTimeWithSecondsPrecision()
            .durationUntil(DynamicInstant.withSecondsPrecision(Instant.ofEpochMilli(targetMillis)))
            .toIntSeconds()
        val safeSeconds = DynamicInt32.onCondition(remainingSeconds.gte(0))
            .use(remainingSeconds)
            .elseUse(0)
        val secondsText = safeSeconds.format()
            .concat(DynamicString.constant(" $secondsSuffix"))
        val minutesText = safeSeconds.plus(59).div(60).format()
            .concat(DynamicString.constant(" $minutesSuffix"))
        val dynamicText = DynamicString.onCondition(safeSeconds.lt(60))
            .use(secondsText)
            .elseUse(minutesText)
        return LayoutString(staticText, dynamicText, CountdownLayoutConstraint)
    }

    fun freshnessIntervalMillis(
        model: NextPassTileDisplayModel,
        nowMillis: Long
    ): Long? {
        val transitionMillis = model.nextTransitionMillis ?: return null
        val intervalMillis = transitionMillis - nowMillis
        return intervalMillis.takeIf { it >= 0L }
            ?.coerceAtLeast(MinTransitionRefreshDelayMillis)
    }
}
