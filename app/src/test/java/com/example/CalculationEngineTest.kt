package com.example

import com.example.smartelectricity.data.model.Distributor
import com.example.smartelectricity.data.model.FbeConfig
import com.example.smartelectricity.data.model.FixedChargeRecoveryRule
import com.example.smartelectricity.data.model.MeterType
import com.example.smartelectricity.data.model.TariffBlock
import com.example.smartelectricity.data.model.TariffProfile
import com.example.smartelectricity.data.model.VerificationStatus
import com.example.smartelectricity.data.model.isCalculationSupported
import com.example.smartelectricity.data.model.isEffectiveOn
import com.example.smartelectricity.data.repository.TariffRepository
import java.time.LocalDate
import com.example.smartelectricity.domain.calculator.CalculationEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculationEngineTest {
    private val profile = TariffProfile(
        id = "test-profile",
        distributorId = "test-distributor",
        code = "TEST",
        name = "Test tariff",
        description = "Test tariff",
        compatibleMeterTypes = listOf(MeterType.PREPAID),
        monthlyFixedChargeRand = 20.0,
        dailyFixedChargeRand = 2.0,
        fixedChargeRecoveryRule = FixedChargeRecoveryRule.DAILY_ACCRUAL_AT_VENDING,
        blocks = listOf(
            TariffBlock(1, 0.0, 100.0, 100.0),
            TariffBlock(2, 100.0, null, 200.0)
        ),
        fbeConfig = FbeConfig(isAvailable = true, freeKwh = 50.0, monthlyUsageCapKwh = 450.0)
    )
    private val distributor = Distributor("test-distributor", "Test Distributor", "Test", profiles = listOf(profile), recognitionClue = "Test")

    @Test
    fun `rand estimate honours carried monthly block`() {
        val result = CalculationEngine.calculateRandToKwh(
            distributor, profile, amountRand = 40.0, isFirstPurchaseOfMonth = false,
            hasClaimedFbeThisMonth = true, unitsAlreadyAllocatedThisMonth = 90.0
        )

        assertEquals(25.0, result.paidKwh, 0.001)
        assertEquals(10.0, result.blockBreakdown.first().kwhAllocated, 0.001)
        assertEquals(15.0, result.blockBreakdown.last().kwhAllocated, 0.001)
    }

    @Test
    fun `reverse estimate includes accrued daily charge and arrears`() {
        val result = CalculationEngine.calculateKwhToRand(
            distributor, profile, targetKwh = 10.0, isFirstPurchaseOfMonth = false,
            hasClaimedFbeThisMonth = true, daysSinceLastPurchase = 3, arrearsDeductionRand = 7.5
        )

        assertEquals(10.0, result.netEnergyPurchaseRand, 0.001)
        assertEquals(6.0, result.fixedChargeDeductedRand, 0.001)
        assertEquals(7.5, result.arrearsDeductedRand, 0.001)
        assertEquals(23.5, result.grossPurchaseRand, 0.001)
        assertEquals(16.0 * 15.0 / 115.0, result.vatAmountRand, 0.001)
    }

    @Test
    fun `reverse estimate never allocates more FBE than the requested units`() {
        val result = CalculationEngine.calculateKwhToRand(
            distributor, profile, targetKwh = 20.0, isFirstPurchaseOfMonth = true,
            hasClaimedFbeThisMonth = false, isIndigentEligible = true
        )

        assertEquals(20.0, result.freeFbeKwh, 0.001)
        assertEquals(0.0, result.paidKwh, 0.001)
        assertEquals(20.0, result.totalKwh, 0.001)
    }

    @Test
    fun `fbe is not projected after the tariff monthly usage cap`() {
        val result = CalculationEngine.calculateRandToKwh(
            distributor, profile, amountRand = 10.0, isFirstPurchaseOfMonth = true,
            hasClaimedFbeThisMonth = false, isIndigentEligible = true,
            unitsAlreadyAllocatedThisMonth = 450.0
        )

        assertEquals(0.0, result.freeFbeKwh, 0.001)
        assertTrue(result.stepExplanations.any { it.contains("usage cap") })
    }

    @Test
    fun `invalid input cannot produce negative money or units`() {
        val result = CalculationEngine.calculateRandToKwh(
            distributor, profile, amountRand = Double.NaN, isFirstPurchaseOfMonth = false,
            hasClaimedFbeThisMonth = true, unitsAlreadyAllocatedThisMonth = -40.0,
            arrearsDeductionRand = -8.0
        )

        assertEquals(0.0, result.grossPurchaseRand, 0.001)
        assertEquals(0.0, result.totalKwh, 0.001)
        assertFalse(result.paidKwh < 0.0)
    }

    @Test
    fun `official Tshwane 500 kWh example reconciles`() {
        val tshwane = TariffRepository.getDistributorById("TSHWANE")!!
        val tariff = TariffRepository.getTariffProfileById("TSH_RESIDENTIAL_IBT")!!

        val result = CalculationEngine.calculateKwhToRand(
            tshwane, tariff, targetKwh = 500.0, isFirstPurchaseOfMonth = false,
            hasClaimedFbeThisMonth = true
        )

        assertEquals(1875.35, result.grossPurchaseRand, 0.01)
        assertEquals(3, result.blockBreakdown.size)
    }

    @Test
    fun `official Eskom Homepower worked bill reconciles`() {
        val eskom = TariffRepository.getDistributorById("ESKOM_DIRECT")!!
        val tariff = TariffRepository.getTariffProfileById("ESK_HOMEPOWER_1")!!

        val result = CalculationEngine.calculateConventionalBill(
            eskom, tariff, openingReading = 1000.0, closingReading = 1450.0,
            billingDays = 30
        )

        assertEquals(2290.62, result.grossPurchaseRand, 0.01)
        assertEquals(5.0903, result.effectiveRandPerKwh, 0.0001)
    }

    @Test
    fun `Cape Town Lifeline FBE uses historic consumption tiers`() {
        val capeTown = TariffRepository.getDistributorById("CAPE_TOWN")!!
        val lifeline = TariffRepository.getTariffProfileById("CPT_LIFELINE")!!

        val lowUse = CalculationEngine.calculateKwhToRand(
            capeTown, lifeline, targetKwh = 70.0, isFirstPurchaseOfMonth = true,
            hasClaimedFbeThisMonth = false, propertyValuationRand = 400000.0,
            historicAverageMonthlyKwh = 200.0
        )
        val mediumUse = CalculationEngine.calculateKwhToRand(
            capeTown, lifeline, targetKwh = 70.0, isFirstPurchaseOfMonth = true,
            hasClaimedFbeThisMonth = false, propertyValuationRand = 400000.0,
            historicAverageMonthlyKwh = 300.0
        )

        assertEquals(60.0, lowUse.freeFbeKwh, 0.001)
        assertEquals(25.0, mediumUse.freeFbeKwh, 0.001)
    }

    @Test
    fun `Cape Town Domestic vending recovers ten accrued daily charges`() {
        val capeTown = TariffRepository.getDistributorById("CAPE_TOWN")!!
        val domestic = TariffRepository.getTariffProfileById("CPT_DOMESTIC")!!

        val result = CalculationEngine.calculateRandToKwh(
            capeTown, domestic, amountRand = 1000.0, isFirstPurchaseOfMonth = true,
            hasClaimedFbeThisMonth = true, daysSinceLastPurchase = 10
        )

        assertEquals(24.60, result.fixedChargeDeductedRand, 0.001)
        assertEquals(235.72, result.totalKwh, 0.05)
    }

    @Test
    fun `Cape Town Home User worked monthly bill reconciles`() {
        val capeTown = TariffRepository.getDistributorById("CAPE_TOWN")!!
        val homeUser = TariffRepository.getTariffProfileById("CPT_HOME_USER")!!

        val result = CalculationEngine.calculateConventionalBill(
            capeTown, homeUser, openingReading = 1000.0, closingReading = 1650.0,
            billingDays = 30
        )

        assertEquals(2794.53, result.grossPurchaseRand, 0.01)
    }

    @Test
    fun `conventional bill rejects a negative meter delta`() {
        val eskom = TariffRepository.getDistributorById("ESKOM_DIRECT")!!
        val tariff = TariffRepository.getTariffProfileById("ESK_HOMEPOWER_1")!!

        val result = CalculationEngine.calculateConventionalBill(
            eskom, tariff, openingReading = 1500.0, closingReading = 1450.0,
            billingDays = 30
        )

        assertFalse(result.isCalculationValid)
        assertEquals(0.0, result.grossPurchaseRand, 0.001)
        assertTrue(result.validationWarnings.any { it.contains("Closing reading") })
    }

    @Test
    fun `unvalidated and incomplete tariffs cannot be selected for calculations`() {
        val joburg = TariffRepository.getTariffProfileById("JHB_PREPAID_LOW")!!
        val ethekwini = TariffRepository.getTariffProfileById("ETH_RES_PREPAID")!!
        val tou = TariffRepository.getTariffProfileById("CPT_RESIDENTIAL_TOU")!!

        assertEquals(VerificationStatus.NEEDS_REVIEW, joburg.verificationStatus)
        assertEquals(VerificationStatus.NEEDS_REVIEW, ethekwini.verificationStatus)
        assertEquals(VerificationStatus.UNSUPPORTED, tou.verificationStatus)
        assertFalse(joburg.isCalculationSupported)
        assertFalse(ethekwini.isCalculationSupported)
        assertFalse(tou.isCalculationSupported)
    }

    @Test
    fun `tariff version is only valid inside its effective period`() {
        val tariff = TariffRepository.getTariffProfileById("ESK_HOMELIGHT_20A")!!

        assertTrue(tariff.isEffectiveOn(LocalDate.parse("2026-04-01")))
        assertTrue(tariff.isEffectiveOn(LocalDate.parse("2027-03-31")))
        assertFalse(tariff.isEffectiveOn(LocalDate.parse("2026-03-31")))
        assertFalse(tariff.isEffectiveOn(LocalDate.parse("2027-04-01")))
    }
}
