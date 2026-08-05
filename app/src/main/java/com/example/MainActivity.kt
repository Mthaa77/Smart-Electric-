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

    val showBottomBar = currentRoute in listOf("home", "households", "history", "concepts", "alerts")

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    windowInsets = WindowInsets.navigationBars
                ) {
                    NavigationBarItem(
                        selected = currentRoute == "home",
                        onClick = {
                            navController.navigate("home") {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                        label = { Text("Home") }
                    )

                    NavigationBarItem(
                        selected = currentRoute == "households",
                        onClick = {
                            navController.navigate("households") {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.HomeWork, contentDescription = "Households") },
                        label = { Text("Households") }
                    )

                    NavigationBarItem(
                        selected = currentRoute == "history",
                        onClick = {
                            navController.navigate("history") {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.History, contentDescription = "History") },
                        label = { Text("History") }
                    )

                    NavigationBarItem(
                        selected = currentRoute == "concepts",
                        onClick = {
                            navController.navigate("concepts") {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.MenuBook, contentDescription = "Education") },
                        label = { Text("Education") }
                    )

                    NavigationBarItem(
                        selected = currentRoute == "alerts",
                        onClick = {
                            navController.navigate("alerts") {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            val unreadCount = alertsList.count { !it.isRead }
                            if (unreadCount > 0) {
                                BadgedBox(badge = { Badge { Text("$unreadCount") } }) {
                                    Icon(Icons.Default.Notifications, contentDescription = "Alerts")
                                }
                            } else {
                                Icon(Icons.Default.Notifications, contentDescription = "Alerts")
                            }
                        },
                        label = { Text("Alerts") }
                    )
                }
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
                    onOpenAdmin = { navController.navigate("admin") }
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
