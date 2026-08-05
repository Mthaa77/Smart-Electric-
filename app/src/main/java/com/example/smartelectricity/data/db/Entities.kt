package com.example.smartelectricity.data.db

import androidx.room.Entity
import androidx.room.ColumnInfo
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "households")
data class HouseholdEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nickname: String,
    val distributorId: String,
    val tariffProfileId: String,
    val meterType: String,
    val suburbOrMunicipality: String,
    val isIndigentRegistered: Boolean = false,
    val propertyValuationRand: Double = 0.0,
    val estimatedMonthlyKwh: Double = 350.0,
    val hasClaimedFbeThisMonth: Boolean = false,
    val updatedTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "calculation_history")
data class CalculationHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val householdId: Int? = null,
    val dateTimestamp: Long = System.currentTimeMillis(),
    val distributorId: String,
    val distributorName: String,
    val tariffProfileId: String,
    val tariffProfileName: String,
    val mode: String, // "RAND_TO_KWH" or "KWH_TO_RAND"
    val inputValue: Double,
    val totalKwh: Double,
    val totalCostRand: Double,
    val fixedChargeDeductedRand: Double,
    val fbeUnitsKwh: Double,
    val effectiveDateStr: String,
    @ColumnInfo(defaultValue = "0") val isCommittedPurchase: Boolean = false,
    val isReconciled: Boolean = false,
    val actualUnitsReceived: Double? = null,
    val mismatchCause: String? = null
)

@Entity(tableName = "tariff_alerts")
data class TariffAlertEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val distributorId: String,
    val distributorName: String,
    val summary: String,
    val status: String, // "APPROVED", "PROPOSED", "EFFECTIVE"
    val effectiveDate: String,
    val percentageChange: Double,
    val sourceDocumentTitle: String,
    val isRead: Boolean = false,
    val createdTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "weekly_spend_logs")
data class WeeklySpendEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val weekLabel: String, // e.g. "Week 1 (Aug 1 - Aug 7)", "Week 2", etc.
    val amountRand: Double,
    val estimatedKwh: Double = 0.0,
    val dateTimestamp: Long = System.currentTimeMillis(),
    val notes: String = ""
)

@Entity(tableName = "distributors")
data class DistributorEntity(
    @PrimaryKey val id: String,
    val name: String,
    val province: String,
    val logoAccentColorHex: String = "#0284C7",
    val recognitionClue: String,
    val activeNotice: String? = null
)

@Entity(
    tableName = "tariff_profiles",
    foreignKeys = [
        ForeignKey(
            entity = DistributorEntity::class,
            parentColumns = ["id"],
            childColumns = ["distributorId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["distributorId"])]
)
data class TariffProfileEntity(
    @PrimaryKey val id: String,
    val distributorId: String,
    val code: String,
    val name: String,
    val description: String,
    val compatibleMeterTypesCsv: String = "PREPAID,SMART",
    val monthlyFixedChargeRand: Double = 0.0,
    val monthlyServiceFeeRand: Double = 0.0,
    @ColumnInfo(defaultValue = "0.0") val dailyFixedChargeRand: Double = 0.0,
    @ColumnInfo(defaultValue = "'NONE'") val fixedChargeRecoveryRule: String = "NONE",
    val vatRatePercent: Double = 15.0,
    val vatInclusiveRates: Boolean = true,
    val fbeAvailable: Boolean = true,
    val fbeFreeKwh: Double = 50.0,
    val fbeMonthlyUsageCapKwh: Double? = 450.0,
    val fbePropertyValuationCapRand: Double? = 150000.0,
    val fbeIndigentRegistrationRequired: Boolean = true,
    @ColumnInfo(defaultValue = "''") val fbeAllocationTiersCsv: String = "",
    val fbeDescription: String = "50 kWh free per month",
    @ColumnInfo(defaultValue = "'2026-07-01'") val effectiveFromStr: String = "2026-07-01",
    @ColumnInfo(defaultValue = "'2027-06-30'") val effectiveToStr: String? = "2027-06-30",
    val effectiveDateStr: String = "1 July 2026",
    @ColumnInfo(defaultValue = "''") val sourceDocumentId: String = "",
    val sourceDocumentTitle: String = "Official 2026/2027 Municipal Tariff Schedule",
    val sourceUrl: String = "https://www.nersa.org.za",
    val verificationStatus: String = "VERIFIED",
    @ColumnInfo(defaultValue = "'2.0.0'") val calculationEngineVersion: String = "2.0.0"
)

@Entity(
    tableName = "tariff_blocks",
    foreignKeys = [
        ForeignKey(
            entity = TariffProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["profileId"])]
)
data class TariffBlockEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val profileId: String,
    val blockNumber: Int,
    val minKwh: Double,
    val maxKwh: Double?,
    val rateCentsPerKwh: Double
)

@Entity(
    tableName = "prepaid_purchases",
    indices = [Index(value = ["householdId", "yearMonth"])]
)
data class PrepaidPurchaseEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val householdId: Int,
    val purchaseTimestamp: Long = System.currentTimeMillis(),
    val yearMonth: String,
    val tenderAmountRand: Double,
    val energyValueRand: Double,
    val fixedDeductionRand: Double,
    val arrearsDeductionRand: Double,
    val paidUnitsKwh: Double,
    val fbeUnitsKwh: Double,
    val estimatedTotalUnitsKwh: Double,
    val actualUnitsKwh: Double? = null,
    val tariffProfileId: String,
    val tariffEffectiveFromStr: String,
    val sourceDocumentId: String,
    val calculationEngineVersion: String
)

@Entity(
    tableName = "monthly_block_ledgers",
    indices = [Index(value = ["householdId", "yearMonth", "tariffProfileId"], unique = true)]
)
data class MonthlyBlockLedgerEntity(
    @PrimaryKey val ledgerKey: String,
    val householdId: Int,
    val yearMonth: String,
    val tariffProfileId: String,
    val paidUnitsAllocatedKwh: Double = 0.0,
    val freeUnitsAllocatedKwh: Double = 0.0,
    val purchasedAmountRand: Double = 0.0,
    val lastPurchaseAt: Long? = null,
    val lastFixedChargeRecoveryAt: Long? = null,
    val updatedAt: Long = System.currentTimeMillis()
)
