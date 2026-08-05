package com.example.smartelectricity.domain.insights

import kotlin.math.max

data class ApplianceCostEstimate(
    val dailyKwh: Double,
    val monthlyKwh: Double,
    val monthlyCostRand: Double,
    val annualCostRand: Double
)

data class MonthlyBudgetForecast(
    val dailyAverageRand: Double,
    val projectedMonthEndRand: Double,
    val remainingBudgetRand: Double,
    val safeDailyBudgetRand: Double,
    val progressFraction: Float,
    val isOnTrack: Boolean
)

object EnergyInsights {
    fun estimateApplianceCost(
        watts: Double,
        hoursPerDay: Double,
        daysPerMonth: Int,
        randPerKwh: Double
    ): ApplianceCostEstimate {
        val safeWatts = watts.coerceAtLeast(0.0)
        val safeHours = hoursPerDay.coerceIn(0.0, 24.0)
        val safeDays = daysPerMonth.coerceIn(0, 31)
        val safeRate = randPerKwh.coerceAtLeast(0.0)
        val dailyKwh = safeWatts / 1000.0 * safeHours
        val monthlyKwh = dailyKwh * safeDays
        val monthlyCost = monthlyKwh * safeRate

        return ApplianceCostEstimate(
            dailyKwh = dailyKwh,
            monthlyKwh = monthlyKwh,
            monthlyCostRand = monthlyCost,
            annualCostRand = monthlyCost * 12.0
        )
    }

    fun forecastMonthlyBudget(
        currentSpendRand: Double,
        monthlyBudgetRand: Double,
        dayOfMonth: Int,
        daysInMonth: Int
    ): MonthlyBudgetForecast {
        val safeDaysInMonth = max(daysInMonth, 1)
        val safeDay = dayOfMonth.coerceIn(1, safeDaysInMonth)
        val spend = currentSpendRand.coerceAtLeast(0.0)
        val budget = monthlyBudgetRand.coerceAtLeast(0.0)
        val remainingDays = (safeDaysInMonth - safeDay).coerceAtLeast(0)
        val dailyAverage = spend / safeDay
        val projected = dailyAverage * safeDaysInMonth
        val remaining = (budget - spend).coerceAtLeast(0.0)
        val safeDaily = if (remainingDays > 0) remaining / remainingDays else 0.0
        val progress = if (budget > 0.0) (spend / budget).toFloat().coerceIn(0f, 1f) else 1f

        return MonthlyBudgetForecast(
            dailyAverageRand = dailyAverage,
            projectedMonthEndRand = projected,
            remainingBudgetRand = remaining,
            safeDailyBudgetRand = safeDaily,
            progressFraction = progress,
            isOnTrack = projected <= budget
        )
    }
}
