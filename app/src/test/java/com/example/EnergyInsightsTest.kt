package com.example

import com.example.smartelectricity.domain.insights.EnergyInsights
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EnergyInsightsTest {
    @Test
    fun `appliance estimate converts watts and time into cost`() {
        val estimate = EnergyInsights.estimateApplianceCost(
            watts = 2000.0,
            hoursPerDay = 3.0,
            daysPerMonth = 30,
            randPerKwh = 3.25
        )

        assertEquals(6.0, estimate.dailyKwh, 0.001)
        assertEquals(180.0, estimate.monthlyKwh, 0.001)
        assertEquals(585.0, estimate.monthlyCostRand, 0.001)
        assertEquals(7020.0, estimate.annualCostRand, 0.001)
    }

    @Test
    fun `appliance estimate clamps impossible time values`() {
        val estimate = EnergyInsights.estimateApplianceCost(
            watts = 1000.0,
            hoursPerDay = 28.0,
            daysPerMonth = 40,
            randPerKwh = 2.0
        )

        assertEquals(24.0, estimate.dailyKwh, 0.001)
        assertEquals(744.0, estimate.monthlyKwh, 0.001)
    }

    @Test
    fun `forecast shows safe daily allowance when on track`() {
        val forecast = EnergyInsights.forecastMonthlyBudget(
            currentSpendRand = 600.0,
            monthlyBudgetRand = 1500.0,
            dayOfMonth = 15,
            daysInMonth = 30
        )

        assertTrue(forecast.isOnTrack)
        assertEquals(1200.0, forecast.projectedMonthEndRand, 0.001)
        assertEquals(60.0, forecast.safeDailyBudgetRand, 0.001)
        assertEquals(0.4f, forecast.progressFraction, 0.001f)
    }

    @Test
    fun `forecast flags projected overspend`() {
        val forecast = EnergyInsights.forecastMonthlyBudget(
            currentSpendRand = 1000.0,
            monthlyBudgetRand = 1500.0,
            dayOfMonth = 10,
            daysInMonth = 30
        )

        assertFalse(forecast.isOnTrack)
        assertEquals(3000.0, forecast.projectedMonthEndRand, 0.001)
    }
}
