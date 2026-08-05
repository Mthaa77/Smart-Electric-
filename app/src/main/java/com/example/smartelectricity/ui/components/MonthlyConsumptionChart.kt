package com.example.smartelectricity.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartelectricity.data.model.TariffProfile

data class MonthlyConsumptionData(
    val monthLabel: String,
    val kwhValue: Double,
    val randValue: Double,
    val isCurrentMonth: Boolean = false,
    val isHighDemandSeason: Boolean = false,
    val notes: String = ""
)

enum class ChartMetric {
    KWH_UNITS,
    RAND_SPEND
}

@Composable
fun MonthlyConsumptionChartCard(
    monthlyData: List<MonthlyConsumptionData> = default6MonthData(),
    selectedProfile: TariffProfile? = null,
    modifier: Modifier = Modifier
) {
    var chartMetric by remember { mutableStateOf(ChartMetric.KWH_UNITS) }
    var selectedMonthIndex by remember { mutableStateOf<Int?>(null) }

    val totalKwh = monthlyData.sumOf { it.kwhValue }
    val totalRand = monthlyData.sumOf { it.randValue }
    val averageKwh = if (monthlyData.isNotEmpty()) totalKwh / monthlyData.size else 0.0
    val averageRand = if (monthlyData.isNotEmpty()) totalRand / monthlyData.size else 0.0

    val maxVal = when (chartMetric) {
        ChartMetric.KWH_UNITS -> (monthlyData.maxOfOrNull { it.kwhValue } ?: 100.0) * 1.15
        ChartMetric.RAND_SPEND -> (monthlyData.maxOfOrNull { it.randValue } ?: 500.0) * 1.15
    }

    val prevMonthValue = if (monthlyData.size >= 2) {
        if (chartMetric == ChartMetric.KWH_UNITS) monthlyData[monthlyData.size - 2].kwhValue else monthlyData[monthlyData.size - 2].randValue
    } else 0.0

    val currentMonthValue = if (monthlyData.isNotEmpty()) {
        if (chartMetric == ChartMetric.KWH_UNITS) monthlyData.last().kwhValue else monthlyData.last().randValue
    } else 0.0

    val pctChange = if (prevMonthValue > 0) {
        ((currentMonthValue - prevMonthValue) / prevMonthValue) * 100.0
    } else 0.0

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = "Consumption Bar Chart",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "6-Month Consumption Trend",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Monthly electricity history & seasonal patterns",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Toggle Button Group (kWh vs Rand)
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.height(36.dp)
                ) {
                    SegmentedButton(
                        selected = chartMetric == ChartMetric.KWH_UNITS,
                        onClick = { chartMetric = ChartMetric.KWH_UNITS },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        modifier = Modifier.testTag("kwh_toggle")
                    ) {
                        Text("kWh", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                    SegmentedButton(
                        selected = chartMetric == ChartMetric.RAND_SPEND,
                        onClick = { chartMetric = ChartMetric.RAND_SPEND },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        modifier = Modifier.testTag("rand_toggle")
                    ) {
                        Text("Rand", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Overview Summary Metrics Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (chartMetric == ChartMetric.KWH_UNITS) "6-Month Total Units" else "6-Month Total Spend",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (chartMetric == ChartMetric.KWH_UNITS) "${"%.0f".format(totalKwh)} kWh" else "R${"%.2f".format(totalRand)}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Monthly Average",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (chartMetric == ChartMetric.KWH_UNITS) "${"%.0f".format(averageKwh)} kWh/m" else "R${"%.0f".format(averageRand)}/m",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "MoM Trend",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(
                            imageVector = if (pctChange >= 0) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                            contentDescription = null,
                            tint = if (pctChange > 0) Color(0xFFDC2626) else Color(0xFF16A34A),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "${"%.1f".format(kotlin.math.abs(pctChange))}%",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (pctChange > 0) Color(0xFFDC2626) else Color(0xFF16A34A)
                        )
                    }
                }
            }

            // Legend Indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(modifier = Modifier.size(12.dp).clip(RoundedCornerShape(3.dp)).background(MaterialTheme.colorScheme.primary))
                    Text("Standard Rate", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(modifier = Modifier.size(12.dp).clip(RoundedCornerShape(3.dp)).background(Color(0xFFEA580C)))
                    Text("Winter Peak Season", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(modifier = Modifier.size(12.dp).clip(RoundedCornerShape(3.dp)).background(Color(0xFF16A34A)))
                    Text("Current Month", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Interactive Bar Chart Layout
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(top = 8.dp)
            ) {
                val primaryColor = MaterialTheme.colorScheme.primary
                val peakColor = Color(0xFFEA580C)
                val currentColor = Color(0xFF16A34A)
                val gridLineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                val avgLineColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f)

                val activeAvgValue = if (chartMetric == ChartMetric.KWH_UNITS) averageKwh else averageRand

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(monthlyData, chartMetric) {
                            detectTapGestures { offset ->
                                val barWidthSpace = size.width / monthlyData.size
                                val tappedIndex = (offset.x / barWidthSpace).toInt().coerceIn(0, monthlyData.size - 1)
                                selectedMonthIndex = if (selectedMonthIndex == tappedIndex) null else tappedIndex
                            }
                        }
                ) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height - 30.dp.toPx() // leave room for bottom month labels
                    val numBars = monthlyData.size
                    val spacing = 16.dp.toPx()
                    val totalSpacing = spacing * (numBars + 1)
                    val barWidth = (canvasWidth - totalSpacing) / numBars

                    // Draw Horizontal Grid Lines (0%, 50%, 100%)
                    for (i in 0..2) {
                        val y = canvasHeight * (i / 2f)
                        drawLine(
                            color = gridLineColor,
                            start = Offset(0f, y),
                            end = Offset(canvasWidth, y),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                        )
                    }

                    // Draw Average Reference Line
                    val avgY = canvasHeight - ((activeAvgValue / maxVal) * canvasHeight).toFloat()
                    if (avgY in 0f..canvasHeight) {
                        drawLine(
                            color = avgLineColor,
                            start = Offset(0f, avgY),
                            end = Offset(canvasWidth, avgY),
                            strokeWidth = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 8f))
                        )
                    }

                    // Draw Bars
                    monthlyData.forEachIndexed { index, data ->
                        val value = if (chartMetric == ChartMetric.KWH_UNITS) data.kwhValue else data.randValue
                        val barHeight = ((value / maxVal) * canvasHeight).toFloat().coerceAtLeast(8.dp.toPx())

                        val left = spacing + index * (barWidth + spacing)
                        val top = canvasHeight - barHeight

                        val barBrush = when {
                            data.isCurrentMonth -> Brush.verticalGradient(listOf(currentColor, currentColor.copy(alpha = 0.7f)))
                            data.isHighDemandSeason -> Brush.verticalGradient(listOf(peakColor, peakColor.copy(alpha = 0.7f)))
                            else -> Brush.verticalGradient(listOf(primaryColor, primaryColor.copy(alpha = 0.6f)))
                        }

                        // Highlight border if selected
                        val isSelected = selectedMonthIndex == index

                        drawRoundRect(
                            brush = barBrush,
                            topLeft = Offset(left, top),
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                        )

                        if (isSelected) {
                            drawRoundRect(
                                color = Color.White,
                                topLeft = Offset(left, top),
                                size = Size(barWidth, barHeight),
                                cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
                                style = Stroke(width = 3.dp.toPx())
                            )
                        }
                    }
                }

                // Month Labels Row below Canvas
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .height(24.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    monthlyData.forEachIndexed { index, data ->
                        val isSelected = selectedMonthIndex == index
                        Text(
                            text = data.monthLabel.take(3),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected || data.isCurrentMonth) FontWeight.ExtraBold else FontWeight.SemiBold,
                            color = if (isSelected) MaterialTheme.colorScheme.primary
                            else if (data.isCurrentMonth) Color(0xFF16A34A)
                            else if (data.isHighDemandSeason) Color(0xFFEA580C)
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                            modifier = Modifier.clickable {
                                selectedMonthIndex = if (selectedMonthIndex == index) null else index
                            }
                        )
                    }
                }
            }

            // Interactive Tooltip Card when a bar is selected
            selectedMonthIndex?.let { idx ->
                val selectedData = monthlyData.getOrNull(idx)
                if (selectedData != null) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = selectedData.monthLabel,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (selectedData.isHighDemandSeason) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFFEA580C).copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                text = "Winter Peak",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFFC2410C),
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = if (selectedData.notes.isNotBlank()) selectedData.notes else "Tap other months to compare history.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${"%.0f".format(selectedData.kwhValue)} kWh",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "R${"%.2f".format(selectedData.randValue)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // High Demand Season Tip Card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFFEF3C7))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = Color(0xFFD97706),
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "South African high-demand winter tariffs apply from 1 June – 31 August. Usage spikes in these months carry higher seasonal rates on TOU and municipal accounts.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF92400E),
                    fontSize = 11.sp
                )
            }
        }
    }
}

private fun default6MonthData(): List<MonthlyConsumptionData> {
    return listOf(
        MonthlyConsumptionData("Mar 2026", 380.0, 1310.0, isHighDemandSeason = false, notes = "Baseline autumn consumption"),
        MonthlyConsumptionData("Apr 2026", 410.0, 1420.0, isHighDemandSeason = false, notes = "Eskom retail tariff adjustment started (+8.76%)"),
        MonthlyConsumptionData("May 2026", 460.0, 1610.0, isHighDemandSeason = false, notes = "Moderate heating usage"),
        MonthlyConsumptionData("Jun 2026", 580.0, 2180.0, isHighDemandSeason = true, notes = "High-demand winter tariff started"),
        MonthlyConsumptionData("Jul 2026", 620.0, 2390.0, isHighDemandSeason = true, notes = "Peak winter heating month"),
        MonthlyConsumptionData("Aug 2026", 490.0, 1850.0, isCurrentMonth = true, isHighDemandSeason = true, notes = "Current month progress")
    )
}
