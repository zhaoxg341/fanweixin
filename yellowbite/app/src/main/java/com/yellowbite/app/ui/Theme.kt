package com.yellowbite.app.ui

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Yellow = Color(0xFFFFC928)
val Ink = Color(0xFF242824)
val Muted = Color(0xFF7D8079)
val Paper = Color(0xFFF6F7F3)
val Green = Color(0xFF4D6840)
val Orange = Color(0xFFE57433)

private val scheme = lightColorScheme(primary = Yellow, onPrimary = Ink, secondary = Green,
    background = Paper, onBackground = Ink, surface = Color.White, onSurface = Ink,
    surfaceVariant = Color(0xFFF0F1EB), onSurfaceVariant = Muted, outline = Color(0xFFDDDFD6))

@Composable
fun DemoTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme.copy(outline = Color(0xFFDDDFD6)),
        typography = Typography(
            headlineLarge = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold),
            headlineMedium = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold),
            titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold),
            titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold),
            bodyLarge = TextStyle(fontSize = 15.sp), bodyMedium = TextStyle(fontSize = 13.sp),
            labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        ), content = content)
}

