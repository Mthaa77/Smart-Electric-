package com.example.smartelectricity.ui.screens.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.smartelectricity.data.model.Distributor
import com.example.smartelectricity.data.model.MeterType
import com.example.smartelectricity.data.model.TariffProfile
import com.example.smartelectricity.data.model.isCalculationSupported
import com.example.smartelectricity.ui.CalculatorUiState
import com.example.smartelectricity.ui.components.LiquidGlassPanel
import com.example.smartelectricity.ui.components.VerificationStatusBadge
import com.example.smartelectricity.ui.components.premiumDepth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HouseholdSetupScreen(
    state: CalculatorUiState,
    isFirstRun: Boolean,
    onSelectDistributor: (Distributor) -> Unit,
    onSelectProfile: (TariffProfile) -> Unit,
    onSetMeterType: (MeterType) -> Unit,
    onSetIndigent: (Boolean) -> Unit,
    onSetBudget: (Double) -> Unit,
    onSaveHousehold: (String, String, Double, Double) -> Unit,
    onComplete: () -> Unit,
    onSkip: () -> Unit,
    onBack: () -> Unit
) {
    var step by remember { mutableIntStateOf(0) }
    var nickname by remember { mutableStateOf("Home") }
    var municipality by remember { mutableStateOf("") }
    var propertyValue by remember { mutableStateOf("120000") }
    var historicUse by remember { mutableStateOf("350") }
    var budget by remember(state.monthlyBudgetLimitRand) {
        mutableStateOf(state.monthlyBudgetLimitRand.toInt().toString())
    }

    val totalSteps = 4
    val canContinue = when (step) {
        1 -> nickname.isNotBlank() && municipality.isNotBlank()
        2 -> state.selectedProfile.isCalculationSupported && state.meterType in state.selectedProfile.compatibleMeterTypes
        else -> true
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(if (isFirstRun) "Set up your home" else "Add another home", fontWeight = FontWeight.Black)
                        Text(
                            "${step + 1} of $totalSteps · ${setupStepTitle(step)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    if (!isFirstRun || step > 0) {
                        IconButton(onClick = { if (step > 0) step-- else onBack() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    if (isFirstRun) {
                        TextButton(onClick = onSkip) { Text("Use without saving") }
                    }
                }
            )
        },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.background) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (step > 0) {
                        OutlinedButton(
                            onClick = { step-- },
                            modifier = Modifier.height(52.dp),
                            shape = RoundedCornerShape(17.dp)
                        ) { Text("Back") }
                    }
                    Button(
                        onClick = {
                            if (step < totalSteps - 1) {
                                step++
                            } else {
                                budget.toDoubleOrNull()?.takeIf { it > 0 }?.let(onSetBudget)
                                onSaveHousehold(
                                    nickname,
                                    municipality,
                                    propertyValue.toDoubleOrNull() ?: 0.0,
                                    historicUse.toDoubleOrNull() ?: 0.0
                                )
                                onComplete()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .premiumDepth(
                                shape = RoundedCornerShape(17.dp),
                                elevation = 9.dp,
                                accentColor = MaterialTheme.colorScheme.primary
                            ),
                        enabled = canContinue,
                        shape = RoundedCornerShape(17.dp)
                    ) {
                        Text(if (step == totalSteps - 1) "Open my dashboard" else "Continue", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(7.dp))
                        Icon(if (step == totalSteps - 1) Icons.Default.Bolt else Icons.Default.ArrowForward, null, Modifier.size(18.dp))
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            LinearProgressIndicator(
                progress = { (step + 1) / totalSteps.toFloat() },
                modifier = Modifier.fillMaxWidth().height(7.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            AnimatedContent(
                targetState = step,
                modifier = Modifier.weight(1f),
                transitionSpec = {
                    androidx.compose.animation.fadeIn(tween(220)) togetherWith
                        androidx.compose.animation.fadeOut(tween(120))
                },
                label = "household-setup-step"
            ) { currentStep ->
                when (currentStep) {
                    0 -> WelcomeStep()
                    1 -> HomeDetailsStep(
                        nickname = nickname,
                        municipality = municipality,
                        meterType = state.meterType,
                        onNicknameChange = { nickname = it },
                        onMunicipalityChange = { municipality = it },
                        onMeterTypeChange = onSetMeterType
                    )
                    2 -> TariffMatchStep(
                        state = state,
                        onSelectDistributor = onSelectDistributor,
                        onSelectProfile = onSelectProfile
                    )
                    else -> PreferencesStep(
                        state = state,
                        budget = budget,
                        propertyValue = propertyValue,
                        historicUse = historicUse,
                        onBudgetChange = { budget = it.decimalOnly() },
                        onPropertyValueChange = { propertyValue = it.decimalOnly() },
                        onHistoricUseChange = { historicUse = it.decimalOnly() },
                        onSetIndigent = onSetIndigent
                    )
                }
            }
        }
    }
}

private fun setupStepTitle(step: Int) = when (step) {
    0 -> "Welcome"
    1 -> "Your home"
    2 -> "Confirm tariff"
    else -> "Personalise"
}

private fun String.decimalOnly(): String = filter { it.isDigit() || it == '.' }

@Composable
private fun WelcomeStep() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        LiquidGlassPanel(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            accentColor = MaterialTheme.colorScheme.primary,
            elevation = 18.dp
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    shadowElevation = 10.dp
                ) {
                    Icon(Icons.Default.ElectricBolt, null, Modifier.padding(20.dp).size(42.dp), MaterialTheme.colorScheme.onPrimary)
                }
                Text(
                    "Electricity that understands your home.",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )
                Text(
                    "Confirm your supplier and tariff once. Smart Electric will remember your monthly blocks, free units and budget for every future estimate.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SetupBenefit("Faster top-ups", Icons.Default.Speed)
                    SetupBenefit("Clearer costs", Icons.Default.ReceiptLong)
                    SetupBenefit("Trusted rates", Icons.Default.Verified)
                }
            }
        }
    }
}

@Composable
private fun SetupBenefit(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.62f)) {
        Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, Modifier.size(20.dp), MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(5.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun HomeDetailsStep(
    nickname: String,
    municipality: String,
    meterType: MeterType,
    onNicknameChange: (String) -> Unit,
    onMunicipalityChange: (String) -> Unit,
    onMeterTypeChange: (MeterType) -> Unit
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp), contentPadding = PaddingValues(bottom = 20.dp)) {
        item {
            Text("Tell us which property this is.", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
            Text("This keeps estimates and purchase history separate for each meter.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            OutlinedTextField(
                value = nickname,
                onValueChange = onNicknameChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Home name") },
                placeholder = { Text("Home, Rental, Shop…") },
                leadingIcon = { Icon(Icons.Default.Home, null) },
                singleLine = true,
                shape = RoundedCornerShape(17.dp)
            )
        }
        item {
            OutlinedTextField(
                value = municipality,
                onValueChange = onMunicipalityChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Suburb or municipality") },
                placeholder = { Text("e.g. Soshanguve, City of Tshwane") },
                leadingIcon = { Icon(Icons.Default.LocationOn, null) },
                singleLine = true,
                shape = RoundedCornerShape(17.dp)
            )
        }
        item { Text("Meter type", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        items(MeterType.entries) { type ->
            val selected = type == meterType
            Surface(
                modifier = Modifier.fillMaxWidth().clickable { onMeterTypeChange(type) },
                shape = RoundedCornerShape(18.dp),
                color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (type == MeterType.CREDIT) Icons.Default.Receipt else Icons.Default.ElectricMeter, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Text(type.displayName, Modifier.weight(1f), fontWeight = FontWeight.Bold)
                    RadioButton(selected = selected, onClick = { onMeterTypeChange(type) })
                }
            }
        }
    }
}

@Composable
private fun TariffMatchStep(
    state: CalculatorUiState,
    onSelectDistributor: (Distributor) -> Unit,
    onSelectProfile: (TariffProfile) -> Unit
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 20.dp)) {
        item {
            Text("Confirm who supplies your electricity.", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
            Text("Use the recognition clue if your municipality appears on a bill or token slip.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        items(state.availableDistributors) { distributor ->
            val selected = distributor.id == state.selectedDistributor.id
            Surface(
                modifier = Modifier.fillMaxWidth().clickable { onSelectDistributor(distributor) },
                shape = RoundedCornerShape(18.dp),
                color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(distributor.name, fontWeight = FontWeight.Bold)
                        Text(distributor.province, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(distributor.recognitionClue, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            Spacer(Modifier.height(4.dp))
            Text("Tariff match", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
        }
        items(state.selectedDistributor.profiles.filter { state.meterType in it.compatibleMeterTypes }) { profile ->
            val selected = profile.id == state.selectedProfile.id
            val supported = profile.isCalculationSupported
            Surface(
                modifier = Modifier.fillMaxWidth().clickable(enabled = supported) { onSelectProfile(profile) },
                shape = RoundedCornerShape(18.dp),
                color = if (selected) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.68f) else MaterialTheme.colorScheme.surface,
                border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(profile.name, Modifier.weight(1f), fontWeight = FontWeight.Bold)
                        VerificationStatusBadge(profile.verificationStatus)
                    }
                    Text(profile.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        if (supported) "Effective ${profile.effectiveDateStr}" else "Calculation not available for this schedule yet",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (supported) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun PreferencesStep(
    state: CalculatorUiState,
    budget: String,
    propertyValue: String,
    historicUse: String,
    onBudgetChange: (String) -> Unit,
    onPropertyValueChange: (String) -> Unit,
    onHistoricUseChange: (String) -> Unit,
    onSetIndigent: (Boolean) -> Unit
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp), contentPadding = PaddingValues(bottom = 20.dp)) {
        item {
            Text("Personalise your guidance.", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
            Text("These details improve budget forecasts and FBE checks. You can change them later.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            OutlinedTextField(
                value = budget,
                onValueChange = onBudgetChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Monthly electricity target") },
                prefix = { Text("R ") },
                leadingIcon = { Icon(Icons.Default.Savings, null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                shape = RoundedCornerShape(17.dp)
            )
        }
        item {
            OutlinedTextField(
                value = historicUse,
                onValueChange = onHistoricUseChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Typical monthly use") },
                suffix = { Text("kWh") },
                supportingText = { Text("Use a recent bill or your best estimate.") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                shape = RoundedCornerShape(17.dp)
            )
        }
        if (state.selectedProfile.fbeConfig.isAvailable) {
            item {
                LiquidGlassPanel(
                    modifier = Modifier.fillMaxWidth(),
                    accentColor = MaterialTheme.colorScheme.secondary,
                    elevation = 8.dp
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.VolunteerActivism, null, tint = MaterialTheme.colorScheme.secondary)
                            Spacer(Modifier.width(10.dp))
                            Text("Free Basic Electricity check", fontWeight = FontWeight.Black)
                        }
                        Text(state.selectedProfile.fbeConfig.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        OutlinedTextField(
                            value = propertyValue,
                            onValueChange = onPropertyValueChange,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Municipal property value") },
                            prefix = { Text("R ") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            shape = RoundedCornerShape(15.dp)
                        )
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Registered as indigent?", fontWeight = FontWeight.Bold)
                                Text("Your municipality makes the final eligibility decision.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(checked = state.isIndigentRegistered, onCheckedChange = onSetIndigent)
                        }
                    }
                }
            }
        }
        item {
            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Default.Lock, null, Modifier.size(19.dp), MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(9.dp))
                    Text("Saved locally on this device. Smart Electric never treats an estimate as an official bill.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
