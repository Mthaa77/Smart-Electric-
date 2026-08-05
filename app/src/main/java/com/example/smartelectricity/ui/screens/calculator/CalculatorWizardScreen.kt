package com.example.smartelectricity.ui.screens.calculator

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartelectricity.data.model.CalculationMode
import com.example.smartelectricity.data.model.MeterType
import com.example.smartelectricity.data.model.TariffProfile
import com.example.smartelectricity.data.model.isCalculationSupported
import com.example.smartelectricity.ui.CalculatorUiState
import com.example.smartelectricity.ui.components.QuickAmountChips
import com.example.smartelectricity.ui.components.VerificationStatusBadge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorWizardScreen(
    state: CalculatorUiState,
    onSelectDistributor: (com.example.smartelectricity.data.model.Distributor) -> Unit,
    onSelectProfile: (TariffProfile) -> Unit,
    onSetMode: (CalculationMode) -> Unit,
    onSetAmount: (String) -> Unit,
    onSetFirstPurchase: (Boolean) -> Unit,
    onSetHasClaimedFbe: (Boolean) -> Unit,
    onSetIsIndigent: (Boolean) -> Unit,
    onSetUnitsAlreadyAllocated: (String) -> Unit,
    onSetDaysSinceLastPurchase: (String) -> Unit,
    onSetArrears: (String) -> Unit,
    onRunCalculation: () -> Unit,
    onCalculationDone: () -> Unit,
    onBackToHome: () -> Unit
) {
    var step by remember { mutableIntStateOf(1) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Guided calculator", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                        Text("Step $step of 5 · ${getStepTitle(step)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (step > 1) step-- else onBackToHome()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (step > 1) {
                        OutlinedButton(
                            onClick = { step-- },
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("Previous")
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    if (step < 5) {
                        Button(
                            onClick = { step++ },
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("Next Step")
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    } else {
                        Button(
                            onClick = {
                                onRunCalculation()
                                onCalculationDone()
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            enabled = state.selectedProfile.isCalculationSupported &&
                                (state.amountInputStr.toDoubleOrNull() ?: 0.0) > 0.0
                        ) {
                            Icon(Icons.Default.Calculate, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Calculate Estimate", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            LinearProgressIndicator(
                progress = { step / 5f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = state.calculationMode == CalculationMode.RAND_TO_KWH,
                    onClick = { onSetMode(CalculationMode.RAND_TO_KWH) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    label = { Text("Rand → kWh", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.Payments, null, Modifier.size(16.dp)) }
                )
                SegmentedButton(
                    selected = state.calculationMode == CalculationMode.KWH_TO_RAND,
                    onClick = { onSetMode(CalculationMode.KWH_TO_RAND) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    label = { Text("kWh → Rand", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.ElectricBolt, null, Modifier.size(16.dp)) }
                )
            }

            Box(modifier = Modifier.weight(1f)) {
                when (step) {
                    1 -> Step1SupplierSelection(
                        state = state,
                        onSelectDistributor = { dist ->
                            onSelectDistributor(dist)
                            step = 2
                        }
                    )
                    2 -> Step2ProfileSelection(
                        state = state,
                        onSelectProfile = { prof ->
                            onSelectProfile(prof)
                            step = 3
                        }
                    )
                    3 -> Step3AmountEntry(
                        state = state,
                        onSetAmount = onSetAmount,
                        onSetFirstPurchase = onSetFirstPurchase,
                        onSetHasClaimedFbe = onSetHasClaimedFbe,
                        onSetUnitsAlreadyAllocated = onSetUnitsAlreadyAllocated,
                        onSetDaysSinceLastPurchase = onSetDaysSinceLastPurchase,
                        onSetArrears = onSetArrears
                    )
                    4 -> Step4FbeQuestions(
                        state = state,
                        onSetIsIndigent = onSetIsIndigent
                    )
                    5 -> Step5ReviewAssumptions(
                        state = state,
                        onEditStep = { step = it }
                    )
                }
            }
        }
    }
}

private fun getStepTitle(step: Int): String = when (step) {
    1 -> "Select Electricity Supplier"
    2 -> "Select Tariff Profile"
    3 -> "Enter Purchase Amount"
    4 -> "FBE Relief Diagnostic"
    5 -> "Review & Calculate"
    else -> ""
}

@Composable
private fun Step1SupplierSelection(
    state: CalculatorUiState,
    onSelectDistributor: (com.example.smartelectricity.data.model.Distributor) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filtered = state.availableDistributors.filter { distributor ->
        query.isBlank() || distributor.name.contains(query, ignoreCase = true) ||
            distributor.province.contains(query, ignoreCase = true) ||
            distributor.recognitionClue.contains(query, ignoreCase = true)
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Who supplies electricity to your meter or property?",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search supplier or suburb (e.g., Tshwane, Cape Town, Eskom)") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(filtered) { dist ->
                val isSelected = dist.id == state.selectedDistributor.id
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectDistributor(dist) },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                    ),
                    border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(dist.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(dist.province, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Text(
                            text = "${dist.profiles.size} Verified Tariff Schedules",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )

                        Text(
                            text = "Recognition Clue: ${dist.recognitionClue}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Step2ProfileSelection(
    state: CalculatorUiState,
    onSelectProfile: (TariffProfile) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Select your meter category or tariff profile for ${state.selectedDistributor.name}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(state.selectedDistributor.profiles) { prof ->
                val isSelected = prof.id == state.selectedProfile.id
                val isSupported = prof.isCalculationSupported
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = isSupported) { onSelectProfile(prof) },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                    ),
                    border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(prof.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            VerificationStatusBadge(status = prof.verificationStatus)
                        }

                        Text(prof.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                        if (!isSupported) {
                            Text(
                                text = if (prof.verificationStatus == com.example.smartelectricity.data.model.VerificationStatus.UNSUPPORTED) {
                                    "Calculation unavailable until the complete time-of-use calendar is imported."
                                } else {
                                    "Official schedule found, but the tariff rows still require manual verification."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (prof.monthlyFixedChargeRand > 0) "Fixed Charge: R${"%.2f".format(prof.monthlyFixedChargeRand)}/m" else "Fixed Charge: R0.00",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (prof.monthlyFixedChargeRand > 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text = "${prof.blocks.size} Tariff Blocks",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Step3AmountEntry(
    state: CalculatorUiState,
    onSetAmount: (String) -> Unit,
    onSetFirstPurchase: (Boolean) -> Unit,
    onSetHasClaimedFbe: (Boolean) -> Unit,
    onSetUnitsAlreadyAllocated: (String) -> Unit,
    onSetDaysSinceLastPurchase: (String) -> Unit,
    onSetArrears: (String) -> Unit
) {
    val isRand = state.calculationMode == CalculationMode.RAND_TO_KWH

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = if (isRand) "How much are you planning to spend?" else "How many electricity units do you need?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            OutlinedTextField(
                value = state.amountInputStr,
                onValueChange = onSetAmount,
                label = { Text(if (isRand) "Amount in Rand (R)" else "Quantity in kWh") },
                prefix = { if (isRand) Text("R ", fontWeight = FontWeight.Bold) },
                suffix = { if (!isRand) Text(" kWh", fontWeight = FontWeight.Bold) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
        }

        item {
            Text("Quick Presets:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            QuickAmountChips(
                selectedAmount = state.amountInputStr.toDoubleOrNull() ?: 0.0,
                onAmountSelected = { onSetAmount(it.toString()) },
                isRandMode = isRand
            )
        }

        item {
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            Text("Monthly Purchase Context", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Is this your 1st purchase of the month?", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            Text("If your tariff profile charges a fixed monthly network access fee, it is recovered on purchase #1.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = state.isFirstPurchaseOfMonth,
                            onCheckedChange = onSetFirstPurchase
                        )
                    }

                    Divider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Have you claimed your FBE free units this month?", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            Text("Free Basic Electricity is issued once per calendar month to registered meters.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = state.hasClaimedFbeThisMonth,
                            onCheckedChange = onSetHasClaimedFbe
                        )
                    }
                }
            }
        }

        item {
            var showAdvanced by remember { mutableStateOf(false) }
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showAdvanced = !showAdvanced },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Advanced purchase context", fontWeight = FontWeight.Bold)
                            Text(
                                if (state.activeHousehold != null) "Monthly block progress is loaded automatically for ${state.activeHousehold.nickname}."
                                else "Use receipt details for a closer match, or select a household for automatic tracking.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(if (showAdvanced) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null)
                    }

                    AnimatedVisibility(showAdvanced) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = state.unitsAlreadyAllocatedThisMonthInputStr,
                                onValueChange = onSetUnitsAlreadyAllocated,
                                label = { Text("Units already bought this month") },
                                suffix = { Text("kWh") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            )
                            if (state.selectedProfile.dailyFixedChargeRand > 0.0) {
                                OutlinedTextField(
                                    value = state.daysSinceLastPurchaseInputStr,
                                    onValueChange = onSetDaysSinceLastPurchase,
                                    label = { Text("Days since last purchase / billing") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }
                            OutlinedTextField(
                                value = state.arrearsInputStr,
                                onValueChange = onSetArrears,
                                label = { Text("Known authorised arrears recovery") },
                                prefix = { Text("R ") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                supportingText = { Text("Leave at R0 unless it appears on your municipal or vending receipt.") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Step4FbeQuestions(
    state: CalculatorUiState,
    onSetIsIndigent: (Boolean) -> Unit
) {
    val fbeConfig = state.selectedProfile.fbeConfig

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Free Basic Electricity (FBE) Relief Check", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Municipal Relief Rule:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                Text(fbeConfig.description, style = MaterialTheme.typography.bodyMedium)
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Registered as Indigent with Municipality?", fontWeight = FontWeight.Bold)
                        Text(
                            text = fbeConfig.propertyValuationCapRand?.let { "Eligibility is confirmed by your municipality; its published property cap is R${"%.0f".format(it)}." }
                                ?: "Eligibility is confirmed by your municipality for this tariff.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Checkbox(
                        checked = state.isIndigentRegistered,
                        onCheckedChange = onSetIsIndigent
                    )
                }
            }
        }
    }
}

@Composable
private fun Step5ReviewAssumptions(
    state: CalculatorUiState,
    onEditStep: (Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Review Calculation Inputs & Assumptions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    AssumptionRow(label = "Supplier", value = state.selectedDistributor.name, onEdit = { onEditStep(1) })
                    Divider()
                    AssumptionRow(label = "Tariff Profile", value = state.selectedProfile.name, onEdit = { onEditStep(2) })
                    Divider()
                    AssumptionRow(
                        label = if (state.calculationMode == CalculationMode.RAND_TO_KWH) "Purchase Amount" else "Target Units",
                        value = if (state.calculationMode == CalculationMode.RAND_TO_KWH) "R${state.amountInputStr}" else "${state.amountInputStr} kWh",
                        onEdit = { onEditStep(3) }
                    )
                    Divider()
                    AssumptionRow(
                        label = "First Monthly Purchase?",
                        value = if (state.isFirstPurchaseOfMonth) "Yes (Recover fixed fees)" else "No (Top-up)",
                        onEdit = { onEditStep(3) }
                    )
                    Divider()
                    AssumptionRow(
                        label = "FBE Eligibility Status",
                        value = if (state.isIndigentRegistered) "Registered Indigent (+50-100 kWh free)" else "Standard Household",
                        onEdit = { onEditStep(4) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AssumptionRow(
    label: String,
    value: String,
    onEdit: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        }
        TextButton(onClick = onEdit) {
            Text("Edit")
        }
    }
}
