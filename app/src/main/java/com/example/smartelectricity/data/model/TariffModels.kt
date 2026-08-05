package com.example.smartelectricity.data.model

import java.time.LocalDate

enum class MeterType(val displayName: String) {
    PREPAID("Prepaid Meter"),
    SMART("Smart Prepaid Meter"),
    CREDIT("Credit / Postpaid Meter")
}

enum class VerificationStatus(val label: String) {
    VERIFIED("Verified"),
    OFFICIAL_PARSED("Official source · Review pending"),
    RECENTLY_CHANGED("Recently Changed"),
    SCHEDULED("Scheduled"),
    ESTIMATE("Estimate"),
    NEEDS_REVIEW("Needs Review"),
    UNSUPPORTED("Unsupported"),
    STALE("Stale Data Warning")
}

enum class FixedChargeRecoveryRule {
    NONE,
    DAILY_ACCRUAL_AT_VENDING,
    FULL_MONTH_AT_FIRST_VENDING,
    MONTHLY_ACCOUNT_CHARGE,
    PRO_RATA_30_DAY_MONTH
}

data class TariffBlock(
    val blockNumber: Int,
    val minKwh: Double,
    val maxKwh: Double?, // null means infinity / upper bound
    val rateCentsPerKwh: Double // VAT inclusive rate in Cents
)

data class FbeConfig(
    val isAvailable: Boolean = true,
    val freeKwh: Double = 50.0,
    val monthlyUsageCapKwh: Double? = null,
    val propertyValuationCapRand: Double? = null,
    val indigentRegistrationRequired: Boolean = true,
    val allocationTiers: List<FbeAllocationTier> = emptyList(),
    val description: String = "50 kWh free per month for qualifying registered indigent or low-usage households."
)

data class FbeAllocationTier(
    val minHistoricAverageKwhInclusive: Double = 0.0,
    val maxHistoricAverageKwhExclusive: Double?,
    val freeKwh: Double
)

data class TariffProfile(
    val id: String,
    val distributorId: String,
    val code: String,
    val name: String,
    val description: String,
    val compatibleMeterTypes: List<MeterType>,
    val monthlyFixedChargeRand: Double = 0.0,
    val monthlyServiceFeeRand: Double = 0.0,
    val dailyFixedChargeRand: Double = 0.0,
    val fixedChargeRecoveryRule: FixedChargeRecoveryRule = FixedChargeRecoveryRule.NONE,
    val vatRatePercent: Double = 15.0,
    val vatInclusiveRates: Boolean = true,
    val blocks: List<TariffBlock>,
    val fbeConfig: FbeConfig = FbeConfig(),
    val effectiveFromStr: String = "2026-07-01",
    val effectiveToStr: String? = "2027-06-30",
    val effectiveDateStr: String = "1 July 2026",
    val sourceDocumentId: String = "",
    val sourceDocumentTitle: String = "Official 2026/2027 Municipal Tariff Schedule",
    val sourceUrl: String = "https://www.nersa.org.za",
    val verificationStatus: VerificationStatus = VerificationStatus.VERIFIED,
    val calculationEngineVersion: String = "2.0.0"
)

val TariffProfile.isCalculationSupported: Boolean
    get() = isEffectiveOn(LocalDate.now()) && (
        verificationStatus == VerificationStatus.VERIFIED ||
            verificationStatus == VerificationStatus.OFFICIAL_PARSED ||
            verificationStatus == VerificationStatus.RECENTLY_CHANGED ||
            verificationStatus == VerificationStatus.ESTIMATE
        )

fun TariffProfile.isEffectiveOn(date: LocalDate): Boolean = try {
    val start = LocalDate.parse(effectiveFromStr)
    val end = effectiveToStr?.let(LocalDate::parse)
    !date.isBefore(start) && (end == null || !date.isAfter(end))
} catch (_: Exception) {
    false
}

data class Distributor(
    val id: String,
    val name: String,
    val province: String,
    val logoAccentColorHex: String = "#0284C7",
    val recognitionClue: String,
    val profiles: List<TariffProfile>,
    val activeNotice: String? = null
)

data class BlockBreakdown(
    val blockNumber: Int,
    val rangeText: String,
    val kwhAllocated: Double,
    val rateCentsPerKwh: Double,
    val costRand: Double
)

data class CalculationResult(
    val mode: CalculationMode,
    val inputAmount: Double,
    val distributor: Distributor,
    val profile: TariffProfile,
    val isFirstPurchaseOfMonth: Boolean,
    val hasClaimedFbeThisMonth: Boolean,
    val grossPurchaseRand: Double,
    val vatAmountRand: Double,
    val fixedChargeDeductedRand: Double,
    val arrearsDeductedRand: Double = 0.0,
    val netEnergyPurchaseRand: Double,
    val paidKwh: Double,
    val freeFbeKwh: Double,
    val totalKwh: Double,
    val averageRateCentsPerKwh: Double,
    val effectiveRandPerKwh: Double = 0.0,
    val blockBreakdown: List<BlockBreakdown>,
    val stepExplanations: List<String>,
    val confidenceMessage: String,
    val verificationStatus: VerificationStatus,
    val effectiveDateStr: String,
    val sourceDocumentId: String,
    val sourceTitle: String,
    val sourceUrl: String,
    val calculationEngineVersion: String,
    val remainingKwhInCurrentBlock: Double? = null,
    val nextBlockRateCentsPerKwh: Double? = null,
    val isCalculationValid: Boolean = true,
    val validationWarnings: List<String> = emptyList()
)

enum class CalculationMode {
    RAND_TO_KWH,
    KWH_TO_RAND,
    CONVENTIONAL_BILL
}

data class ReconciliationResult(
    val actualUnitsReceived: Double,
    val expectedUnits: Double,
    val differenceKwh: Double,
    val percentageDiff: Double,
    val primaryDiagnosis: String,
    val detailedCauses: List<String>,
    val isWithinNormalTolerance: Boolean
)
