package com.example.smartelectricity.data.db

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
interface PurchaseLedgerDao {
    @Query("SELECT * FROM monthly_block_ledgers WHERE householdId = :householdId AND yearMonth = :yearMonth AND tariffProfileId = :tariffProfileId LIMIT 1")
    fun observeLedger(householdId: Int, yearMonth: String, tariffProfileId: String): Flow<MonthlyBlockLedgerEntity?>

    @Query("SELECT * FROM monthly_block_ledgers WHERE ledgerKey = :ledgerKey LIMIT 1")
    suspend fun getLedger(ledgerKey: String): MonthlyBlockLedgerEntity?

    @Query("SELECT COALESCE(SUM(tenderAmountRand), 0.0) FROM prepaid_purchases WHERE yearMonth = :yearMonth")
    fun observeTotalSpendForMonth(yearMonth: String): Flow<Double>

    @Query("SELECT * FROM prepaid_purchases WHERE householdId = :householdId ORDER BY purchaseTimestamp DESC")
    fun observePurchasesForHousehold(householdId: Int): Flow<List<PrepaidPurchaseEntity>>

    @Insert
    suspend fun insertPurchase(purchase: PrepaidPurchaseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLedger(ledger: MonthlyBlockLedgerEntity)

    @Transaction
    suspend fun recordPurchase(purchase: PrepaidPurchaseEntity, ledger: MonthlyBlockLedgerEntity): Long {
        val purchaseId = insertPurchase(purchase)
        upsertLedger(ledger)
        return purchaseId
    }
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

    @Query("DELETE FROM tariff_blocks")
    suspend fun deleteAllTariffBlocks()

    @Query("DELETE FROM tariff_profiles")
    suspend fun deleteAllTariffProfiles()

    @Query("DELETE FROM distributors")
    suspend fun deleteAllDistributors()

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

    @Transaction
    suspend fun replaceBundledTariffData(
        distributors: List<DistributorEntity>,
        profiles: List<TariffProfileEntity>,
        blocks: List<TariffBlockEntity>
    ) {
        deleteAllTariffBlocks()
        deleteAllTariffProfiles()
        deleteAllDistributors()
        insertFullTariffData(distributors, profiles, blocks)
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
        TariffBlockEntity::class,
        PrepaidPurchaseEntity::class,
        MonthlyBlockLedgerEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun householdDao(): HouseholdDao
    abstract fun calculationHistoryDao(): CalculationHistoryDao
    abstract fun tariffAlertDao(): TariffAlertDao
    abstract fun weeklySpendDao(): WeeklySpendDao
    abstract fun tariffDao(): TariffDao
    abstract fun purchaseLedgerDao(): PurchaseLedgerDao

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
                .addMigrations(MIGRATION_3_4)
                .addMigrations(MIGRATION_4_5)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE tariff_profiles ADD COLUMN dailyFixedChargeRand REAL NOT NULL DEFAULT 0.0"
                )
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE calculation_history ADD COLUMN isCommittedPurchase INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE tariff_profiles ADD COLUMN fixedChargeRecoveryRule TEXT NOT NULL DEFAULT 'NONE'")
                database.execSQL("ALTER TABLE tariff_profiles ADD COLUMN fbeAllocationTiersCsv TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE tariff_profiles ADD COLUMN effectiveFromStr TEXT NOT NULL DEFAULT '2026-07-01'")
                database.execSQL("ALTER TABLE tariff_profiles ADD COLUMN effectiveToStr TEXT DEFAULT '2027-06-30'")
                database.execSQL("ALTER TABLE tariff_profiles ADD COLUMN sourceDocumentId TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE tariff_profiles ADD COLUMN calculationEngineVersion TEXT NOT NULL DEFAULT '2.0.0'")
                database.execSQL(
                    """CREATE TABLE IF NOT EXISTS prepaid_purchases (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        householdId INTEGER NOT NULL,
                        purchaseTimestamp INTEGER NOT NULL,
                        yearMonth TEXT NOT NULL,
                        tenderAmountRand REAL NOT NULL,
                        energyValueRand REAL NOT NULL,
                        fixedDeductionRand REAL NOT NULL,
                        arrearsDeductionRand REAL NOT NULL,
                        paidUnitsKwh REAL NOT NULL,
                        fbeUnitsKwh REAL NOT NULL,
                        estimatedTotalUnitsKwh REAL NOT NULL,
                        actualUnitsKwh REAL,
                        tariffProfileId TEXT NOT NULL,
                        tariffEffectiveFromStr TEXT NOT NULL,
                        sourceDocumentId TEXT NOT NULL,
                        calculationEngineVersion TEXT NOT NULL
                    )""".trimIndent()
                )
                database.execSQL("CREATE INDEX IF NOT EXISTS index_prepaid_purchases_householdId_yearMonth ON prepaid_purchases (householdId, yearMonth)")
                database.execSQL(
                    """CREATE TABLE IF NOT EXISTS monthly_block_ledgers (
                        ledgerKey TEXT PRIMARY KEY NOT NULL,
                        householdId INTEGER NOT NULL,
                        yearMonth TEXT NOT NULL,
                        tariffProfileId TEXT NOT NULL,
                        paidUnitsAllocatedKwh REAL NOT NULL,
                        freeUnitsAllocatedKwh REAL NOT NULL,
                        purchasedAmountRand REAL NOT NULL,
                        lastPurchaseAt INTEGER,
                        lastFixedChargeRecoveryAt INTEGER,
                        updatedAt INTEGER NOT NULL
                    )""".trimIndent()
                )
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_monthly_block_ledgers_householdId_yearMonth_tariffProfileId ON monthly_block_ledgers (householdId, yearMonth, tariffProfileId)")
            }
        }
    }
}
