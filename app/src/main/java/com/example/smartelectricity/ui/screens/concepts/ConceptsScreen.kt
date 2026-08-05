package com.example.smartelectricity.ui.screens.concepts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConceptsScreen(
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Understand Your Electricity", fontWeight = FontWeight.Bold) },
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
                ConceptCard(
                    title = "Inclining Block Tariffs (IBT)",
                    subtitle = "Why electricity gets more expensive as you buy more in a month",
                    body = "South African municipalities use Inclining Block Tariffs. Block 1 (e.g., 0-100 kWh) is cheapest. Once you buy more than 100 kWh in the same calendar month, your next purchase enters Block 2 (higher c/kWh rate). On the 1st day of every month, your usage block resets back to Block 1."
                )
            }

            item {
                ConceptCard(
                    title = "Free Basic Electricity (FBE)",
                    subtitle = "Government subsidized free units for qualifying households",
                    body = "Under national policy, certified indigent households or low-usage consumers receive 50 kWh to 100 kWh of free electricity every month. FBE must be claimed on your 1st token purchase of the month. Unclaimed FBE units normally expire at month end."
                )
            }

            item {
                ConceptCard(
                    title = "Monthly Fixed Network Access Charges",
                    subtitle = "Daily service fees recovered on prepaid meters",
                    body = "Certain distributors (such as City of Cape Town Home User or Johannesburg City Power) charge a fixed monthly fee (e.g. R235.80/m) to maintain grid infrastructure. When you make your 1st purchase of the month, the vending system auto-deducts this fixed fee first, converting only the remaining balance to energy kWh."
                )
            }

            item {
                ConceptCard(
                    title = "How to Read Your Token Receipt Slip",
                    subtitle = "Identifying meter numbers, 20-digit token PINs, and fee deductions",
                    body = "Every prepaid token receipt displays:\n1. 20-Digit Token Key (e.g., 1234-5678-9012-3456-7890)\n2. Meter Serial Number (11 or 14 digits)\n3. Tariff Code & Block Rate\n4. Gross Rand Amount & VAT (15%)\n5. Debt / Arrears Deductions (if applicable)"
                )
            }
        }
    }
}

@Composable
private fun ConceptCard(
    title: String,
    subtitle: String,
    body: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MenuBook,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            Text(subtitle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)

            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
