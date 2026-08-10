package com.xianming.watch4sat.wear.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.CurvedTextStyle
import androidx.wear.compose.material3.ConfirmationDialogDefaults
import androidx.wear.compose.material3.TimeTextDefaults
import androidx.wear.compose.material3.Typography
import com.xianming.watch4sat.R

private val GoogleSansFlexDefaultOpticalSize = 18.sp

private data class GoogleSansFlexTokenConfig(
    val axisWeight: Int,
    val axisWidth: Float
)

private object GoogleSansFlexToken {
    val DisplayLarge = GoogleSansFlexTokenConfig(axisWeight = 500, axisWidth = 105f)
    val DisplayMedium = GoogleSansFlexTokenConfig(axisWeight = 520, axisWidth = 105f)
    val DisplaySmall = GoogleSansFlexTokenConfig(axisWeight = 550, axisWidth = 105f)
    val TitleLarge = GoogleSansFlexTokenConfig(axisWeight = 500, axisWidth = 100f)
    val TitleMedium = GoogleSansFlexTokenConfig(axisWeight = 550, axisWidth = 100f)
    val TitleSmall = GoogleSansFlexTokenConfig(axisWeight = 550, axisWidth = 100f)
    val LabelLarge = GoogleSansFlexTokenConfig(axisWeight = 500, axisWidth = 100f)
    val LabelMedium = GoogleSansFlexTokenConfig(axisWeight = 500, axisWidth = 100f)
    val LabelSmall = GoogleSansFlexTokenConfig(axisWeight = 500, axisWidth = 100f)
    val BodyLarge = GoogleSansFlexTokenConfig(axisWeight = 450, axisWidth = 100f)
    val BodyMedium = GoogleSansFlexTokenConfig(axisWeight = 450, axisWidth = 100f)
    val BodySmall = GoogleSansFlexTokenConfig(axisWeight = 500, axisWidth = 100f)
    val BodyExtraSmall = GoogleSansFlexTokenConfig(axisWeight = 500, axisWidth = 100f)
    val NumeralExtraLarge = GoogleSansFlexTokenConfig(axisWeight = 560, axisWidth = 105f)
    val NumeralLarge = GoogleSansFlexTokenConfig(axisWeight = 580, axisWidth = 105f)
    val NumeralMedium = GoogleSansFlexTokenConfig(axisWeight = 580, axisWidth = 100f)
    val NumeralSmall = GoogleSansFlexTokenConfig(axisWeight = 550, axisWidth = 100f)
    val NumeralExtraSmall = GoogleSansFlexTokenConfig(axisWeight = 550, axisWidth = 100f)
    val ArcLarge = GoogleSansFlexTokenConfig(axisWeight = 599, axisWidth = 100f)
    val ArcMedium = GoogleSansFlexTokenConfig(axisWeight = 599, axisWidth = 100f)
    val ArcSmall = GoogleSansFlexTokenConfig(axisWeight = 560, axisWidth = 100f)
}

@OptIn(ExperimentalTextApi::class)
private val GoogleSansFlexRoundness = FontVariation.Setting("ROND", 100.0f)

@OptIn(ExperimentalTextApi::class)
private fun googleSansFlexVariationSettings(
    axisWeight: Int,
    axisWidth: Float,
    opticalSize: TextUnit = GoogleSansFlexDefaultOpticalSize
): FontVariation.Settings {
    return FontVariation.Settings(
        FontVariation.grade(0),
        FontVariation.weight(axisWeight),
        FontVariation.slant(0f),
        FontVariation.width(axisWidth),
        FontVariation.opticalSizing(opticalSize),
        GoogleSansFlexRoundness
    )
}

@OptIn(ExperimentalTextApi::class)
private fun googleSansFlexFont(
    weight: FontWeight,
    axisWeight: Int,
    axisWidth: Float,
    opticalSize: TextUnit = GoogleSansFlexDefaultOpticalSize
): Font {
    return Font(
        resId = R.font.google_sans_flex_variable,
        weight = weight,
        variationSettings = googleSansFlexVariationSettings(
            axisWeight = axisWeight,
            axisWidth = axisWidth,
            opticalSize = opticalSize
        )
    )
}

@OptIn(ExperimentalTextApi::class)
private fun googleSansFlexFontFamily(
    token: GoogleSansFlexTokenConfig = GoogleSansFlexToken.BodyMedium,
    composeWeight: FontWeight = FontWeight.Normal,
    opticalSize: TextUnit = GoogleSansFlexDefaultOpticalSize
): FontFamily {
    return FontFamily(
        googleSansFlexFont(
            weight = composeWeight,
            axisWeight = token.axisWeight,
            axisWidth = token.axisWidth,
            opticalSize = opticalSize
        )
    )
}

@OptIn(ExperimentalTextApi::class)
val GoogleSansFlexFontFamily = googleSansFlexFontFamily()

private fun TextStyle.withGoogleSansFlex(token: GoogleSansFlexTokenConfig): TextStyle {
    val weight = fontWeight ?: FontWeight.Normal
    return copy(
        fontFamily = googleSansFlexFontFamily(token, weight, fontSize.googleSansFlexOpticalSize())
    )
}

private fun CurvedTextStyle.withGoogleSansFlex(token: GoogleSansFlexTokenConfig): CurvedTextStyle {
    val weight = fontWeight ?: FontWeight.Normal
    return copy(
        fontFamily = googleSansFlexFontFamily(token, weight, fontSize.googleSansFlexOpticalSize())
    )
}

@Composable
fun googleSansFlexTimeTextStyle(color: Color = Color.Unspecified): CurvedTextStyle {
    return TimeTextDefaults.timeTextStyle(color = color).withGoogleSansFlex(GoogleSansFlexToken.ArcMedium)
}

@Composable
fun googleSansFlexConfirmationCurvedTextStyle(): CurvedTextStyle {
    return ConfirmationDialogDefaults.curvedTextStyle.withGoogleSansFlex(GoogleSansFlexToken.ArcLarge)
}

val WatchTypography = Typography().let { defaults ->
    Typography(
        displayLarge = defaults.displayLarge.withGoogleSansFlex(GoogleSansFlexToken.DisplayLarge),
        displayMedium = defaults.displayMedium.withGoogleSansFlex(GoogleSansFlexToken.DisplayMedium),
        displaySmall = defaults.displaySmall.withGoogleSansFlex(GoogleSansFlexToken.DisplaySmall),
        titleLarge = defaults.titleLarge.withGoogleSansFlex(GoogleSansFlexToken.TitleLarge),
        titleMedium = defaults.titleMedium.withGoogleSansFlex(GoogleSansFlexToken.TitleMedium),
        titleSmall = defaults.titleSmall.withGoogleSansFlex(GoogleSansFlexToken.TitleSmall),
        labelLarge = defaults.labelLarge.withGoogleSansFlex(GoogleSansFlexToken.LabelLarge),
        labelMedium = defaults.labelMedium.withGoogleSansFlex(GoogleSansFlexToken.LabelMedium),
        labelSmall = defaults.labelSmall.withGoogleSansFlex(GoogleSansFlexToken.LabelSmall),
        bodyLarge = defaults.bodyLarge.withGoogleSansFlex(GoogleSansFlexToken.BodyLarge),
        bodyMedium = defaults.bodyMedium.withGoogleSansFlex(GoogleSansFlexToken.BodyMedium),
        bodySmall = defaults.bodySmall.withGoogleSansFlex(GoogleSansFlexToken.BodySmall),
        bodyExtraSmall = defaults.bodyExtraSmall.withGoogleSansFlex(GoogleSansFlexToken.BodyExtraSmall),
        numeralExtraLarge = defaults.numeralExtraLarge.withGoogleSansFlex(GoogleSansFlexToken.NumeralExtraLarge),
        numeralLarge = defaults.numeralLarge.withGoogleSansFlex(GoogleSansFlexToken.NumeralLarge),
        numeralMedium = defaults.numeralMedium.withGoogleSansFlex(GoogleSansFlexToken.NumeralMedium),
        numeralSmall = defaults.numeralSmall.withGoogleSansFlex(GoogleSansFlexToken.NumeralSmall),
        numeralExtraSmall = defaults.numeralExtraSmall.withGoogleSansFlex(GoogleSansFlexToken.NumeralExtraSmall),
        arcLarge = defaults.arcLarge.withGoogleSansFlex(GoogleSansFlexToken.ArcLarge),
        arcMedium = defaults.arcMedium.withGoogleSansFlex(GoogleSansFlexToken.ArcMedium),
        arcSmall = defaults.arcSmall.withGoogleSansFlex(GoogleSansFlexToken.ArcSmall)
    )
}

private fun TextUnit.googleSansFlexOpticalSize(): TextUnit {
    return if (isSp) this else GoogleSansFlexDefaultOpticalSize
}
