package com.example.mygymstatsapp.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.mygymstatsapp.R

/** Condensed industrial display face: numbers, titles, section headers. Never below 20sp. */
val BigShoulders = FontFamily(
    Font(R.font.big_shoulders_display_700, FontWeight.Bold),
    Font(R.font.big_shoulders_display_800, FontWeight.ExtraBold),
    Font(R.font.big_shoulders_display_900, FontWeight.Black),
)

/** Text face: everything readable at small sizes. */
val Inter = FontFamily(
    Font(R.font.inter_400, FontWeight.Normal),
    Font(R.font.inter_500, FontWeight.Medium),
    Font(R.font.inter_600, FontWeight.SemiBold),
    Font(R.font.inter_700, FontWeight.Bold),
)

// Tabular figures keep timers and counters from jittering when digits change.
private val Display = TextStyle(fontFamily = BigShoulders, fontFeatureSettings = "tnum")
private val Body = TextStyle(fontFamily = Inter)

val Typography = Typography(
    // Big numbers: timer, volume, PRs
    displayLarge = Display.copy(fontWeight = FontWeight.Black, fontSize = 76.sp, lineHeight = 76.sp),
    displayMedium = Display.copy(fontWeight = FontWeight.Black, fontSize = 54.sp, lineHeight = 56.sp),
    displaySmall = Display.copy(fontWeight = FontWeight.ExtraBold, fontSize = 40.sp, lineHeight = 44.sp),

    headlineLarge = Display.copy(fontWeight = FontWeight.ExtraBold, fontSize = 36.sp, lineHeight = 40.sp, letterSpacing = 0.3.sp),
    headlineMedium = Display.copy(fontWeight = FontWeight.ExtraBold, fontSize = 30.sp, lineHeight = 34.sp, letterSpacing = 0.3.sp),
    headlineSmall = Display.copy(fontWeight = FontWeight.ExtraBold, fontSize = 27.sp, lineHeight = 30.sp, letterSpacing = 0.3.sp),

    titleLarge = Display.copy(fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, lineHeight = 28.sp, letterSpacing = 0.5.sp),
    titleMedium = Body.copy(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp, letterSpacing = 0.1.sp),
    titleSmall = Body.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),

    bodyLarge = Body.copy(fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.1.sp),
    bodyMedium = Body.copy(fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
    bodySmall = Body.copy(fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.2.sp),

    labelLarge = Body.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.5.sp),
    labelMedium = Body.copy(fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp),
    labelSmall = Body.copy(fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp),
)
