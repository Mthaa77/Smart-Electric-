package com.example.smartelectricity.ui

import android.app.Application
import android.content.Context
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
    val availableDistributors: List<Distributor> = TariffRepository.distributors,
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
    val monthlyBudgetLimitRand: Double = 1500.0,
    val purchaseSaveMessage: String? = null,
    val isActiveResultRecorded: Boolean = false,
    val hasCompletedOnboarding: Boolean = false
)

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val preferences = application.getSharedPreferences("smart_electricity_preferences", Context.MODE_PRIVATE)

    private val db = AppDatabase.getDatabase(application)
    private val householdDao = db.householdDao()
    private val historyDao = db.calculationHistoryDao()
    private val alertDao = db.tariffAlertDao()
    private val weeklySpendDao = db.weeklySpendDao()
    private val tariffDao = db.tariffDao()
    private val purchaseLedgerDao = db.purchaseLedgerDao()
    private var recordPurchaseAfterHouseholdSave = false
    private var lastRecordedPurchaseId: Long? = null

    val tariffDataRepository = TariffDataRepository(tariffDao)

    private val _uiState = MutableStateFlow(
        CalculatorUiState(
            hasCompletedOnboarding = preferences.getBoolean("has_completed_onboarding", false),
            monthlyBudgetLimitRand = preferences.getFloat("monthly_budget_limit", 1500f).toDouble()
        )
    )
    val uiState: StateFlow<CalculatorUiState> = _uiState.asStateFlow()

    val allHouseholds: StateFlow<List<HouseholdEntity>> = householdDao.getAllHouseholds()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val calculationHistory: StateFlow<List<CalculationHistoryEntity>> = historyDao.getAllHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val weeklySpends: StateFlow<List<WeeklySpendEntity>> = weeklySpendDao.getAllWeeklySpends()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val currentYearMonth: String
        get() = java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.US).format(java.util.Date())

    val monthlySpendingTotal: StateFlow<Double> = uiState
        .map { it.activeHousehold?.id }
        .distinctUntilChanged()
        .flatMapLatest { householdId ->
            if (householdId == null) purchaseLedgerDao.observeTotalSpendForMonth(currentYearMonth)
            else purchaseLedgerDao.observeTotalSpendForHouseholdMonth(householdId, currentYearMonth)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val activePurchases: StateFlow<List<PrepaidPurchaseEntity>> = uiState
        .map { it.activeHousehold?.id }
        .distinctUntilChanged()
        .flatMapLatest { householdId ->
            if (householdId == null) flowOf(emptyList())
            else purchaseLedgerDao.observePurchasesForHousehold(householdId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeLedger: StateFlow<MonthlyBlockLedgerEntity?> = uiState
        .map { state -> Triple(state.activeHousehold?.id, currentYearMonth, state.selectedProfile.id) }
        .distinctUntilChanged()
        .flatMapLatest { (householdId, yearMonth, tariffProfileId) ->
            if (householdId == null) flowOf(null)
            else purchaseLedgerDao.observeLedger(householdId, yearMonth, tariffProfileId)
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val tariffAlerts: StateFlow<List<TariffAlertEntity>> = alertDao.getAllAlerts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            householdDao.getAllHouseholds().firstOrNull()?.firstOrNull()?.let { savedHousehold ->
                if (_uiState.value.activeHousehold == null) selectHousehold(savedHousehold)
            }
        }
        viewModelScope.launch {
            tariffDataRepository.refreshBundledTariffs()
        }
        viewModelScope.launch {
            tariffDataRepository.getAllDistributorsFlow().collect { distributors ->
                if (distributors.isEmpty()) return@collect
                _uiState.update { state ->
                    val selectedDistributor = distributors.firstOrNull { it.id == state.selectedDistributor.id }
                        ?: distributors.first()
                    val selectedProfile = selectedDistributor.profiles.firstOrNull { it.id == state.selectedProfile.id }
                        ?: selectedDistributor.profiles.first()
                    state.copy(
                        availableDistributors = distributors,
                        selectedDistributor = selectedDistributor,
                        selectedProfile = selectedProfile
                    )
                }
            }
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
        val defaultVal = when (mode) {
            CalculationMode.RAND_TO_KWH -> "200.00"
            CalculationMode.KWH_TO_RAND -> "100.0"
            CalculationMode.CONVENTIONAL_BILL -> "0.0"
        }
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
        val safeLimit = limit.coerceAtLeast(10.0)
        preferences.edit().putFloat("monthly_budget_limit", safeLimit.toFloat()).apply()
        _uiState.update { it.copy(monthlyBudgetLimitRand = safeLimit) }
    }

    fun setMeterType(meterType: MeterType) {
        _uiState.update { it.copy(meterType = meterType) }
    }

    fun completeOnboarding() {
        preferences.edit().putBoolean("has_completed_onboarding", true).apply()
        _uiState.update { it.copy(hasCompletedOnboarding = true) }
    }

    fun cancelPendingPurchaseRecord() {
        recordPurchaseAfterHouseholdSave = false
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
        val ledger = activeLedger.value
        val hasAutomaticLedger = state.activeHousehold != null && ledger?.tariffProfileId == state.selectedProfile.id
        val unitsAlreadyAllocated = if (hasAutomaticLedger) ledger?.paidUnitsAllocatedKwh ?: 0.0
            else state.unitsAlreadyAllocatedThisMonthInputStr.toDoubleOrNull() ?: 0.0
        val hasClaimedFbe = if (hasAutomaticLedger) (ledger?.freeUnitsAllocatedKwh ?: 0.0) > 0.0
            else state.hasClaimedFbeThisMonth
        val isFirstPurchase = if (state.activeHousehold != null) ledger?.lastPurchaseAt == null
            else state.isFirstPurchaseOfMonth
        val daysSinceLastPurchase = if (ledger?.lastPurchaseAt != null) {
            (((System.currentTimeMillis() - ledger.lastPurchaseAt) / 86_400_000L).coerceAtLeast(0L)).toInt()
        } else state.daysSinceLastPurchaseInputStr.toIntOrNull() ?: 0
        val household = state.activeHousehold

        val result = when (state.calculationMode) {
            CalculationMode.RAND_TO_KWH -> {
                CalculationEngine.calculateRandToKwh(
                    distributor = state.selectedDistributor,
                    profile = state.selectedProfile,
                    amountRand = amount,
                    isFirstPurchaseOfMonth = isFirstPurchase,
                    hasClaimedFbeThisMonth = hasClaimedFbe,
                    isIndigentEligible = state.isIndigentRegistered,
                    propertyValuationRand = household?.propertyValuationRand ?: state.propertyValuationRand,
                    historicAverageMonthlyKwh = household?.estimatedMonthlyKwh,
                    unitsAlreadyAllocatedThisMonth = unitsAlreadyAllocated,
                    daysSinceLastPurchase = daysSinceLastPurchase,
                    arrearsDeductionRand = state.arrearsInputStr.toDoubleOrNull() ?: 0.0
                )
            }
            CalculationMode.KWH_TO_RAND -> {
                CalculationEngine.calculateKwhToRand(
                    distributor = state.selectedDistributor,
                    profile = state.selectedProfile,
                    targetKwh = amount,
                    isFirstPurchaseOfMonth = isFirstPurchase,
                    hasClaimedFbeThisMonth = hasClaimedFbe,
                    isIndigentEligible = state.isIndigentRegistered,
                    propertyValuationRand = household?.propertyValuationRand ?: state.propertyValuationRand,
                    historicAverageMonthlyKwh = household?.estimatedMonthlyKwh,
                    unitsAlreadyAllocatedThisMonth = unitsAlreadyAllocated,
                    daysSinceLastPurchase = daysSinceLastPurchase,
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

        _uiState.update { it.copy(activeResult = result, purchaseSaveMessage = null, isActiveResultRecorded = false) }
    }

    private suspend fun saveCalculationToHistory(result: CalculationResult, isCommittedPurchase: Boolean) {
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
                effectiveDateStr = result.effectiveDateStr,
                isCommittedPurchase = isCommittedPurchase
            )
            historyDao.insertHistory(entity)
    }

    fun recordActivePurchase() {
        val state = _uiState.value
        val household = state.activeHousehold
        val result = state.activeResult
        if (household == null) {
            recordPurchaseAfterHouseholdSave = true
            _uiState.update { it.copy(purchaseSaveMessage = "Save this home to connect the purchase to its monthly tariff block.") }
            return
        }
        if (result == null || result.mode != CalculationMode.RAND_TO_KWH) {
            _uiState.update { it.copy(purchaseSaveMessage = "Only completed rand-to-kWh purchases can be recorded.") }
            return
        }
        if (state.isActiveResultRecorded) {
            _uiState.update { it.copy(purchaseSaveMessage = "This purchase has already been recorded.") }
            return
        }

        _uiState.update { it.copy(isActiveResultRecorded = true, purchaseSaveMessage = "Recording purchase…") }
        viewModelScope.launch {
            try {
            val now = System.currentTimeMillis()
            val yearMonth = currentYearMonth
            val ledgerKey = "${household.id}:$yearMonth:${result.profile.id}"
            val existing = purchaseLedgerDao.getLedger(ledgerKey)
            val purchase = PrepaidPurchaseEntity(
                householdId = household.id,
                purchaseTimestamp = now,
                yearMonth = yearMonth,
                tenderAmountRand = result.grossPurchaseRand,
                energyValueRand = result.netEnergyPurchaseRand,
                fixedDeductionRand = result.fixedChargeDeductedRand,
                arrearsDeductionRand = result.arrearsDeductedRand,
                paidUnitsKwh = result.paidKwh,
                fbeUnitsKwh = result.freeFbeKwh,
                estimatedTotalUnitsKwh = result.totalKwh,
                tariffProfileId = result.profile.id,
                tariffEffectiveFromStr = result.profile.effectiveFromStr,
                sourceDocumentId = result.sourceDocumentId,
                calculationEngineVersion = result.calculationEngineVersion
            )
            val updatedLedger = MonthlyBlockLedgerEntity(
                ledgerKey = ledgerKey,
                householdId = household.id,
                yearMonth = yearMonth,
                tariffProfileId = result.profile.id,
                paidUnitsAllocatedKwh = (existing?.paidUnitsAllocatedKwh ?: 0.0) + result.paidKwh,
                freeUnitsAllocatedKwh = (existing?.freeUnitsAllocatedKwh ?: 0.0) + result.freeFbeKwh,
                purchasedAmountRand = (existing?.purchasedAmountRand ?: 0.0) + result.grossPurchaseRand,
                lastPurchaseAt = now,
                lastFixedChargeRecoveryAt = if (result.fixedChargeDeductedRand > 0.0) now else existing?.lastFixedChargeRecoveryAt,
                updatedAt = now
            )
            lastRecordedPurchaseId = purchaseLedgerDao.recordPurchase(purchase, updatedLedger)
            saveCalculationToHistory(result, isCommittedPurchase = true)
            _uiState.update {
                it.copy(
                    purchaseSaveMessage = "Purchase recorded. Your monthly tariff block is now updated automatically.",
                    isActiveResultRecorded = true
                )
            }
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        purchaseSaveMessage = "The purchase could not be recorded. Please try again.",
                        isActiveResultRecorded = false
                    )
                }
            }
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
        lastRecordedPurchaseId?.let { purchaseId ->
            viewModelScope.launch {
                purchaseLedgerDao.updateActualUnits(purchaseId, actualUnits)
            }
        }
    }

    fun saveHousehold(nickname: String, suburb: String, propertyValueRand: Double, historicAverageMonthlyKwh: Double) {
        viewModelScope.launch {
            val state = _uiState.value
            val entity = HouseholdEntity(
                nickname = nickname.ifBlank { "My Household" },
                distributorId = state.selectedDistributor.id,
                tariffProfileId = state.selectedProfile.id,
                meterType = state.meterType.name,
                suburbOrMunicipality = suburb,
                isIndigentRegistered = state.isIndigentRegistered,
                propertyValuationRand = propertyValueRand.coerceAtLeast(0.0),
                estimatedMonthlyKwh = historicAverageMonthlyKwh.coerceAtLeast(0.0)
            )
            val newId = householdDao.insertHousehold(entity)
            val savedHousehold = householdDao.getHouseholdById(newId.toInt())
            _uiState.update {
                it.copy(
                    activeHousehold = savedHousehold,
                    propertyValuationRand = propertyValueRand.coerceAtLeast(0.0)
                )
            }
            if (recordPurchaseAfterHouseholdSave && savedHousehold != null && _uiState.value.activeResult != null) {
                recordPurchaseAfterHouseholdSave = false
                recordActivePurchase()
            }
        }
    }

    fun selectHousehold(household: HouseholdEntity) {
        _uiState.update {
            it.copy(
                activeHousehold = household,
                propertyValuationRand = household.propertyValuationRand,
                isIndigentRegistered = household.isIndigentRegistered,
                hasClaimedFbeThisMonth = household.hasClaimedFbeThisMonth
            )
        }
        TariffRepository.getDistributorById(household.distributorId)?.let { dist ->
            selectDistributor(dist)
            TariffRepository.getTariffProfileById(household.tariffProfileId)?.let { prof ->
                selectProfile(prof)
            }
        }
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
