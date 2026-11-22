package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val AppleFitnessDarkScheme = darkColorScheme(
    primary = KinetixTokens.Colors.Workout,
    onPrimary = KinetixTokens.Colors.OnWorkout,
    primaryContainer = KinetixTokens.Colors.WorkoutMutedBg,
    onPrimaryContainer = KinetixTokens.Colors.Workout,
    secondary = KinetixTokens.Colors.Hydration,
    onSecondary = KinetixTokens.Colors.OnHydration,
    secondaryContainer = KinetixTokens.Colors.HydrationMutedBg,
    onSecondaryContainer = KinetixTokens.Colors.Hydration,
    tertiary = KinetixTokens.Colors.Calories,
    onTertiary = KinetixTokens.Colors.TextPrimary,
    tertiaryContainer = KinetixTokens.Colors.CaloriesMutedBg,
    onTertiaryContainer = KinetixTokens.Colors.Calories,
    background = KinetixTokens.Colors.Background,
    onBackground = KinetixTokens.Colors.TextPrimary,
    surface = KinetixTokens.Colors.CardSurface,
    onSurface = KinetixTokens.Colors.TextPrimary,
    surfaceVariant = KinetixTokens.Colors.NestedSurface,
    onSurfaceVariant = KinetixTokens.Colors.TextSecondary,
    surfaceContainerLow = KinetixTokens.Colors.CardSurface,
    surfaceContainer = KinetixTokens.Colors.CardSurface,
    surfaceContainerHigh = KinetixTokens.Colors.NestedSurface,
    outline = KinetixTokens.Colors.Hairline,
    outlineVariant = KinetixTokens.Colors.Hairline,
    error = KinetixTokens.Colors.StatusLowFg,
    onError = KinetixTokens.Colors.TextPrimary,
    errorContainer = KinetixTokens.Colors.StatusLowBg,
    onErrorContainer = KinetixTokens.Colors.StatusLowFg
)

private val AppleFitnessShapes = Shapes(
    small = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(22.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(26.dp)
)

@Composable
fun KinetixTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AppleFitnessDarkScheme,
        typography = KinetixTypography,
        shapes = AppleFitnessShapes,
        content = content
    )
}
