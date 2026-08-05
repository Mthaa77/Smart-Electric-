package com.example.smartelectricity.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartelectricity.data.db.*
import com.example.smartelectricity.data.model.*
import com.example.smartelectricity.data.repository.TariffDataRepository
import com.example.smartelectricity.data.repository.TariffRepository
import com.example.smartelectricity.domain.calculator.CalculationEngine
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class CalculatorUiState(
    val selectedDistributor: Distributor = TariffRepository.distributors.first(),
    val selectedProfile: TariffProfile = TariffRepository.distributors.first().profiles.first(),
    val meterType: MeterType = MeterType.PREPAID,
    val calculationMode: CalculationMode = CalculationMode.RAND_TO_KWH,
    val amountInputStr: String = "200.00",
    val openingReadingInputStr: String = "1000.0",
    val closingReadingInputStr: String = "1450.0",
    val billingDaysInputStr: String = "30",
    val daysSinceLastPurchaseInputStr: String = "0",
    val arrearsInputStr: String = "0.0",
    val unitsAlreadyAllocatedThisMonthInputStr: String = "0.0",
    val isFirstPurchaseOfMonth: Boolean = true,
    val hasClaimedFbeThisMonth: Boolean = false,
    val isIndigentRegistered: Boolean = false,
    val propertyValuationRand: Double = 120000.0,
    val distributorQuery: String = "",
    val activeResult: CalculationResult? = null,
    val activeReconciliation: ReconciliationResult? = null,
    val actualUnitsInputStr: String = "",
    val activeHousehold: HouseholdEntity? = null,
    val monthlyBudgetLimitRand: Double = 1500.0
)

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val householdDao = db.householdDao()
    private val historyDao = db.calculationHistoryDao()
    private val alertDao = db.tariffAlertDao()
    private val weeklySpendDao = db.weeklySpendDao()
    private val tariffDao = db.tariffDao()

    val tariffDataRepository = TariffDataRepository(tariffDao)

    private val _uiState = MutableStateFlow(CalculatorUiState())
    val uiState: StateFlow<CalculatorUiState> = _uiState.asStateFlow()

    val allHouseholds: StateFlow<List<HouseholdEntity>> = householdDao.getAllHouseholds()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val calculationHistory: StateFlow<List<CalculationHistoryEntity>> = historyDao.getAllHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val weeklySpends: StateFlow<List<WeeklySpendEntity>> = weeklySpendDao.getAllWeeklySpends()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val monthlySpendingTotal: StateFlow<Double> = historyDao.getAllHistory()
        .map { history ->
            val cal = java.util.Calendar.getInstance()
            val curMonth = cal.get(java.util.Calendar.MONTH)
            val curYear = cal.get(java.util.Calendar.YEAR)
            val currentMonthPurchases = history.filter { item ->
                val itemCal = java.util.Calendar.getInstance().apply { timeInMillis = item.dateTimestamp }
                itemCal.get(java.util.Calendar.MONTH) == curMonth &&
                itemCal.get(java.util.Calendar.YEAR) == curYear
            }
            currentMonthPurchases.sumOf { it.totalCostRand }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val tariffAlerts: StateFlow<List<TariffAlertEntity>> = alertDao.getAllAlerts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Seed predefined South African electricity tariff blocks and distributor rates into Room
        viewModelScope.launch {
            tariffDataRepository.seedPredefinedTariffsIfEmpty()
        }

    }


    fun selectDistributor(distributor: Distributor) {
        val firstProfile = distributor.profiles.firstOrNull() ?: TariffRepository.distributors.first().profiles.first()
        _uiState.update {
            it.copy(
                selectedDistributor = distributor,
                selectedProfile = firstProfile
            )
        }
    }

    fun selectProfile(profile: TariffProfile) {
        _uiState.update { it.copy(selectedProfile = profile) }
    }

    fun setCalculationMode(mode: CalculationMode) {
        val defaultVal = if (mode == CalculationMode.RAND_TO_KWH) "200.00" else "100.0"
        _uiState.update {
            it.copy(
                calculationMode = mode,
                amountInputStr = defaultVal
            )
        }
    }

    fun setAmountInput(input: String) {
        _uiState.update { it.copy(amountInputStr = input) }
    }

    fun setOpeningReadingInput(input: String) {
        _uiState.update { it.copy(openingReadingInputStr = input) }
    }

    fun setClosingReadingInput(input: String) {
        _uiState.update { it.copy(closingReadingInputStr = input) }
    }

    fun setBillingDaysInput(input: String) {
        _uiState.update { it.copy(billingDaysInputStr = input) }
    }

    fun setDaysSinceLastPurchaseInput(input: String) {
        _uiState.update { it.copy(daysSinceLastPurchaseInputStr = input) }
    }

    fun setArrearsInput(input: String) {
        _uiState.update { it.copy(arrearsInputStr = input) }
    }

    fun setUnitsAlreadyAllocatedThisMonthInput(input: String) {
        _uiState.update { it.copy(unitsAlreadyAllocatedThisMonthInputStr = input) }
    }

    fun setMonthlyBudgetLimit(limit: Double) {
        _uiState.update { it.copy(monthlyBudgetLimitRand = limit.coerceAtLeast(10.0)) }
    }

    fun setFirstPurchaseOfMonth(isFirst: Boolean) {
        _uiState.update { it.copy(isFirstPurchaseOfMonth = isFirst) }
    }

    fun setHasClaimedFbeThisMonth(hasClaimed: Boolean) {
        _uiState.update { it.copy(hasClaimedFbeThisMonth = hasClaimed) }
    }

    fun setIsIndigentRegistered(isIndigent: Boolean) {
        _uiState.update { it.copy(isIndigentRegistered = isIndigent) }
    }

    fun setDistributorQuery(query: String) {
        _uiState.update { it.copy(distributorQuery = query) }
    }

    fun setActualUnitsInput(units: String) {
        _uiState.update { it.copy(actualUnitsInputStr = units) }
    }

    fun runCalculation() {
        val state = _uiState.value
        val amount = state.amountInputStr.toDoubleOrNull() ?: 0.0

        val result = when (state.calculationMode) {
            CalculationMode.RAND_TO_KWH -> {
                CalculationEngine.calculateRandToKwh(
                    distributor = state.selectedDistributor,
                    profile = state.selectedProfile,
                    amountRand = amount,
                    isFirstPurchaseOfMonth = state.isFirstPurchaseOfMonth,
                    hasClaimedFbeThisMonth = state.hasClaimedFbeThisMonth,
                    isIndigentEligible = state.isIndigentRegistered,
                    unitsAlreadyAllocatedThisMonth = state.unitsAlreadyAllocatedThisMonthInputStr.toDoubleOrNull() ?: 0.0,
                    daysSinceLastPurchase = state.daysSinceLastPurchaseInputStr.toIntOrNull() ?: 0,
                    arrearsDeductionRand = state.arrearsInputStr.toDoubleOrNull() ?: 0.0
                )
            }
            CalculationMode.KWH_TO_RAND -> {
                CalculationEngine.calculateKwhToRand(
                    distributor = state.selectedDistributor,
                    profile = state.selectedProfile,
                    targetKwh = amount,
                    isFirstPurchaseOfMonth = state.isFirstPurchaseOfMonth,
                    hasClaimedFbeThisMonth = state.hasClaimedFbeThisMonth,
                    isIndigentEligible = state.isIndigentRegistered,
                    unitsAlreadyAllocatedThisMonth = state.unitsAlreadyAllocatedThisMonthInputStr.toDoubleOrNull() ?: 0.0,
                    daysSinceLastPurchase = state.daysSinceLastPurchaseInputStr.toIntOrNull() ?: 0,
                    arrearsDeductionRand = state.arrearsInputStr.toDoubleOrNull() ?: 0.0
                )
            }
            CalculationMode.CONVENTIONAL_BILL -> {
                val opening = state.openingReadingInputStr.toDoubleOrNull() ?: 0.0
                val closing = state.closingReadingInputStr.toDoubleOrNull() ?: 0.0
                val days = state.billingDaysInputStr.toIntOrNull() ?: 30
                CalculationEngine.calculateConventionalBill(
                    distributor = state.selectedDistributor,
                    profile = state.selectedProfile,
                    openingReading = opening,
                    closingReading = closing,
                    meterMultiplier = 1.0,
                    billingDays = days
                )
            }
        }

        _uiState.update { it.copy(activeResult = result) }

        // Automatically save calculation to history
        saveCalculationToHistory(result)
    }

    private fun saveCalculationToHistory(result: CalculationResult) {
        viewModelScope.launch {
            val entity = CalculationHistoryEntity(
                householdId = _uiState.value.activeHousehold?.id,
                distributorId = result.distributor.id,
                distributorName = result.distributor.name,
                tariffProfileId = result.profile.id,
                tariffProfileName = result.profile.name,
                mode = result.mode.name,
                inputValue = result.inputAmount,
                totalKwh = result.totalKwh,
                totalCostRand = result.grossPurchaseRand,
                fixedChargeDeductedRand = result.fixedChargeDeductedRand,
                fbeUnitsKwh = result.freeFbeKwh,
                effectiveDateStr = result.effectiveDateStr
            )
            historyDao.insertHistory(entity)
        }
    }

    fun runReconciliation() {
        val state = _uiState.value
        val actualUnits = state.actualUnitsInputStr.toDoubleOrNull() ?: 0.0
        val expectedResult = state.activeResult ?: return

        val reconciliation = CalculationEngine.reconcilePurchase(
            actualUnits = actualUnits,
            expectedResult = expectedResult
        )

        _uiState.update { it.copy(activeReconciliation = reconciliation) }
    }

    fun saveHousehold(nickname: String, suburb: String) {
        viewModelScope.launch {
            val state = _uiState.value
            val entity = HouseholdEntity(
                nickname = nickname.ifBlank { "My Household" },
                distributorId = state.selectedDistributor.id,
                tariffProfileId = state.selectedProfile.id,
                meterType = state.meterType.name,
                suburbOrMunicipality = suburb,
                isIndigentRegistered = state.isIndigentRegistered,
                propertyValuationRand = state.propertyValuationRand
            )
            val newId = householdDao.insertHousehold(entity)
            val savedHousehold = householdDao.getHouseholdById(newId.toInt())
            _uiState.update { it.copy(activeHousehold = savedHousehold) }
        }
    }

    fun selectHousehold(household: HouseholdEntity) {
        _uiState.update { it.copy(activeHousehold = household) }
        TariffRepository.getDistributorById(household.distributorId)?.let { dist ->
            selectDistributor(dist)
            TariffRepository.getTariffProfileById(household.tariffProfileId)?.let { prof ->
                selectProfile(prof)
            }
        }
        setIsIndigentRegistered(household.isIndigentRegistered)
    }

    fun addWeeklySpend(weekLabel: String, amountRand: Double, notes: String = "") {
        viewModelScope.launch {
            val state = _uiState.value
            val calcResult = CalculationEngine.calculateRandToKwh(
                distributor = state.selectedDistributor,
                profile = state.selectedProfile,
                amountRand = amountRand,
                isFirstPurchaseOfMonth = false,
                hasClaimedFbeThisMonth = false,
                isIndigentEligible = state.isIndigentRegistered
            )
            val entity = WeeklySpendEntity(
                weekLabel = weekLabel.ifBlank { "Week ${System.currentTimeMillis() % 1000}" },
                amountRand = amountRand,
                estimatedKwh = calcResult.totalKwh,
                notes = notes
            )
            weeklySpendDao.insertWeeklySpend(entity)
        }
    }

    fun deleteWeeklySpend(id: Int) {
        viewModelScope.launch {
            weeklySpendDao.deleteWeeklySpendById(id)
        }
    }

    fun markAlertAsRead(alertId: Int) {
        viewModelScope.launch {
            alertDao.markAsRead(alertId)
        }
    }
}
