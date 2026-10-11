package com.dawaii.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

import androidx.compose.ui.text.font.Font
import com.dawaii.app.R

val TallSerifFamily = FontFamily(Font(R.font.bodoni))
val BirchTypography = FontFamily(Font(R.font.bodoni))
val BodoniPosterFamily = FontFamily(Font(R.font.bodoni))

val DawaiiTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = TallSerifFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        letterSpacing = 2.sp,
        color = BlushPinkPrimary
    ),
    headlineMedium = TextStyle(
        fontFamily = TallSerifFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        letterSpacing = 1.5.sp,
        color = BlushPinkPrimary
    ),
    titleLarge = TextStyle(
        fontFamily = TallSerifFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        letterSpacing = 1.2.sp,
        color = SoftWhite
    ),
    titleMedium = TextStyle(
        fontFamily = TallSerifFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        letterSpacing = 0.8.sp,
        color = BlushPinkPrimary
    ),
    bodyLarge = TextStyle(
        fontFamily = BodoniPosterFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        letterSpacing = 0.3.sp,
        color = SoftWhite
    ),
    bodyMedium = TextStyle(
        fontFamily = BodoniPosterFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        letterSpacing = 0.25.sp,
        color = MutedGreyPink
    ),
    bodySmall = TextStyle(
        fontFamily = BodoniPosterFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        letterSpacing = 0.2.sp,
        color = MutedGreyPink
    ),
    labelSmall = TextStyle(
        fontFamily = BodoniPosterFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        letterSpacing = 0.4.sp,
        color = MutedGreyPink
    )
)
