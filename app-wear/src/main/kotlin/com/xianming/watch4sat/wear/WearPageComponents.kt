package com.xianming.watch4sat.wear

import androidx.compose.foundation.background
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.TrackChanges
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalScrollCaptureInProgress
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnItemScope
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnState
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.CardDefaults
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeaderDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.ScrollIndicator
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.TransformationSpec
import androidx.wear.compose.material3.lazy.ResponsiveTransformationSpec
import androidx.wear.compose.material3.lazy.TransformationVariableSpec
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import com.xianming.watch4sat.wear.state.EdgeButtonContent
import com.xianming.watch4sat.wear.state.EdgeButtonContentPolicy
import com.xianming.watch4sat.wear.state.EdgeButtonContentType
import com.xianming.watch4sat.wear.state.EdgeButtonIcon
import com.xianming.watch4sat.wear.state.RoundListSurface
import com.xianming.watch4sat.wear.state.RoundListTransformationPolicy
import com.xianming.watch4sat.wear.theme.LocalWatchThemeColors

private val LocalRoundListTransformationSpec = staticCompositionLocalOf<TransformationSpec?> { null }
private val LocalTimeTextVisibilityReporter = staticCompositionLocalOf<(Boolean) -> Unit> { {} }

@Composable
fun AppTimeTextVisibilityProvider(
    onVisibilityChanged: (Boolean) -> Unit,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalTimeTextVisibilityReporter provides onVisibilityChanged) {
        content()
    }
}

@Composable
fun ReportTimeTextVisibility(visible: Boolean) {
    val reporter = LocalTimeTextVisibilityReporter.current
    SideEffect {
        reporter(visible)
    }
}

@Composable
fun RoundListTransformationProvider(content: @Composable () -> Unit) {
    val transformationSpec = rememberTransformationSpec(
        ResponsiveTransformationSpec.smallScreen(
            contentAlpha = TransformationVariableSpec(
                topValue = RoundListTransformationPolicy.edgeContentAlpha,
                targetValue = 1f,
                bottomValue = RoundListTransformationPolicy.edgeContentAlpha
            ),
            containerAlpha = TransformationVariableSpec(
                topValue = RoundListTransformationPolicy.edgeContainerAlpha,
                targetValue = 1f,
                bottomValue = RoundListTransformationPolicy.edgeContainerAlpha
            ),
            scale = TransformationVariableSpec(
                topValue = RoundListTransformationPolicy.edgeScaleTarget,
                targetValue = 1f,
                bottomValue = RoundListTransformationPolicy.edgeScaleTarget
            )
        ),
        ResponsiveTransformationSpec.largeScreen(
            contentAlpha = TransformationVariableSpec(
                topValue = RoundListTransformationPolicy.edgeContentAlpha,
                targetValue = 1f,
                bottomValue = RoundListTransformationPolicy.edgeContentAlpha
            ),
            containerAlpha = TransformationVariableSpec(
                topValue = RoundListTransformationPolicy.edgeContainerAlpha,
                targetValue = 1f,
                bottomValue = RoundListTransformationPolicy.edgeContainerAlpha
            ),
            scale = TransformationVariableSpec(
                topValue = RoundListTransformationPolicy.edgeScaleTarget,
                targetValue = 1f,
                bottomValue = RoundListTransformationPolicy.edgeScaleTarget
            )
        )
    )
    CompositionLocalProvider(LocalRoundListTransformationSpec provides transformationSpec) {
        content()
    }
}

@Composable
fun Modifier.roundListTransformedHeight(
    itemScope: TransformingLazyColumnItemScope?,
    surface: RoundListSurface
): Modifier {
    val transformationSpec = LocalRoundListTransformationSpec.current
    return if (
        itemScope != null &&
        transformationSpec != null &&
        RoundListTransformationPolicy.appliesTo(surface)
    ) {
        itemScope.run {
            when (surface) {
                RoundListSurface.STANDARD_CARD -> minimumVerticalContentPadding(
                    CardDefaults.minimumVerticalListContentPadding
                )
                RoundListSurface.STANDARD_BUTTON -> minimumVerticalContentPadding(
                    ButtonDefaults.minimumVerticalListContentPadding
                )
                // Wear M3 1.6.2 has no separate list-padding token for these
                // button-family controls.
                RoundListSurface.SWITCH_BUTTON,
                RoundListSurface.RADIO_BUTTON,
                RoundListSurface.SPLIT_CHECKBOX_BUTTON -> minimumVerticalContentPadding(
                    ButtonDefaults.minimumVerticalListContentPadding
                )
                RoundListSurface.LIST_HEADER -> minimumVerticalContentPadding(
                    ListHeaderDefaults.minimumTopListContentPadding,
                    ListHeaderDefaults.minimumBottomListContentPadding
                )
                else -> this@roundListTransformedHeight
            }.transformedHeight(this, transformationSpec)
        }
    } else {
        this
    }
}

@Composable
fun roundListSurfaceTransformation(
    itemScope: TransformingLazyColumnItemScope?,
    surface: RoundListSurface
): SurfaceTransformation? {
    val transformationSpec = LocalRoundListTransformationSpec.current
    return if (
        itemScope != null &&
        transformationSpec != null &&
        RoundListTransformationPolicy.appliesTo(surface)
    ) {
        itemScope.run { SurfaceTransformation(transformationSpec) }
    } else {
        null
    }
}

@Composable
fun ThemedEdgeButton(
    label: String,
    content: EdgeButtonContent = EdgeButtonContent.Text,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val colors = LocalWatchThemeColors.current
    EdgeButton(
        onClick = onClick,
        modifier = modifier
            .semantics {
                contentDescription = label
            },
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.primary,
            contentColor = colors.onPrimary,
            disabledContainerColor = colors.surfaceVariant.copy(alpha = 0.62f),
            disabledContentColor = colors.mutedText.copy(alpha = 0.64f)
        )
    ) {
        when (EdgeButtonContentPolicy.contentType(content)) {
            EdgeButtonContentType.Icon -> {
                Icon(
                    imageVector = when (EdgeButtonContentPolicy.icon(content)) {
                        EdgeButtonIcon.DeleteSweep -> Icons.Rounded.DeleteSweep
                        EdgeButtonIcon.TrackChanges -> Icons.Rounded.TrackChanges
                        EdgeButtonIcon.Check,
                        null -> Icons.Rounded.Check
                    },
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.IconSize)
                )
            }
            EdgeButtonContentType.Text -> {
                Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
            }
        }
    }
}

@Composable
fun Modifier.edgeButtonScrollable(state: LazyListState): Modifier {
    return scrollable(
        state = state,
        orientation = Orientation.Vertical,
        reverseDirection = true,
        overscrollEffect = rememberOverscrollEffect()
    )
}

@Composable
fun ThemedBottomActionButton(
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val colors = LocalWatchThemeColors.current
    Button(
        onClick = onClick,
        modifier = modifier
            .height(WatchUiMetrics.ActionButtonHeight)
            .semantics { contentDescription = label },
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.primary,
            contentColor = colors.onPrimary,
            disabledContainerColor = colors.surfaceVariant.copy(alpha = 0.62f),
            disabledContentColor = colors.mutedText.copy(alpha = 0.64f)
        )
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1)
    }
}

@Composable
fun WearScrollIndicator(state: TransformingLazyColumnState) {
    if (!LocalScrollCaptureInProgress.current) {
        ScrollIndicator(state = state)
    }
}

@Composable
fun WearScrollIndicator(state: ScrollState) {
    if (!LocalScrollCaptureInProgress.current) {
        ScrollIndicator(state = state)
    }
}

@Composable
fun RoundListPage(
    title: String,
    titleKey: Any = "round_list_title_$title",
    edgeButton: (@Composable () -> Unit)? = null,
    overlay: (@Composable BoxScope.(TransformingLazyColumnState) -> Unit)? = null,
    bottomSpacer: Dp = 10.dp,
    content: androidx.wear.compose.foundation.lazy.TransformingLazyColumnScope.() -> Unit
) {
    val listState = rememberTransformingLazyColumnState()
    val showTimeText by remember(listState) {
        derivedStateOf { !listState.canScrollBackward }
    }
    ReportTimeTextVisibility(showTimeText)

    @Composable
    fun PageContent(contentPadding: PaddingValues) {
        RoundListTransformationProvider {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val horizontalPadding = WatchUiMetrics.roundListHorizontalPadding(maxWidth)
                TransformingLazyColumn(
                    state = listState,
                    contentPadding = contentPadding,
                    rotaryScrollableBehavior = RotaryScrollableDefaults.behavior(scrollableState = listState),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = horizontalPadding)
                ) {
                    item(key = titleKey) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    content()
                    item { androidx.compose.foundation.layout.Spacer(Modifier.height(bottomSpacer)) }
                }
            }
        }
    }

    @Composable
    fun ScaffoldContent(contentPadding: PaddingValues) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            PageContent(contentPadding)
            overlay?.invoke(this, listState)
        }
    }

    if (edgeButton == null) {
        ScreenScaffold(
            scrollState = listState,
            scrollIndicator = { WearScrollIndicator(state = listState) }
        ) { contentPadding: PaddingValues ->
            ScaffoldContent(contentPadding)
        }
    } else {
        ScreenScaffold(
            scrollState = listState,
            edgeButton = { edgeButton.invoke() },
            scrollIndicator = { WearScrollIndicator(state = listState) }
        ) { contentPadding: PaddingValues ->
            ScaffoldContent(contentPadding)
        }
    }
}

@Composable
fun RoundVisualPage(
    title: String,
    scrollable: Boolean = false,
    itemSpacing: Dp = 4.dp,
    content: @Composable () -> Unit
) {
    val scrollState = rememberScrollState()
    val colors = LocalWatchThemeColors.current
    val showTimeText by remember(scrollable, scrollState) {
        derivedStateOf {
            !scrollable || scrollState.value == 0
        }
    }
    ReportTimeTextVisibility(showTimeText)

    @Composable
    fun PageContent(contentPadding: PaddingValues) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(contentPadding)
        ) {
            val horizontalPadding = WatchUiMetrics.roundListHorizontalPadding(maxWidth)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (scrollable) Modifier.verticalScroll(scrollState) else Modifier)
                    .padding(horizontal = horizontalPadding)
                    .padding(
                        top = WatchUiMetrics.VisualPageTopSafe,
                        bottom = WatchUiMetrics.VisualPageBottomSafe
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(itemSpacing)
            ) {
                Text(
                    text = title,
                    style = if (title.length > 16) {
                        MaterialTheme.typography.titleMedium
                    } else {
                        MaterialTheme.typography.titleLarge
                    },
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                content()
            }
        }
    }

    if (scrollable) {
        ScreenScaffold(
            scrollState = scrollState,
            scrollIndicator = { WearScrollIndicator(state = scrollState) }
        ) { contentPadding ->
            PageContent(contentPadding)
        }
    } else {
        ScreenScaffold { contentPadding ->
            PageContent(contentPadding)
        }
    }
}

@Composable
fun MainVisualPage(
    title: String,
    scrollable: Boolean = false,
    content: @Composable () -> Unit
) {
    RoundVisualPage(
        title = title,
        scrollable = scrollable,
        itemSpacing = 4.dp,
        content = content
    )
}

@Composable
fun RoundEdgeActionVisualPage(
    edgeButtonLabel: String,
    edgeButtonContent: EdgeButtonContent = EdgeButtonContent.Text,
    onEdgeButtonClick: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val listState = rememberLazyListState()
    ReportTimeTextVisibility(true)
    ScreenScaffold(
        scrollState = listState,
        edgeButton = {
            ThemedEdgeButton(
                label = edgeButtonLabel,
                content = edgeButtonContent,
                modifier = Modifier.edgeButtonScrollable(listState),
                onClick = onEdgeButtonClick
            )
        },
        scrollIndicator = {}
    ) { contentPadding ->
        LazyColumn(
            state = listState,
            userScrollEnabled = false,
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentPadding = contentPadding
        ) {
            item {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillParentMaxSize()
                        .background(Color.Black)
                ) {
                    val horizontalPadding = WatchUiMetrics.roundListHorizontalPadding(maxWidth)
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = horizontalPadding)
                            .padding(
                                top = WatchUiMetrics.VisualPageTopSafe,
                                bottom = WatchUiMetrics.VisualPageBottomSafe
                            ),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        content = content
                    )
                }
            }
        }
    }
}

@Composable
fun CompactPickerPage(
    title: String,
    value: String,
    showValue: Boolean = true,
    helper: String,
    applyLabel: String,
    onApply: () -> Unit,
    pickerContent: @Composable () -> Unit
) {
    val colors = LocalWatchThemeColors.current
    RoundEdgeActionVisualPage(
        edgeButtonLabel = applyLabel,
        edgeButtonContent = EdgeButtonContent.Apply,
        onEdgeButtonClick = onApply
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        if (showValue) {
            Text(
                text = value,
                style = MaterialTheme.typography.displayMedium,
                color = colors.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = WatchUiMetrics.EdgeActionContentCenterOffset),
            contentAlignment = Alignment.Center
        ) {
            pickerContent()
        }
    }
}

@Composable
fun PeerActionButton(
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentDescription: String = label,
    onClick: () -> Unit
) {
    val colors = LocalWatchThemeColors.current
    Button(
        modifier = modifier
            .height(WatchUiMetrics.ActionButtonHeight)
            .semantics { this.contentDescription = contentDescription },
        enabled = enabled,
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.surfaceVariant,
            contentColor = Color.White,
            disabledContainerColor = colors.surface.copy(alpha = 0.46f),
            disabledContentColor = colors.mutedText.copy(alpha = 0.56f)
        )
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1)
    }
}

@Composable
fun RoundAction(
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    contentDescription: String = label,
    itemScope: TransformingLazyColumnItemScope? = null,
    onClick: () -> Unit
) {
    val colors = LocalWatchThemeColors.current
    Button(
        modifier = modifier
            .height(WatchUiMetrics.ActionButtonHeight)
            .semantics { this.contentDescription = contentDescription }
            .roundListTransformedHeight(itemScope, RoundListSurface.STANDARD_BUTTON),
        enabled = enabled,
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.surfaceVariant,
            contentColor = Color.White,
            disabledContainerColor = colors.surface.copy(alpha = 0.46f),
            disabledContentColor = colors.mutedText.copy(alpha = 0.56f)
        ),
        transformation = roundListSurfaceTransformation(itemScope, RoundListSurface.STANDARD_BUTTON)
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
            }
            Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1)
        }
    }
}

@Composable
fun InfoCard(
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null,
    itemScope: TransformingLazyColumnItemScope? = null
) {
    val colors = LocalWatchThemeColors.current
    val modifier = Modifier
        .fillMaxWidth()
        .roundListTransformedHeight(itemScope, RoundListSurface.STANDARD_CARD)
    val transformation = roundListSurfaceTransformation(itemScope, RoundListSurface.STANDARD_CARD)
    if (onClick == null) {
        Card(
            modifier = modifier,
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(
                containerColor = colors.surface,
                contentColor = Color.White
            ),
            transformation = transformation
        ) {
            InfoCardContent(title, subtitle)
        }
    } else {
        Card(
            modifier = modifier,
            onClick = onClick,
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(
                containerColor = colors.surface,
                contentColor = Color.White
            ),
            transformation = transformation
        ) {
            InfoCardContent(title, subtitle)
        }
    }
}

@Composable
private fun InfoCardContent(title: String, subtitle: String) {
    Column(modifier = Modifier.padding(WatchUiMetrics.CardPadding), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall
        )
        Text(
            subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = LocalWatchThemeColors.current.mutedText
        )
    }
}

@Composable
fun StatusTextBlock(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            textAlign = TextAlign.Center,
            color = Color.White,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = LocalWatchThemeColors.current.mutedText,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
