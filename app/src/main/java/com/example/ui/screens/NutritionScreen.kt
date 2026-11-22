package com.example.ui.screens

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.FoodItemCacheEntity
import com.example.data.local.entity.FoodLogEntity
import com.example.domain.model.MealSlot
import com.example.ui.components.AppleCollapsedNavBar
import com.example.ui.components.AppleLargeTitleHeader
import com.example.ui.components.BottomSheet
import com.example.ui.components.CalorieRingGauge
import com.example.ui.components.Card
import com.example.ui.components.EmptyState
import com.example.ui.components.MacroProgressRow
import com.example.ui.components.PrimaryButton
import com.example.ui.components.SegmentedControl
import com.example.ui.components.ShimmerListPlaceholder
import com.example.ui.components.StatChip
import com.example.ui.components.applePressScale
import com.example.ui.theme.KinetixTokens
import com.example.ui.viewmodel.NutritionUiState
import kotlin.math.roundToInt

private enum class NutritionTab(val label: String) {
    TODAY("Today"),
    SEARCH("Search"),
    MY_FOODS("My Foods")
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun NutritionScreen(
    state: NutritionUiState,
    onQueryChange: (String) -> Unit,
    onRefreshSearch: () -> Unit,
    onLogFood: (FoodItemCacheEntity, Double, MealSlot) -> Unit,
    onAddCustomMealTotals: (
        name: String,
        totalKcal: Double,
        totalProteinG: Double,
        totalCarbsG: Double,
        totalFatG: Double,
        mealSlot: MealSlot,
        alsoLog: Boolean
    ) -> Unit,
    onAddCustomFoodPer100g: (
        name: String,
        kcalPer100g: Double,
        proteinPer100g: Double,
        carbsPer100g: Double,
        fatPer100g: Double,
        gramsToLog: Double,
        mealSlot: MealSlot,
        alsoLog: Boolean
    ) -> Unit,
    onDeleteCustomFood: (String) -> Unit,
    onUpdateLog: (FoodLogEntity) -> Unit,
    onDeleteLog: (Long) -> Unit
) {
    val view = LocalView.current
    val listState = rememberLazyListState()
    val navInsetBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val bottomClearance = KinetixTokens.Spacing.NavBarClearance + navInsetBottom

    var activeTab by remember { mutableStateOf(NutritionTab.TODAY) }
    var selectedSlot by remember { mutableStateOf(MealSlot.LUNCH) }
    var foodToLog by remember { mutableStateOf<FoodItemCacheEntity?>(null) }
    var logToEdit by remember { mutableStateOf<FoodLogEntity?>(null) }
    var showCustomSheet by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KinetixTokens.Colors.Background),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp),
            contentPadding = PaddingValues(
                start = KinetixTokens.Spacing.ScreenHorizontal,
                top = 12.dp,
                end = KinetixTokens.Spacing.ScreenHorizontal,
                bottom = bottomClearance
            ),
            verticalArrangement = Arrangement.spacedBy(KinetixTokens.Spacing.CardGap)
        ) {
            item(key = "nutrition_header") {
                AppleLargeTitleHeader(
                    title = "Nutrition",
                    subtitle = "Fuel & Macros",
                    trailing = {
                        PrimaryButton(
                            text = "Quick Custom",
                            onClick = { showCustomSheet = true },
                            icon = Icons.Rounded.Add,
                            containerColor = KinetixTokens.Colors.Calories,
                            contentColor = KinetixTokens.Colors.TextPrimary,
                            modifier = Modifier.testTag("add_custom_food_btn")
                        )
                    }
                )
            }

            item(key = "ring_summary") {
                Card {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CalorieRingGauge(
                            consumedKcal = state.consumedKcal,
                            targetKcal = state.targets.targetKcal,
                            size = 120.dp,
                            accentColor = KinetixTokens.Colors.Calories
                        )
                        Spacer(Modifier.width(16.dp))
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            MacroProgressRow(
                                label = "Protein",
                                consumedG = state.consumedProteinG,
                                targetG = state.targets.proteinG,
                                color = KinetixTokens.Colors.Protein
                            )
                            MacroProgressRow(
                                label = "Carbs",
                                consumedG = state.consumedCarbsG,
                                targetG = state.targets.carbsG,
                                color = KinetixTokens.Colors.Carbs
                            )
                            MacroProgressRow(
                                label = "Fat",
                                consumedG = state.consumedFatG,
                                targetG = state.targets.fatG,
                                color = KinetixTokens.Colors.Fat
                            )
                        }
                    }
                }
            }

            item(key = "nutrition_segments") {
                SegmentedControl(
                    options = NutritionTab.entries,
                    selected = activeTab,
                    labelFor = { tab ->
                        when (tab) {
                            NutritionTab.TODAY -> if (state.todayLogs.isNotEmpty()) "${tab.label} (${state.todayLogs.size})" else tab.label
                            NutritionTab.SEARCH -> tab.label
                            NutritionTab.MY_FOODS -> if (state.customFoods.isNotEmpty()) "${tab.label} (${state.customFoods.size})" else tab.label
                        }
                    },
                    onSelect = { activeTab = it },
                    tagPrefix = "nutrition_tab"
                )
            }

            when (activeTab) {
                NutritionTab.TODAY -> {
                    item(key = "today_logs_header") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("consumed_food_top_section"),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Consumed Today",
                                style = MaterialTheme.typography.titleLarge,
                                color = KinetixTokens.Colors.TextPrimary
                            )
                            StatChip(
                                text = "${state.todayLogs.size} items · ${state.consumedKcal} kcal",
                                foregroundColor = KinetixTokens.Colors.Calories,
                                backgroundColor = KinetixTokens.Colors.CaloriesMutedBg
                            )
                        }
                    }

                    if (state.todayLogs.isEmpty()) {
                        item(key = "empty_logs") {
                            Card {
                                EmptyState(
                                    icon = Icons.Rounded.Restaurant,
                                    message = "No meals logged today yet.",
                                    accentColor = KinetixTokens.Colors.Calories,
                                    actionLabel = "Log food",
                                    onAction = { showCustomSheet = true }
                                )
                            }
                        }
                    } else {
                        items(state.todayLogs, key = { it.id }) { log ->
                            val isDirectTotal = log.foodKey.startsWith("custom_total_")
                            val portionLabel = if (isDirectTotal) "1 meal" else "${log.grams.toInt()}g"
                            val dismissState = rememberSwipeToDismissBoxState(
                                confirmValueChange = { value ->
                                    if (value == SwipeToDismissBoxValue.EndToStart) {
                                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                        onDeleteLog(log.id)
                                        true
                                    } else false
                                }
                            )

                            SwipeToDismissBox(
                                state = dismissState,
                                modifier = Modifier.animateItem(),
                                enableDismissFromStartToEnd = false,
                                backgroundContent = {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(KinetixTokens.Radius.CardShape)
                                            .background(KinetixTokens.Colors.StatusLowBg)
                                            .padding(horizontal = 20.dp),
                                        contentAlignment = Alignment.CenterEnd
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Delete,
                                            contentDescription = "Delete logged meal",
                                            tint = KinetixTokens.Colors.StatusLowFg
                                        )
                                    }
                                }
                            ) {
                                Card(
                                    contentPadding = 14.dp,
                                    onClick = { logToEdit = log },
                                    modifier = Modifier.testTag("consumed_food_item_${log.id}")
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Text(
                                                    text = "${log.foodName} ($portionLabel)",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    color = KinetixTokens.Colors.TextPrimary
                                                )
                                                StatChip(
                                                    text = log.mealSlot,
                                                    foregroundColor = KinetixTokens.Colors.Calories,
                                                    backgroundColor = KinetixTokens.Colors.CaloriesMutedBg
                                                )
                                            }
                                            Spacer(Modifier.height(4.dp))
                                            Text(
                                                text = "${log.kcal} kcal · P ${log.proteinG.toInt()}g · C ${log.carbsG.toInt()}g · F ${log.fatG.toInt()}g",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = KinetixTokens.Colors.TextSecondary
                                            )
                                        }
                                        IconButton(
                                            onClick = { logToEdit = log },
                                            modifier = Modifier
                                                .size(48.dp)
                                                .testTag("edit_food_log_${log.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Edit,
                                                contentDescription = "Edit meal",
                                                tint = KinetixTokens.Colors.TextSecondary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                NutritionTab.SEARCH -> {
                    item(key = "meal_slots_search") {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(MealSlot.entries, key = { it.name }) { slot ->
                                    val selected = selectedSlot == slot
                                    FilterChip(
                                        selected = selected,
                                        onClick = { selectedSlot = slot },
                                        label = { Text(slot.label) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = KinetixTokens.Colors.CaloriesMutedBg,
                                            selectedLabelColor = KinetixTokens.Colors.Calories
                                        )
                                    )
                                }
                            }
                            OutlinedTextField(
                                value = state.query,
                                onValueChange = onQueryChange,
                                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                                trailingIcon = {
                                    if (state.query.isNotBlank()) {
                                        TextButton(onClick = onRefreshSearch) {
                                            Text("Sync", color = KinetixTokens.Colors.Calories)
                                        }
                                    }
                                },
                                placeholder = { Text("Search foods (Open Food Facts + cached)") },
                                singleLine = true,
                                shape = KinetixTokens.Radius.ControlShape,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("food_search_input")
                            )
                            if (state.remoteError != null) {
                                Text(
                                    text = state.remoteError,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = KinetixTokens.Colors.StatusLowFg
                                )
                            }
                        }
                    }

                    if (state.isRefreshingRemote) {
                        item(key = "search_shimmer") {
                            ShimmerListPlaceholder(rowCount = 3)
                        }
                    }

                    val displayedResults = if (state.query.isBlank()) {
                        state.searchResults.take(10)
                    } else {
                        state.searchResults
                    }
                    if (displayedResults.isEmpty() && !state.isRefreshingRemote) {
                        item(key = "empty_search") {
                            Card {
                                EmptyState(
                                    icon = Icons.Rounded.Search,
                                    message = if (state.query.isBlank()) {
                                        "Type a food name to search Open Food Facts."
                                    } else {
                                        "No cached matches for \"${state.query}\"."
                                    },
                                    accentColor = KinetixTokens.Colors.Calories,
                                    actionLabel = if (state.query.isNotBlank()) "Sync" else "Custom",
                                    onAction = {
                                        if (state.query.isNotBlank()) onRefreshSearch() else showCustomSheet = true
                                    }
                                )
                            }
                        }
                    } else {
                        items(displayedResults, key = { it.barcodeOrKey }) { food ->
                            FoodCacheRowCard(
                                food = food,
                                onSelect = {
                                    val isTotalCustom = food.barcodeOrKey.startsWith("custom_total_")
                                    if (isTotalCustom) {
                                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                        onLogFood(food, 100.0, selectedSlot)
                                        activeTab = NutritionTab.TODAY
                                    } else {
                                        foodToLog = food
                                    }
                                }
                            )
                        }
                    }
                }

                NutritionTab.MY_FOODS -> {
                    item(key = "my_foods_slot_picker") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Saved Custom Foods",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = KinetixTokens.Colors.TextPrimary
                                )
                                TextButton(onClick = { showCustomSheet = true }) {
                                    Text("+ New Custom", color = KinetixTokens.Colors.Calories)
                                }
                            }
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(MealSlot.entries, key = { it.name }) { slot ->
                                    FilterChip(
                                        selected = selectedSlot == slot,
                                        onClick = { selectedSlot = slot },
                                        label = { Text("Log to ${slot.label}") },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = KinetixTokens.Colors.CaloriesMutedBg,
                                            selectedLabelColor = KinetixTokens.Colors.Calories
                                        )
                                    )
                                }
                            }
                        }
                    }

                    if (state.customFoods.isEmpty()) {
                        item(key = "empty_custom_foods") {
                            Card {
                                EmptyState(
                                    icon = Icons.Rounded.BookmarkBorder,
                                    message = "No saved custom foods yet.",
                                    accentColor = KinetixTokens.Colors.Calories,
                                    actionLabel = "Create",
                                    onAction = { showCustomSheet = true }
                                )
                            }
                        }
                    } else {
                        items(state.customFoods, key = { it.barcodeOrKey }) { food ->
                            val isTotalCustom = food.barcodeOrKey.startsWith("custom_total_")
                            val unitSuffix = if (isTotalCustom) "/ meal" else "/ 100g"
                            Card(
                                contentPadding = 14.dp,
                                modifier = Modifier
                                    .animateItem()
                                    .testTag("saved_custom_food_${food.barcodeOrKey}")
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = food.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = KinetixTokens.Colors.TextPrimary
                                        )
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            text = "${food.kcalPer100g.toInt()} kcal · P ${food.proteinPer100g.toInt()}g · C ${food.carbsPer100g.toInt()}g · F ${food.fatPer100g.toInt()}g $unitSuffix",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = KinetixTokens.Colors.TextSecondary
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            onClick = {
                                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                                if (isTotalCustom) {
                                                    onLogFood(food, 100.0, selectedSlot)
                                                    activeTab = NutritionTab.TODAY
                                                } else {
                                                    foodToLog = food
                                                }
                                            },
                                            shape = KinetixTokens.Radius.PillShape,
                                            color = KinetixTokens.Colors.CaloriesMutedBg,
                                            modifier = Modifier
                                                .height(40.dp)
                                                .applePressScale()
                                        ) {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier.padding(horizontal = 14.dp)
                                            ) {
                                                Text(
                                                    text = "Log",
                                                    style = MaterialTheme.typography.labelLarge,
                                                    color = KinetixTokens.Colors.Calories
                                                )
                                            }
                                        }
                                        IconButton(
                                            onClick = { onDeleteCustomFood(food.barcodeOrKey) },
                                            modifier = Modifier.size(48.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Delete,
                                                contentDescription = "Delete custom food",
                                                tint = KinetixTokens.Colors.TextSecondary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    val nonCustomRecents = state.recentFoods.filter { it.isCustom == 0 }
                    if (nonCustomRecents.isNotEmpty()) {
                        item(key = "recent_foods_label") {
                            Text(
                                text = "Recently Logged",
                                style = MaterialTheme.typography.titleMedium,
                                color = KinetixTokens.Colors.TextSecondary
                            )
                        }
                        items(nonCustomRecents, key = { "recent_${it.barcodeOrKey}" }) { item ->
                            FoodCacheRowCard(
                                food = item,
                                onSelect = { foodToLog = item }
                            )
                        }
                    }
                }
            }
        }

        AppleCollapsedNavBar(
            title = "Nutrition",
            listState = listState,
            modifier = Modifier.align(Alignment.TopCenter),
            trailing = {
                TextButton(onClick = { showCustomSheet = true }) {
                    Text(
                        text = "+ Custom",
                        color = KinetixTokens.Colors.Calories,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        )
    }

    logToEdit?.let { log ->
        var name by remember(log.id) { mutableStateOf(log.foodName) }
        var kcalText by remember(log.id) { mutableStateOf(log.kcal.toString()) }
        var proteinText by remember(log.id) { mutableStateOf(log.proteinG.roundToInt().toString()) }
        var carbsText by remember(log.id) { mutableStateOf(log.carbsG.roundToInt().toString()) }
        var fatText by remember(log.id) { mutableStateOf(log.fatG.roundToInt().toString()) }
        var slot by remember(log.id) {
            mutableStateOf(runCatching { MealSlot.valueOf(log.mealSlot) }.getOrDefault(MealSlot.LUNCH))
        }

        BottomSheet(
            title = "Edit Meal",
            subtitle = "Adjust macros or move meal slot",
            onDismiss = { logToEdit = null }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MealSlot.entries.forEach { s ->
                        FilterChip(
                            selected = slot == s,
                            onClick = { slot = s },
                            label = { Text(s.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = KinetixTokens.Colors.CaloriesMutedBg,
                                selectedLabelColor = KinetixTokens.Colors.Calories
                            )
                        )
                    }
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Meal name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = kcalText,
                        onValueChange = { kcalText = it.filter(Char::isDigit) },
                        label = { Text("Calories") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = proteinText,
                        onValueChange = { proteinText = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Protein (g)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = fatText,
                        onValueChange = { fatText = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Fat (g)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = carbsText,
                        onValueChange = { carbsText = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Carbs (g)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            onDeleteLog(log.id)
                            logToEdit = null
                        },
                        shape = KinetixTokens.Radius.ControlShape,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text("Delete", color = KinetixTokens.Colors.StatusLowFg)
                    }
                    PrimaryButton(
                        text = "Save Changes",
                        onClick = {
                            onUpdateLog(
                                log.copy(
                                    foodName = name.trim().ifBlank { log.foodName },
                                    kcal = kcalText.toIntOrNull()?.coerceAtLeast(0) ?: log.kcal,
                                    proteinG = proteinText.toDoubleOrNull()?.coerceAtLeast(0.0) ?: log.proteinG,
                                    carbsG = carbsText.toDoubleOrNull()?.coerceAtLeast(0.0) ?: log.carbsG,
                                    fatG = fatText.toDoubleOrNull()?.coerceAtLeast(0.0) ?: log.fatG,
                                    mealSlot = slot.name
                                )
                            )
                            logToEdit = null
                        },
                        containerColor = KinetixTokens.Colors.Calories,
                        contentColor = KinetixTokens.Colors.TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }

    foodToLog?.let { item ->
        var gramsText by remember(item.barcodeOrKey) { mutableStateOf(item.servingSizeG.toInt().toString()) }
        BottomSheet(
            title = item.name,
            subtitle = "${item.kcalPer100g.toInt()} kcal / 100g · P ${item.proteinPer100g.toInt()}g",
            onDismiss = { foodToLog = null }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = gramsText,
                    onValueChange = { gramsText = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Amount (grams)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                PrimaryButton(
                    text = "Log ${selectedSlot.label}",
                    onClick = {
                        val g = gramsText.toDoubleOrNull()?.coerceAtLeast(1.0) ?: 100.0
                        onLogFood(item, g, selectedSlot)
                        foodToLog = null
                        activeTab = NutritionTab.TODAY
                    },
                    containerColor = KinetixTokens.Colors.Calories,
                    contentColor = KinetixTokens.Colors.TextPrimary,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
            }
        }
    }

    if (showCustomSheet) {
        var useTotalMode by remember { mutableStateOf(true) }
        var name by remember { mutableStateOf("") }
        var kcal by remember { mutableStateOf("") }
        var protein by remember { mutableStateOf("") }
        var carbs by remember { mutableStateOf("") }
        var fat by remember { mutableStateOf("") }
        var grams by remember { mutableStateOf("100") }

        val pVal = protein.toDoubleOrNull() ?: 0.0
        val cVal = carbs.toDoubleOrNull() ?: 0.0
        val fVal = fat.toDoubleOrNull() ?: 0.0
        val autoKcal = ((pVal * 4.0) + (cVal * 4.0) + (fVal * 9.0)).roundToInt()

        val submitCustom: (alsoLog: Boolean) -> Unit = { alsoLog ->
            val finalName = name.trim().ifBlank { "Custom Meal" }
            val kcalVal = kcal.toDoubleOrNull() ?: 0.0
            if (useTotalMode) {
                onAddCustomMealTotals(
                    finalName,
                    kcalVal,
                    pVal,
                    cVal,
                    fVal,
                    selectedSlot,
                    alsoLog
                )
            } else {
                onAddCustomFoodPer100g(
                    finalName,
                    kcalVal,
                    pVal,
                    cVal,
                    fVal,
                    grams.toDoubleOrNull() ?: 100.0,
                    selectedSlot,
                    alsoLog
                )
            }
            showCustomSheet = false
            activeTab = if (alsoLog) NutritionTab.TODAY else NutritionTab.MY_FOODS
        }

        BottomSheet(
            title = if (useTotalMode) "Quick Custom Meal" else "Custom Ingredient",
            subtitle = "Saved permanently to My Foods",
            largeDetent = true,
            onDismiss = { showCustomSheet = false }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SegmentedControl(
                    options = listOf(true, false),
                    selected = useTotalMode,
                    labelFor = { if (it) "Total Values" else "Per 100g" },
                    onSelect = { useTotalMode = it },
                    tagPrefix = "custom_food_mode"
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(if (useTotalMode) "Meal / item name" else "Food name (per 100g)") },
                    placeholder = { Text("e.g. Paneer Bowl") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_food_name_input")
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = kcal,
                        onValueChange = { kcal = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text(if (useTotalMode) "Total kcal" else "Kcal/100g") },
                        placeholder = { Text(if (autoKcal > 0) "$autoKcal" else "665") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("custom_food_kcal_input")
                    )
                    OutlinedTextField(
                        value = protein,
                        onValueChange = { protein = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Protein (g)") },
                        placeholder = { Text("42") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("custom_food_protein_input")
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = fat,
                        onValueChange = { fat = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Fat (g)") },
                        placeholder = { Text("23") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("custom_food_fat_input")
                    )
                    OutlinedTextField(
                        value = carbs,
                        onValueChange = { carbs = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Carbs (g)") },
                        placeholder = { Text("0") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("custom_food_carbs_input")
                    )
                }
                AnimatedVisibility(visible = !useTotalMode) {
                    OutlinedTextField(
                        value = grams,
                        onValueChange = { grams = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Serving logged (g)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Text(
                    text = if (kcal.isBlank() && autoKcal > 0) {
                        "Auto-kcal from macros: ~$autoKcal kcal · Saved permanently to My Foods"
                    } else {
                        "Saved permanently to My Foods for 1-tap reuse"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = KinetixTokens.Colors.TextSecondary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { submitCustom(false) },
                        shape = KinetixTokens.Radius.ControlShape,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("save_only_custom_food_btn")
                    ) {
                        Text("Save Only", color = KinetixTokens.Colors.TextPrimary)
                    }
                    PrimaryButton(
                        text = "Save & Log",
                        onClick = { submitCustom(true) },
                        containerColor = KinetixTokens.Colors.Calories,
                        contentColor = KinetixTokens.Colors.TextPrimary,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("save_custom_food_btn")
                    )
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun FoodCacheRowCard(
    food: FoodItemCacheEntity,
    onSelect: () -> Unit
) {
    val isTotalCustom = food.barcodeOrKey.startsWith("custom_total_")
    Card(
        contentPadding = 14.dp,
        onClick = onSelect
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = food.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = KinetixTokens.Colors.TextPrimary
                )
                Spacer(Modifier.height(2.dp))
                val brandLabel = if (food.brand.isNotBlank()) "${food.brand} · " else ""
                val unitSuffix = if (isTotalCustom) "/ meal" else "/ 100g"
                Text(
                    text = "${brandLabel}${food.kcalPer100g.toInt()} kcal · P ${food.proteinPer100g.toInt()}g · C ${food.carbsPer100g.toInt()}g · F ${food.fatPer100g.toInt()}g $unitSuffix",
                    style = MaterialTheme.typography.bodyMedium,
                    color = KinetixTokens.Colors.TextSecondary
                )
            }
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = "Log ${food.name}",
                tint = KinetixTokens.Colors.Calories
            )
        }
    }
}
