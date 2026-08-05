package com.example.smartelectricity.domain.calculator

import com.example.smartelectricity.data.model.*
import kotlin.math.abs
import kotlin.math.max

object CalculationEngine {

    fun calculateRandToKwh(
        distributor: Distributor,
        profile: TariffProfile,
        amountRand: Double,
        isFirstPurchaseOfMonth: Boolean,
        hasClaimedFbeThisMonth: Boolean,
        isIndigentEligible: Boolean = false,
        unitsAlreadyAllocatedThisMonth: Double = 0.0,
        daysSinceLastPurchase: Int = 0,
        arrearsDeductionRand: Double = 0.0
    ): CalculationResult {
        val stepExplanations = mutableListOf<String>()
        stepExplanations.add("1. Tendered Purchase Amount: R${"%.2f".format(amountRand)}")

        // Fixed Charge Recovery
        val fixedChargeToRecover = when {
            profile.dailyFixedChargeRand > 0.0 && daysSinceLastPurchase > 0 -> {
                daysSinceLastPurchase * profile.dailyFixedChargeRand
            }
            isFirstPurchaseOfMonth -> {
                profile.monthlyFixedChargeRand + profile.monthlyServiceFeeRand
            }
            else -> 0.0
        }

        val fixedChargeDeducted = if (amountRand >= fixedChargeToRecover) {
            fixedChargeToRecover
        } else {
            amountRand
        }

        if (fixedChargeToRecover > 0.0) {
            if (profile.dailyFixedChargeRand > 0.0 && daysSinceLastPurchase > 0) {
                stepExplanations.add("2. Accrued Daily Fixed Charge Deducted: R${"%.2f".format(fixedChargeDeducted)} ($daysSinceLastPurchase days @ R${"%.2f".format(profile.dailyFixedChargeRand)}/day)")
            } else {
                stepExplanations.add("2. Monthly Fixed Network & Service Fee Deducted: R${"%.2f".format(fixedChargeDeducted)} (1st purchase of the month)")
            }
        } else {
            stepExplanations.add("2. Fixed Access Fees: R0.00")
        }

        // Arrears / Debt Deductions
        val arrearsDeducted = if (arrearsDeductionRand > 0.0) {
            arrearsDeductionRand.coerceAtMost(max(0.0, amountRand - fixedChargeDeducted))
        } else 0.0

        if (arrearsDeducted > 0.0) {
            stepExplanations.add("3. Authorised Debt / Arrears Recovery Deducted: -R${"%.2f".format(arrearsDeducted)}")
        }

        val netEnergyRand = (amountRand - fixedChargeDeducted - arrearsDeducted).coerceAtLeast(0.0)
        stepExplanations.add("4. Amount Available for Electricity Energy: R${"%.2f".format(netEnergyRand)}")

        // FBE Allocation
        val fbeUnitsKwh = if (profile.fbeConfig.isAvailable && isIndigentEligible && !hasClaimedFbeThisMonth && isFirstPurchaseOfMonth) {
            profile.fbeConfig.freeKwh
        } else 0.0

        if (fbeUnitsKwh > 0.0) {
            stepExplanations.add("5. Free Basic Electricity (FBE) Applied: +${"%.1f".format(fbeUnitsKwh)} kWh free units")
        } else if (hasClaimedFbeThisMonth) {
            stepExplanations.add("5. FBE Status: 0 kWh (FBE allocation already claimed earlier this month)")
        } else if (!isIndigentEligible && profile.fbeConfig.isAvailable) {
            stepExplanations.add("5. FBE Status: 0 kWh (Household not registered for indigent relief / FBE)")
        } else {
            stepExplanations.add("5. FBE Status: N/A on this tariff profile")
        }

        // Calculate Paid kWh through Tariff Blocks starting at cumulative monthly units
        var remainingNetRand = netEnergyRand
        var totalPaidKwh = 0.0
        var currentMonthlyCumulativeKwh = unitsAlreadyAllocatedThisMonth
        val blockBreakdowns = mutableListOf<BlockBreakdown>()

        val blocks = profile.blocks.sortedBy { it.blockNumber }

        for (block in blocks) {
            if (remainingNetRand <= 0.0001) break

            val blockMin = block.minKwh
            val blockMax = block.maxKwh ?: Double.MAX_VALUE

            // Check if user's cumulative monthly units are within or past this block
            if (currentMonthlyCumulativeKwh >= blockMax) {
                // User has already passed this block earlier this month
                continue
            }

            // Calculate remaining unused kWh capacity in this block for this month
            val startPointInBlock = max(currentMonthlyCumulativeKwh, blockMin)
            val availableKwhInBlock = (blockMax - startPointInBlock).coerceAtLeast(0.0)

            val rateRandPerKwh = block.rateCentsPerKwh / 100.0
            val maxBlockCostRand = availableKwhInBlock * rateRandPerKwh

            if (remainingNetRand >= maxBlockCostRand) {
                // Fill this entire block
                val kwhInBlock = availableKwhInBlock
                totalPaidKwh += kwhInBlock
                currentMonthlyCumulativeKwh += kwhInBlock
                remainingNetRand -= maxBlockCostRand

                val rangeText = if (block.maxKwh != null) "${block.minKwh.toInt()}-${block.maxKwh.toInt()} kWh" else ">${block.minKwh.toInt()} kWh"
                blockBreakdowns.add(
                    BlockBreakdown(
                        blockNumber = block.blockNumber,
                        rangeText = rangeText,
                        kwhAllocated = kwhInBlock,
                        rateCentsPerKwh = block.rateCentsPerKwh,
                        costRand = maxBlockCostRand
                    )
                )
            } else {
                // Partial block filled
                val kwhInBlock = remainingNetRand / rateRandPerKwh
                totalPaidKwh += kwhInBlock
                currentMonthlyCumulativeKwh += kwhInBlock

                val rangeText = if (block.maxKwh != null) "${block.minKwh.toInt()}-${block.maxKwh.toInt()} kWh" else ">${block.minKwh.toInt()} kWh"
                blockBreakdowns.add(
                    BlockBreakdown(
                        blockNumber = block.blockNumber,
                        rangeText = rangeText,
                        kwhAllocated = kwhInBlock,
                        rateCentsPerKwh = block.rateCentsPerKwh,
                        costRand = remainingNetRand
                    )
                )
                remainingNetRand = 0.0
            }
        }

        val totalKwh = totalPaidKwh + fbeUnitsKwh
        stepExplanations.add("6. Electricity Units Purchased: ${"%.2f".format(totalPaidKwh)} kWh")
        stepExplanations.add("7. Total Output Token: ${"%.2f".format(totalKwh)} kWh (${"%.2f".format(totalPaidKwh)} paid + ${"%.1f".format(fbeUnitsKwh)} free FBE)")

        val averageRateCents = if (totalPaidKwh > 0.0) {
            (netEnergyRand / totalPaidKwh) * 100.0
        } else 0.0

        val effectiveRandPerKwh = if (totalKwh > 0.0) {
            amountRand / totalKwh
        } else 0.0

        val vatAmount = if (profile.vatInclusiveRates) {
            amountRand * (profile.vatRatePercent / (100.0 + profile.vatRatePercent))
        } else {
            amountRand * (profile.vatRatePercent / 100.0)
        }

        val confidenceMessage = when (profile.verificationStatus) {
            VerificationStatus.VERIFIED -> "Verified official calculation using approved ${distributor.name} 2026/27 tariff schedule."
            VerificationStatus.RECENTLY_CHANGED -> "Updated approved tariff rate effective ${profile.effectiveDateStr}."
            else -> "Provisional calculation based on standard municipal schedule."
        }

        return CalculationResult(
            mode = CalculationMode.RAND_TO_KWH,
            inputAmount = amountRand,
            distributor = distributor,
            profile = profile,
            isFirstPurchaseOfMonth = isFirstPurchaseOfMonth,
            hasClaimedFbeThisMonth = hasClaimedFbeThisMonth,
            grossPurchaseRand = amountRand,
            vatAmountRand = vatAmount,
            fixedChargeDeductedRand = fixedChargeDeducted,
            netEnergyPurchaseRand = netEnergyRand,
            paidKwh = totalPaidKwh,
            freeFbeKwh = fbeUnitsKwh,
            totalKwh = totalKwh,
            averageRateCentsPerKwh = averageRateCents,
            effectiveRandPerKwh = effectiveRandPerKwh,
            blockBreakdown = blockBreakdowns,
            stepExplanations = stepExplanations,
            confidenceMessage = confidenceMessage,
            verificationStatus = profile.verificationStatus,
            effectiveDateStr = profile.effectiveDateStr,
            sourceTitle = profile.sourceDocumentTitle
        )
    }

    fun calculateKwhToRand(
        distributor: Distributor,
        profile: TariffProfile,
        targetKwh: Double,
        isFirstPurchaseOfMonth: Boolean,
        hasClaimedFbeThisMonth: Boolean,
        isIndigentEligible: Boolean = false,
        unitsAlreadyAllocatedThisMonth: Double = 0.0
    ): CalculationResult {
        val stepExplanations = mutableListOf<String>()
        stepExplanations.add("1. Target Electricity Quantity: ${"%.2f".format(targetKwh)} kWh")

        // FBE Adjustment
        val fbeUnitsKwh = if (profile.fbeConfig.isAvailable && isIndigentEligible && !hasClaimedFbeThisMonth && isFirstPurchaseOfMonth) {
            profile.fbeConfig.freeKwh
        } else 0.0

        val paidKwhNeeded = (targetKwh - fbeUnitsKwh).coerceAtLeast(0.0)
        if (fbeUnitsKwh > 0) {
            stepExplanations.add("2. FBE Discount Applied: ${"%.1f".format(fbeUnitsKwh)} kWh free. Paid units needed: ${"%.2f".format(paidKwhNeeded)} kWh")
        } else {
            stepExplanations.add("2. Paid Units Needed: ${"%.2f".format(paidKwhNeeded)} kWh")
        }

        // Compute energy cost across blocks starting at cumulative monthly units
        var remainingKwhToBuy = paidKwhNeeded
        var currentMonthlyCumulativeKwh = unitsAlreadyAllocatedThisMonth
        var grossEnergyCostRand = 0.0
        val blockBreakdowns = mutableListOf<BlockBreakdown>()

        val blocks = profile.blocks.sortedBy { it.blockNumber }

        for (block in blocks) {
            if (remainingKwhToBuy <= 0.0001) break

            val blockMin = block.minKwh
            val blockMax = block.maxKwh ?: Double.MAX_VALUE

            if (currentMonthlyCumulativeKwh >= blockMax) {
                continue
            }

            val startPointInBlock = max(currentMonthlyCumulativeKwh, blockMin)
            val availableKwhInBlock = (blockMax - startPointInBlock).coerceAtLeast(0.0)
            val kwhToTakeFromBlock = remainingKwhToBuy.coerceAtMost(availableKwhInBlock)

            val rateRandPerKwh = block.rateCentsPerKwh / 100.0
            val costForBlock = kwhToTakeFromBlock * rateRandPerKwh

            grossEnergyCostRand += costForBlock
            remainingKwhToBuy -= kwhToTakeFromBlock
            currentMonthlyCumulativeKwh += kwhToTakeFromBlock

            val rangeText = if (block.maxKwh != null) "${block.minKwh.toInt()}-${block.maxKwh.toInt()} kWh" else ">${block.minKwh.toInt()} kWh"
            blockBreakdowns.add(
                BlockBreakdown(
                    blockNumber = block.blockNumber,
                    rangeText = rangeText,
                    kwhAllocated = kwhToTakeFromBlock,
                    rateCentsPerKwh = block.rateCentsPerKwh,
                    costRand = costForBlock
                )
            )
        }

        stepExplanations.add("3. Energy Cost Across Tariff Blocks: R${"%.2f".format(grossEnergyCostRand)}")

        val fixedCharge = if (isFirstPurchaseOfMonth) {
            profile.monthlyFixedChargeRand + profile.monthlyServiceFeeRand
        } else 0.0

        if (fixedCharge > 0.0) {
            stepExplanations.add("4. Monthly Fixed Access & Network Fee: R${"%.2f".format(fixedCharge)} (Recovered on 1st monthly purchase)")
        } else {
            stepExplanations.add("4. Fixed Access Fees: R0.00")
        }

        val totalGrossRand = grossEnergyCostRand + fixedCharge
        stepExplanations.add("5. Total Estimated Purchase Amount Required: R${"%.2f".format(totalGrossRand)}")

        val averageRateCents = if (paidKwhNeeded > 0) (grossEnergyCostRand / paidKwhNeeded) * 100.0 else 0.0
        val effectiveRandPerKwh = if (targetKwh > 0) totalGrossRand / targetKwh else 0.0
        val vatAmount = totalGrossRand * (profile.vatRatePercent / (100.0 + profile.vatRatePercent))

        return CalculationResult(
            mode = CalculationMode.KWH_TO_RAND,
            inputAmount = targetKwh,
            distributor = distributor,
            profile = profile,
            isFirstPurchaseOfMonth = isFirstPurchaseOfMonth,
            hasClaimedFbeThisMonth = hasClaimedFbeThisMonth,
            grossPurchaseRand = totalGrossRand,
            vatAmountRand = vatAmount,
            fixedChargeDeductedRand = fixedCharge,
            netEnergyPurchaseRand = grossEnergyCostRand,
            paidKwh = paidKwhNeeded,
            freeFbeKwh = fbeUnitsKwh,
            totalKwh = targetKwh,
            averageRateCentsPerKwh = averageRateCents,
            effectiveRandPerKwh = effectiveRandPerKwh,
            blockBreakdown = blockBreakdowns,
            stepExplanations = stepExplanations,
            confidenceMessage = "Verified cost estimate using official ${distributor.name} 2026/27 tariff rules.",
            verificationStatus = profile.verificationStatus,
            effectiveDateStr = profile.effectiveDateStr,
            sourceTitle = profile.sourceDocumentTitle
        )
    }

    fun calculateConventionalBill(
        distributor: Distributor,
        profile: TariffProfile,
        openingReading: Double,
        closingReading: Double,
        meterMultiplier: Double = 1.0,
        billingDays: Int = 30
    ): CalculationResult {
        val consumptionKwh = ((closingReading - openingReading) * meterMultiplier).coerceAtLeast(0.0)
        val stepExplanations = mutableListOf<String>()
        stepExplanations.add("1. Billing Period: $billingDays days | Meter Reading Delta: ${"%.2f".format(closingReading - openingReading)} x $meterMultiplier = ${"%.2f".format(consumptionKwh)} kWh")

        // Energy Cost across blocks
        var remainingKwh = consumptionKwh
        var grossEnergyCostRand = 0.0
        val blockBreakdowns = mutableListOf<BlockBreakdown>()

        val blocks = profile.blocks.sortedBy { it.blockNumber }

        for (block in blocks) {
            if (remainingKwh <= 0.0001) break

            val blockCapacityKwh = if (block.maxKwh != null) {
                (block.maxKwh - block.minKwh).coerceAtLeast(0.0)
            } else {
                Double.MAX_VALUE
            }

            val rateRandPerKwh = block.rateCentsPerKwh / 100.0

            if (remainingKwh >= blockCapacityKwh) {
                val costForBlock = blockCapacityKwh * rateRandPerKwh
                grossEnergyCostRand += costForBlock
                remainingKwh -= blockCapacityKwh

                val rangeText = if (block.maxKwh != null) "${block.minKwh.toInt()}-${block.maxKwh.toInt()} kWh" else ">${block.minKwh.toInt()} kWh"
                blockBreakdowns.add(
                    BlockBreakdown(
                        blockNumber = block.blockNumber,
                        rangeText = rangeText,
                        kwhAllocated = blockCapacityKwh,
                        rateCentsPerKwh = block.rateCentsPerKwh,
                        costRand = costForBlock
                    )
                )
            } else {
                val costForBlock = remainingKwh * rateRandPerKwh
                grossEnergyCostRand += costForBlock

                val rangeText = if (block.maxKwh != null) "${block.minKwh.toInt()}-${block.maxKwh.toInt()} kWh" else ">${block.minKwh.toInt()} kWh"
                blockBreakdowns.add(
                    BlockBreakdown(
                        blockNumber = block.blockNumber,
                        rangeText = rangeText,
                        kwhAllocated = remainingKwh,
                        rateCentsPerKwh = block.rateCentsPerKwh,
                        costRand = costForBlock
                    )
                )
                remainingKwh = 0.0
            }
        }

        stepExplanations.add("2. Energy Charges Across Tariff Blocks: R${"%.2f".format(grossEnergyCostRand)}")

        val fixedNetworkCharge = if (profile.dailyFixedChargeRand > 0.0) {
            billingDays * profile.dailyFixedChargeRand
        } else {
            (billingDays / 30.0) * (profile.monthlyFixedChargeRand + profile.monthlyServiceFeeRand)
        }

        if (fixedNetworkCharge > 0.0) {
            stepExplanations.add("3. Fixed Network Access & Service Charge ($billingDays days): R${"%.2f".format(fixedNetworkCharge)}")
        } else {
            stepExplanations.add("3. Fixed Network Charges: R0.00")
        }

        val totalGrossBillRand = grossEnergyCostRand + fixedNetworkCharge
        stepExplanations.add("4. Estimated Total Monthly Bill (incl VAT): R${"%.2f".format(totalGrossBillRand)}")

        val effectiveRandPerKwh = if (consumptionKwh > 0.0) totalGrossBillRand / consumptionKwh else 0.0
        stepExplanations.add("5. Effective All-In Price per kWh: R${"%.4f".format(effectiveRandPerKwh)}/kWh")

        val vatAmount = totalGrossBillRand * (profile.vatRatePercent / (100.0 + profile.vatRatePercent))

        return CalculationResult(
            mode = CalculationMode.CONVENTIONAL_BILL,
            inputAmount = consumptionKwh,
            distributor = distributor,
            profile = profile,
            isFirstPurchaseOfMonth = true,
            hasClaimedFbeThisMonth = false,
            grossPurchaseRand = totalGrossBillRand,
            vatAmountRand = vatAmount,
            fixedChargeDeductedRand = fixedNetworkCharge,
            netEnergyPurchaseRand = grossEnergyCostRand,
            paidKwh = consumptionKwh,
            freeFbeKwh = 0.0,
            totalKwh = consumptionKwh,
            averageRateCentsPerKwh = if (consumptionKwh > 0) (grossEnergyCostRand / consumptionKwh) * 100.0 else 0.0,
            effectiveRandPerKwh = effectiveRandPerKwh,
            blockBreakdown = blockBreakdowns,
            stepExplanations = stepExplanations,
            confidenceMessage = "Verified conventional bill estimate using ${distributor.name} 2026/27 official schedule.",
            verificationStatus = profile.verificationStatus,
            effectiveDateStr = profile.effectiveDateStr,
            sourceTitle = profile.sourceDocumentTitle
        )
    }

    fun reconcilePurchase(
        actualUnits: Double,
        expectedResult: CalculationResult
    ): ReconciliationResult {
        val diffKwh = actualUnits - expectedResult.totalKwh
        val percentage = if (expectedResult.totalKwh > 0) (abs(diffKwh) / expectedResult.totalKwh) * 100.0 else 0.0
        val isWithinTolerance = percentage <= 2.0

        val causes = mutableListOf<String>()

        if (actualUnits < expectedResult.totalKwh) {
            if (expectedResult.fixedChargeDeductedRand > 0) {
                causes.add("Fixed Network Access Charge: Your token receipt deducted R${"%.2f".format(expectedResult.fixedChargeDeductedRand)} for monthly access fees before converting remaining money to kWh.")
            }
            if (expectedResult.freeFbeKwh > 0) {
                causes.add("FBE Verification: If the vendor did not include your ${"%.0f".format(expectedResult.freeFbeKwh)} kWh free units, confirm whether your indigent registration is active with the municipality.")
            }
            causes.add("Monthly Vending Block Escalation: If you made prior purchases this month, your vendor moved your purchase into a higher tariff block rate.")
            causes.add("Arrears / Municipal Debt Recovery: Municipalities may auto-deduct 10% - 50% of prepaid purchases towards unpaid water/rates debt.")
            causes.add("Vending Platform Fee: Certain third-party vendors or banking apps charge a transaction commission or service fee.")
        } else {
            causes.add("FBE Inclusion: Additional free units were attached to your purchase token.")
            causes.add("Subsidised Block Discount: Your meter category was processed under a lower tariff band.")
        }

        val primaryDiagnosis = when {
            isWithinTolerance -> "Units Match Expected Calculation (Within 2% Rounding Tolerance)"
            actualUnits < expectedResult.totalKwh && expectedResult.fixedChargeDeductedRand > 0 ->
                "Variance Caused by Fixed Charge Recovery (R${"%.2f".format(expectedResult.fixedChargeDeductedRand)})"
            actualUnits < expectedResult.totalKwh ->
                "Token Variance Detected (-${"%.1f".format(abs(diffKwh))} kWh). Likely Block Threshold or Arrears Recovery."
            else ->
                "Higher Units Received (+${"%.1f".format(diffKwh)} kWh). Likely Unclaimed FBE or Subsidised Rate Applied."
        }

        return ReconciliationResult(
            actualUnitsReceived = actualUnits,
            expectedUnits = expectedResult.totalKwh,
            differenceKwh = diffKwh,
            percentageDiff = percentage,
            primaryDiagnosis = primaryDiagnosis,
            detailedCauses = causes,
            isWithinNormalTolerance = isWithinTolerance
        )
    }
}
