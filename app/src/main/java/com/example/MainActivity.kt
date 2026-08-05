package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.example.smartelectricity.ui.AppViewModel
import com.example.smartelectricity.ui.components.LiquidGlassPanel
import com.example.smartelectricity.ui.screens.alerts.AlertsScreen
import com.example.smartelectricity.ui.screens.calculator.CalculatorWizardScreen
import com.example.smartelectricity.ui.screens.concepts.ConceptsScreen
import com.example.smartelectricity.ui.screens.fbe.FbeCalculatorScreen
import com.example.smartelectricity.ui.screens.history.HistoryScreen
import com.example.smartelectricity.ui.screens.home.HomeScreen
import com.example.smartelectricity.ui.screens.households.HouseholdsScreen
import com.example.smartelectricity.ui.screens.more.MoreScreen
import com.example.smartelectricity.ui.screens.onboarding.HouseholdSetupScreen
import com.example.smartelectricity.ui.screens.reconcile.ReconciliationScreen
import com.example.smartelectricity.ui.screens.result.ResultScreen
import com.example.smartelectricity.ui.screens.sources.TariffSourcesScreen
import com.example.smartelectricity.ui.screens.tools.SmartToolsScreen
import com.example.smartelectricity.ui.screens.tracker.TrackerScreen
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
    val activeLedger by viewModel.activeLedger.collectAsStateWithLifecycle()
    val activePurchases by viewModel.activePurchases.collectAsStateWithLifecycle()

    val navBackStackEntry by navController.currentBackStackEntryAsState()

    val currentRoute = navBackStackEntry?.destination?.route ?: "home"

    val showBottomBar = currentRoute in listOf("home", "tracker", "tools", "more")

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
        },
        floatingActionButton = {
            if (showBottomBar) {
                ExtendedFloatingActionButton(
                    onClick = {
                        viewModel.setCalculationMode(com.example.smartelectricity.data.model.CalculationMode.RAND_TO_KWH)
                        navController.navigate("calculator")
                    },
                    icon = { Icon(Icons.Default.Calculate, contentDescription = null) },
                    text = { Text("Calculate", fontWeight = FontWeight.Bold) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            }
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = if (state.hasCompletedOnboarding) "home" else "onboarding",
            modifier = Modifier.padding(paddingValues)
        ) {
            composable("onboarding") {
                HouseholdSetupScreen(
                    state = state,
                    isFirstRun = true,
                    onSelectDistributor = viewModel::selectDistributor,
                    onSelectProfile = viewModel::selectProfile,
                    onSetMeterType = viewModel::setMeterType,
                    onSetIndigent = viewModel::setIsIndigentRegistered,
                    onSetBudget = viewModel::setMonthlyBudgetLimit,
                    onSaveHousehold = viewModel::saveHousehold,
                    onComplete = {
                        viewModel.completeOnboarding()
                        navController.navigate("home") {
                            popUpTo("onboarding") { inclusive = true }
                        }
                    },
                    onSkip = {
                        viewModel.completeOnboarding()
                        navController.navigate("home") {
                            popUpTo("onboarding") { inclusive = true }
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable("household_setup") {
                HouseholdSetupScreen(
                    state = state,
                    isFirstRun = false,
                    onSelectDistributor = viewModel::selectDistributor,
                    onSelectProfile = viewModel::selectProfile,
                    onSetMeterType = viewModel::setMeterType,
                    onSetIndigent = viewModel::setIsIndigentRegistered,
                    onSetBudget = viewModel::setMonthlyBudgetLimit,
                    onSaveHousehold = viewModel::saveHousehold,
                    onComplete = { navController.navigate("home") },
                    onSkip = { navController.popBackStack() },
                    onBack = { navController.popBackStack() }
                )
            }

            composable("household_setup_result") {
                HouseholdSetupScreen(
                    state = state,
                    isFirstRun = false,
                    onSelectDistributor = viewModel::selectDistributor,
                    onSelectProfile = viewModel::selectProfile,
                    onSetMeterType = viewModel::setMeterType,
                    onSetIndigent = viewModel::setIsIndigentRegistered,
                    onSetBudget = viewModel::setMonthlyBudgetLimit,
                    onSaveHousehold = viewModel::saveHousehold,
                    onComplete = {
                        navController.navigate("result") {
                            popUpTo("result") { inclusive = false }
                            launchSingleTop = true
                        }
                    },
                    onSkip = {
                        viewModel.cancelPendingPurchaseRecord()
                        navController.popBackStack()
                    },
                    onBack = {
                        viewModel.cancelPendingPurchaseRecord()
                        navController.popBackStack()
                    }
                )
            }

            composable("home") {
                HomeScreen(
                    currentSpentRand = monthlySpendingTotal,
                    budgetLimitRand = state.monthlyBudgetLimitRand,
                    activeHousehold = state.activeHousehold,
                    activeDistributor = state.selectedDistributor,
                    activeProfile = state.selectedProfile,
                    activeLedger = activeLedger,
                    onUpdateBudgetLimit = { viewModel.setMonthlyBudgetLimit(it) },
                    onStartCalculator = { mode ->
                        viewModel.setCalculationMode(mode)
                        navController.navigate("calculator")
                    },
                    onRunQuickEstimate = { amount, distributor ->
                        if (state.selectedDistributor.id != distributor.id) {
                            viewModel.selectDistributor(distributor)
                        }
                        viewModel.setCalculationMode(com.example.smartelectricity.data.model.CalculationMode.RAND_TO_KWH)
                        viewModel.setAmountInput(amount)
                        viewModel.runCalculation()
                        navController.navigate("result")
                    },
                    onOpenTools = { navController.navigate("tools") },
                    onOpenBudgetForecast = { navController.navigate("budget_forecast") },
                    onOpenHouseholds = {
                        navController.navigate(if (households.isEmpty()) "household_setup" else "households")
                    }
                )
            }

            composable("tracker") {
                TrackerScreen(
                    householdName = state.activeHousehold?.nickname,
                    profile = state.selectedProfile,
                    ledger = activeLedger,
                    purchases = activePurchases,
                    budgetLimitRand = state.monthlyBudgetLimitRand,
                    onCalculate = {
                        viewModel.setCalculationMode(com.example.smartelectricity.data.model.CalculationMode.RAND_TO_KWH)
                        navController.navigate("calculator")
                    },
                    onOpenHouseholds = { navController.navigate(if (households.isEmpty()) "household_setup" else "households") }
                )
            }

            composable("more") {
                MoreScreen(
                    activeHouseholdName = state.activeHousehold?.nickname,
                    activeProfile = state.selectedProfile,
                    unreadAlertCount = alertsList.count { !it.isRead },
                    onOpenHouseholds = { navController.navigate(if (households.isEmpty()) "household_setup" else "households") },
                    onOpenTariffSources = { navController.navigate("tariff_sources") },
                    onOpenFbe = { navController.navigate("fbe_calculator") },
                    onOpenConcepts = { navController.navigate("concepts") },
                    onOpenAlerts = { navController.navigate("alerts") },
                    onOpenHistory = { navController.navigate("history") }
                )
            }

            composable("tools") {
                SmartToolsScreen(
                    profile = state.selectedProfile,
                    currentSpentRand = monthlySpendingTotal,
                    budgetLimitRand = state.monthlyBudgetLimitRand,
                    onUpdateBudgetLimit = { viewModel.setMonthlyBudgetLimit(it) },
                    onNavigateBack = { navController.popBackStack() },
                    showBack = false
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
                    onSetUnitsAlreadyAllocated = { viewModel.setUnitsAlreadyAllocatedThisMonthInput(it) },
                    onSetDaysSinceLastPurchase = { viewModel.setDaysSinceLastPurchaseInput(it) },
                    onSetArrears = { viewModel.setArrearsInput(it) },
                    onSetOpeningReading = { viewModel.setOpeningReadingInput(it) },
                    onSetClosingReading = { viewModel.setClosingReadingInput(it) },
                    onSetBillingDays = { viewModel.setBillingDaysInput(it) },
                    onRunCalculation = { viewModel.runCalculation() },
                    onCalculationDone = { navController.navigate("result") },
                    onBackToHome = { navController.popBackStack() }
                )
            }

            composable("result") {
                ResultScreen(
                    result = state.activeResult,
                    hasActiveHousehold = state.activeHousehold != null,
                    onOpenReconcile = { navController.navigate("reconcile") },
                    onRecordPurchase = {
                        if (state.activeHousehold == null) {
                            viewModel.recordActivePurchase()
                            navController.navigate("household_setup_result")
                        } else {
                            viewModel.recordActivePurchase()
                        }
                    },
                    purchaseSaveMessage = state.purchaseSaveMessage,
                    isPurchaseRecorded = state.isActiveResultRecorded,
                    onSaveHouseholdClick = { navController.navigate("household_setup_result") },
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
                    onSelectHousehold = {
                        viewModel.selectHousehold(it)
                        navController.popBackStack()
                    },
                    onAddHousehold = { navController.navigate("household_setup") },
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

            composable("tariff_sources") {
                TariffSourcesScreen(
                    distributors = state.availableDistributors,
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
    BottomDestination("tracker", "Tracker", Icons.Default.QueryStats, Icons.Default.QueryStats),
    BottomDestination("tools", "Tools", Icons.Default.AutoAwesome, Icons.Default.AutoAwesome),
    BottomDestination("more", "More", Icons.Default.GridView, Icons.Default.GridView)
)

@Composable
private fun PremiumBottomNavigation(
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.92f))
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        LiquidGlassPanel(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            accentColor = MaterialTheme.colorScheme.primary,
            elevation = 18.dp
        ) {
            NavigationBar(
                modifier = Modifier.fillMaxWidth(),
                containerColor = Color.Transparent,
                tonalElevation = 0.dp,
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
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f),
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    }
}
