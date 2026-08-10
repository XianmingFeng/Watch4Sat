package com.xianming.watch4sat.tile

import android.content.Context
import androidx.wear.protolayout.DeviceParametersBuilders
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.tooling.preview.Preview
import androidx.wear.tiles.tooling.preview.TilePreviewData
import androidx.wear.tiles.tooling.preview.TilePreviewHelper
import androidx.wear.tooling.preview.devices.WearDevices

@Preview(
    name = "Next pass loaded",
    device = WearDevices.LARGE_ROUND
)
fun nextPassLoadedTilePreview(context: Context): TilePreviewData {
    return TilePreviewData { request ->
        NextPassTilePreview.tile(
            context = context,
            deviceConfiguration = request.deviceConfiguration
        )
    }
}

internal object NextPassTilePreview {
    const val AssetSizePx = 400
    const val LogicalSizeDp = 200
    const val AssetDensity = 2f
    const val AllowDynamicTheme = false
    const val OutputFileName = "tile_preview_next_pass.png"

    val Fixture = NextPassTileDisplayModel(
        kind = NextPassTileKind.ActivePass,
        header = "Watch4Sat",
        title = "ISS (ZARYA)",
        countdown = "5 min",
        meta = "LOS 12:34 · Max 67°",
        ctaLabel = "Radar",
        tone = NextPassTileTone.Primary,
        showProgress = true,
        progress = 0.58f,
        countdownTargetMillis = null,
        progressStartMillis = null,
        progressEndMillis = null,
        nextTransitionMillis = null,
        launchAction = TileLaunchAction(TileLaunchDestination.Radar),
        accessibilityDescription =
            "ISS (ZARYA), active pass, 5 minutes to LOS at 12:34, " +
                "maximum elevation 67 degrees, opens Radar"
    )

    fun tile(
        context: Context,
        deviceConfiguration: DeviceParametersBuilders.DeviceParameters
    ): TileBuilders.Tile {
        return TilePreviewHelper.singleTimelineEntryTileBuilder(
            layout(
                context = context,
                deviceConfiguration = deviceConfiguration
            )
        ).build()
    }

    fun layout(
        context: Context,
        deviceConfiguration: DeviceParametersBuilders.DeviceParameters
    ): LayoutElementBuilders.LayoutElement {
        return NextPassTileLayoutPolicy.layout(
            context = context,
            deviceConfiguration = deviceConfiguration,
            model = Fixture,
            tileClick = ModifiersBuilders.Clickable.Builder()
                .setId("next_pass_tile_preview")
                .build(),
            allowDynamicTheme = AllowDynamicTheme
        )
    }
}
