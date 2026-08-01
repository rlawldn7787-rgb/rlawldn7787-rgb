package com.woohaeng.board.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object Brand {
    val Navy = Color(0xFF0B1F3A)
    val NavyDeep = Color(0xFF061527)
    val Cyan = Color(0xFF19B7E6)
    val CyanSoft = Color(0x2419B7E6)
    val Ink = Color(0xFF122033)
    val Muted = Color(0xFF5D6D82)
    val Line = Color(0xFFD7E0EA)
    val Panel = Color(0xFFFFFFFF)
    val Bg = Color(0xFFEEF3F8)
    val BgTop = Color(0xFFF5F8FC)
    val Danger = Color(0xFFC0392B)
}

val SoftShape = RoundedCornerShape(14.dp)
val SoftShapeSm = RoundedCornerShape(10.dp)

private val WoohaengColors = lightColorScheme(
    primary = Brand.Navy,
    onPrimary = Color.White,
    secondary = Brand.Cyan,
    onSecondary = Brand.NavyDeep,
    background = Brand.Bg,
    onBackground = Brand.Ink,
    surface = Brand.Panel,
    onSurface = Brand.Ink,
    surfaceVariant = Color(0xFFF2F6FA),
    onSurfaceVariant = Brand.Muted,
    outline = Brand.Line,
    error = Brand.Danger
)

private val WoohaengTypography = Typography(
    headlineLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        letterSpacing = (-0.8).sp,
        color = Brand.Navy
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        letterSpacing = (-0.5).sp,
        color = Brand.Navy
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        letterSpacing = (-0.3).sp,
        color = Brand.Navy
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        color = Brand.Navy
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        color = Brand.Ink
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        color = Brand.Ink
    ),
    bodySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        color = Brand.Muted
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp
    )
)

@Composable
fun WoohaengTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = WoohaengColors,
        typography = WoohaengTypography,
        content = content
    )
}
