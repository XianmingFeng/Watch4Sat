package com.xianming.watch4sat.wear.radar

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.pager.HorizontalPager
import androidx.wear.compose.foundation.pager.PagerDefaults
import androidx.wear.compose.foundation.pager.rememberPagerState
import androidx.wear.compose.material3.AnimatedPage
import androidx.wear.compose.material3.HorizontalPageIndicator
import androidx.wear.compose.material3.HorizontalPagerScaffold
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.PagerScaffoldDefaults
import com.xianming.watch4sat.domain.model.SatellitePass
import com.xianming.watch4sat.wear.WatchUiMetrics
import com.xianming.watch4sat.wear.WatchUiState
import com.xianming.watch4sat.wear.state.PassCardInteractionReducer
import com.xianming.watch4sat.wear.state.RadarDetailOverlayPolicy
import com.xianming.watch4sat.wear.state.RadarDetailPage
import com.xianming.watch4sat.wear.theme.LocalWatchThemeColors

@Composable
fun RadarDetailPager(
    state: WatchUiState,
    orientation: RadarOrientationSnapshot,
    onClose: () -> Unit,
    onSelectRadarTransmitter: (String?) -> Unit,
    onSelectRadarPass: (SatellitePass) -> Unit,
    modifier: Modifier = Modifier
) {
    val pages = RadarDetailOverlayPolicy.pages
    val pagerState = rememberPagerState(
        initialPage = RadarDetailOverlayPolicy.defaultPageIndex,
        pageCount = { pages.size }
    )
    var expandedFuturePassKey by rememberSaveable { mutableStateOf<String?>(null) }
    val colors = LocalWatchThemeColors.current

    BackHandler(enabled = true) {
        val currentPage = pages.getOrNull(pagerState.currentPage)
        if (currentPage == RadarDetailPage.FuturePasses && expandedFuturePassKey != null) {
            expandedFuturePassKey = null
        } else {
            onClose()
        }
    }

    HorizontalPagerScaffold(
        modifier = modifier.fillMaxSize(),
        pagerState = pagerState,
        pageIndicator = {
            HorizontalPageIndicator(
                pagerState = pagerState,
                selectedColor = colors.primary,
                unselectedColor = colors.mutedText.copy(alpha = 0.48f),
                backgroundColor = Color.Transparent,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = WatchUiMetrics.SatellitePageIndicatorBottomPadding)
            )
        }
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(0.dp),
            beyondViewportPageCount = PagerDefaults.BeyondViewportPageCount,
            flingBehavior = PagerDefaults.snapFlingBehavior(
                state = pagerState,
                maxFlingPages = 1,
                snapPositionalThreshold = PagerScaffoldDefaults.HighSnapPositionalThreshold,
                snapAnimationSpec = MaterialTheme.motionScheme.defaultSpatialSpec()
            ),
            gestureInclusion = PagerDefaults.gestureInclusion(pagerState)
        ) { pageIndex ->
            AnimatedPage(pageIndex = pageIndex, pagerState = pagerState) {
                when (pages[pageIndex]) {
                    RadarDetailPage.PassTask -> RadarDetailPassTaskPage(
                        state = state,
                        orientation = orientation
                    )
                    RadarDetailPage.Radio -> RadarDetailRadioPage(
                        state = state,
                        onSelectRadarTransmitter = onSelectRadarTransmitter
                    )
                    RadarDetailPage.FuturePasses -> RadarDetailFuturePassesPage(
                        state = state,
                        expandedPassKey = expandedFuturePassKey,
                        onPassTap = { pass, passKey ->
                            val result = PassCardInteractionReducer.onCardTap(
                                expandedPassKey = expandedFuturePassKey,
                                tappedPassKey = passKey
                            )
                            expandedFuturePassKey = result.expandedPassKey
                            if (result.openRadar) {
                                onSelectRadarPass(pass)
                                onClose()
                            }
                        }
                    )
                    RadarDetailPage.Diagnostics -> RadarDetailDiagnosticsPage(
                        state = state,
                        orientation = orientation
                    )
                }
            }
        }
    }
}
