package com.example.smartelectricity.ui.screens.sources

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.smartelectricity.data.model.Distributor
import com.example.smartelectricity.ui.components.VerificationStatusBadge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TariffSourcesScreen(
    distributors: List<Distributor>,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Tariff sources", fontWeight = FontWeight.Black)
                        Text("Official schedules behind your estimates", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.62f)) {
                    Text(
                        "Every calculation stores its tariff version, effective date, source ID and calculation-engine version. Unsupported schedules are blocked from producing confident estimates.",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            distributors.forEach { distributor ->
                item {
                    Text(distributor.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 4.dp))
                }
                items(distributor.profiles, key = { it.id }) { profile ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 4.dp
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(profile.name, Modifier.weight(1f), fontWeight = FontWeight.Bold)
                                VerificationStatusBadge(profile.verificationStatus)
                            }
                            Text(profile.sourceDocumentTitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Effective ${profile.effectiveDateStr} · Source ${profile.sourceDocumentId.ifBlank { "not supplied" }}", style = MaterialTheme.typography.labelSmall)
                            TextButton(
                                onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(profile.sourceUrl))) },
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("Open official source")
                                Spacer(Modifier.width(5.dp))
                                Icon(Icons.Default.OpenInNew, null, Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
