package com.example.smartelectricity.data.repository

import com.example.smartelectricity.data.model.*

object TariffRepository {

    val distributors: List<Distributor> = listOf(
        Distributor(
            id = "ESKOM_DIRECT",
            name = "Eskom Direct",
            province = "National / Township / Rural",
            logoAccentColorHex = "#15803D",
            recognitionClue = "Look for Eskom 11-digit meter numbers starting with 04, or 'Eskom Holdings' on your purchase receipt.",
            activeNotice = "Eskom 2026/2027 Retail Tariff Adjustment effective 1 April 2026.",
            profiles = listOf(
                TariffProfile(
                    id = "ESK_HOMELIGHT_20A",
                    distributorId = "ESKOM_DIRECT",
                    code = "HOMELIGHT_20A",
                    name = "Eskom Homelight 20A",
                    description = "Subsidised low-use single-phase supply for Eskom prepaid meters.",
                    compatibleMeterTypes = listOf(MeterType.PREPAID),
                    monthlyFixedChargeRand = 0.0,
                    dailyFixedChargeRand = 0.0,
                    fixedChargeRecoveryRule = FixedChargeRecoveryRule.NONE,
                    vatInclusiveRates = true,
                    blocks = listOf(
                        TariffBlock(1, 0.0, null, 270.30)
                    ),
                    fbeConfig = FbeConfig(
                        freeKwh = 50.0,
                        monthlyUsageCapKwh = 350.0,
                        indigentRegistrationRequired = true,
                        description = "50 kWh free monthly via municipal indigent registration."
                    ),
                    effectiveFromStr = "2026-04-01",
                    effectiveToStr = "2027-03-31",
                    effectiveDateStr = "1 April 2026",
                    sourceDocumentId = "src_eskom_standard_prices_2026",
                    sourceDocumentTitle = "Eskom Schedule of Standard Prices 2026/2027",
                    sourceUrl = "https://www.eskom.co.za/distribution/2026-2027-tariff-increase/",
                    verificationStatus = VerificationStatus.VERIFIED
                ),
                TariffProfile(
                    id = "ESK_HOMELIGHT_60A",
                    distributorId = "ESKOM_DIRECT",
                    code = "HOMELIGHT_60A",
                    name = "Eskom Homelight 60A",
                    description = "Standard 60A single-phase supply for Eskom direct residential customers.",
                    compatibleMeterTypes = listOf(MeterType.PREPAID, MeterType.SMART),
                    monthlyFixedChargeRand = 0.0,
                    dailyFixedChargeRand = 0.0,
                    fixedChargeRecoveryRule = FixedChargeRecoveryRule.NONE,
                    vatInclusiveRates = true,
                    blocks = listOf(
                        TariffBlock(1, 0.0, null, 343.61)
                    ),
                    fbeConfig = FbeConfig(
                        freeKwh = 50.0,
                        monthlyUsageCapKwh = 400.0,
                        indigentRegistrationRequired = true
                    ),
                    effectiveFromStr = "2026-04-01",
                    effectiveToStr = "2027-03-31",
                    effectiveDateStr = "1 April 2026",
                    sourceDocumentId = "src_eskom_standard_prices_2026",
                    sourceDocumentTitle = "Eskom Schedule of Standard Prices 2026/2027",
                    sourceUrl = "https://www.eskom.co.za/distribution/2026-2027-tariff-increase/",
                    verificationStatus = VerificationStatus.VERIFIED
                ),
                TariffProfile(
                    id = "ESK_HOMEPOWER_1",
                    distributorId = "ESKOM_DIRECT",
                    code = "HOMEPOWER_1",
                    name = "Eskom Homepower 1",
                    description = "Residential supply with daily fixed capacity/network charges (R23.02/day) and unbundled energy rates (355.56 c/kWh).",
                    compatibleMeterTypes = listOf(MeterType.CREDIT, MeterType.SMART),
                    monthlyFixedChargeRand = 0.0,
                    dailyFixedChargeRand = 23.02, // R15.17 network capacity + R6.60 service/admin + R1.25 GCC = R23.02/day
                    fixedChargeRecoveryRule = FixedChargeRecoveryRule.MONTHLY_ACCOUNT_CHARGE,
                    vatInclusiveRates = true,
                    blocks = listOf(
                        TariffBlock(1, 0.0, null, 355.56) // 322.06 energy + 0.52 ancillary + 32.98 network demand
                    ),
                    fbeConfig = FbeConfig(
                        freeKwh = 0.0,
                        isAvailable = false,
                        description = "Homepower 1 does not qualify for FBE."
                    ),
                    effectiveFromStr = "2026-04-01",
                    effectiveToStr = "2027-03-31",
                    effectiveDateStr = "1 April 2026",
                    sourceDocumentId = "src_eskom_standard_prices_2026",
                    sourceDocumentTitle = "Eskom Schedule of Standard Prices 2026/2027 (Homepower)",
                    sourceUrl = "https://www.eskom.co.za/distribution/2026-2027-tariff-increase/",
                    verificationStatus = VerificationStatus.VERIFIED
                ),
                TariffProfile(
                    id = "ESK_BUSINESSRATE_2",
                    distributorId = "ESKOM_DIRECT",
                    code = "BUSINESSRATE_2",
                    name = "Eskom Businessrate 2",
                    description = "Commercial supply up to 100 kVA. Daily fixed charges R61.33/day and variable energy R3.0422/kWh.",
                    compatibleMeterTypes = listOf(MeterType.CREDIT, MeterType.SMART),
                    monthlyFixedChargeRand = 0.0,
                    dailyFixedChargeRand = 61.33, // R37.79 + R18.39 + R5.15 = R61.33/day
                    fixedChargeRecoveryRule = FixedChargeRecoveryRule.MONTHLY_ACCOUNT_CHARGE,
                    vatInclusiveRates = true,
                    blocks = listOf(
                        TariffBlock(1, 0.0, null, 304.22) // 279.34 + 0.52 + 18.18 + 6.18 = 304.22 c/kWh
                    ),
                    fbeConfig = FbeConfig(isAvailable = false),
                    effectiveFromStr = "2026-04-01",
                    effectiveToStr = "2027-03-31",
                    effectiveDateStr = "1 April 2026",
                    sourceDocumentId = "src_eskom_standard_prices_2026",
                    sourceDocumentTitle = "Eskom Schedule of Standard Prices 2026/2027 (Businessrate)",
                    sourceUrl = "https://www.eskom.co.za/distribution/2026-2027-tariff-increase/",
                    verificationStatus = VerificationStatus.VERIFIED
                )
            )
        ),
        Distributor(
            id = "TSHWANE",
            name = "City of Tshwane",
            province = "Gauteng",
            logoAccentColorHex = "#0284C7",
            recognitionClue = "Look for 'Tshwane PrePaid' or municipal account numbers starting with 010/020.",
            activeNotice = "NERSA approved 2026/27 residential electricity tariff structure effective 1 July 2026.",
            profiles = listOf(
                TariffProfile(
                    id = "TSH_RESIDENTIAL_IBT",
                    distributorId = "TSHWANE",
                    code = "RESIDENTIAL_IBT",
                    name = "Tshwane Residential IBT",
                    description = "4-tier Increasing Block Tariff for Tshwane prepaid & postpaid residential consumers.",
                    compatibleMeterTypes = listOf(MeterType.PREPAID, MeterType.SMART, MeterType.CREDIT),
                    monthlyFixedChargeRand = 0.0,
                    dailyFixedChargeRand = 0.0,
                    fixedChargeRecoveryRule = FixedChargeRecoveryRule.NONE,
                    vatInclusiveRates = true,
                    blocks = listOf(
                        TariffBlock(1, 0.0, 100.0, 324.12),
                        TariffBlock(2, 100.0, 400.0, 379.32),
                        TariffBlock(3, 400.0, 650.0, 413.27),
                        TariffBlock(4, 650.0, null, 445.51)
                    ),
                    fbeConfig = FbeConfig(
                        freeKwh = 100.0,
                        monthlyUsageCapKwh = 450.0,
                        indigentRegistrationRequired = true,
                        description = "100 kWh free monthly for certified indigent households."
                    ),
                    effectiveFromStr = "2026-07-01",
                    effectiveToStr = "2027-06-30",
                    effectiveDateStr = "1 July 2026",
                    sourceDocumentId = "src_tshwane_notice_2026",
                    sourceDocumentTitle = "City of Tshwane Approved Electricity Tariffs 2026/2027",
                    sourceUrl = "https://www.tshwane.gov.za/",
                    verificationStatus = VerificationStatus.OFFICIAL_PARSED
                )
            )
        ),
        Distributor(
            id = "CAPE_TOWN",
            name = "City of Cape Town",
            province = "Western Cape",
            logoAccentColorHex = "#0D9488",
            recognitionClue = "Look for 'City of Cape Town' or 14-digit meter serials on your token slip.",
            activeNotice = "City of Cape Town 2026/2027 Electricity Tariff Schedule.",
            profiles = listOf(
                TariffProfile(
                    id = "CPT_DOMESTIC",
                    distributorId = "CAPE_TOWN",
                    code = "DOMESTIC",
                    name = "Cape Town Domestic",
                    description = "Monthly fixed charge R74.77 (~R2.46/day via prepaid) with 2-tier Increasing Block Tariff.",
                    compatibleMeterTypes = listOf(MeterType.PREPAID, MeterType.CREDIT),
                    monthlyFixedChargeRand = 74.77,
                    dailyFixedChargeRand = 2.46,
                    fixedChargeRecoveryRule = FixedChargeRecoveryRule.DAILY_ACCRUAL_AT_VENDING,
                    vatInclusiveRates = true,
                    blocks = listOf(
                        TariffBlock(1, 0.0, 600.0, 413.79),
                        TariffBlock(2, 600.0, null, 493.90)
                    ),
                    fbeConfig = FbeConfig(
                        freeKwh = 0.0,
                        isAvailable = false,
                        description = "Domestic tariff does not qualify for FBE."
                    ),
                    effectiveFromStr = "2026-07-01",
                    effectiveToStr = "2027-06-30",
                    effectiveDateStr = "1 July 2026",
                    sourceDocumentId = "src_cct_residential_2026",
                    sourceDocumentTitle = "City of Cape Town Understanding Residential Tariffs 2026/27",
                    sourceUrl = "https://www.capetown.gov.za/tariffs/",
                    verificationStatus = VerificationStatus.VERIFIED
                ),
                TariffProfile(
                    id = "CPT_HOME_USER",
                    distributorId = "CAPE_TOWN",
                    code = "HOME_USER",
                    name = "Cape Town Home User",
                    description = "Monthly fixed charge R424.30 with lower per-unit energy rates (355.95 c/kWh).",
                    compatibleMeterTypes = listOf(MeterType.PREPAID, MeterType.SMART, MeterType.CREDIT),
                    monthlyFixedChargeRand = 424.30,
                    dailyFixedChargeRand = 0.0,
                    fixedChargeRecoveryRule = FixedChargeRecoveryRule.FULL_MONTH_AT_FIRST_VENDING,
                    vatInclusiveRates = true,
                    blocks = listOf(
                        TariffBlock(1, 0.0, 600.0, 355.95),
                        TariffBlock(2, 600.0, null, 469.06)
                    ),
                    fbeConfig = FbeConfig(isAvailable = false),
                    effectiveFromStr = "2026-07-01",
                    effectiveToStr = "2027-06-30",
                    effectiveDateStr = "1 July 2026",
                    sourceDocumentId = "src_cct_residential_2026",
                    sourceDocumentTitle = "City of Cape Town Electricity Tariffs 2026/27",
                    sourceUrl = "https://www.capetown.gov.za/tariffs/",
                    verificationStatus = VerificationStatus.VERIFIED
                ),
                TariffProfile(
                    id = "CPT_LIFELINE",
                    distributorId = "CAPE_TOWN",
                    code = "LIFELINE",
                    name = "Cape Town Lifeline",
                    description = "Subsidised flat rate (283.04 c/kWh) for properties valued under R500k & average usage < 450 kWh/m.",
                    compatibleMeterTypes = listOf(MeterType.PREPAID),
                    monthlyFixedChargeRand = 0.0,
                    dailyFixedChargeRand = 0.0,
                    fixedChargeRecoveryRule = FixedChargeRecoveryRule.NONE,
                    vatInclusiveRates = true,
                    blocks = listOf(
                        TariffBlock(1, 0.0, null, 283.04)
                    ),
                    fbeConfig = FbeConfig(
                        freeKwh = 60.0,
                        monthlyUsageCapKwh = 450.0,
                        propertyValuationCapRand = 500000.0,
                        indigentRegistrationRequired = false,
                        allocationTiers = listOf(
                            FbeAllocationTier(0.0, 250.0, 60.0),
                            FbeAllocationTier(250.0, 450.000001, 25.0)
                        ),
                        description = "60 kWh free per month for average usage < 250 kWh/m, or 25 kWh free for 250-450 kWh/m."
                    ),
                    effectiveFromStr = "2026-07-01",
                    effectiveToStr = "2027-06-30",
                    effectiveDateStr = "1 July 2026",
                    sourceDocumentId = "src_cct_residential_2026",
                    sourceDocumentTitle = "City of Cape Town Lifeline & Social Relief Policy 2026",
                    sourceUrl = "https://www.capetown.gov.za/tariffs/",
                    verificationStatus = VerificationStatus.VERIFIED
                ),
                TariffProfile(
                    id = "CPT_RESIDENTIAL_TOU",
                    distributorId = "CAPE_TOWN",
                    code = "RESIDENTIAL_TOU",
                    name = "Cape Town Residential TOU",
                    description = "Time-of-Use tariff with seasonal Peak/Standard/Off-Peak energy rates and fixed admin (R396.16) + capacity (R288.48) charges.",
                    compatibleMeterTypes = listOf(MeterType.SMART),
                    monthlyFixedChargeRand = 684.64, // R396.16 admin + R288.48 capacity
                    dailyFixedChargeRand = 0.0,
                    fixedChargeRecoveryRule = FixedChargeRecoveryRule.MONTHLY_ACCOUNT_CHARGE,
                    vatInclusiveRates = true,
                    blocks = listOf(
                        TariffBlock(1, 0.0, null, 418.54) // Default summer peak
                    ),
                    fbeConfig = FbeConfig(isAvailable = false),
                    effectiveFromStr = "2026-07-01",
                    effectiveToStr = "2027-06-30",
                    effectiveDateStr = "1 July 2026",
                    sourceDocumentId = "src_cct_residential_2026",
                    sourceDocumentTitle = "City of Cape Town Residential Time-of-Use Schedule 2026",
                    sourceUrl = "https://www.capetown.gov.za/tariffs/",
                    verificationStatus = VerificationStatus.UNSUPPORTED
                )
            )
        ),
        Distributor(
            id = "JOHANNESBURG_POWER",
            name = "City Power Johannesburg",
            province = "Gauteng",
            logoAccentColorHex = "#EA580C",
            recognitionClue = "Look for 'City Power Joburg' or Joburg Water/Electricity combined account numbers.",
            activeNotice = "City Power 2026/27 NERSA Approved Tariffs.",
            profiles = listOf(
                TariffProfile(
                    id = "JHB_PREPAID_LOW",
                    distributorId = "JOHANNESBURG_POWER",
                    code = "PREPAID_LOW",
                    name = "City Power Prepaid Low Usage",
                    description = "Increasing Block Tariff for low-to-medium usage households in Johannesburg.",
                    compatibleMeterTypes = listOf(MeterType.PREPAID, MeterType.SMART),
                    monthlyFixedChargeRand = 0.0,
                    dailyFixedChargeRand = 0.0,
                    fixedChargeRecoveryRule = FixedChargeRecoveryRule.NONE,
                    vatInclusiveRates = true,
                    blocks = listOf(
                        TariffBlock(1, 0.0, 300.0, 278.40),
                        TariffBlock(2, 300.0, 500.0, 342.10),
                        TariffBlock(3, 500.0, null, 412.50)
                    ),
                    fbeConfig = FbeConfig(
                        freeKwh = 50.0,
                        monthlyUsageCapKwh = 500.0,
                        indigentRegistrationRequired = true,
                        description = "50 kWh free monthly under the Expanded Social Package (ESP)."
                    ),
                    effectiveFromStr = "2026-07-01",
                    effectiveToStr = "2027-06-30",
                    effectiveDateStr = "1 July 2026",
                    sourceDocumentId = "src_joburg_electricity_2026",
                    sourceDocumentTitle = "City Power Johannesburg Schedule of Charges 2026",
                    sourceUrl = "https://www.citypower.co.za/",
                    verificationStatus = VerificationStatus.NEEDS_REVIEW
                )
            )
        ),
        Distributor(
            id = "ETHEKWINI",
            name = "eThekwini Municipality (Durban)",
            province = "KwaZulu-Natal",
            logoAccentColorHex = "#7C3AED",
            recognitionClue = "Look for 'eThekwini Electricity' or Durban Metro vendor references on receipt.",
            activeNotice = "eThekwini Electricity Tariff Schedule 2026/27.",
            profiles = listOf(
                TariffProfile(
                    id = "ETH_RES_PREPAID",
                    distributorId = "ETHEKWINI",
                    code = "RES_PREPAID",
                    name = "eThekwini Single Phase Prepaid",
                    description = "Single-tier flat residential rate for eThekwini Durban prepaid consumers.",
                    compatibleMeterTypes = listOf(MeterType.PREPAID, MeterType.SMART),
                    monthlyFixedChargeRand = 0.0,
                    dailyFixedChargeRand = 0.0,
                    fixedChargeRecoveryRule = FixedChargeRecoveryRule.NONE,
                    vatInclusiveRates = true,
                    blocks = listOf(
                        TariffBlock(1, 0.0, null, 328.70)
                    ),
                    fbeConfig = FbeConfig(
                        freeKwh = 65.0,
                        monthlyUsageCapKwh = 500.0,
                        indigentRegistrationRequired = true,
                        description = "65 kWh free per month for poverty-alleviation registered households."
                    ),
                    effectiveFromStr = "2026-07-01",
                    effectiveToStr = "2027-06-30",
                    effectiveDateStr = "1 July 2026",
                    sourceDocumentId = "src_ethekwini_tariffs_2026",
                    sourceDocumentTitle = "eThekwini Electricity Rates Gazette 2026",
                    sourceUrl = "https://www.durban.gov.za/",
                    verificationStatus = VerificationStatus.NEEDS_REVIEW
                )
            )
        )
    )

    fun getDistributorById(id: String): Distributor? {
        return distributors.find { it.id.equals(id, ignoreCase = true) }
    }

    fun getTariffProfileById(id: String): TariffProfile? {
        distributors.forEach { dist ->
            dist.profiles.find { it.id.equals(id, ignoreCase = true) }?.let { return it }
        }
        return null
    }

    fun searchDistributor(query: String): List<Distributor> {
        if (query.isBlank()) return distributors
        val q = query.trim().lowercase()
        return distributors.filter {
            it.name.lowercase().contains(q) ||
            it.province.lowercase().contains(q) ||
            it.recognitionClue.lowercase().contains(q)
        }
    }
}
