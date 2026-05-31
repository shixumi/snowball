package com.snowball.ui.theme

import androidx.compose.ui.graphics.Color

object SnowColors {
    // Surfaces — deep, slightly blue night for the "Momentum" identity.
    val Night = Color(0xFF0A0E16)
    val NightElev = Color(0xFF111A2B)
    val CardElev = Color(0xFF16223A)
    val Line = Color(0x1AFFFFFF)
    val LineStrong = Color(0x2EFFFFFF)
    val TopHighlight = Color(0x26FFFFFF) // 1px glassy top-edge highlight

    // Text
    val Frost = Color(0xFFF3F6FA)
    val FrostMute = Color(0xFFA6B2C4)
    val FrostDim = Color(0xFF76839A)
    val FrostDeep = Color(0xFF3A4356)

    // Accents
    val Ice = Color(0xFF5B8DEF)        // primary "Velocity" — interactive / primary
    val IceSoft = Color(0x335B8DEF)
    val Charge = Color(0xFF6FE3CE)     // momentum / streak / cleared
    val ChargeSoft = Color(0x2E6FE3CE)
    val Champagne = Color(0xFFE8C68A)
    val Ember = Color(0xFFE07856)      // short / overdue
    val Green = Color(0xFF8FD9B2)      // mark-paid swipe background
}
