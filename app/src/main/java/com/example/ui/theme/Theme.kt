package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val RustCraftColorScheme = darkColorScheme(
    primary = EmeraldPrimary,
    onPrimary = OnEmerald,
    primaryContainer = EmeraldContainer,
    onPrimaryContainer = TextPrimary,
    secondary = RustOrange,
    onSecondary = OnRust,
    secondaryContainer = RustContainer,
    onSecondaryContainer = TextPrimary,
    tertiary = DiamondCyan,
    tertiaryContainer = DiamondContainer,
    onTertiaryContainer = TextPrimary,
    background = ObsidianBg,
    onBackground = TextPrimary,
    surface = ObsidianSurface,
    onSurface = TextPrimary,
    surfaceVariant = ObsidianSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    surfaceContainerHigh = ObsidianSurfaceHigh,
    outline = ObsidianBorder,
    error = CrimsonRed,
)

val RustCraftShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun RustCraftTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = RustCraftColorScheme,
        typography = RustCraftTypography,
        shapes = RustCraftShapes,
        content = content,
    )
}
