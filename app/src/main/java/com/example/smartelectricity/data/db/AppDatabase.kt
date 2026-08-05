package com.example.smartelectricity.data.db

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface HouseholdDao {
    @Query("SELECT * FROM households ORDER BY updatedTimestamp DESC")
    fun getAllHouseholds(): Flow<List<HouseholdEntity>>

    @Query("SELECT * FROM households WHERE id = :id")
    suspend fun getHouseholdById(id: Int): HouseholdEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHousehold(household: HouseholdEntity): Long

    @Update
    suspend fun updateHousehold(household: HouseholdEntity)

    @Delete
    suspend fun deleteHousehold(household: HouseholdEntity)
}

@Dao
interface CalculationHistoryDao {
    @Query("SELECT * FROM calculation_history ORDER BY dateTimestamp DESC")
    fun getAllHistory(): Flow<List<CalculationHistoryEntity>>

    @Query("SELECT * FROM calculation_history WHERE householdId = :householdId ORDER BY dateTimestamp DESC")
    fun getHistoryForHousehold(householdId: Int): Flow<List<CalculationHistoryEntity>>

    @Query("SELECT * FROM calculation_history WHERE id = :id")
    suspend fun getHistoryById(id: Int): CalculationHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(item: CalculationHistoryEntity): Long

    @Update
    suspend fun updateHistory(item: CalculationHistoryEntity)

    @Query("DELETE FROM calculation_history WHERE id = :id")
    suspend fun deleteHistoryById(id: Int)
}

@Dao
interface TariffAlertDao {
    @Query("SELECT * FROM tariff_alerts ORDER BY createdTimestamp DESC")
    fun getAllAlerts(): Flow<List<TariffAlertEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlerts(alerts: List<TariffAlertEntity>)

    @Query("UPDATE tariff_alerts SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: Int)
}

@Dao
interface WeeklySpendDao {
    @Query("SELECT * FROM weekly_spend_logs ORDER BY dateTimestamp DESC")
    fun getAllWeeklySpends(): Flow<List<WeeklySpendEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeeklySpend(spend: WeeklySpendEntity): Long

    @Query("DELETE FROM weekly_spend_logs WHERE id = :id")
    suspend fun deleteWeeklySpendById(id: Int)
}

@Dao
interface TariffDao {
    @Query("SELECT * FROM distributors ORDER BY name ASC")
    fun getAllDistributors(): Flow<List<DistributorEntity>>

    @Query("SELECT * FROM distributors WHERE id = :id")
    suspend fun getDistributorById(id: String): DistributorEntity?

    @Query("SELECT * FROM tariff_profiles WHERE distributorId = :distributorId")
    fun getProfilesForDistributor(distributorId: String): Flow<List<TariffProfileEntity>>

    @Query("SELECT * FROM tariff_profiles WHERE distributorId = :distributorId")
    suspend fun getProfilesForDistributorSync(distributorId: String): List<TariffProfileEntity>

    @Query("SELECT * FROM tariff_profiles WHERE id = :profileId")
    suspend fun getProfileById(profileId: String): TariffProfileEntity?

    @Query("SELECT * FROM tariff_blocks WHERE profileId = :profileId ORDER BY blockNumber ASC")
    fun getBlocksForProfile(profileId: String): Flow<List<TariffBlockEntity>>

    @Query("SELECT * FROM tariff_blocks WHERE profileId = :profileId ORDER BY blockNumber ASC")
    suspend fun getBlocksForProfileSync(profileId: String): List<TariffBlockEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDistributors(distributors: List<DistributorEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTariffProfiles(profiles: List<TariffProfileEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTariffBlocks(blocks: List<TariffBlockEntity>)

    @Transaction
    suspend fun insertFullTariffData(
        distributors: List<DistributorEntity>,
        profiles: List<TariffProfileEntity>,
        blocks: List<TariffBlockEntity>
    ) {
        insertDistributors(distributors)
        insertTariffProfiles(profiles)
        insertTariffBlocks(blocks)
    }
}

@Database(
    entities = [
        HouseholdEntity::class,
        CalculationHistoryEntity::class,
        TariffAlertEntity::class,
        WeeklySpendEntity::class,
        DistributorEntity::class,
        TariffProfileEntity::class,
        TariffBlockEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun householdDao(): HouseholdDao
    abstract fun calculationHistoryDao(): CalculationHistoryDao
    abstract fun tariffAlertDao(): TariffAlertDao
    abstract fun weeklySpendDao(): WeeklySpendDao
    abstract fun tariffDao(): TariffDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "smart_electricity_calculator.db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

