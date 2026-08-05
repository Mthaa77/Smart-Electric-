package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.example.smartelectricity.ui.AppViewModel
import com.example.smartelectricity.ui.screens.admin.AdminEvidenceScreen
import com.example.smartelectricity.ui.screens.alerts.AlertsScreen
import com.example.smartelectricity.ui.screens.calculator.CalculatorWizardScreen
import com.example.smartelectricity.ui.screens.concepts.ConceptsScreen
import com.example.smartelectricity.ui.screens.fbe.FbeCalculatorScreen
import com.example.smartelectricity.ui.screens.history.HistoryScreen
import com.example.smartelectricity.ui.screens.home.HomeScreen
import com.example.smartelectricity.ui.screens.households.HouseholdsScreen
import com.example.smartelectricity.ui.screens.reconcile.ReconciliationScreen
import com.example.smartelectricity.ui.screens.result.ResultScreen
import com.example.smartelectricity.ui.screens.tools.SmartToolsScreen
import com.example.smartelectricity.ui.screens.weekly.WeeklyTrackingScreen
import com.example.ui.theme.SmartElectricityTheme


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SmartElectricityTheme {
                MainAppScreen()
            }
        }
    }
}

@Composable
fun MainAppScreen(
    viewModel: AppViewModel = viewModel()
) {
    val navController = rememberNavController()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val households by viewModel.allHouseholds.collectAsStateWithLifecycle()
    val historyList by viewModel.calculationHistory.collectAsStateWithLifecycle()
    val alertsList by viewModel.tariffAlerts.collectAsStateWithLifecycle()
    val monthlySpendingTotal by viewModel.monthlySpendingTotal.collectAsStateWithLifecycle()
    val weeklySpends by viewModel.weeklySpends.collectAsStateWithLifecycle()

    val navBackStackEntry by navController.currentBackStackEntryAsState()

    val currentRoute = navBackStackEntry?.destination?.route ?: "home"

    val showBottomBar = currentRoute in listOf("home", "households", "tools", "budget_forecast", "history", "concepts", "alerts")

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                PremiumBottomNavigation(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(paddingValues)
        ) {
            composable("home") {
                HomeScreen(
                    currentSpentRand = monthlySpendingTotal,
                    budgetLimitRand = state.monthlyBudgetLimitRand,
                    onUpdateBudgetLimit = { viewModel.setMonthlyBudgetLimit(it) },
                    onOpenWeeklyTracker = { navController.navigate("weekly_tracking") },
                    onStartCalculator = { mode ->
                        viewModel.setCalculationMode(mode)
                        navController.navigate("calculator")
                    },
                    onSelectDistributor = { dist ->
                        viewModel.selectDistributor(dist)
                        navController.navigate("calculator")
                    },
                    onOpenReconcile = { navController.navigate("reconcile") },
                    onOpenFbeGuide = { navController.navigate("fbe_calculator") },
                    onOpenAlerts = { navController.navigate("alerts") },
                    onOpenAdmin = { navController.navigate("admin") },
                    onOpenTools = { navController.navigate("tools") },
                    onOpenBudgetForecast = { navController.navigate("budget_forecast") },
                    onOpenHouseholds = { navController.navigate("households") },
                    onOpenConcepts = { navController.navigate("concepts") }
                )
            }

            composable("tools") {
                SmartToolsScreen(
                    profile = state.selectedProfile,
                    currentSpentRand = monthlySpendingTotal,
                    budgetLimitRand = state.monthlyBudgetLimitRand,
                    onUpdateBudgetLimit = { viewModel.setMonthlyBudgetLimit(it) },
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable("budget_forecast") {
                SmartToolsScreen(
                    profile = state.selectedProfile,
                    currentSpentRand = monthlySpendingTotal,
                    budgetLimitRand = state.monthlyBudgetLimitRand,
                    onUpdateBudgetLimit = { viewModel.setMonthlyBudgetLimit(it) },
                    onNavigateBack = { navController.popBackStack() },
                    initialTool = 1
                )
            }

            composable("fbe_calculator") {
                FbeCalculatorScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable("weekly_tracking") {
                WeeklyTrackingScreen(
                    weeklySpends = weeklySpends,
                    monthlyBudgetLimitRand = state.monthlyBudgetLimitRand,
                    onAddWeeklySpend = { weekLabel, amount, notes -> viewModel.addWeeklySpend(weekLabel, amount, notes) },
                    onDeleteWeeklySpend = { id -> viewModel.deleteWeeklySpend(id) },
                    onNavigateBack = { navController.popBackStack() }
                )
            }


            composable("calculator") {
                CalculatorWizardScreen(
                    state = state,
                    onSelectDistributor = { viewModel.selectDistributor(it) },
                    onSelectProfile = { viewModel.selectProfile(it) },
                    onSetMode = { viewModel.setCalculationMode(it) },
                    onSetAmount = { viewModel.setAmountInput(it) },
                    onSetFirstPurchase = { viewModel.setFirstPurchaseOfMonth(it) },
                    onSetHasClaimedFbe = { viewModel.setHasClaimedFbeThisMonth(it) },
                    onSetIsIndigent = { viewModel.setIsIndigentRegistered(it) },
                    onRunCalculation = { viewModel.runCalculation() },
                    onCalculationDone = { navController.navigate("result") },
                    onBackToHome = { navController.popBackStack() }
                )
            }

            composable("result") {
                ResultScreen(
                    result = state.activeResult,
                    onOpenReconcile = { navController.navigate("reconcile") },
                    onSaveHouseholdClick = { navController.navigate("households") },
                    onBackToHome = { navController.navigate("home") }
                )
            }

            composable("reconcile") {
                ReconciliationScreen(
                    state = state,
                    onSetActualUnits = { viewModel.setActualUnitsInput(it) },
                    onRunReconciliation = { viewModel.runReconciliation() },
                    onBack = { navController.popBackStack() }
                )
            }

            composable("households") {
                HouseholdsScreen(
                    households = households,
                    activeHousehold = state.activeHousehold,
                    onSelectHousehold = { viewModel.selectHousehold(it) },
                    onSaveHousehold = { name, suburb -> viewModel.saveHousehold(name, suburb) },
                    onBack = { navController.popBackStack() }
                )
            }

            composable("history") {
                HistoryScreen(
                    historyList = historyList,
                    onBack = { navController.popBackStack() }
                )
            }

            composable("alerts") {
                AlertsScreen(
                    alerts = alertsList,
                    onMarkAsRead = { viewModel.markAlertAsRead(it) },
                    onBack = { navController.popBackStack() }
                )
            }

            composable("concepts") {
                ConceptsScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            composable("admin") {
                AdminEvidenceScreen(
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

private data class BottomDestination(
    val route: String,
    val label: String,
    val selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val unselectedIcon: androidx.compose.ui.graphics.vector.ImageVector
)

private val bottomDestinations = listOf(
    BottomDestination("home", "Home", Icons.Default.Home, Icons.Default.Home),
    BottomDestination("households", "Homes", Icons.Default.HomeWork, Icons.Default.HomeWork),
    BottomDestination("tools", "Smart tools", Icons.Default.AutoAwesome, Icons.Default.AutoAwesome),
    BottomDestination("history", "Activity", Icons.Default.History, Icons.Default.History)
)

@Composable
private fun PremiumBottomNavigation(
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    Surface(color = MaterialTheme.colorScheme.background) {
        NavigationBar(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 7.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            windowInsets = WindowInsets.navigationBars,
        ) {
            bottomDestinations.forEach { destination ->
                val selected = currentRoute == destination.route ||
                    (destination.route == "tools" && currentRoute == "budget_forecast")
                NavigationBarItem(
                    selected = selected,
                    onClick = { onNavigate(destination.route) },
                    icon = {
                        Icon(
                            if (selected) destination.selectedIcon else destination.unselectedIcon,
                            contentDescription = destination.label
                        )
                    },
                    label = {
                        Text(
                            destination.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }
        }
    }
}
