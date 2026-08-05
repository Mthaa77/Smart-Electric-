package com.example.smartelectricity.ui.screens.tools

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.smartelectricity.data.model.TariffProfile
import com.example.smartelectricity.domain.insights.EnergyInsights
import java.util.Calendar

private data class AppliancePreset(
    val name: String,
    val watts: Int,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

private val appliancePresets = listOf(
    AppliancePreset("Heater", 2000, Icons.Default.LocalFireDepartment),
    AppliancePreset("Geyser", 3000, Icons.Default.WaterDrop),
    AppliancePreset("Fridge", 150, Icons.Default.Kitchen),
    AppliancePreset("TV", 120, Icons.Default.Tv),
    AppliancePreset("Kettle", 2200, Icons.Default.Coffee)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartToolsScreen(
    profile: TariffProfile,
    currentSpentRand: Double,
    budgetLimitRand: Double,
    onUpdateBudgetLimit: (Double) -> Unit,
    onNavigateBack: () -> Unit,
    initialTool: Int = 0
) {
    var selectedTool by remember(initialTool) { mutableIntStateOf(initialTool.coerceIn(0, 1)) }
    val calendar = remember { Calendar.getInstance() }
    val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Smart tools", fontWeight = FontWeight.ExtraBold)
                        Text(
                            "Powered by ${profile.name}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f))
            ) {
                Row(modifier = Modifier.padding(5.dp)) {
                    ToolSegment(
                        text = "Appliance cost",
                        icon = Icons.Default.Bolt,
                        selected = selectedTool == 0,
                        modifier = Modifier.weight(1f),
                        onClick = { selectedTool = 0 }
                    )
                    ToolSegment(
                        text = "Budget forecast",
                        icon = Icons.Default.QueryStats,
                        selected = selectedTool == 1,
                        modifier = Modifier.weight(1f),
                        onClick = { selectedTool = 1 }
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            AnimatedContent(
                targetState = selectedTool,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                transitionSpec = {
                    androidx.compose.animation.fadeIn(tween(220)) togetherWith
                        androidx.compose.animation.fadeOut(tween(140))
                },
                label = "tool-content"
            ) { tool ->
                if (tool == 0) {
                    ApplianceCostLab(profile = profile)
                } else {
                    BudgetForecastLab(
                        currentSpentRand = currentSpentRand,
                        budgetLimitRand = budgetLimitRand,
                        dayOfMonth = dayOfMonth,
                        daysInMonth = daysInMonth,
                        onUpdateBudgetLimit = onUpdateBudgetLimit
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.ToolSegment(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier,
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (selected) MaterialTheme.colorScheme.surface else Color.Transparent,
        shadowElevation = if (selected) 3.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(7.dp))
            Text(
                text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ApplianceCostLab(profile: TariffProfile) {
    var selectedPreset by remember { mutableStateOf(appliancePresets.first()) }
    var wattsInput by remember { mutableStateOf(selectedPreset.watts.toString()) }
    var hoursPerDay by remember { mutableFloatStateOf(3f) }
    var daysPerMonth by remember { mutableFloatStateOf(30f) }
    val rate = remember(profile) { (profile.blocks.firstOrNull()?.rateCentsPerKwh ?: 0.0) / 100.0 }
    val estimate = EnergyInsights.estimateApplianceCost(
        watts = wattsInput.toDoubleOrNull() ?: 0.0,
        hoursPerDay = hoursPerDay.toDouble(),
        daysPerMonth = daysPerMonth.toInt(),
        randPerKwh = rate
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ToolHero(
            eyebrow = "APPLIANCE COST LAB",
            title = "See what your appliances really cost.",
            body = "Pick an appliance, adjust the usage, and get an instant monthly estimate.",
            icon = Icons.Default.ElectricMeter,
            colors = listOf(Color(0xFF0B5FFF), Color(0xFF6C3BFF), Color(0xFFFF4D8D))
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            appliancePresets.forEach { preset ->
                val selected = preset.name == selectedPreset.name
                Surface(
                    modifier = Modifier.clickable {
                        selectedPreset = preset
                        wattsInput = preset.watts.toString()
                    },
                    shape = RoundedCornerShape(50),
                    color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        1.dp,
                        if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                        else MaterialTheme.colorScheme.outlineVariant
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 13.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(preset.icon, null, Modifier.size(17.dp), MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(6.dp))
                        Text(preset.name, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = wattsInput,
                    onValueChange = { wattsInput = it.filter { char -> char.isDigit() || char == '.' } },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Appliance power") },
                    suffix = { Text("watts") },
                    leadingIcon = { Icon(Icons.Default.Power, null) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )

                LabeledSlider(
                    label = "Daily use",
                    valueLabel = "${"%.1f".format(hoursPerDay)} hours",
                    value = hoursPerDay,
                    range = 0.5f..24f,
                    steps = 46,
                    onValueChange = { hoursPerDay = it }
                )
                LabeledSlider(
                    label = "Days used per month",
                    valueLabel = "${daysPerMonth.toInt()} days",
                    value = daysPerMonth,
                    range = 1f..31f,
                    steps = 29,
                    onValueChange = { daysPerMonth = it }
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF071A3A),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text("ESTIMATED MONTHLY COST", style = MaterialTheme.typography.labelSmall, color = Color(0xFFAEC8FF))
                        Text(
                            "R${"%,.2f".format(estimate.monthlyCostRand)}",
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                    Surface(color = Color.White.copy(alpha = 0.1f), shape = RoundedCornerShape(12.dp)) {
                        Text(
                            "${"%.1f".format(estimate.monthlyKwh)} kWh",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            color = Color(0xFF73E6B1),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                HorizontalDivider(color = Color.White.copy(alpha = 0.12f))
                Text(
                    "Calculated at R${"%.2f".format(rate)}/kWh from ${profile.name}. Fixed charges are excluded.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.72f)
                )
            }
        }
    }
}

@Composable
private fun BudgetForecastLab(
    currentSpentRand: Double,
    budgetLimitRand: Double,
    dayOfMonth: Int,
    daysInMonth: Int,
    onUpdateBudgetLimit: (Double) -> Unit
) {
    var budgetInput by remember(budgetLimitRand) { mutableStateOf("${budgetLimitRand.toInt()}") }
    val forecast = EnergyInsights.forecastMonthlyBudget(
        currentSpendRand = currentSpentRand,
        monthlyBudgetRand = budgetInput.toDoubleOrNull() ?: budgetLimitRand,
        dayOfMonth = dayOfMonth,
        daysInMonth = daysInMonth
    )
    val statusColor = if (forecast.isOnTrack) Color(0xFF18A66A) else Color(0xFFFF7A45)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ToolHero(
            eyebrow = "MONTHLY BUDGET FORECAST",
            title = if (forecast.isOnTrack) "You’re pacing beautifully." else "Let’s bring the month back on track.",
            body = "A live forecast based on what you’ve spent by day $dayOfMonth of $daysInMonth.",
            icon = Icons.Default.AutoGraph,
            colors = listOf(Color(0xFF007F73), Color(0xFF0AAE88), Color(0xFFFFC857))
        )

        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = budgetInput,
                    onValueChange = { budgetInput = it.filter { char -> char.isDigit() || char == '.' } },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Monthly electricity budget") },
                    prefix = { Text("R ") },
                    leadingIcon = { Icon(Icons.Default.Savings, null) },
                    trailingIcon = {
                        IconButton(onClick = {
                            val value = budgetInput.toDoubleOrNull()
                            if (value != null && value > 0) onUpdateBudgetLimit(value)
                        }) {
                            Icon(Icons.Default.CheckCircle, contentDescription = "Save budget")
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ForecastMetric("Spent", "R${"%,.0f".format(currentSpentRand)}", Modifier.weight(1f))
                    ForecastMetric("Remaining", "R${"%,.0f".format(forecast.remainingBudgetRand)}", Modifier.weight(1f))
                }

                LinearProgressIndicator(
                    progress = { forecast.progressFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(CircleShape),
                    color = statusColor,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Budget used", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${(forecast.progressFraction * 100).toInt()}% of budget", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(24.dp),
            color = statusColor.copy(alpha = 0.11f),
            border = BorderStroke(1.dp, statusColor.copy(alpha = 0.28f))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(shape = CircleShape, color = statusColor.copy(alpha = 0.16f)) {
                    Icon(
                        if (forecast.isOnTrack) Icons.Default.RocketLaunch else Icons.Default.TipsAndUpdates,
                        null,
                        Modifier.padding(12.dp).size(26.dp),
                        statusColor
                    )
                }
                Text("Safe daily allowance", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "R${"%,.2f".format(forecast.safeDailyBudgetRand)}",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Black,
                    color = statusColor
                )
                Text(
                    "At your current pace, the month is projected to close at R${"%,.0f".format(forecast.projectedMonthEndRand)}.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Default.Lightbulb, null, tint = MaterialTheme.colorScheme.primary)
                Column {
                    Text("Smart nudge", fontWeight = FontWeight.Bold)
                    Text(
                        if (forecast.isOnTrack)
                            "Keep each remaining day near the allowance above and you should finish inside your target."
                        else
                            "Prioritise geyser and heater hours first; they usually create the fastest meaningful saving.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ToolHero(
    eyebrow: String,
    title: String,
    body: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    colors: List<Color>
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(Brush.linearGradient(colors))
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(end = 52.dp)) {
            Text(eyebrow, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = Color.White.copy(alpha = 0.76f))
            Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = Color.White)
            Text(body, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.82f))
        }
        Surface(
            modifier = Modifier.align(Alignment.TopEnd),
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.15f)
        ) {
            Icon(icon, null, Modifier.padding(11.dp).size(25.dp), Color.White)
        }
    }
}

@Composable
private fun LabeledSlider(
    label: String,
    valueLabel: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            Text(valueLabel, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
        Slider(value = value, onValueChange = onValueChange, valueRange = range, steps = steps)
    }
}

@Composable
private fun ForecastMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
        }
    }
}
