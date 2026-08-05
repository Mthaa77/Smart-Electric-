package com.example.smartelectricity.ui.screens.tracker

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.smartelectricity.data.db.MonthlyBlockLedgerEntity
import com.example.smartelectricity.data.db.PrepaidPurchaseEntity
import com.example.smartelectricity.data.model.TariffProfile
import com.example.smartelectricity.ui.components.LiquidGlassPanel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackerScreen(
    householdName: String?,
    profile: TariffProfile,
    ledger: MonthlyBlockLedgerEntity?,
    purchases: List<PrepaidPurchaseEntity>,
    budgetLimitRand: Double,
    onCalculate: () -> Unit,
    onOpenHouseholds: () -> Unit
) {
    val currentYearMonth = SimpleDateFormat("yyyy-MM", Locale.US).format(Date())
    val currentPurchases = purchases.filter { it.yearMonth == currentYearMonth }
    val spent = currentPurchases.sumOf { it.tenderAmountRand }
    val totalUnits = currentPurchases.sumOf { it.estimatedTotalUnitsKwh }
    val budgetProgress = (spent / budgetLimitRand.coerceAtLeast(1.0)).toFloat().coerceIn(0f, 1f)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Tracker", fontWeight = FontWeight.Black)
                        Text(householdName ?: "Choose a home to start tracking", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                actions = {
                    TextButton(onClick = onOpenHouseholds) { Text(if (householdName == null) "Set up" else "Switch") }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                LiquidGlassPanel(
                    modifier = Modifier.fillMaxWidth(),
                    accentColor = MaterialTheme.colorScheme.secondary,
                    elevation = 14.dp
                ) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text("THIS MONTH", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Black)
                                Text("R${"%,.0f".format(spent)}", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black)
                                Text("${"%.1f".format(totalUnits)} kWh recorded", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) {
                                Icon(Icons.Default.QueryStats, null, Modifier.padding(14.dp).size(28.dp), MaterialTheme.colorScheme.secondary)
                            }
                        }
                        LinearProgressIndicator(
                            progress = { budgetProgress },
                            modifier = Modifier.fillMaxWidth().height(10.dp).clip(CircleShape),
                            color = if (spent <= budgetLimitRand) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error,
                            trackColor = MaterialTheme.colorScheme.surface
                        )
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${(budgetProgress * 100).toInt()}% of budget", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            Text("Target R${"%,.0f".format(budgetLimitRand)}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            item { BlockJourney(profile = profile, ledger = ledger) }

            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Purchase timeline", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                        Text("Recorded top-ups power your budget and blocks", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    FilledIconButton(onClick = onCalculate) { Icon(Icons.Default.Add, contentDescription = "Calculate a purchase") }
                }
            }

            if (householdName == null) {
                item { TrackerEmptyState("Set up a home to keep purchases, blocks and budgets connected.", "Set up a home", onOpenHouseholds) }
            } else if (currentPurchases.isEmpty()) {
                item { TrackerEmptyState("No recorded purchases yet. Calculate your next top-up and record it from the result.", "Calculate a top-up", onCalculate) }
            } else {
                items(currentPurchases, key = { it.id }) { purchase -> PurchaseTimelineCard(purchase) }
            }
        }
    }
}

@Composable
private fun BlockJourney(profile: TariffProfile, ledger: MonthlyBlockLedgerEntity?) {
    val paidUnits = ledger?.paidUnitsAllocatedKwh ?: 0.0
    val blocks = profile.blocks.sortedBy { it.blockNumber }
    val current = blocks.firstOrNull { it.maxKwh == null || paidUnits < it.maxKwh } ?: blocks.lastOrNull()
    val used = current?.let { (paidUnits - it.minKwh).coerceAtLeast(0.0) } ?: 0.0
    val capacity = current?.maxKwh?.let { it - current.minKwh }
    val progress = capacity?.let { (used / it).toFloat().coerceIn(0f, 1f) } ?: if (paidUnits > 0) 1f else 0f

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 5.dp
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Tariff block journey", fontWeight = FontWeight.Black)
                    Text(profile.name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text("Block ${current?.blockNumber ?: 1}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black)
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(12.dp).clip(CircleShape),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primaryContainer
            )
            Text(
                capacity?.let { "${"%.1f".format((it - used).coerceAtLeast(0.0))} kWh until the next price block" }
                    ?: if (paidUnits > 0) "You are in the open-ended top block" else "Record a purchase to start your monthly journey",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PurchaseTimelineCard(purchase: PrepaidPurchaseEntity) {
    val formatter = SimpleDateFormat("dd MMM · HH:mm", Locale.getDefault())
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 3.dp
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(42.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Default.Bolt, null, tint = MaterialTheme.colorScheme.primary) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("R${"%.2f".format(purchase.tenderAmountRand)} top-up", fontWeight = FontWeight.Bold)
                Text(formatter.format(Date(purchase.purchaseTimestamp)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    purchase.actualUnitsKwh?.let { "${"%.1f".format(it)} kWh actual" }
                        ?: "${"%.1f".format(purchase.estimatedTotalUnitsKwh)} kWh",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Black
                )
                purchase.actualUnitsKwh?.let { actual ->
                    val difference = actual - purchase.estimatedTotalUnitsKwh
                    Text(
                        "${if (difference >= 0) "+" else ""}${"%.1f".format(difference)} vs estimate",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (purchase.fbeUnitsKwh > 0) Text("+${"%.0f".format(purchase.fbeUnitsKwh)} FBE", style = MaterialTheme.typography.labelSmall, color = Color(0xFF07815F))
            }
        }
    }
}

@Composable
private fun TrackerEmptyState(message: String, action: String, onClick: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
        Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Default.ReceiptLong, null, Modifier.size(42.dp), MaterialTheme.colorScheme.primary)
            Text(message, textAlign = androidx.compose.ui.text.style.TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = onClick, shape = RoundedCornerShape(15.dp)) { Text(action) }
        }
    }
}
