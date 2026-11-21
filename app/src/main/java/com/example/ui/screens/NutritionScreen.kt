package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import com.example.domain.model.MealSlot
import com.example.ui.components.CalorieRingGauge
import com.example.ui.components.KinetixAmbientBackdrop
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
        mealSlot: MealSlot
    ) -> Unit,
    onAddCustomFoodPer100g: (
        name: String,
        kcalPer100g: Double,
        proteinPer100g: Double,
        carbsPer100g: Double,
        fatPer100g: Double,
        gramsToLog: Double,
        mealSlot: MealSlot
    ) -> Unit,
    onDeleteLog: (Long) -> Unit
) {
    var selectedSlot by remember { mutableStateOf(MealSlot.LUNCH) }
    var foodToLog by remember { mutableStateOf<FoodItemCacheEntity?>(null) }
    var showCustomDialog by remember { mutableStateOf(false) }

    KinetixAmbientBackdrop(
        primaryOrb = WarmAmber,
        secondaryOrb = ElectricVolt,
        tertiaryOrb = GlacialCyan
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 640.dp),
                contentPadding = PaddingValues(16.dp),
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
                            modifier = Modifier.testTag("add_custom_food_btn")
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
                                size = 116.dp,
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

                // Consumed food placed at the top right below the summary ring
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
                                text = "Nothing logged yet today. Tap Quick Custom to enter total protein, fat & calories, or search below.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
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
                                IconButton(onClick = { onDeleteLog(log.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete log", tint = TextMuted)
                                }
                            }
                        }
                    }
                }

                item(key = "meal_slots_search") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Add Food", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
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
                            placeholder = { Text("Search Open Food Facts (cached offline first)") },
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

                if (state.recentFoods.isNotEmpty() && state.query.isBlank()) {
                    item(key = "recent_foods_row") {
                        Column {
                            Text("Recent & Custom", style = MaterialTheme.typography.titleMedium, color = TextSecondary)
                            Spacer(Modifier.height(6.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(state.recentFoods, key = { it.barcodeOrKey }) { item ->
                                    val isTotalCustom = item.barcodeOrKey.startsWith("custom_total_")
                                    val label = if (isTotalCustom) {
                                        "${item.name} (${item.kcalPer100g.toInt()} kcal · ${item.proteinPer100g.toInt()}g P)"
                                    } else {
                                        "${item.name} (${item.kcalPer100g.toInt()} kcal/100g)"
                                    }
                                    FilledTonalButton(
                                        onClick = {
                                            if (isTotalCustom) {
                                                onLogFood(item, 100.0, selectedSlot)
                                            } else {
                                                foodToLog = item
                                            }
                                        }
                                    ) {
                                        Text(label)
                                    }
                                }
                            }
                        }
                    }
                }

                if (state.searchResults.isNotEmpty()) {
                    item(key = "results_label") {
                        Text(
                            text = if (state.query.isBlank()) "Cached Food Database" else "Cached & Open Food Facts Results",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextSecondary
                        )
                    }
                    items(state.searchResults, key = { it.barcodeOrKey }) { food ->
                        val isTotalCustom = food.barcodeOrKey.startsWith("custom_total_")
                        KinetixCard(
                            accentGlow = GlacialCyan,
                            onClick = {
                                if (isTotalCustom) {
                                    onLogFood(food, 100.0, selectedSlot)
                                } else {
                                    foodToLog = food
                                }
                            }
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
                }
            }
        }
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
                            placeholder = { Text("665") },
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
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val finalName = name.trim().ifBlank { "Custom Meal" }
                        val kcalVal = kcal.toDoubleOrNull() ?: 0.0
                        val pVal = protein.toDoubleOrNull() ?: 0.0
                        val cVal = carbs.toDoubleOrNull() ?: 0.0
                        val fVal = fat.toDoubleOrNull() ?: 0.0
                        if (useTotalMode) {
                            onAddCustomMealTotals(
                                finalName,
                                kcalVal,
                                pVal,
                                cVal,
                                fVal,
                                selectedSlot
                            )
                        } else {
                            onAddCustomFoodPer100g(
                                finalName,
                                kcalVal,
                                pVal,
                                cVal,
                                fVal,
                                grams.toDoubleOrNull() ?: 100.0,
                                selectedSlot
                            )
                        }
                        showCustomDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricVolt, contentColor = OnElectricVolt),
                    modifier = Modifier.testTag("save_custom_food_btn")
                ) { Text("Log ${selectedSlot.label}") }
            },
            dismissButton = { TextButton(onClick = { showCustomDialog = false }) { Text("Cancel") } }
        )
    }
}
