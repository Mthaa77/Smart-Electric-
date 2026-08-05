package com.example.smartelectricity.data.repository

import com.example.smartelectricity.data.db.DistributorEntity
import com.example.smartelectricity.data.db.TariffBlockEntity
import com.example.smartelectricity.data.db.TariffDao
import com.example.smartelectricity.data.db.TariffProfileEntity
import com.example.smartelectricity.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TariffDataRepository(private val tariffDao: TariffDao) {

    /**
     * Observe all distributors with their full tariff profiles and blocks reactively from Room.
     */
    fun getAllDistributorsFlow(): Flow<List<Distributor>> {
        return tariffDao.getAllDistributors().map { distributorEntities ->
            distributorEntities.map { distEntity ->
                buildDistributorFromDb(distEntity)
            }
        }
    }

    /**
     * Get a single distributor by ID with populated profiles and tariff blocks from Room.
     */
    suspend fun getDistributorById(id: String): Distributor? {
        val distEntity = tariffDao.getDistributorById(id) ?: return null
        return buildDistributorFromDb(distEntity)
    }

    /**
     * Get a specific tariff profile with its ordered tariff blocks by profile ID.
     */
    suspend fun getTariffProfileById(profileId: String): TariffProfile? {
        val profileEntity = tariffDao.getProfileById(profileId) ?: return null
        val blocks = tariffDao.getBlocksForProfileSync(profileEntity.id).map { it.toDomainModel() }
        return profileEntity.toDomainModel(blocks)
    }

    /**
     * Seed predefined South African electricity tariff blocks and distributor rates into Room if database is empty.
     */
    suspend fun seedPredefinedTariffsIfEmpty() {
        val existing = tariffDao.getDistributorById("TSHWANE")
        if (existing == null) {
            val (distEntities, profileEntities, blockEntities) = mapDomainToEntities(TariffRepository.distributors)
            tariffDao.insertFullTariffData(distEntities, profileEntities, blockEntities)
        }
    }

    /**
     * Insert or update a full Distributor along with its Tariff Profiles and Tariff Blocks into Room.
     */
    suspend fun saveDistributorWithRates(distributor: Distributor) {
        val (distEntities, profileEntities, blockEntities) = mapDomainToEntities(listOf(distributor))
        tariffDao.insertFullTariffData(distEntities, profileEntities, blockEntities)
    }

    /**
     * Helper to construct domain Distributor from database entities.
     */
    private suspend fun buildDistributorFromDb(distEntity: DistributorEntity): Distributor {
        val profileEntities = tariffDao.getProfilesForDistributorSync(distEntity.id)
        val domainProfiles = profileEntities.map { profileEntity ->
            val blockEntities = tariffDao.getBlocksForProfileSync(profileEntity.id)
            profileEntity.toDomainModel(blockEntities.map { it.toDomainModel() })
        }
        return Distributor(
            id = distEntity.id,
            name = distEntity.name,
            province = distEntity.province,
            logoAccentColorHex = distEntity.logoAccentColorHex,
            recognitionClue = distEntity.recognitionClue,
            profiles = domainProfiles,
            activeNotice = distEntity.activeNotice
        )
    }

    companion object {
        fun mapDomainToEntities(distributors: List<Distributor>): Triple<List<DistributorEntity>, List<TariffProfileEntity>, List<TariffBlockEntity>> {
            val distEntities = mutableListOf<DistributorEntity>()
            val profileEntities = mutableListOf<TariffProfileEntity>()
            val blockEntities = mutableListOf<TariffBlockEntity>()

            distributors.forEach { dist ->
                distEntities.add(
                    DistributorEntity(
                        id = dist.id,
                        name = dist.name,
                        province = dist.province,
                        logoAccentColorHex = dist.logoAccentColorHex,
                        recognitionClue = dist.recognitionClue,
                        activeNotice = dist.activeNotice
                    )
                )

                dist.profiles.forEach { profile ->
                    profileEntities.add(
                        TariffProfileEntity(
                            id = profile.id,
                            distributorId = profile.distributorId,
                            code = profile.code,
                            name = profile.name,
                            description = profile.description,
                            compatibleMeterTypesCsv = profile.compatibleMeterTypes.joinToString(",") { it.name },
                            monthlyFixedChargeRand = profile.monthlyFixedChargeRand,
                            monthlyServiceFeeRand = profile.monthlyServiceFeeRand,
                            dailyFixedChargeRand = profile.dailyFixedChargeRand,
                            vatRatePercent = profile.vatRatePercent,
                            vatInclusiveRates = profile.vatInclusiveRates,
                            fbeAvailable = profile.fbeConfig.isAvailable,
                            fbeFreeKwh = profile.fbeConfig.freeKwh,
                            fbeMonthlyUsageCapKwh = profile.fbeConfig.monthlyUsageCapKwh,
                            fbePropertyValuationCapRand = profile.fbeConfig.propertyValuationCapRand,
                            fbeIndigentRegistrationRequired = profile.fbeConfig.indigentRegistrationRequired,
                            fbeDescription = profile.fbeConfig.description,
                            effectiveDateStr = profile.effectiveDateStr,
                            sourceDocumentTitle = profile.sourceDocumentTitle,
                            sourceUrl = profile.sourceUrl,
                            verificationStatus = profile.verificationStatus.name
                        )
                    )

                    profile.blocks.forEach { block ->
                        blockEntities.add(
                            TariffBlockEntity(
                                profileId = profile.id,
                                blockNumber = block.blockNumber,
                                minKwh = block.minKwh,
                                maxKwh = block.maxKwh,
                                rateCentsPerKwh = block.rateCentsPerKwh
                            )
                        )
                    }
                }
            }

            return Triple(distEntities, profileEntities, blockEntities)
        }
    }
}

// Extension function to convert TariffBlockEntity to domain TariffBlock
fun TariffBlockEntity.toDomainModel(): TariffBlock {
    return TariffBlock(
        blockNumber = this.blockNumber,
        minKwh = this.minKwh,
        maxKwh = this.maxKwh,
        rateCentsPerKwh = this.rateCentsPerKwh
    )
}

// Extension function to convert TariffProfileEntity to domain TariffProfile
fun TariffProfileEntity.toDomainModel(blocks: List<TariffBlock>): TariffProfile {
    val meterTypes = this.compatibleMeterTypesCsv.split(",").mapNotNull {
        try {
            MeterType.valueOf(it.trim())
        } catch (e: Exception) {
            MeterType.PREPAID
        }
    }

    val status = try {
        VerificationStatus.valueOf(this.verificationStatus)
    } catch (e: Exception) {
        VerificationStatus.VERIFIED
    }

    return TariffProfile(
        id = this.id,
        distributorId = this.distributorId,
        code = this.code,
        name = this.name,
        description = this.description,
        compatibleMeterTypes = meterTypes,
        monthlyFixedChargeRand = this.monthlyFixedChargeRand,
        monthlyServiceFeeRand = this.monthlyServiceFeeRand,
        dailyFixedChargeRand = this.dailyFixedChargeRand,
        vatRatePercent = this.vatRatePercent,
        vatInclusiveRates = this.vatInclusiveRates,
        blocks = blocks,
        fbeConfig = FbeConfig(
            isAvailable = this.fbeAvailable,
            freeKwh = this.fbeFreeKwh,
            monthlyUsageCapKwh = this.fbeMonthlyUsageCapKwh,
            propertyValuationCapRand = this.fbePropertyValuationCapRand,
            indigentRegistrationRequired = this.fbeIndigentRegistrationRequired,
            description = this.fbeDescription
        ),
        effectiveDateStr = this.effectiveDateStr,
        sourceDocumentTitle = this.sourceDocumentTitle,
        sourceUrl = this.sourceUrl,
        verificationStatus = status
    )
}
