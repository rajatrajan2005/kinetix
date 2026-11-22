package com.example.ui.theme

import android.content.Context
import android.provider.Settings
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

/**
 * Unified Apple Fitness / iOS OLED Design Token System for Kinetix.
 * Every color, spacing, radius, motion spec, and accessible helper lives here.
 */
object KinetixTokens {

    object Colors {
        // Pure OLED Canvas & Elevated Surfaces
        val Background = Color(0xFF000000)
        val CardSurface = Color(0xFF1C1C1E)
        val NestedSurface = Color(0xFF2C2C2E)
        val Hairline = Color(0x14FFFFFF) // 0.5dp @ ~8% white opacity
        val BlurredChrome = Color(0xD9161618) // Translucent iOS material chrome for nav & sheets
        val SheetHeaderTint = Color(0xE61C1C1E)

        // Text Hierarchy
        val TextPrimary = Color(0xFFFFFFFF)
        val TextSecondary = Color(0xFF8E8E93)
        val TextTertiary = Color(0xFF48484A)

        // Single Semantic Accent Per Meaning
        val Calories = Color(0xFFFA114F)       // Red-Pink (Move ring / Calories)
        val Workout = Color(0xFFA6FF00)        // Lime-Green (Exercise / Workout, used sparingly)
        val OnWorkout = Color(0xFF070A00)
        val Hydration = Color(0xFF1EEAEF)      // Cyan-Blue (Water / Steps / Stand)
        val OnHydration = Color(0xFF021114)

        // Distinct Macro Ring Colors
        val Protein = Color(0xFFA6FF00)
        val Carbs = Color(0xFF1EEAEF)
        val Fat = Color(0xFFFF9F0A)

        // Semantic Status Chips (high contrast)
        val StatusLowFg = Color(0xFFFF6961)
        val StatusLowBg = Color(0x2EFA114F)
        val StatusInRangeFg = Color(0xFF30D158)
        val StatusInRangeBg = Color(0x2630D158)
        val StatusHighFg = Color(0xFFFFB340)
        val StatusHighBg = Color(0x2BFF9F0A)

        // Subtle Tinted Containers
        val CaloriesMutedBg = Color(0x24FA114F)
        val WorkoutMutedBg = Color(0x22A6FF00)
        val HydrationMutedBg = Color(0x221EEAEF)
        val Cycle = Color(0xFFFF4D8D)
        val CycleMutedBg = Color(0x29FF4D8D)
    }

    object Spacing {
        val xs = 4.dp
        val sm = 8.dp
        val md = 12.dp
        val lg = 16.dp
        val xl = 20.dp
        val xxl = 24.dp

        val ScreenHorizontal = 18.dp
        val CardGap = 14.dp
        val CardPadding = 16.dp
        val NavBarHeight = 62.dp
        val NavBarMargin = 14.dp
        val NavBarClearance = 96.dp
    }

    object Radius {
        val Card = 22.dp
        val Nested = 14.dp
        val Control = 14.dp
        val Pill = 999.dp

        val CardShape = RoundedCornerShape(Card)
        val NestedShape = RoundedCornerShape(Nested)
        val ControlShape = RoundedCornerShape(Control)
        val PillShape = RoundedCornerShape(Pill)
        val SheetShape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)
    }

    object Motion {
        const val TAB_TRANSITION_MS = 200
        const val PRESS_SCALE = 0.97f

        fun <T> standardSpring(reduced: Boolean = false): FiniteAnimationSpec<T> =
            if (reduced) snap()
            else spring(
                dampingRatio = 0.85f,
                stiffness = Spring.StiffnessMediumLow // ~350-450ms smooth settle, subtle bounce
            )

        fun <T> ringSpring(reduced: Boolean = false): FiniteAnimationSpec<T> =
            if (reduced) snap()
            else spring(
                dampingRatio = 0.82f,
                stiffness = 280f
            )

        fun <T> pressSpring(reduced: Boolean = false): FiniteAnimationSpec<T> =
            if (reduced) snap()
            else spring(
                dampingRatio = 0.80f,
                stiffness = 620f
            )

        fun intOffsetSpring(reduced: Boolean = false): FiniteAnimationSpec<IntOffset> =
            if (reduced) snap()
            else spring(
                dampingRatio = 0.86f,
                stiffness = Spring.StiffnessMediumLow
            )

        fun <T> tabFadeTween(reduced: Boolean = false): FiniteAnimationSpec<T> =
            if (reduced) snap()
            else tween(durationMillis = TAB_TRANSITION_MS)
    }
}

fun isSystemReduceAnimationsEnabled(context: Context): Boolean {
    return runCatching {
        val scale = Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        )
        scale <= 0f
    }.getOrDefault(false)
}

@Composable
fun rememberReduceAnimations(): Boolean {
    val context = LocalContext.current
    return remember(context) { isSystemReduceAnimationsEnabled(context) }
}
