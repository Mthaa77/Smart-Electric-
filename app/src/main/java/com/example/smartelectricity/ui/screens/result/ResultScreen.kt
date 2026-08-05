package com.example.smartelectricity.ui.screens.result

import android.content.Intent
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
import com.example.smartelectricity.ui.components.VerificationStatusBadge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    result: CalculationResult?,
    onOpenReconcile: () -> Unit,
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

    var showMathBreakdown by remember { mutableStateOf(true) }
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
            // Hero Overview Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
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
                        } else {
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
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Money & Units Allocation Breakdown", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

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
                            Text("Step-by-Step Calculation Math", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
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
                    }
                }
            }

            // Quick Actions
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onOpenReconcile,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(Icons.Default.Receipt, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Why did I get fewer units? (Reconcile Token)")
                    }

                    OutlinedButton(
                        onClick = onSaveHouseholdClick,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.BookmarkBorder, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save to Household Profile")
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
