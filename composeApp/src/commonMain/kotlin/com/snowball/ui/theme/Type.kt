package com.snowball.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.snowball.resources.Res
import com.snowball.resources.Fraunces_Variable
import com.snowball.resources.DMSans_Variable
import org.jetbrains.compose.resources.Font

@Composable
fun fraunces(): FontFamily = FontFamily(Font(Res.font.Fraunces_Variable))

@Composable
fun dmSans(): FontFamily = FontFamily(Font(Res.font.DMSans_Variable))

@Composable
fun snowballTypography(): Typography {
    val display = fraunces()
    val body = dmSans()
    return Typography(
        displayLarge = TextStyle(fontFamily = display, fontSize = 96.sp, fontWeight = FontWeight.W300),
        displayMedium = TextStyle(fontFamily = display, fontSize = 64.sp, fontWeight = FontWeight.W400),
        displaySmall = TextStyle(fontFamily = display, fontSize = 32.sp, fontWeight = FontWeight.W400),
        headlineLarge = TextStyle(fontFamily = display, fontSize = 28.sp, fontWeight = FontWeight.W500),
        headlineMedium = TextStyle(fontFamily = display, fontSize = 22.sp, fontWeight = FontWeight.W500),
        headlineSmall = TextStyle(fontFamily = display, fontSize = 18.sp, fontWeight = FontWeight.W500),
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

val FraunsesItalic: FontStyle = FontStyle.Italic
