package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val AmoledColorScheme = darkColorScheme(
    primary = ElectricVolt,
    onPrimary = OnElectricVolt,
    primaryContainer = VoltContainer,
    onPrimaryContainer = ElectricVolt,
    secondary = GlacialCyan,
    onSecondary = AmoledBlack,
    secondaryContainer = CyanContainer,
    onSecondaryContainer = GlacialCyan,
    tertiary = WarmAmber,
    onTertiary = AmoledBlack,
    tertiaryContainer = AmberContainer,
    onTertiaryContainer = WarmAmber,
    background = AmoledBlack,
    onBackground = TextPrimary,
    surface = ObsidianSurface,
    onSurface = TextPrimary,
    surfaceVariant = CarbonSurface,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLow = ObsidianSurface,
    surfaceContainer = CarbonSurface,
    surfaceContainerHigh = ElevatedCarbon,
    outline = HairlineBorder,
    outlineVariant = HairlineBorder,
    error = CrimsonAlert,
    onError = AmoledBlack,
    errorContainer = CrimsonContainer,
    onErrorContainer = CrimsonAlert
)

/**
 * Default AMOLED black theme for maximum OLED power savings and high-contrast gym readability.
 */
@Composable
fun KinetixTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AmoledColorScheme,
        typography = KinetixTypography,
        content = content
    )
}
