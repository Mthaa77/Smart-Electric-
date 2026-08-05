package com.example.smartelectricity.ui.screens.result

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartelectricity.data.model.CalculationMode
import com.example.smartelectricity.data.model.CalculationResult
import com.example.smartelectricity.ui.components.BlockVisualizer
import com.example.smartelectricity.ui.components.LiquidGlassPanel
import com.example.smartelectricity.ui.components.VerificationStatusBadge
import com.example.smartelectricity.ui.components.premiumDepth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    result: CalculationResult?,
    hasActiveHousehold: Boolean,
    onOpenReconcile: () -> Unit,
    onRecordPurchase: () -> Unit,
    purchaseSaveMessage: String?,
    isPurchaseRecorded: Boolean,
    onSaveHouseholdClick: () -> Unit,
    onBackToHome: () -> Unit
) {
    val context = LocalContext.current

    if (result == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("No calculation result active.")
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onBackToHome) { Text("Go to Home") }
            }
        }
        return
    }

    var showMathBreakdown by remember { mutableStateOf(false) }
    var showSourceInspector by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calculation Result", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackToHome) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val shareText = "Smart Electricity Estimate:\nSupplier: ${result.distributor.name}\nTariff: ${result.profile.name}\nPurchase: R${"%.2f".format(result.grossPurchaseRand)}\nEstimated Units: ${"%.1f".format(result.totalKwh)} kWh (${"%.1f".format(result.paidKwh)} paid + ${"%.1f".format(result.freeFbeKwh)} FBE)\nEffective Date: ${result.effectiveDateStr}"
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, shareText)
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share Electricity Estimate"))
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            if (!result.isCalculationValid) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Calculation needs corrected inputs", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onErrorContainer)
                            result.validationWarnings.forEach { warning ->
                                Text(warning, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                            }
                        }
                    }
                }
            }

            // Hero Overview Card
            item {
                LiquidGlassPanel(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    accentColor = MaterialTheme.colorScheme.primary,
                    elevation = 16.dp
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${result.distributor.name} • ${result.profile.name}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            VerificationStatusBadge(status = result.verificationStatus)
                        }

                        if (result.mode == CalculationMode.RAND_TO_KWH) {
                            Text(
                                text = "${"%.1f".format(result.totalKwh)} kWh",
                                style = MaterialTheme.typography.displayMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Total Estimated Token Output for R${"%.2f".format(result.grossPurchaseRand)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        } else if (result.mode == CalculationMode.KWH_TO_RAND) {
                            Text(
                                text = "R${"%.2f".format(result.grossPurchaseRand)}",
                                style = MaterialTheme.typography.displayMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Total Estimated Purchase Required for ${"%.1f".format(result.totalKwh)} kWh",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        } else {
                            Text(
                                text = "R${"%.2f".format(result.grossPurchaseRand)}",
                                style = MaterialTheme.typography.displayMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Estimated bill for ${"%.1f".format(result.totalKwh)} kWh used",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }

                        Divider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Verified,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = result.confidenceMessage,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            // Money Allocation Table
            item {
                LiquidGlassPanel(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    accentColor = MaterialTheme.colorScheme.secondary,
                    elevation = 9.dp
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Where your money and units go", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                        AllocationRow(label = "Gross Purchase Amount", value = "R${"%.2f".format(result.grossPurchaseRand)}")
                        if (result.fixedChargeDeductedRand > 0) {
                            AllocationRow(label = "Monthly Network Access Charge Deducted", value = "-R${"%.2f".format(result.fixedChargeDeductedRand)}", isHighlight = true)
                        }
                        if (result.arrearsDeductedRand > 0) {
                            AllocationRow(label = "Authorised arrears recovery", value = "-R${"%.2f".format(result.arrearsDeductedRand)}", isHighlight = true)
                        }
                        AllocationRow(label = "Net Money Available for Energy", value = "R${"%.2f".format(result.netEnergyPurchaseRand)}")
                        AllocationRow(label = "Included VAT (15%)", value = "R${"%.2f".format(result.vatAmountRand)}")

                        Divider()

                        AllocationRow(label = "Paid Electricity Purchased", value = "${"%.2f".format(result.paidKwh)} kWh")
                        if (result.freeFbeKwh > 0) {
                            AllocationRow(label = "Free Basic Electricity (FBE)", value = "+${"%.1f".format(result.freeFbeKwh)} kWh", isHighlight = true)
                        }
                        AllocationRow(label = "Final Token Units Output", value = "${"%.2f".format(result.totalKwh)} kWh", isBold = true)
                        AllocationRow(label = "Average Variable Energy Rate", value = "${"%.2f".format(result.averageRateCentsPerKwh)} c/kWh")
                        if (result.effectiveRandPerKwh > 0.0) {
                            AllocationRow(label = "Effective All-In Unit Price", value = "R${"%.4f".format(result.effectiveRandPerKwh)} / kWh", isHighlight = true)
                        }
                        result.remainingKwhInCurrentBlock?.let { remaining ->
                            AllocationRow(label = "Units left in this price block", value = "${"%.1f".format(remaining)} kWh", isHighlight = true)
                        }
                        result.nextBlockRateCentsPerKwh?.let { nextRate ->
                            AllocationRow(label = "Next block energy rate", value = "${"%.2f".format(nextRate)} c/kWh")
                        }
                    }
                }
            }

            // Block Visualizer
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        BlockVisualizer(blocks = result.blockBreakdown)
                    }
                }
            }

            // Step by Step Math Explanation
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("How this was calculated", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            TextButton(onClick = { showMathBreakdown = !showMathBreakdown }) {
                                Text(if (showMathBreakdown) "Hide" else "Show")
                            }
                        }

                        AnimatedVisibility(visible = showMathBreakdown) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                result.stepExplanations.forEach { stepText ->
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = stepText,
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.padding(10.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Source Document Inspector
            item {
                LiquidGlassPanel(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    accentColor = MaterialTheme.colorScheme.tertiary,
                    elevation = 8.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Official Source & Tariff Schedule", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            IconButton(onClick = { showSourceInspector = !showSourceInspector }) {
                                Icon(Icons.Outlined.HelpOutline, contentDescription = "Inspect Source")
                            }
                        }

                        Text("Schedule: ${result.sourceTitle}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                        Text("Effective Date: ${result.effectiveDateStr}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        AnimatedVisibility(showSourceInspector) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Source ID: ${result.sourceDocumentId.ifBlank { "Not supplied" }}", style = MaterialTheme.typography.labelSmall)
                                Text("Calculation engine: ${result.calculationEngineVersion}", style = MaterialTheme.typography.labelSmall)
                                Text("Estimate only. The official supplier bill or vending result remains authoritative.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                                TextButton(
                                    onClick = {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(result.sourceUrl)))
                                    },
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("Open official source")
                                    Spacer(Modifier.width(4.dp))
                                    Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Quick Actions
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (result.mode == CalculationMode.RAND_TO_KWH) {
                        Button(
                            onClick = onRecordPurchase,
                            enabled = !isPurchaseRecorded,
                            modifier = Modifier
                                .fillMaxWidth()
                                .premiumDepth(
                                    shape = RoundedCornerShape(12.dp),
                                    elevation = 9.dp,
                                    accentColor = MaterialTheme.colorScheme.primary
                                ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.AddTask, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                when {
                                    isPurchaseRecorded -> "Purchase recorded"
                                    !hasActiveHousehold -> "Save home & record purchase"
                                    else -> "Record purchase & update monthly block"
                                }
                            )
                        }
                    }

                    purchaseSaveMessage?.let { message ->
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (message.startsWith("Purchase recorded")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                    }

                    if (result.mode == CalculationMode.RAND_TO_KWH) {
                        Button(
                            onClick = onOpenReconcile,
                            modifier = Modifier
                                .fillMaxWidth()
                                .premiumDepth(
                                    shape = RoundedCornerShape(12.dp),
                                    elevation = 7.dp,
                                    accentColor = MaterialTheme.colorScheme.secondary
                                ),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Icon(Icons.Default.Receipt, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Compare with my token receipt")
                        }
                    }

                    if (!hasActiveHousehold && result.mode != CalculationMode.RAND_TO_KWH) {
                        OutlinedButton(
                            onClick = onSaveHouseholdClick,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.BookmarkBorder, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Save this home")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AllocationRow(
    label: String,
    value: String,
    isBold: Boolean = false,
    isHighlight: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isHighlight) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (isBold || isHighlight) FontWeight.Bold else FontWeight.Normal
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isHighlight) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
            fontWeight = if (isBold || isHighlight) FontWeight.Bold else FontWeight.SemiBold
        )
    }
}
