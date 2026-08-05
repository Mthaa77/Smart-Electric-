package com.example.smartelectricity.ui.screens.reconcile

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.smartelectricity.ui.CalculatorUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReconciliationScreen(
    state: CalculatorUiState,
    onSetActualUnits: (String) -> Unit,
    onRunReconciliation: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Token Receipt Reconciliation", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Why did you get fewer (or more) units than expected?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Enter the exact number of token kWh printed on your receipt slip to run a step-by-step diagnostic.", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        state.activeResult?.let { res ->
                            Text("Active Estimate Context:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${res.distributor.name} (${res.profile.name})", fontWeight = FontWeight.Bold)
                            Text("Purchase: R${"%.2f".format(res.grossPurchaseRand)} → Expected Output: ${"%.1f".format(res.totalKwh)} kWh", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }

                        OutlinedTextField(
                            value = state.actualUnitsInputStr,
                            onValueChange = onSetActualUnits,
                            label = { Text("Actual Token Units Printed on Slip (kWh)") },
                            suffix = { Text("kWh") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Button(
                            onClick = onRunReconciliation,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            enabled = state.actualUnitsInputStr.isNotBlank()
                        ) {
                            Icon(Icons.Default.FindInPage, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Run Discrepancy Diagnosis", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            state.activeReconciliation?.let { recon ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (recon.isWithinNormalTolerance) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.tertiaryContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(recon.primaryDiagnosis, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Expected: ${"%.1f".format(recon.expectedUnits)} kWh")
                                Text("Actual: ${"%.1f".format(recon.actualUnitsReceived)} kWh")
                                Text("Diff: ${"%.1f".format(recon.differenceKwh)} kWh", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                item {
                    Text("Ranked Potential Causes:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                items(recon.detailedCauses) { causeText ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(causeText, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}
