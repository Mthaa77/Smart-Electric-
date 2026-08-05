package com.example.smartelectricity.ui.screens.home

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.smartelectricity.data.model.CalculationMode
import com.example.smartelectricity.data.model.Distributor
import com.example.smartelectricity.data.model.TariffProfile
import com.example.smartelectricity.data.model.isCalculationSupported
import com.example.smartelectricity.data.db.MonthlyBlockLedgerEntity
import com.example.smartelectricity.data.repository.TariffRepository
import com.example.smartelectricity.domain.calculator.CalculationEngine
import com.example.smartelectricity.domain.insights.EnergyInsights
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    currentSpentRand: Double = 650.0,
    budgetLimitRand: Double = 1500.0,
    activeHouseholdName: String? = null,
    activeProfile: TariffProfile? = null,
    activeLedger: MonthlyBlockLedgerEntity? = null,
    onUpdateBudgetLimit: (Double) -> Unit = {},
    onOpenWeeklyTracker: () -> Unit = {},
    onStartCalculator: (CalculationMode) -> Unit,
    onSelectDistributor: (Distributor) -> Unit,
    onOpenReconcile: () -> Unit,
    onOpenFbeGuide: () -> Unit,
    onOpenAlerts: () -> Unit,
    onOpenAdmin: () -> Unit,
    onOpenTools: () -> Unit = {},
    onOpenBudgetForecast: () -> Unit = {},
    onOpenHouseholds: () -> Unit = {},
    onOpenConcepts: () -> Unit = {}
) {
    var quickRandInput by remember { mutableStateOf("200") }
    var quickDistributor by remember { mutableStateOf(TariffRepository.distributors.first()) }
    var showBudgetDialog by remember { mutableStateOf(false) }
    var budgetInput by remember(budgetLimitRand) { mutableStateOf(budgetLimitRand.toInt().toString()) }

    val quickResult = remember(quickRandInput, quickDistributor) {
        val amount = quickRandInput.toDoubleOrNull() ?: 0.0
        val profile = quickDistributor.profiles.firstOrNull { it.isCalculationSupported }
        if (amount > 0 && profile != null) {
            CalculationEngine.calculateRandToKwh(
                distributor = quickDistributor,
                profile = profile,
                amountRand = amount,
                isFirstPurchaseOfMonth = true,
                hasClaimedFbeThisMonth = false
            ).totalKwh
        } else null
    }
    val calendar = remember { Calendar.getInstance() }
    val forecast = EnergyInsights.forecastMonthlyBudget(
        currentSpendRand = currentSpentRand,
        monthlyBudgetRand = budgetLimitRand,
        dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH),
        daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    )
    val animatedProgress by animateFloatAsState(
        targetValue = forecast.progressFraction,
        animationSpec = tween(700),
        label = "budget-progress"
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFF006DFF), Color(0xFF6946FF), Color(0xFFFF4D8D))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.ElectricBolt, null, tint = Color.White)
                        }
                        Spacer(Modifier.width(11.dp))
                        Column {
                            Text("Smart Electric", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                            Text(
                                "Your energy co-pilot",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onOpenAdmin) {
                        Icon(Icons.Default.AdminPanelSettings, contentDescription = "Tariff evidence")
                    }
                    IconButton(onClick = onOpenAlerts) {
                        BadgedBox(badge = { Badge() }) {
                            Icon(Icons.Outlined.Notifications, contentDescription = "Tariff alerts")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                HeroCard(
                    onCalculate = { onStartCalculator(CalculationMode.RAND_TO_KWH) },
                    onEstimate = { onStartCalculator(CalculationMode.KWH_TO_RAND) }
                )
            }

            if (activeHouseholdName != null && activeProfile != null) {
                item {
                    MonthlyBlockProgressCard(
                        householdName = activeHouseholdName,
                        profile = activeProfile,
                        ledger = activeLedger,
                        onOpenHouseholds = onOpenHouseholds
                    )
                }
            }

            item {
                BudgetPulseCard(
                    spent = currentSpentRand,
                    budget = budgetLimitRand,
                    projected = forecast.projectedMonthEndRand,
                    progress = animatedProgress,
                    isOnTrack = forecast.isOnTrack,
                    onClick = { showBudgetDialog = true },
                    onOpenForecast = onOpenBudgetForecast
                )
            }

            item {
                SectionTitle(
                    eyebrow = "QUICK ESTIMATE",
                    title = "What will my rand buy?",
                    action = "Full calculator",
                    onAction = { onStartCalculator(CalculationMode.RAND_TO_KWH) }
                )
            }

            item {
                QuickEstimatorCard(
                    amount = quickRandInput,
                    onAmountChange = { quickRandInput = it },
                    selectedDistributor = quickDistributor,
                    onDistributorChange = { quickDistributor = it },
                    estimatedKwh = quickResult,
                    onContinue = {
                        onSelectDistributor(quickDistributor)
                    }
                )
            }

            item {
                SectionTitle(
                    eyebrow = "SMART TOOLS",
                    title = "Make every unit work harder",
                    action = "Open tools",
                    onAction = onOpenTools
                )
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SmartToolCard(
                        title = "Appliance\nCost Lab",
                        subtitle = "See what your heater, geyser or TV costs.",
                        icon = Icons.Default.Bolt,
                        colors = listOf(Color(0xFF2854FF), Color(0xFF8748FF)),
                        onClick = onOpenTools
                    )
                    SmartToolCard(
                        title = "Monthly\nForecast",
                        subtitle = "Know your safe daily budget and month-end spend.",
                        icon = Icons.Default.AutoGraph,
                        colors = listOf(Color(0xFF008879), Color(0xFF16B982)),
                        onClick = onOpenBudgetForecast
                    )
                }
            }

            item {
                SectionTitle(eyebrow = "EXPLORE", title = "Everything in one place")
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        QuickLinkCard("Weekly tracker", "Log top-ups", Icons.Default.CalendarMonth, Modifier.weight(1f), onOpenWeeklyTracker)
                        QuickLinkCard("Households", "Save meter profiles", Icons.Default.HomeWork, Modifier.weight(1f), onOpenHouseholds)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        QuickLinkCard("FBE guide", "Check free units", Icons.Default.VolunteerActivism, Modifier.weight(1f), onOpenFbeGuide)
                        QuickLinkCard("Learn", "Understand tariffs", Icons.Default.School, Modifier.weight(1f), onOpenConcepts)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        QuickLinkCard("Check a token", "Compare expected units", Icons.Default.FactCheck, Modifier.weight(1f), onOpenReconcile)
                        QuickLinkCard("Rate alerts", "See official changes", Icons.Default.NotificationsActive, Modifier.weight(1f), onOpenAlerts)
                    }
                }
            }

            item {
                SectionTitle(eyebrow = "COVERAGE", title = "Built for South African tariffs")
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TariffRepository.distributors.forEach { distributor ->
                        DistributorChip(distributor = distributor, onClick = { onSelectDistributor(distributor) })
                    }
                }
            }
        }
    }

    if (showBudgetDialog) {
        AlertDialog(
            onDismissRequest = { showBudgetDialog = false },
            icon = { Icon(Icons.Default.Savings, null) },
            title = { Text("Set your monthly target", fontWeight = FontWeight.Black) },
            text = {
                OutlinedTextField(
                    value = budgetInput,
                    onValueChange = { budgetInput = it.filter { char -> char.isDigit() || char == '.' } },
                    label = { Text("Electricity budget") },
                    prefix = { Text("R ") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            },
            confirmButton = {
                Button(onClick = {
                    budgetInput.toDoubleOrNull()?.takeIf { it > 0 }?.let(onUpdateBudgetLimit)
                    showBudgetDialog = false
                }) { Text("Save target") }
            },
            dismissButton = { TextButton(onClick = { showBudgetDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun MonthlyBlockProgressCard(
    householdName: String,
    profile: TariffProfile,
    ledger: MonthlyBlockLedgerEntity?,
    onOpenHouseholds: () -> Unit
) {
    val paidUnits = ledger?.paidUnitsAllocatedKwh ?: 0.0
    val blocks = profile.blocks.sortedBy { it.blockNumber }
    val currentIndex = blocks.indexOfFirst { it.maxKwh == null || paidUnits < it.maxKwh }.coerceAtLeast(0)
    val currentBlock = blocks.getOrNull(currentIndex)
    val usedInBlock = currentBlock?.let { (paidUnits - it.minKwh).coerceAtLeast(0.0) } ?: 0.0
    val blockCapacity = currentBlock?.maxKwh?.let { it - currentBlock.minKwh }
    val progress = blockCapacity?.takeIf { it > 0.0 }?.let { (usedInBlock / it).toFloat().coerceIn(0f, 1f) } ?: 1f
    val remaining = blockCapacity?.let { (it - usedInBlock).coerceAtLeast(0.0) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f))
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("LIVE MONTHLY LEDGER", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                    Text(householdName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                    Text(profile.name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = onOpenHouseholds) { Text("Switch") }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("${"%.1f".format(paidUnits)} kWh", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                    Text("paid units this month", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Block ${currentBlock?.blockNumber ?: 1}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("${"%.2f".format(currentBlock?.rateCentsPerKwh ?: 0.0)} c/kWh", style = MaterialTheme.typography.labelSmall)
                }
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(9.dp).clip(CircleShape),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surface
            )

            Text(
                text = remaining?.let { "${"%.1f".format(it)} kWh remaining before the next price block." }
                    ?: "You are in the tariff's open-ended top block.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (ledger == null) {
                Text("Record your next completed purchase to start automatic block tracking.", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun HeroCard(onCalculate: () -> Unit, onEstimate: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(16.dp, RoundedCornerShape(30.dp), ambientColor = Color(0x330064FF), spotColor = Color(0x440064FF)),
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF071A3A))
    ) {
        Box(Modifier.fillMaxWidth().height(330.dp)) {
            Image(
                painter = painterResource(R.drawable.img_hero_electricity),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0x55020C20), Color(0xD907193A), Color(0xFF071A3A))
                        )
                    )
            )
            Column(
                modifier = Modifier.fillMaxSize().padding(21.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Surface(color = Color(0xFF6FF0BC).copy(alpha = 0.18f), shape = CircleShape) {
                        Row(Modifier.padding(horizontal = 11.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Verified, null, Modifier.size(15.dp), Color(0xFF74EFC0))
                            Spacer(Modifier.width(6.dp))
                            Text("2026/27 rates active", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFFB9FFE2))
                        }
                    }
                    Surface(color = Color.White.copy(alpha = 0.12f), shape = CircleShape) {
                        Icon(Icons.Default.AutoAwesome, null, Modifier.padding(9.dp).size(18.dp), Color(0xFFFFD866))
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Text(
                        "Electricity,\nfinally made simple.",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Text(
                        "Know what every rand buys, understand deductions and plan your month with confidence.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.76f)
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = onCalculate,
                            modifier = Modifier.weight(1f).height(50.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1C70FF))
                        ) {
                            Icon(Icons.Default.Calculate, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(7.dp))
                            Text("Calculate", fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = onEstimate,
                            modifier = Modifier.weight(1f).height(50.dp),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Text("Plan kWh", fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(5.dp))
                            Icon(Icons.Default.ArrowForward, null, Modifier.size(17.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BudgetPulseCard(
    spent: Double,
    budget: Double,
    projected: Double,
    progress: Float,
    isOnTrack: Boolean,
    onClick: () -> Unit,
    onOpenForecast: () -> Unit
) {
    val accent = if (isOnTrack) Color(0xFF0AA873) else Color(0xFFFF7043)
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 2.dp
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(13.dp), color = accent.copy(alpha = 0.13f)) {
                        Icon(Icons.Default.AccountBalanceWallet, null, Modifier.padding(9.dp).size(21.dp), accent)
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("Monthly pulse", fontWeight = FontWeight.Black)
                        Text(if (isOnTrack) "Looking healthy" else "Needs attention", style = MaterialTheme.typography.labelSmall, color = accent, fontWeight = FontWeight.Bold)
                    }
                }
                TextButton(onClick = onOpenForecast) { Text("Forecast") }
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(10.dp).clip(CircleShape),
                color = accent,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                BudgetStat("Spent", "R${"%,.0f".format(spent)}")
                BudgetStat("Target", "R${"%,.0f".format(budget)}")
                BudgetStat("Projected", "R${"%,.0f".format(projected)}", Alignment.End)
            }
        }
    }
}

@Composable
private fun BudgetStat(label: String, value: String, alignment: Alignment.Horizontal = Alignment.Start) {
    Column(horizontalAlignment = alignment) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun QuickEstimatorCard(
    amount: String,
    onAmountChange: (String) -> Unit,
    selectedDistributor: Distributor,
    onDistributorChange: (Distributor) -> Unit,
    estimatedKwh: Double?,
    onContinue: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Surface(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Box {
                Surface(
                    modifier = Modifier.fillMaxWidth().clickable { expanded = true },
                    shape = RoundedCornerShape(15.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationCity, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Distributor", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(selectedDistributor.name, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Icon(Icons.Default.ExpandMore, null)
                    }
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    TariffRepository.distributors.forEach { distributor ->
                        DropdownMenuItem(
                            text = { Text(distributor.name) },
                            leadingIcon = { Icon(Icons.Default.ElectricMeter, null) },
                            onClick = { onDistributorChange(distributor); expanded = false }
                        )
                    }
                }
            }
            OutlinedTextField(
                value = amount,
                onValueChange = { onAmountChange(it.filter { char -> char.isDigit() || char == '.' }) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Top-up amount") },
                prefix = { Text("R ") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
            Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.58f)) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Estimated token", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            estimatedKwh?.let { "${"%.2f".format(it)} kWh" } ?: "— kWh",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    FilledIconButton(onClick = onContinue) {
                        Icon(Icons.Default.ArrowForward, contentDescription = "Continue in calculator")
                    }
                }
            }
            Text(
                "Quick estimate uses the first tariff profile and assumes the first purchase of the month.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SmartToolCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    colors: List<Color>,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.width(235.dp).height(175.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(25.dp),
        color = Color.Transparent
    ) {
        Box(Modifier.fillMaxSize().background(Brush.linearGradient(colors)).padding(18.dp)) {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.16f)) {
                    Icon(icon, null, Modifier.padding(10.dp).size(22.dp), Color.White)
                }
                Column {
                    Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = Color.White)
                    Spacer(Modifier.height(4.dp))
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.78f))
                }
            }
            Icon(Icons.Default.ArrowOutward, null, Modifier.align(Alignment.TopEnd).size(20.dp), Color.White.copy(alpha = 0.8f))
        }
    }
}

@Composable
private fun QuickLinkCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)) {
                Icon(icon, null, Modifier.padding(8.dp).size(19.dp), MaterialTheme.colorScheme.primary)
            }
            Column {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
        }
    }
}

@Composable
private fun DistributorChip(distributor: Distributor, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.width(195.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) {
                Icon(Icons.Default.ElectricMeter, null, Modifier.padding(9.dp).size(20.dp), MaterialTheme.colorScheme.secondary)
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(distributor.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(distributor.province, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
            Icon(Icons.Default.ChevronRight, null, Modifier.size(18.dp), MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SectionTitle(
    eyebrow: String,
    title: String,
    action: String? = null,
    onAction: () -> Unit = {}
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
        Column {
            Text(eyebrow, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
        }
        if (action != null) {
            TextButton(onClick = onAction, contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)) {
                Text(action)
                Icon(Icons.Default.ChevronRight, null, Modifier.size(17.dp))
            }
        }
    }
}
