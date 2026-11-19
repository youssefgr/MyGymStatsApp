package com.example.mygymstatsapp.wear.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Typography
import com.example.mygymstatsapp.shared.WearMuscle
import com.example.mygymstatsapp.shared.WearSetType
import com.example.mygymstatsapp.wear.R

// Brand "Fonte" (docs/BRAND.md): the phone's dark palette, on a true-black watch background.
val Fonte = Color(0xFFFF5A1F)
val FonteDeep = Color(0xFF5C1E06)
val FonteGlow = Color(0xFFFFB494)
val Steel = Color(0xFF8FB3C9)
val Gold = Color(0xFFE8B04B)
val Plum = Color(0xFFA68BEB)
val Danger = Color(0xFFF0455A)
val Charcoal = Color(0xFF15120F)
val Soot = Color(0xFF1E1A16)
val Iron = Color(0xFF28231E)
val IronHigh = Color(0xFF342D27)
val Rivet = Color(0xFF4D433A)
val Chalk = Color(0xFFEDE6DA)
val Ash = Color(0xFFA39A8C)

private val FonteColors = ColorScheme(
    primary = Fonte,
    primaryDim = Color(0xFFC2410C),
    primaryContainer = FonteDeep,
    onPrimary = Charcoal,
    onPrimaryContainer = FonteGlow,
    secondary = Steel,
    secondaryDim = Color(0xFF3F6A85),
    secondaryContainer = IronHigh,
    onSecondary = Charcoal,
    onSecondaryContainer = Chalk,
    tertiary = Gold,
    tertiaryDim = Color(0xFF9A6B00),
    tertiaryContainer = IronHigh,
    onTertiary = Charcoal,
    onTertiaryContainer = Gold,
    surfaceContainerLow = Soot,
    surfaceContainer = Iron,
    surfaceContainerHigh = IronHigh,
    onSurface = Chalk,
    onSurfaceVariant = Ash,
    outline = Rivet,
    outlineVariant = IronHigh,
    background = Color.Black,
    onBackground = Chalk,
    error = Danger,
    errorDim = Color(0xFFC62839),
    onError = Charcoal,
)

/** Condensed industrial face: numbers and titles, never below 20sp. Tabular figures keep timers steady. */
val BigShoulders = FontFamily(
    Font(R.font.big_shoulders_display_800, FontWeight.ExtraBold),
    Font(R.font.big_shoulders_display_900, FontWeight.Black),
)

val Inter = FontFamily(
    Font(R.font.inter_400, FontWeight.Normal),
    Font(R.font.inter_500, FontWeight.Medium),
    Font(R.font.inter_600, FontWeight.SemiBold),
    Font(R.font.inter_700, FontWeight.Bold),
)

private fun TextStyle.display(weight: FontWeight = FontWeight.ExtraBold) =
    copy(fontFamily = BigShoulders, fontWeight = weight, fontFeatureSettings = "tnum")

private val FonteTypography: Typography = Typography(defaultFontFamily = Inter).let { base ->
    base.copy(
        displayLarge = base.displayLarge.display(FontWeight.Black),
        displayMedium = base.displayMedium.display(FontWeight.Black),
        displaySmall = base.displaySmall.display(),
        titleLarge = base.titleLarge.display(),
        numeralExtraLarge = base.numeralExtraLarge.display(FontWeight.Black),
        numeralLarge = base.numeralLarge.display(FontWeight.Black),
        numeralMedium = base.numeralMedium.display(FontWeight.Black),
        numeralSmall = base.numeralSmall.display(),
        numeralExtraSmall = base.numeralExtraSmall.display(),
    )
}

@Composable
fun FonteWearTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = FonteColors, typography = FonteTypography, content = content)
}

/** Same hues as the phone's MuscleGroupColors.kt. */
val WearMuscle.color: Color
    get() = when (this) {
        WearMuscle.CHEST -> Color(0xFFF2766B)
        WearMuscle.BACK -> Color(0xFF5FA8D3)
        WearMuscle.SHOULDERS -> Color(0xFFF2A65A)
        WearMuscle.BICEPS -> Color(0xFFA68BEB)
        WearMuscle.TRICEPS -> Color(0xFFD98AD6)
        WearMuscle.LEGS -> Color(0xFF7DC87A)
        WearMuscle.GLUTES -> Color(0xFFEF8FB2)
        WearMuscle.CALVES -> Color(0xFF4FC4A8)
        WearMuscle.ABS -> Color(0xFFE9C46A)
        WearMuscle.FOREARMS -> Color(0xFFB5CC6A)
        WearMuscle.CARDIO -> Color(0xFF6CCFE0)
    }

/** Warm-up steel, drop plum, failure danger; normal sets use the regular text color. */
val WearSetType.color: Color
    get() = when (this) {
        WearSetType.WARMUP -> Steel
        WearSetType.NORMAL -> Color.Unspecified
        WearSetType.DROP -> Plum
        WearSetType.FAILURE -> Danger
    }
