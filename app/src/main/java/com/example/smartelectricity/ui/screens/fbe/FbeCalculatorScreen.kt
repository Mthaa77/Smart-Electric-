package com.example.smartelectricity.ui.screens.fbe

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartelectricity.data.model.Distributor
import com.example.smartelectricity.data.repository.TariffRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FbeCalculatorScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val distributors = TariffRepository.distributors
    var selectedDistributor by remember { mutableStateOf(distributors.first()) }
    var householdSize by remember { mutableStateOf(4) } // Default 4 members
    var isIndigentRegistered by remember { mutableStateOf(true) }
    var propertyValuationRand by remember { mutableStateOf(120000.0) } // Default R120,000
    var estimatedMonthlySpendRand by remember { mutableStateOf(400.0) }

    // FBE Calculation Logic
    val profile = selectedDistributor.profiles.firstOrNull()
    val fbeConfig = profile?.fbeConfig
    val block1RateCents = profile?.blocks?.firstOrNull()?.rateCentsPerKwh ?: 250.0
    val block1RateRand = block1RateCents / 100.0

    // Dynamic Free Basic Electricity kWh determination based on municipality rules
    val freeKwhPerMonth = when (selectedDistributor.id) {
        "TSHWANE" -> if (isIndigentRegistered) 100.0 else 50.0
        "CAPE_TOWN" -> if (propertyValuationRand <= 500000.0) 60.0 else 0.0
        "ETHEKWINI" -> if (isIndigentRegistered) 65.0 else 0.0
        "JHB_CITY_POWER" -> if (isIndigentRegistered) 100.0 else 0.0
        else -> if (isIndigentRegistered || propertyValuationRand <= 150000.0) 50.0 else 0.0
    }

    val monthlyRandSavings = freeKwhPerMonth * block1RateRand
    val annualRandSavings = monthlyRandSavings * 12.0
    val estimatedKwhConsumption = estimatedMonthlySpendRand / block1RateRand
    val coveragePercentage = if (estimatedKwhConsumption > 0) {
        ((freeKwhPerMonth / estimatedKwhConsumption) * 100).coerceAtMost(100.0)
    } else 0.0

    val isEligible = freeKwhPerMonth > 0

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Free Basic Electricity Tool",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Estimate SA municipal FBE savings & eligibility",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Hero Savings Summary Banner
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isEligible) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (isEligible) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        else MaterialTheme.colorScheme.outlineVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
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
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isEligible) Color(0xFF16A34A) else Color(0xFFD97706)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isEligible) Icons.Default.VolunteerActivism else Icons.Default.Info,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Column {
                                    Text(
                                        text = if (isEligible) "Qualifies for Free Units!" else "Action Required for FBE",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = selectedDistributor.name,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (isEligible) Color(0xFFDCFCE7) else Color(0xFFFEF3C7)
                            ) {
                                Text(
                                    text = if (isEligible) "ELIGIBLE" else "CHECK STATUS",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isEligible) Color(0xFF15803D) else Color(0xFFB45309),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }

                        // Hero Savings Highlight
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Column {
                                Text(
                                    text = "Estimated Free Monthly Allowance",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(
                                    verticalAlignment = Alignment.Bottom,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "${freeKwhPerMonth.toInt()} kWh",
                                        style = MaterialTheme.typography.displayMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "/ month",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Rand Value Savings",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "R${"%.2f".format(monthlyRandSavings)}",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF16A34A)
                                )
                                Text(
                                    text = "R${"%.0f".format(annualRandSavings)} / year",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Coverage Progress Bar
                        if (isEligible && estimatedMonthlySpendRand > 0) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "FBE Bill Coverage",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "${coveragePercentage.toInt()}% of monthly usage",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                LinearProgressIndicator(
                                    progress = (coveragePercentage / 100.0).toFloat().coerceIn(0f, 1f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(10.dp)
                                        .clip(RoundedCornerShape(5.dp)),
                                    color = Color(0xFF16A34A),
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Interactive Input Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        Text(
                            text = "Household & Municipality Parameters",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        // Municipality Selector Dropdown/Chips
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Select Municipality / Distributor",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                distributors.take(3).forEach { dist ->
                                    val isSelected = dist.id == selectedDistributor.id
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                                            )
                                            .clickable { selectedDistributor = dist }
                                            .padding(vertical = 10.dp, horizontal = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = dist.name.split(" ").firstOrNull() ?: dist.name,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }

                        // Registered Indigent Status Switch
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Registered Indigent Household",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Registered on municipal indigent support register",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = isIndigentRegistered,
                                    onCheckedChange = { isIndigentRegistered = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = MaterialTheme.colorScheme.primary
                                    )
                                )
                            }
                        }

                        // Household Size Slider
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Household Occupants",
                                    style = MaterialTheme.typography.labelLarge
                                )
                                Text(
                                    text = "$householdSize persons",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Slider(
                                value = householdSize.toFloat(),
                                onValueChange = { householdSize = it.toInt() },
                                valueRange = 1f..10f,
                                steps = 8,
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }

                        // Property Valuation Slider
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Municipal Property Valuation",
                                    style = MaterialTheme.typography.labelLarge
                                )
                                Text(
                                    text = "R${"%.0f".format(propertyValuationRand)}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Slider(
                                value = propertyValuationRand.toFloat(),
                                onValueChange = { propertyValuationRand = it.toDouble() },
                                valueRange = 50000f..750000f,
                                steps = 14,
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }

                        // Estimated Monthly Spend Slider
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Estimated Monthly Spend (Rand)",
                                    style = MaterialTheme.typography.labelLarge
                                )
                                Text(
                                    text = "R${estimatedMonthlySpendRand.toInt()}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Slider(
                                value = estimatedMonthlySpendRand.toFloat(),
                                onValueChange = { estimatedMonthlySpendRand = it.toDouble() },
                                valueRange = 100f..2000f,
                                steps = 18
                            )
                        }
                    }
                }
            }

            // Step-by-Step Municipal Application Guide Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "How to Claim Free Basic Electricity",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            FbeStepItem(
                                stepNumber = "1",
                                title = "Check Municipal Qualification Criteria",
                                description = "Most SA municipalities require property valuation below R150,000 OR combined household income under R5,000/month."
                            )
                            FbeStepItem(
                                stepNumber = "2",
                                title = "Gather Required Documents",
                                description = "Copy of SA ID, 3-month proof of income / affidavit, municipal account statement & meter number."
                            )
                            FbeStepItem(
                                stepNumber = "3",
                                title = "Submit Indigent Application Form",
                                description = "Visit your local municipal customer care centre or apply via Eskom Indigent Registration desk."
                            )
                            FbeStepItem(
                                stepNumber = "4",
                                title = "Claim Monthly Free Token",
                                description = "Once approved, receive your 50-100 free kWh automatically on your first monthly purchase or via *120*321#."
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FbeStepItem(
    stepNumber: String,
    title: String,
    description: String
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stepNumber,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }

        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
