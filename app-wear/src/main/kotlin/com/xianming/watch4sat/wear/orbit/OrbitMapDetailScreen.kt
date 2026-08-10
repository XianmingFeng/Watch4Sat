package com.xianming.watch4sat.wear.orbit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnItemScope
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices
import androidx.wear.compose.ui.tooling.preview.WearPreviewFontScales
import com.xianming.watch4sat.R
import com.xianming.watch4sat.data.settings.AppThemePreset
import com.xianming.watch4sat.wear.RoundListTransformationProvider
import com.xianming.watch4sat.wear.ReportTimeTextVisibility
import com.xianming.watch4sat.wear.WearScrollIndicator
import com.xianming.watch4sat.wear.roundListSurfaceTransformation
import com.xianming.watch4sat.wear.roundListTransformedHeight
import com.xianming.watch4sat.wear.state.RoundListSurface
import com.xianming.watch4sat.wear.theme.LocalWatchThemeColors
import com.xianming.watch4sat.wear.theme.WatchThemeCatalog
import com.xianming.watch4sat.wear.theme.WatchTypography
import kotlin.math.absoluteValue

object OrbitMapDetailTestTags {
    const val List = "orbit_detail_list"
    const val Title = "orbit_detail_title"
    const val Summary = "orbit_detail_summary"
    const val CurrentAltitude = "orbit_detail_current_altitude"
    const val Footprint = "orbit_detail_footprint"
    const val CurrentDistance = "orbit_detail_current_distance"
    const val OrbitSection = "orbit_detail_orbit_section"
    const val TransmittersSection = "orbit_detail_transmitters_section"
}

// The title begins near the top of the round viewport, where the visible chord is narrower.
private const val OrbitDetailTitleMaxWidthFraction = 0.74f

@Composable
fun OrbitMapDetailScreen(
    detail: OrbitMapDetail?,
    modifier: Modifier = Modifier
) {
    val listState = rememberTransformingLazyColumnState()
    val colors = LocalWatchThemeColors.current
    val showTimeText by remember(listState) {
        derivedStateOf { !listState.canScrollBackward }
    }
    ReportTimeTextVisibility(showTimeText)

    ScreenScaffold(
        modifier = modifier,
        scrollState = listState,
        scrollIndicator = { WearScrollIndicator(state = listState) }
    ) { contentPadding ->
        val layoutDirection = LocalLayoutDirection.current
        val titleHorizontalPadding = contentPadding.calculateLeftPadding(layoutDirection)
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            val titleMaxWidth = maxWidth * OrbitDetailTitleMaxWidthFraction
            RoundListTransformationProvider {
                TransformingLazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(
                        start = 4.dp,
                        end = 4.dp,
                        top = contentPadding.calculateTopPadding() + 12.dp,
                        bottom = contentPadding.calculateBottomPadding() + 18.dp
                    ),
                    rotaryScrollableBehavior =
                    RotaryScrollableDefaults.behavior(scrollableState = listState),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag(OrbitMapDetailTestTags.List)
                ) {
                    if (detail == null) {
                        item(key = "orbit-detail-empty-title") {
                            Text(
                                text = stringResource(R.string.orbit_detail_title),
                                style = MaterialTheme.typography.titleLarge,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = titleHorizontalPadding)
                                    .semantics { heading() }
                            )
                        }
                        item(key = "orbit-detail-empty-message") {
                            DetailTransformingSurface(itemScope = this) {
                                DetailValueRow(
                                    label = stringResource(
                                        R.string.orbit_detail_no_satellite
                                    ),
                                    value = stringResource(
                                        R.string.orbit_detail_select_satellite_guidance
                                    )
                                )
                            }
                        }
                    } else {
                        item(key = "orbit-detail-heading") {
                            val titleDescription = stringResource(
                                R.string.orbit_detail_content_description,
                                detail.satelliteName,
                                detail.catalogLine
                            )
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = titleHorizontalPadding)
                                    .testTag(OrbitMapDetailTestTags.Title)
                                    .semantics(mergeDescendants = true) {
                                        heading()
                                        contentDescription = titleDescription
                                    },
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = detail.satelliteName,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        lineBreak = LineBreak.Heading
                                    ),
                                    color = colors.primary,
                                    textAlign = TextAlign.Center,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .widthIn(max = titleMaxWidth)
                                        .fillMaxWidth()
                                )
                                Text(
                                    text = detail.catalogLine,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.mutedText,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1
                                )
                            }
                        }
                        item(key = "orbit-detail-summary") {
                            DetailTransformingSurface(itemScope = this) {
                                OrbitSummary(
                                    currentAltitudeKm = detail.currentAltitudeKm,
                                    footprintDiameterKm = detail.footprintDiameterKm,
                                    currentDistanceKm = detail.currentDistanceKm
                                )
                            }
                        }
                        item(key = "orbit-detail-position") {
                            DetailTransformingSurface(itemScope = this) {
                                val position = detailPositionText(detail.currentPosition)
                                DetailValueRow(
                                    label = stringResource(
                                        R.string.orbit_detail_current_position
                                    ),
                                    value = position.visible,
                                    contentDescription = position.accessibility
                                )
                            }
                        }
                        item(key = "orbit-detail-updated") {
                            DetailTransformingSurface(itemScope = this) {
                                DetailValueRow(
                                    label = stringResource(R.string.orbit_detail_updated),
                                    value = detailUpdateText(detail.updatedAt)
                                )
                            }
                        }
                        item(key = "orbit-detail-section-orbit") {
                            DetailSectionHeading(
                                itemScope = this,
                                title = stringResource(R.string.orbit_detail_orbit_section),
                                testTag = OrbitMapDetailTestTags.OrbitSection
                            )
                        }
                        detail.orbitRows.forEach { row ->
                            item(key = row.key) {
                                DetailTransformingSurface(itemScope = this) {
                                    DetailValueRow(
                                        label = orbitMetricLabel(row.metric),
                                        value = orbitMetricValue(row)
                                    )
                                }
                            }
                        }
                        item(key = "orbit-detail-section-transmitters") {
                            DetailSectionHeading(
                                itemScope = this,
                                title = stringResource(
                                    R.string.orbit_detail_transmitters_section
                                ),
                                testTag = OrbitMapDetailTestTags.TransmittersSection
                            )
                        }
                        if (detail.transmitters.isEmpty()) {
                            item(key = "orbit-detail-transmitters-empty") {
                                DetailTransformingSurface(itemScope = this) {
                                    DetailValueRow(
                                        label = stringResource(
                                            R.string.orbit_detail_transmitters_section
                                        ),
                                        value = stringResource(
                                            R.string.orbit_detail_no_active_transmitters
                                        )
                                    )
                                }
                            }
                        } else {
                            detail.transmitters.forEach { transmitter ->
                                item(key = "transmitter-${transmitter.key}") {
                                    DetailTransformingSurface(itemScope = this) {
                                        DetailValueRow(
                                            label = transmitter.title,
                                            value = transmitterDetails(transmitter)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailTransformingSurface(
    itemScope: TransformingLazyColumnItemScope,
    surface: RoundListSurface = RoundListSurface.STANDARD_CARD,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val transformation = roundListSurfaceTransformation(itemScope, surface)
    val transparentPainter = remember { ColorPainter(Color.Transparent) }
    val transformedPainter = remember(transformation, transparentPainter) {
        transformation?.createContainerPainter(
            painter = transparentPainter,
            shape = RectangleShape
        )
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .roundListTransformedHeight(itemScope, surface)
            .then(
                if (transformation != null && transformedPainter != null) {
                    Modifier
                        .paint(transformedPainter)
                        .graphicsLayer {
                            with(transformation) {
                                applyContainerTransformation()
                            }
                        }
                } else {
                    Modifier
                }
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (transformation != null) {
                        Modifier.graphicsLayer {
                            with(transformation) {
                                applyContentTransformation()
                            }
                        }
                    } else {
                        Modifier
                    }
                )
        ) {
            content()
        }
    }
}

@Composable
private fun OrbitSummary(
    currentAltitudeKm: Int?,
    footprintDiameterKm: Int?,
    currentDistanceKm: Int?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(OrbitMapDetailTestTags.Summary),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        SummaryMetric(
            label = stringResource(R.string.orbit_detail_altitude),
            accessibilityLabel = stringResource(
                R.string.orbit_detail_current_altitude
            ),
            kilometers = currentAltitudeKm,
            testTag = OrbitMapDetailTestTags.CurrentAltitude,
            modifier = Modifier.weight(1f)
        )
        SummaryDivider()
        SummaryMetric(
            label = stringResource(R.string.orbit_detail_diameter),
            accessibilityLabel = stringResource(
                R.string.orbit_detail_footprint_diameter
            ),
            kilometers = footprintDiameterKm,
            testTag = OrbitMapDetailTestTags.Footprint,
            modifier = Modifier.weight(1f)
        )
        SummaryDivider()
        SummaryMetric(
            label = stringResource(R.string.orbit_detail_distance),
            accessibilityLabel = stringResource(
                R.string.orbit_detail_current_distance
            ),
            kilometers = currentDistanceKm,
            testTag = OrbitMapDetailTestTags.CurrentDistance,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SummaryDivider() {
    Spacer(
        modifier = Modifier
            .width(1.dp)
            .heightIn(min = 34.dp)
            .background(LocalWatchThemeColors.current.mutedText.copy(alpha = 0.28f))
    )
}

@Composable
private fun SummaryMetric(
    label: String,
    accessibilityLabel: String,
    kilometers: Int?,
    testTag: String,
    modifier: Modifier = Modifier
) {
    val unavailable = stringResource(R.string.orbit_detail_unavailable)
    val visibleValue = kilometers?.let {
        stringResource(R.string.orbit_detail_kilometers_summary, it)
    } ?: unavailable
    val accessibilityValue = kilometers?.let {
        stringResource(R.string.orbit_detail_kilometers, it)
    } ?: unavailable
    val metricDescription = stringResource(
        R.string.orbit_detail_content_description,
        accessibilityLabel,
        accessibilityValue
    )
    Column(
        modifier = modifier
            .heightIn(min = 44.dp)
            .testTag(testTag)
            .semantics(mergeDescendants = true) {
                contentDescription = metricDescription
            }
            .padding(vertical = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyExtraSmall,
            color = LocalWatchThemeColors.current.mutedText,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = visibleValue,
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun DetailSectionHeading(
    itemScope: TransformingLazyColumnItemScope,
    title: String,
    testTag: String
) {
    val surface = RoundListSurface.LIST_HEADER
    val transformation = roundListSurfaceTransformation(itemScope, surface)
    val sectionDescription = stringResource(
        R.string.orbit_detail_section_description,
        title
    )
    ListHeader(
        modifier = Modifier
            .fillMaxWidth()
            .roundListTransformedHeight(itemScope, surface)
            .testTag(testTag)
            .semantics(mergeDescendants = true) {
                heading()
                contentDescription = sectionDescription
            },
        transformation = transformation
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = LocalWatchThemeColors.current.primary,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun DetailValueRow(
    label: String,
    value: String,
    contentDescription: String? = null
) {
    val resolvedContentDescription = contentDescription ?: stringResource(
        R.string.orbit_detail_content_description,
        label,
        value
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = OrbitMapDetailLayoutPolicy.MinimumRowHeightDp.dp)
            .semantics(mergeDescendants = true) {
                this.contentDescription = resolvedContentDescription
            }
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = LocalWatchThemeColors.current.mutedText,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

private data class OrbitMapPositionText(
    val visible: String,
    val accessibility: String
)

@Composable
private fun detailPositionText(position: OrbitMapDetailPosition?): OrbitMapPositionText {
    val unavailable = stringResource(R.string.orbit_detail_unavailable)
    if (position == null) {
        return OrbitMapPositionText(
            visible = unavailable,
            accessibility = stringResource(
                R.string.orbit_detail_content_description,
                stringResource(R.string.orbit_detail_current_position),
                unavailable
            )
        )
    }
    val latitudeHemisphere = stringResource(
        if (position.latitudeDegrees < 0.0) {
            R.string.orbit_detail_hemisphere_south
        } else {
            R.string.orbit_detail_hemisphere_north
        }
    )
    val longitudeHemisphere = stringResource(
        if (position.longitudeDegrees < 0.0) {
            R.string.orbit_detail_hemisphere_west
        } else {
            R.string.orbit_detail_hemisphere_east
        }
    )
    val coordinates = stringResource(
        R.string.orbit_detail_coordinates,
        position.latitudeDegrees.absoluteValue,
        latitudeHemisphere,
        position.longitudeDegrees.absoluteValue,
        longitudeHemisphere
    )
    val visible = position.grid?.let { grid ->
        stringResource(R.string.orbit_detail_coordinates_with_grid, coordinates, grid)
    } ?: coordinates
    val accessibilityValue = position.grid?.let { grid ->
        stringResource(
            R.string.orbit_detail_coordinates_grid_description,
            coordinates,
            grid
        )
    } ?: coordinates
    return OrbitMapPositionText(
        visible = visible,
        accessibility = stringResource(
            R.string.orbit_detail_content_description,
            stringResource(R.string.orbit_detail_current_position),
            accessibilityValue
        )
    )
}

@Composable
private fun detailUpdateText(update: OrbitMapDetailUpdate?): String {
    return update?.let {
        stringResource(R.string.orbit_detail_updated_value, it.date, it.time)
    } ?: stringResource(R.string.orbit_detail_unavailable)
}

@Composable
private fun orbitMetricLabel(metric: OrbitMapOrbitMetric): String {
    return stringResource(
        when (metric) {
            OrbitMapOrbitMetric.MeanAltitude ->
                R.string.orbit_detail_mean_orbit_altitude
            OrbitMapOrbitMetric.Period -> R.string.orbit_detail_period
            OrbitMapOrbitMetric.MeanMotion -> R.string.orbit_detail_mean_motion
            OrbitMapOrbitMetric.Inclination -> R.string.orbit_detail_inclination
            OrbitMapOrbitMetric.Eccentricity -> R.string.orbit_detail_eccentricity
            OrbitMapOrbitMetric.RightAscensionAscendingNode -> R.string.orbit_detail_raan
            OrbitMapOrbitMetric.ArgumentOfPerigee ->
                R.string.orbit_detail_argument_of_perigee
        }
    )
}

@Composable
private fun orbitMetricValue(row: OrbitMapDetailRow): String {
    val value = row.value ?: return stringResource(R.string.orbit_detail_unavailable)
    if (row.metric == OrbitMapOrbitMetric.MeanAltitude) {
        val altitude = value as? OrbitMapOrbitValue.MeanAltitude
            ?: return stringResource(R.string.orbit_detail_unavailable)
        return stringResource(
            R.string.orbit_detail_mean_altitude_value,
            altitude.meanKilometers,
            altitude.perigeeKilometers,
            altitude.apogeeKilometers
        )
    }
    val scalar = (value as? OrbitMapOrbitValue.Scalar)?.value
        ?: return stringResource(R.string.orbit_detail_unavailable)
    return when (row.metric) {
        OrbitMapOrbitMetric.Period -> stringResource(
            R.string.orbit_detail_period_value,
            scalar
        )
        OrbitMapOrbitMetric.MeanMotion -> stringResource(
            R.string.orbit_detail_mean_motion_value,
            scalar
        )
        OrbitMapOrbitMetric.Inclination,
        OrbitMapOrbitMetric.RightAscensionAscendingNode,
        OrbitMapOrbitMetric.ArgumentOfPerigee -> stringResource(
            R.string.orbit_detail_degrees_value,
            scalar
        )
        OrbitMapOrbitMetric.Eccentricity -> stringResource(
            R.string.orbit_detail_eccentricity_value,
            scalar
        )
        OrbitMapOrbitMetric.MeanAltitude ->
            stringResource(R.string.orbit_detail_unavailable)
    }
}

@Composable
private fun transmitterDetails(transmitter: OrbitMapDetailTransmitter): String {
    val status = transmitter.status ?: stringResource(
        if (transmitter.isAlive) {
            R.string.orbit_detail_transmitter_active
        } else {
            R.string.orbit_detail_transmitter_inactive
        }
    )
    return buildList {
        add(status)
        transmitter.downlink?.let {
            add(
                stringResource(
                    R.string.orbit_detail_downlink,
                    radioLinkText(it)
                )
            )
        }
        transmitter.uplink?.let {
            add(
                stringResource(
                    R.string.orbit_detail_uplink,
                    radioLinkText(it)
                )
            )
        }
        if (transmitter.isInverted) {
            add(stringResource(R.string.orbit_detail_inverted))
        }
    }.joinToString("\n")
}

@Composable
private fun radioLinkText(link: OrbitMapRadioLink): String {
    val lowMhz = link.lowHz?.let {
        stringResource(R.string.orbit_detail_frequency_value, it / 1_000_000.0)
    }
    val highMhz = link.highHz?.let {
        stringResource(R.string.orbit_detail_frequency_value, it / 1_000_000.0)
    }
    val frequency = when {
        lowMhz != null && highMhz != null && lowMhz != highMhz -> stringResource(
            R.string.orbit_detail_frequency_range_mhz,
            lowMhz,
            highMhz
        )
        lowMhz != null -> stringResource(R.string.orbit_detail_frequency_mhz, lowMhz)
        highMhz != null -> stringResource(R.string.orbit_detail_frequency_mhz, highMhz)
        else -> stringResource(R.string.orbit_detail_unavailable)
    }
    return link.mode?.let { mode ->
        stringResource(R.string.orbit_detail_frequency_with_mode, frequency, mode)
    } ?: frequency
}

@WearPreviewDevices
@WearPreviewFontScales
@Composable
private fun OrbitMapDetailScreenPreview() {
    val previewColors = WatchThemeCatalog.colorsFor(AppThemePreset.SKY_BLUE)
    MaterialTheme(
        colorScheme = WatchThemeCatalog.wearColorSchemeFor(previewColors),
        typography = WatchTypography
    ) {
        CompositionLocalProvider(LocalWatchThemeColors provides previewColors) {
            AppScaffold {
                OrbitMapDetailScreen(
                    detail = OrbitMapDetail(
                        catalogNumber = 25544,
                        satelliteName = "RS-44 & BREEZE-KM R/B",
                        catalogLine = "#25544",
                        currentAltitudeKm = 418,
                        footprintDiameterKm = 4_620,
                        currentDistanceKm = 1_423,
                        currentPosition = OrbitMapDetailPosition(
                            latitudeDegrees = 31.230,
                            longitudeDegrees = 121.474,
                            grid = "PM01RF"
                        ),
                        updatedAt = OrbitMapDetailUpdate(
                            date = "Jul 29",
                            time = "21:18:42"
                        ),
                        orbitRows = listOf(
                            OrbitMapDetailRow(
                                key = "mean-altitude",
                                metric = OrbitMapOrbitMetric.MeanAltitude,
                                value = OrbitMapOrbitValue.MeanAltitude(
                                    meanKilometers = 417,
                                    perigeeKilometers = 410,
                                    apogeeKilometers = 424
                                )
                            ),
                            OrbitMapDetailRow(
                                key = "period",
                                metric = OrbitMapOrbitMetric.Period,
                                value = OrbitMapOrbitValue.Scalar(92.9)
                            ),
                            OrbitMapDetailRow(
                                key = "inclination",
                                metric = OrbitMapOrbitMetric.Inclination,
                                value = OrbitMapOrbitValue.Scalar(51.6)
                            ),
                        ),
                        transmitters = listOf(
                            OrbitMapDetailTransmitter(
                                key = "preview-fm",
                                title = "FM voice repeater",
                                status = "active",
                                isAlive = true,
                                downlink = OrbitMapRadioLink(
                                    lowHz = 145_800_000,
                                    highHz = null,
                                    mode = "FM"
                                ),
                                uplink = OrbitMapRadioLink(
                                    lowHz = 437_800_000,
                                    highHz = null,
                                    mode = "FM"
                                ),
                                isInverted = false
                            )
                        )
                    )
                )
            }
        }
    }
}
