package com.ayan.ritual.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.ayan.ritual.R
import com.ayan.ritual.render.Palette

val Cream = Color(Palette.CREAM)
val Paper = Color(Palette.PAPER)
val Ink = Color(Palette.INK)
val InkSoft = Color(0x8F12120F)
val InkFaint = Color(0x3312120F)
val Lime = Color(0xFFC9F73F)
val Red = Color(0xFFE5331C)

val Archivo = FontFamily(
    Font(R.font.archivo_medium, FontWeight.Medium),
    Font(R.font.archivo_semibold, FontWeight.SemiBold),
    Font(R.font.archivo_extrabold, FontWeight.ExtraBold)
)

/** The statement type: set tight, always ExtraBold. */
val Display = TextStyle(
    fontFamily = Archivo,
    fontWeight = FontWeight.ExtraBold,
    fontSize = 40.sp,
    lineHeight = 41.sp,
    letterSpacing = (-1.2).sp,
    color = Ink
)

/** Small caps used for every label and category. */
val Caps = TextStyle(
    fontFamily = Archivo,
    fontWeight = FontWeight.SemiBold,
    fontSize = 10.sp,
    letterSpacing = 1.1.sp,
    color = InkSoft
)

val Body = TextStyle(
    fontFamily = Archivo,
    fontWeight = FontWeight.Medium,
    fontSize = 14.sp,
    lineHeight = 21.sp,
    color = InkSoft
)

@Composable
fun RitualTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Ink,
            onPrimary = Paper,
            background = Cream,
            onBackground = Ink,
            surface = Paper,
            onSurface = Ink,
            outline = InkFaint
        ),
        typography = Typography(),
        content = content
    )
}
