package com.xianming.watch4sat.wear.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Text
import com.xianming.watch4sat.wear.theme.LocalWatchThemeColors
import kotlinx.coroutines.delay

@Composable
fun TransientMapBadge(
    label: String,
    textStyle: TextStyle,
    transient: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = LocalWatchThemeColors.current
    var visible by remember(label) { mutableStateOf(true) }

    LaunchedEffect(label, transient) {
        visible = true
        if (transient) {
            delay(MapBadgeVisibilityPolicy.TransientBadgeMillis)
            visible = false
        }
    }

    if (!visible) return

    Text(
        text = label,
        style = textStyle,
        color = Color.White,
        modifier = modifier
            .background(colors.surface.copy(alpha = 0.74f), RoundedCornerShape(8.dp))
            .padding(horizontal = 7.dp, vertical = 3.dp)
    )
}
