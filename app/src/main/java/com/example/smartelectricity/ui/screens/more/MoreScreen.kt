package com.example.smartelectricity.ui.screens.more

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.smartelectricity.data.model.TariffProfile
import com.example.smartelectricity.ui.components.LiquidGlassPanel
import com.example.smartelectricity.ui.components.VerificationStatusBadge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(
    activeHouseholdName: String?,
    activeProfile: TariffProfile,
    unreadAlertCount: Int,
    onOpenHouseholds: () -> Unit,
    onOpenTariffSources: () -> Unit,
    onOpenFbe: () -> Unit,
    onOpenConcepts: () -> Unit,
    onOpenAlerts: () -> Unit,
    onOpenHistory: () -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("More", fontWeight = FontWeight.Black)
                        Text("Your home, tariff and support", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                LiquidGlassPanel(
                    modifier = Modifier.fillMaxWidth(),
                    accentColor = MaterialTheme.colorScheme.primary,
                    elevation = 12.dp
                ) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("ACTIVE HOME", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black)
                                Text(activeHouseholdName ?: "One-off mode", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                            }
                            VerificationStatusBadge(activeProfile.verificationStatus)
                        }
                        Text(activeProfile.name, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        TextButton(onClick = onOpenHouseholds, contentPadding = PaddingValues(0.dp)) {
                            Text(if (activeHouseholdName == null) "Set up a home" else "Manage homes")
                            Icon(Icons.Default.ChevronRight, null)
                        }
                    }
                }
            }

            item { MoreSectionTitle("MY ELECTRICITY") }
            item {
                MoreGroup {
                    MoreRow(Icons.Default.HomeWork, "Homes and meters", "Switch or add a property", onOpenHouseholds)
                    HorizontalDivider()
                    MoreRow(Icons.Default.FactCheck, "Tariff sources", "Official schedules and effective dates", onOpenTariffSources)
                    HorizontalDivider()
                    MoreRow(Icons.Default.VolunteerActivism, "FBE guide", "Understand free basic electricity", onOpenFbe)
                }
            }

            item { MoreSectionTitle("ACTIVITY AND UPDATES") }
            item {
                MoreGroup {
                    MoreRow(Icons.Default.History, "Calculation history", "Previous estimates and recorded purchases", onOpenHistory)
                    HorizontalDivider()
                    MoreRow(
                        Icons.Default.NotificationsActive,
                        "Tariff updates",
                        if (unreadAlertCount > 0) "$unreadAlertCount unread update${if (unreadAlertCount == 1) "" else "s"}" else "No unread updates",
                        onOpenAlerts,
                        badge = unreadAlertCount.takeIf { it > 0 }?.toString()
                    )
                }
            }

            item { MoreSectionTitle("LEARN") }
            item {
                MoreGroup {
                    MoreRow(Icons.Default.School, "Electricity concepts", "Tariff blocks, VAT, deductions and meters", onOpenConcepts)
                }
            }

            item {
                Text(
                    "Smart Electric provides planning estimates. Your supplier's official bill or vending result remains authoritative.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun MoreSectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black)
}

@Composable
private fun MoreGroup(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 5.dp
    ) {
        Column(content = content)
    }
}

@Composable
private fun MoreRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    badge: String? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(shape = RoundedCornerShape(13.dp), color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.68f)) {
            Icon(icon, null, Modifier.padding(9.dp).size(21.dp), MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (badge != null) Badge { Text(badge) } else Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
