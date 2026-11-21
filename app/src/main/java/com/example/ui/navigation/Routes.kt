package com.example.ui.navigation

import kotlinx.serialization.Serializable

sealed interface KinetixRoute {
    @Serializable
    data object Today : KinetixRoute

    @Serializable
    data object Workout : KinetixRoute

    @Serializable
    data object Nutrition : KinetixRoute

    @Serializable
    data object Insights : KinetixRoute

    @Serializable
    data object Settings : KinetixRoute
}
