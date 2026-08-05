package com.example.smartelectricity.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartelectricity.data.model.BlockBreakdown
import com.example.smartelectricity.data.model.VerificationStatus

@Composable
fun VerificationStatusBadge(
    status: VerificationStatus,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, icon) = when (status) {
        VerificationStatus.VERIFIED -> Triple(Color(0xFFDCFCE7), Color(0xFF15803D), Icons.Default.CheckCircle)
        VerificationStatus.OFFICIAL_PARSED -> Triple(Color(0xFFE0F2FE), Color(0xFF0369A1), Icons.Default.FactCheck)
        VerificationStatus.RECENTLY_CHANGED -> Triple(Color(0xFFE0F2FE), Color(0xFF0369A1), Icons.Default.Update)
        VerificationStatus.SCHEDULED -> Triple(Color(0xFFF1F5F9), Color(0xFF475569), Icons.Default.Schedule)
        VerificationStatus.ESTIMATE -> Triple(Color(0xFFFEF3C7), Color(0xFFB45309), Icons.Default.Warning)
        VerificationStatus.NEEDS_REVIEW -> Triple(Color(0xFFFEF3C7), Color(0xFFB45309), Icons.Default.SyncProblem)
        VerificationStatus.UNSUPPORTED -> Triple(Color(0xFFFEE2E2), Color(0xFFB91C1C), Icons.Default.Cancel)
        VerificationStatus.STALE -> Triple(Color(0xFFFEE2E2), Color(0xFFB91C1C), Icons.Default.ErrorOutline)
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(20.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = status.label,
                color = textColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun BlockVisualizer(
    blocks: List<BlockBreakdown>,
    modifier: Modifier = Modifier
) {
    if (blocks.isEmpty()) return

    val totalKwh = blocks.sumOf { it.kwhAllocated }.coerceAtLeast(1.0)

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Tariff Block Distribution",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Total: ${"%.1f".format(totalKwh)} kWh",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Horizontal Multi-Segment Progress Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            val colors = listOf(
                Color(0xFF0284C7),
                Color(0xFF0D9488),
                Color(0xFFD97706),
                Color(0xFFDC2626)
            )

            blocks.forEachIndexed { index, block ->
                val weight = (block.kwhAllocated / totalKwh).toFloat().coerceAtLeast(0.01f)
                val blockColor = colors.getOrElse(index) { Color(0xFF64748B) }

                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(weight)
                        .background(blockColor)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Block Legend Cards
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            val colors = listOf(
                Color(0xFF0284C7),
                Color(0xFF0D9488),
                Color(0xFFD97706),
                Color(0xFFDC2626)
            )

            blocks.forEachIndexed { index, block ->
                val blockColor = colors.getOrElse(index) { Color(0xFF64748B) }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surface,
                            RoundedCornerShape(8.dp)
                        )
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(blockColor)
                        )
                        Text(
                            text = "Block ${block.blockNumber} (${block.rangeText})",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Text(
                        text = "${"%.1f".format(block.kwhAllocated)} kWh @ ${"%.2f".format(block.rateCentsPerKwh)}c/kWh (R${"%.2f".format(block.costRand)})",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun QuickAmountChips(
    selectedAmount: Double,
    onAmountSelected: (Double) -> Unit,
    isRandMode: Boolean,
    modifier: Modifier = Modifier
) {
    val options = if (isRandMode) {
        listOf(50.0, 100.0, 200.0, 300.0, 500.0, 1000.0)
    } else {
        listOf(50.0, 100.0, 200.0, 350.0, 500.0)
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { value ->
            val isSelected = selectedAmount == value
            val label = if (isRandMode) "R${value.toInt()}" else "${value.toInt()} kWh"

            FilterChip(
                selected = isSelected,
                onClick = { onAmountSelected(value) },
                label = { Text(label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    }
}

@Composable
fun MonthlyBudgetProgressBarCard(
    currentSpentRand: Double,
    budgetLimitRand: Double,
    onUpdateBudgetLimit: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    var showEditDialog by remember { mutableStateOf(false) }
    var inputBudgetText by remember(budgetLimitRand) { mutableStateOf(budgetLimitRand.toInt().toString()) }

    val progressFraction = (currentSpentRand / budgetLimitRand.coerceAtLeast(1.0)).toFloat().coerceIn(0f, 1f)
    val percentageUsed = ((currentSpentRand / budgetLimitRand.coerceAtLeast(1.0)) * 100).toInt()
    val isOverBudget = currentSpentRand > budgetLimitRand
    val remainingRand = budgetLimitRand - currentSpentRand

    val progressColor = when {
        isOverBudget -> Color(0xFFDC2626) // Red
        percentageUsed >= 85 -> Color(0xFFD97706) // Amber
        percentageUsed >= 70 -> Color(0xFF0284C7) // Blue
        else -> Color(0xFF16A34A) // Green
    }

    val statusText = when {
        isOverBudget -> "Over Budget by R${"%.2f".format(currentSpentRand - budgetLimitRand)}"
        percentageUsed >= 85 -> "Near Limit (R${"%.2f".format(remainingRand)} left)"
        else -> "On Track (R${"%.2f".format(remainingRand)} remaining)"
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(progressColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isOverBudget) Icons.Default.Warning else Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = progressColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Monthly Electricity Budget",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.bodySmall,
                            color = progressColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                IconButton(onClick = { showEditDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Budget Limit",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Amounts Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "Spent this Month",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "R${"%.2f".format(currentSpentRand)}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Budget Limit",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "R${"%.2f".format(budgetLimitRand)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Visual Progress Bar
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Usage Progress",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$percentageUsed%",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = progressColor
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction = progressFraction)
                            .clip(RoundedCornerShape(6.dp))
                            .background(progressColor)
                    )
                }
            }
        }
    }

    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = {
                Text(
                    text = "Set Monthly Electricity Budget",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Enter your planned monthly electricity spend limit (Rand).",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = inputBudgetText,
                        onValueChange = { inputBudgetText = it },
                        label = { Text("Monthly Limit (Rand)") },
                        prefix = { Text("R ") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = inputBudgetText.toDoubleOrNull() ?: budgetLimitRand
                        onUpdateBudgetLimit(parsed)
                        showEditDialog = false
                    }
                ) {
                    Text("Save Budget")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
