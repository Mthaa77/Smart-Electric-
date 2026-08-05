package com.example.smartelectricity.ui.screens.households

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.smartelectricity.data.db.HouseholdEntity
import com.example.smartelectricity.data.repository.TariffRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HouseholdsScreen(
    households: List<HouseholdEntity>,
    activeHousehold: HouseholdEntity?,
    onSelectHousehold: (HouseholdEntity) -> Unit,
    onSaveHousehold: (String, String, Double, Double) -> Unit,
    onBack: () -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var nickname by remember { mutableStateOf("") }
    var suburb by remember { mutableStateOf("") }
    var propertyValue by remember { mutableStateOf("120000") }
    var historicAverageKwh by remember { mutableStateOf("350") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Saved Households", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Household")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            if (households.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.HomeWork,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No Saved Households Yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Save your household tariff settings to track history and receive tariff alerts.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Household Profile")
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    items(households) { hh ->
                        val isSelected = hh.id == activeHousehold?.id
                        val distributor = TariffRepository.getDistributorById(hh.distributorId)
                        val profile = TariffRepository.getTariffProfileById(hh.tariffProfileId)

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectHousehold(hh) },
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
                                    Text(hh.nickname, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    if (isSelected) {
                                        Badge { Text("Active") }
                                    }
                                }

                                Text("Supplier: ${distributor?.name ?: hh.distributorId}", style = MaterialTheme.typography.bodyMedium)
                                Text("Tariff: ${profile?.name ?: hh.tariffProfileId}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Location: ${hh.suburbOrMunicipality.ifBlank { "Not set" }}", style = MaterialTheme.typography.labelSmall)
                                Text("Historic average: ${"%.0f".format(hh.estimatedMonthlyKwh)} kWh/month", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }

            if (showAddDialog) {
                AlertDialog(
                    onDismissRequest = { showAddDialog = false },
                    title = { Text("Save Household Profile") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = nickname,
                                onValueChange = { nickname = it },
                                label = { Text("Household Name (e.g. Home, Rental)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = suburb,
                                onValueChange = { suburb = it },
                                label = { Text("Suburb / Municipality") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = propertyValue,
                                onValueChange = { propertyValue = it.filter { char -> char.isDigit() || char == '.' } },
                                label = { Text("Municipal property value") },
                                prefix = { Text("R ") },
                                supportingText = { Text("Used only for tariff and FBE eligibility checks.") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = historicAverageKwh,
                                onValueChange = { historicAverageKwh = it.filter { char -> char.isDigit() || char == '.' } },
                                label = { Text("Historic average monthly use") },
                                suffix = { Text("kWh") },
                                supportingText = { Text("Find this on recent bills, or enter your best estimate.") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                onSaveHousehold(
                                    nickname,
                                    suburb,
                                    propertyValue.toDoubleOrNull() ?: 0.0,
                                    historicAverageKwh.toDoubleOrNull() ?: 0.0
                                )
                                showAddDialog = false
                            }
                        ) {
                            Text("Save Profile")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showAddDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}
