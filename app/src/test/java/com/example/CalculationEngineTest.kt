package com.example

import com.example.smartelectricity.data.model.Distributor
import com.example.smartelectricity.data.model.FbeConfig
import com.example.smartelectricity.data.model.MeterType
import com.example.smartelectricity.data.model.TariffBlock
import com.example.smartelectricity.data.model.TariffProfile
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
}
