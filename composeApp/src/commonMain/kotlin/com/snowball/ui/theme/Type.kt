package com.snowball.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.snowball.resources.Res
import com.snowball.resources.SpaceGrotesk_Variable
import com.snowball.resources.Inter_Variable
import org.jetbrains.compose.resources.Font

// Momentum identity: Space Grotesk (kinetic display + numbers) + Inter (neutral body, scannable lists).
@Composable
fun spaceGrotesk(): FontFamily = FontFamily(Font(Res.font.SpaceGrotesk_Variable))

@Composable
fun inter(): FontFamily = FontFamily(Font(Res.font.Inter_Variable))

@Composable
fun snowballTypography(): Typography {
    val display = spaceGrotesk()
    val body = inter()
    return Typography(
        // Confident, capped hero numerals (Momentum favours presence over thinness).
        displayLarge = TextStyle(fontFamily = display, fontSize = 64.sp, fontWeight = FontWeight.W600),
        displayMedium = TextStyle(fontFamily = display, fontSize = 46.sp, fontWeight = FontWeight.W600),
        displaySmall = TextStyle(fontFamily = display, fontSize = 30.sp, fontWeight = FontWeight.W600),
        headlineLarge = TextStyle(fontFamily = display, fontSize = 26.sp, fontWeight = FontWeight.W600),
        headlineMedium = TextStyle(fontFamily = display, fontSize = 22.sp, fontWeight = FontWeight.W600),
        headlineSmall = TextStyle(fontFamily = display, fontSize = 18.sp, fontWeight = FontWeight.W600),
        titleLarge = TextStyle(fontFamily = body, fontSize = 16.sp, fontWeight = FontWeight.Medium),
        titleMedium = TextStyle(fontFamily = body, fontSize = 14.sp, fontWeight = FontWeight.Medium),
        bodyLarge = TextStyle(fontFamily = body, fontSize = 16.sp, fontWeight = FontWeight.Normal),
        bodyMedium = TextStyle(fontFamily = body, fontSize = 14.sp, fontWeight = FontWeight.Normal),
        bodySmall = TextStyle(fontFamily = body, fontSize = 12.sp, fontWeight = FontWeight.Normal),
        labelLarge = TextStyle(fontFamily = body, fontSize = 12.sp, fontWeight = FontWeight.Medium),
        labelMedium = TextStyle(fontFamily = body, fontSize = 11.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.0.sp),
        labelSmall = TextStyle(fontFamily = body, fontSize = 10.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.5.sp),
    )
}
