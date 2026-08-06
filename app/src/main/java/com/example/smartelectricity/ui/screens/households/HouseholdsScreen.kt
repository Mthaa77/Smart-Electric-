package com.example.smartelectricity.ui.screens.households

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.smartelectricity.data.db.HouseholdEntity
import com.example.smartelectricity.data.repository.TariffRepository
import com.example.smartelectricity.ui.components.LiquidGlassPanel
import com.example.smartelectricity.ui.components.VerificationStatusBadge
import com.example.smartelectricity.ui.components.premiumDepth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HouseholdsScreen(
    households: List<HouseholdEntity>,
    activeHousehold: HouseholdEntity?,
    onSelectHousehold: (HouseholdEntity) -> Unit,
    onAddHousehold: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Homes and meters", fontWeight = FontWeight.Black)
                        Text("Tap a home to make it active", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onAddHousehold) {
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
                    Text("No homes saved yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Add a home to connect estimates, purchases, tariff blocks and updates.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onAddHousehold,
                        modifier = Modifier.premiumDepth(
                            shape = RoundedCornerShape(16.dp),
                            elevation = 9.dp,
                            accentColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
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

                        LiquidGlassPanel(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectHousehold(hh) },
                            shape = RoundedCornerShape(12.dp),
                            accentColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                            elevation = if (isSelected) 13.dp else 7.dp
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
                                    Text(hh.nickname, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
                                        profile?.let { VerificationStatusBadge(it.verificationStatus) }
                                        if (isSelected) Badge { Text("Active") }
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

        }
    }
}
