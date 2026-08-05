package com.example.smartelectricity.data.model

enum class MeterType(val displayName: String) {
    PREPAID("Prepaid Meter"),
    SMART("Smart Prepaid Meter"),
    CREDIT("Credit / Postpaid Meter")
}

enum class VerificationStatus(val label: String) {
    VERIFIED("Verified"),
    RECENTLY_CHANGED("Recently Changed"),
    SCHEDULED("Scheduled"),
    ESTIMATE("Estimate"),
    NEEDS_REVIEW("Needs Review"),
    UNSUPPORTED("Unsupported"),
    STALE("Stale Data Warning")
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
    val monthlyUsageCapKwh: Double? = 450.0,
    val propertyValuationCapRand: Double? = 150000.0,
    val indigentRegistrationRequired: Boolean = true,
    val description: String = "50 kWh free per month for qualifying registered indigent or low-usage households."
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
    val vatRatePercent: Double = 15.0,
    val vatInclusiveRates: Boolean = true,
    val blocks: List<TariffBlock>,
    val fbeConfig: FbeConfig = FbeConfig(),
    val effectiveDateStr: String = "1 July 2026",
    val sourceDocumentTitle: String = "Official 2026/2027 Municipal Tariff Schedule",
    val sourceUrl: String = "https://www.nersa.org.za",
    val verificationStatus: VerificationStatus = VerificationStatus.VERIFIED
)

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
    val sourceTitle: String
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
