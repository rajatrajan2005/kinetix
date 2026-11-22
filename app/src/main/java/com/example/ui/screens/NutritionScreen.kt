package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.FoodItemCacheEntity
import com.example.data.local.entity.FoodLogEntity
import com.example.domain.model.MealSlot
import com.example.ui.components.CalorieRingGauge
import com.example.ui.components.KinetixCard
import com.example.ui.components.MacroProgressRow
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.CarbonSurface
import com.example.ui.theme.ElectricVolt
import com.example.ui.theme.GlacialCyan
import com.example.ui.theme.GlassRimHighlight
import com.example.ui.theme.OnElectricVolt
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VoltContainer
import com.example.ui.theme.WarmAmber
import com.example.ui.viewmodel.NutritionUiState
import kotlin.math.roundToInt

private enum class NutritionTab(val label: String) {
    TODAY("Today"),
    SEARCH("Search"),
    MY_FOODS("My Foods")
}

@OptIn(ExperimentalLayoutApi::class)
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
    var activeTab by remember { mutableStateOf(NutritionTab.TODAY) }
    var selectedSlot by remember { mutableStateOf(MealSlot.LUNCH) }
    var foodToLog by remember { mutableStateOf<FoodItemCacheEntity?>(null) }
    var logToEdit by remember { mutableStateOf<FoodLogEntity?>(null) }
    var showCustomDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp),
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 108.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "nutrition_header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("FUEL & MACROS", style = MaterialTheme.typography.labelMedium, color = WarmAmber)
                        Text("Nutrition", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
                    }
                    FilledTonalButton(
                        onClick = { showCustomDialog = true },
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("add_custom_food_btn")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Quick Custom")
                    }
                }
            }

            item(key = "ring_summary") {
                KinetixCard(accentGlow = WarmAmber) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CalorieRingGauge(
                            consumedKcal = state.consumedKcal,
                            targetKcal = state.targets.targetKcal,
                            size = 114.dp,
                            accentColor = WarmAmber
                        )
                        Spacer(Modifier.width(14.dp))
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MacroProgressRow("Protein", state.consumedProteinG, state.targets.proteinG, ElectricVolt)
                            MacroProgressRow("Carbs", state.consumedCarbsG, state.targets.carbsG, GlacialCyan)
                            MacroProgressRow("Fat", state.consumedFatG, state.targets.fatG, WarmAmber)
                        }
                    }
                }
            }

            // Sleek 3-segment switcher eliminates stacked scroll clutter
            item(key = "nutrition_segments") {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = CarbonSurface,
                    border = BorderStroke(1.dp, GlassRimHighlight.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        NutritionTab.entries.forEach { tab ->
                            val selected = activeTab == tab
                            val badgeCount = when (tab) {
                                NutritionTab.TODAY -> state.todayLogs.size
                                NutritionTab.SEARCH -> null
                                NutritionTab.MY_FOODS -> state.customFoods.size
                            }
                            val label = if (badgeCount != null && badgeCount > 0) "${tab.label} ($badgeCount)" else tab.label
                            Surface(
                                onClick = { activeTab = tab },
                                shape = RoundedCornerShape(12.dp),
                                color = if (selected) AmberContainer else CarbonSurface,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("nutrition_tab_${tab.name}")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (selected) WarmAmber else TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
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
                            Text("Consumed Today", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                            Text(
                                text = "${state.todayLogs.size} items · ${state.consumedKcal} kcal",
                                style = MaterialTheme.typography.labelMedium,
                                color = WarmAmber,
                                modifier = Modifier
                                    .background(AmberContainer, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    if (state.todayLogs.isEmpty()) {
                        item(key = "empty_logs") {
                            KinetixCard(containerColor = CarbonSurface, accentGlow = WarmAmber) {
                                Text(
                                    text = "No meals logged today yet. Tap Quick Custom to log macros, or switch to Search / My Foods.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                                Spacer(Modifier.height(10.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FilledTonalButton(onClick = { showCustomDialog = true }) {
                                        Text("Add Custom Food")
                                    }
                                    FilledTonalButton(onClick = { activeTab = NutritionTab.SEARCH }) {
                                        Text("Search Foods")
                                    }
                                }
                            }
                        }
                    } else {
                        items(state.todayLogs, key = { it.id }) { log ->
                            val isDirectTotal = log.foodKey.startsWith("custom_total_")
                            val portionLabel = if (isDirectTotal) "1 meal" else "${log.grams.toInt()}g"
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("consumed_food_item_${log.id}"),
                                shape = RoundedCornerShape(16.dp),
                                color = CarbonSurface,
                                border = BorderStroke(1.dp, GlassRimHighlight.copy(alpha = 0.22f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "${log.foodName} ($portionLabel)",
                                                style = MaterialTheme.typography.titleMedium,
                                                color = TextPrimary
                                            )
                                            Spacer(Modifier.width(6.dp))
                                            Text(
                                                text = log.mealSlot,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = WarmAmber,
                                                modifier = Modifier
                                                    .background(AmberContainer, RoundedCornerShape(6.dp))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            text = "${log.kcal} kcal · P ${log.proteinG.toInt()}g · C ${log.carbsG.toInt()}g · F ${log.fatG.toInt()}g",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextSecondary
                                        )
                                    }
                                    Row {
                                        IconButton(
                                            onClick = { logToEdit = log },
                                            modifier = Modifier.testTag("edit_food_log_${log.id}")
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit meal", tint = WarmAmber)
                                        }
                                        IconButton(onClick = { onDeleteLog(log.id) }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete log", tint = TextMuted)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                NutritionTab.SEARCH -> {
                    item(key = "meal_slots_search") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(MealSlot.entries, key = { it.name }) { slot ->
                                    FilterChip(
                                        selected = selectedSlot == slot,
                                        onClick = { selectedSlot = slot },
                                        label = { Text(slot.label) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = AmberContainer,
                                            selectedLabelColor = WarmAmber
                                        )
                                    )
                                }
                            }
                            OutlinedTextField(
                                value = state.query,
                                onValueChange = onQueryChange,
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                trailingIcon = {
                                    if (state.query.isNotBlank()) {
                                        TextButton(onClick = onRefreshSearch) { Text("Sync") }
                                    }
                                },
                                placeholder = { Text("Search foods (Open Food Facts + cached)") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("food_search_input")
                            )
                            if (state.isRefreshingRemote) {
                                LinearProgressIndicator(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = WarmAmber
                                )
                            }
                            if (state.remoteError != null) {
                                Text(
                                    text = state.remoteError,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }

                    val displayedResults = if (state.query.isBlank()) {
                        state.searchResults.take(10)
                    } else {
                        state.searchResults
                    }
                    if (displayedResults.isEmpty()) {
                        item(key = "empty_search") {
                            KinetixCard(containerColor = CarbonSurface, accentGlow = GlacialCyan) {
                                Text(
                                    text = if (state.query.isBlank()) {
                                        "Type a food name above to search Open Food Facts or saved items."
                                    } else {
                                        "No cached matches for \"${state.query}\" yet. Tap Sync or create a Quick Custom entry."
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
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
                                Text("Saved Custom Foods", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                                TextButton(onClick = { showCustomDialog = true }) {
                                    Text("+ New Custom", color = ElectricVolt)
                                }
                            }
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(MealSlot.entries, key = { it.name }) { slot ->
                                    FilterChip(
                                        selected = selectedSlot == slot,
                                        onClick = { selectedSlot = slot },
                                        label = { Text("Log to ${slot.label}") },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = AmberContainer,
                                            selectedLabelColor = WarmAmber
                                        )
                                    )
                                }
                            }
                        }
                    }

                    if (state.customFoods.isEmpty()) {
                        item(key = "empty_custom_foods") {
                            KinetixCard(containerColor = CarbonSurface, accentGlow = ElectricVolt) {
                                Text(
                                    text = "No saved custom foods yet. Tap Quick Custom to save a meal or ingredient for 1-tap logging anytime.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                        }
                    } else {
                        items(state.customFoods, key = { it.barcodeOrKey }) { food ->
                            val isTotalCustom = food.barcodeOrKey.startsWith("custom_total_")
                            val unitSuffix = if (isTotalCustom) "/ meal" else "/ 100g"
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("saved_custom_food_${food.barcodeOrKey}"),
                                shape = RoundedCornerShape(16.dp),
                                color = CarbonSurface,
                                border = BorderStroke(1.dp, GlassRimHighlight.copy(alpha = 0.25f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(food.name, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                        Text(
                                            text = "${food.kcalPer100g.toInt()} kcal · P ${food.proteinPer100g.toInt()}g · C ${food.carbsPer100g.toInt()}g · F ${food.fatPer100g.toInt()}g $unitSuffix",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextSecondary
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        FilledTonalButton(
                                            onClick = {
                                                if (isTotalCustom) {
                                                    onLogFood(food, 100.0, selectedSlot)
                                                    activeTab = NutritionTab.TODAY
                                                } else {
                                                    foodToLog = food
                                                }
                                            },
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                        ) { Text("Log") }
                                        IconButton(onClick = { onDeleteCustomFood(food.barcodeOrKey) }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete custom food", tint = TextMuted)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    val nonCustomRecents = state.recentFoods.filter { it.isCustom == 0 }
                    if (nonCustomRecents.isNotEmpty()) {
                        item(key = "recent_foods_label") {
                            Text("Recently Logged Foods", style = MaterialTheme.typography.titleMedium, color = TextSecondary)
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

        AlertDialog(
            onDismissRequest = { logToEdit = null },
            title = { Text("Edit Logged Meal") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        MealSlot.entries.forEach { s ->
                            FilterChip(
                                selected = slot == s,
                                onClick = { slot = s },
                                label = { Text(s.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AmberContainer,
                                    selectedLabelColor = WarmAmber
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
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                }
            },
            confirmButton = {
                Button(
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
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricVolt, contentColor = OnElectricVolt)
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { logToEdit = null }) { Text("Cancel") }
            }
        )
    }

    foodToLog?.let { item ->
        var gramsText by remember(item.barcodeOrKey) { mutableStateOf(item.servingSizeG.toInt().toString()) }
        AlertDialog(
            onDismissRequest = { foodToLog = null },
            title = { Text(item.name) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "${item.kcalPer100g.toInt()} kcal / 100g · P ${item.proteinPer100g.toInt()}g",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    OutlinedTextField(
                        value = gramsText,
                        onValueChange = { gramsText = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Amount (grams)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val g = gramsText.toDoubleOrNull()?.coerceAtLeast(1.0) ?: 100.0
                        onLogFood(item, g, selectedSlot)
                        foodToLog = null
                        activeTab = NutritionTab.TODAY
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WarmAmber, contentColor = OnElectricVolt)
                ) { Text("Log ${selectedSlot.label}") }
            },
            dismissButton = { TextButton(onClick = { foodToLog = null }) { Text("Cancel") } }
        )
    }

    if (showCustomDialog) {
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
            showCustomDialog = false
            activeTab = if (alsoLog) NutritionTab.TODAY else NutritionTab.MY_FOODS
        }

        AlertDialog(
            onDismissRequest = { showCustomDialog = false },
            title = { Text(if (useTotalMode) "Quick Custom Meal" else "Custom Ingredient (per 100g)") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = useTotalMode,
                            onClick = { useTotalMode = true },
                            label = { Text("Total Values") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = VoltContainer,
                                selectedLabelColor = ElectricVolt
                            )
                        )
                        FilterChip(
                            selected = !useTotalMode,
                            onClick = { useTotalMode = false },
                            label = { Text("Per 100g") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AmberContainer,
                                selectedLabelColor = WarmAmber
                            )
                        )
                    }
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(if (useTotalMode) "Meal / item name" else "Food name (per 100g)") },
                        placeholder = { Text("e.g. Paneer Bowl") },
                        singleLine = true,
                        modifier = Modifier.testTag("custom_food_name_input")
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                        style = MaterialTheme.typography.labelSmall,
                        color = ElectricVolt
                    )
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(
                        onClick = { submitCustom(false) },
                        modifier = Modifier.testTag("save_only_custom_food_btn")
                    ) { Text("Save Only") }
                    Button(
                        onClick = { submitCustom(true) },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricVolt, contentColor = OnElectricVolt),
                        modifier = Modifier.testTag("save_custom_food_btn")
                    ) { Text("Save & Log") }
                }
            },
            dismissButton = { TextButton(onClick = { showCustomDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun FoodCacheRowCard(
    food: FoodItemCacheEntity,
    onSelect: () -> Unit
) {
    val isTotalCustom = food.barcodeOrKey.startsWith("custom_total_")
    KinetixCard(
        accentGlow = GlacialCyan,
        onClick = onSelect
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(food.name, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                val brandLabel = if (food.brand.isNotBlank()) "${food.brand} · " else ""
                val unitSuffix = if (isTotalCustom) "/ meal" else "/ 100g"
                Text(
                    text = "${brandLabel}${food.kcalPer100g.toInt()} kcal · P ${food.proteinPer100g.toInt()}g · C ${food.carbsPer100g.toInt()}g · F ${food.fatPer100g.toInt()}g $unitSuffix",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
            Icon(Icons.Default.Add, contentDescription = "Log ${food.name}", tint = WarmAmber)
        }
    }
}
