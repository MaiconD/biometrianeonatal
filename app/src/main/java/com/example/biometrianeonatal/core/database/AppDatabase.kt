package com.example.biometrianeonatal.core.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase as FrameworkSQLiteDatabase
import android.util.Log
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.biometrianeonatal.core.config.AppRuntimeConfig
import java.io.BufferedInputStream
import java.io.File
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.flow.Flow
import net.sqlcipher.database.SQLiteDatabase as SqlCipherDatabase
import net.sqlcipher.database.SupportFactory

enum class UserRole {
    OPERADOR,
    ADMINISTRADOR,
    PESQUISADOR,
}

enum class Sex {
    MASCULINO,
    FEMININO,
    NAO_INFORMADO,
}

enum class GuardianRelation {
    MAE,
    PAI,
    RESPONSAVEL,
}

enum class SyncStatus {
    PENDING,
    SYNCING,
    SYNCED,
    ERROR,
}

enum class SessionLifecycleStatus {
    DRAFT,
    IN_PROGRESS,
    COMPLETED,
}

class AppConverters {
    @TypeConverter
    fun fromUserRole(value: UserRole): String = value.name

    @TypeConverter
    fun toUserRole(value: String): UserRole = UserRole.valueOf(value)

    @TypeConverter
    fun fromSex(value: Sex): String = value.name

    @TypeConverter
    fun toSex(value: String): Sex = Sex.valueOf(value)

    @TypeConverter
    fun fromGuardianRelation(value: GuardianRelation): String = value.name

    @TypeConverter
    fun toGuardianRelation(value: String): GuardianRelation = GuardianRelation.valueOf(value)

    @TypeConverter
    fun fromSyncStatus(value: SyncStatus): String = value.name

    @TypeConverter
    fun toSyncStatus(value: String): SyncStatus = SyncStatus.valueOf(value)

    @TypeConverter
    fun fromSessionLifecycleStatus(value: SessionLifecycleStatus): String = value.name

    @TypeConverter
    fun toSessionLifecycleStatus(value: String): SessionLifecycleStatus = SessionLifecycleStatus.valueOf(value)
}

@Entity(tableName = "hospitals")
data class HospitalEntity(
    @PrimaryKey val id: String,
    val name: String,
    val city: String,
    val state: String,
)

@Entity(tableName = "app_users")
data class UserEntity(
    @PrimaryKey val id: String,
    val name: String,
    val email: String,
    val password: String,
    val role: UserRole,
    val hospitalId: String,
)

@Entity(tableName = "babies")
data class BabyEntity(
    @PrimaryKey val id: String,
    val name: String,
    val birthDate: String,
    val birthTime: String,
    val sex: Sex,
    val weightGrams: String,
    val heightCm: String,
    val hospitalId: String,
    val medicalRecord: String,
    val observations: String,
    val createdAt: String,
    val updatedAt: String,
    val deletedAt: String?,
)

@Entity(tableName = "guardians")
data class GuardianEntity(
    @PrimaryKey val id: String,
    val babyId: String,
    val name: String,
    val document: String,
    val phone: String,
    val relation: GuardianRelation,
    val addressCity: String,
    val addressState: String,
    val addressLine: String,
    val signatureBase64: String?,
    val createdAt: String,
    val updatedAt: String,
    val deletedAt: String?,
)

@Entity(tableName = "biometric_sessions")
data class BiometricSessionEntity(
    @PrimaryKey val id: String,
    val babyId: String,
    val operatorId: String,
    val sessionDate: String,
    val deviceId: String,
    val sensorSerial: String,
    val lifecycleStatus: SessionLifecycleStatus,
    val syncStatus: SyncStatus,
    val notes: String,
)

@Entity(tableName = "fingerprints")
data class FingerprintEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val fingerCode: String,
    val imagePath: String,
    val imageChecksumSha256: String,
    val templateBase64: String,
    val qualityScore: Int,
    val fps: Int,
    val resolution: String,
    val capturedAt: String,
)

@Entity(tableName = "sync_queue")
data class SyncQueueEntity(
    @PrimaryKey val id: String,
    val entityType: String,
    val entityId: String,
    val status: SyncStatus,
    val createdAt: String,
    val lastAttemptAt: String?,
    val errorMessage: String?,
)

@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey val id: String,
    val actorUserId: String,
    val action: String,
    val targetEntity: String,
    val targetEntityId: String,
    val createdAt: String,
)

data class DashboardMetricsProjection(
    val babiesToday: Int,
    val collectionsToday: Int,
    val pendingSync: Int,
)

data class BabyListItemProjection(
    val id: String,
    val name: String,
    val birthDate: String,
    val birthTime: String,
    val hospitalName: String,
    val status: SyncStatus,
)

data class PendingSyncProjection(
    val sessionId: String,
    val babyName: String,
    val sessionDate: String,
    val syncStatus: SyncStatus,
)

data class SessionHistoryProjection(
    val id: String,
    val babyName: String,
    val sessionDate: String,
    val lifecycleStatus: SessionLifecycleStatus,
    val syncStatus: SyncStatus,
)

data class SessionHistoryDetailProjection(
    val id: String,
    val babyId: String,
    val babyName: String,
    val operatorName: String,
    val hospitalName: String,
    val sessionDate: String,
    val deviceId: String,
    val sensorSerial: String,
    val lifecycleStatus: SessionLifecycleStatus,
    val syncStatus: SyncStatus,
    val notes: String,
)

data class FingerprintHistoryProjection(
    val id: String,
    val fingerCode: String,
    val imagePath: String,
    val imageChecksumSha256: String,
    val qualityScore: Int,
    val fps: Int,
    val resolution: String,
    val capturedAt: String,
)

data class SessionContextProjection(
    val babyName: String,
    val birthDate: String,
    val operatorName: String,
    val hospitalName: String,
)

data class LatestBabySessionProjection(
    val id: String,
    val sessionDate: String,
    val lifecycleStatus: SessionLifecycleStatus,
    val syncStatus: SyncStatus,
)

@Dao
interface HospitalDao {
    @Query("SELECT * FROM hospitals ORDER BY name")
    fun observeAll(): Flow<List<HospitalEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<HospitalEntity>)

    @Query("SELECT COUNT(*) FROM hospitals")
    suspend fun count(): Int
}

@Dao
interface UserDao {
    @Query(
        "SELECT * FROM app_users WHERE email = :email AND password = :password AND hospitalId = :hospitalId LIMIT 1"
    )
    suspend fun login(email: String, password: String, hospitalId: String): UserEntity?

    @Query("SELECT * FROM app_users WHERE id = :userId LIMIT 1")
    fun observeById(userId: String): Flow<UserEntity?>

    @Query("SELECT * FROM app_users WHERE id = :userId LIMIT 1")
    suspend fun getById(userId: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<UserEntity>)

    @Query("SELECT COUNT(*) FROM app_users")
    suspend fun count(): Int
}

@Dao
interface BabyDao {
    @Query(
        """
        SELECT b.id, b.name, b.birthDate, b.birthTime, h.name AS hospitalName,
               COALESCE(
                   (SELECT syncStatus FROM biometric_sessions s WHERE s.babyId = b.id ORDER BY s.sessionDate DESC LIMIT 1),
                   'PENDING'
               ) AS status
        FROM babies b
        INNER JOIN hospitals h ON h.id = b.hospitalId
        WHERE b.deletedAt IS NULL
        ORDER BY b.birthDate DESC, b.birthTime DESC, b.name ASC
        """
    )
    fun observeBabyListItems(): Flow<List<BabyListItemProjection>>

    @Query("SELECT * FROM babies WHERE id = :babyId AND deletedAt IS NULL LIMIT 1")
    fun observeById(babyId: String): Flow<BabyEntity?>

    @Query("SELECT * FROM babies WHERE id = :babyId LIMIT 1")
    suspend fun getById(babyId: String): BabyEntity?

    @Query("SELECT COUNT(*) FROM babies WHERE birthDate = date('now') AND deletedAt IS NULL")
    fun observeBabiesTodayCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: BabyEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<BabyEntity>)

    @Query("SELECT * FROM babies WHERE deletedAt IS NULL")
    suspend fun getAllActive(): List<BabyEntity>

    @Query("UPDATE babies SET updatedAt = :updatedAt, deletedAt = :deletedAt WHERE id = :babyId")
    suspend fun softDelete(babyId: String, updatedAt: String, deletedAt: String)

    @Query("SELECT COUNT(*) FROM babies")
    suspend fun count(): Int
}

@Dao
interface GuardianDao {
    @Query("SELECT * FROM guardians WHERE babyId = :babyId AND deletedAt IS NULL ORDER BY relation ASC, name ASC")
    fun observeByBabyId(babyId: String): Flow<List<GuardianEntity>>

    @Query("SELECT * FROM guardians WHERE babyId = :babyId AND deletedAt IS NULL")
    suspend fun getActiveByBabyId(babyId: String): List<GuardianEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<GuardianEntity>)

    @Query("SELECT * FROM guardians WHERE deletedAt IS NULL")
    suspend fun getAllActive(): List<GuardianEntity>

    @Query("UPDATE guardians SET updatedAt = :updatedAt, deletedAt = :deletedAt WHERE id IN (:guardianIds)")
    suspend fun softDeleteByIds(guardianIds: List<String>, updatedAt: String, deletedAt: String)

    @Query("UPDATE guardians SET updatedAt = :updatedAt, deletedAt = :deletedAt WHERE babyId = :babyId AND deletedAt IS NULL")
    suspend fun softDeleteByBabyId(babyId: String, updatedAt: String, deletedAt: String)
}

@Dao
interface SessionDao {
    @Query("SELECT COUNT(*) FROM biometric_sessions WHERE substr(sessionDate, 1, 10) = date('now')")
    fun observeCollectionsTodayCount(): Flow<Int>

    @Query(
        """
        SELECT s.id, b.name AS babyName, s.sessionDate, s.lifecycleStatus, s.syncStatus
        FROM biometric_sessions s
        INNER JOIN babies b ON b.id = s.babyId
        ORDER BY s.sessionDate DESC, b.name ASC
        """
    )
    fun observeSessionHistory(): Flow<List<SessionHistoryProjection>>

    @Query(
        """
        SELECT s.id, s.babyId, b.name AS babyName, u.name AS operatorName, h.name AS hospitalName,
               s.sessionDate, s.deviceId, s.sensorSerial, s.lifecycleStatus, s.syncStatus, s.notes
        FROM biometric_sessions s
        INNER JOIN babies b ON b.id = s.babyId
        INNER JOIN app_users u ON u.id = s.operatorId
        INNER JOIN hospitals h ON h.id = b.hospitalId
        WHERE s.id = :sessionId
        LIMIT 1
        """
    )
    fun observeSessionHistoryDetail(sessionId: String): Flow<SessionHistoryDetailProjection?>

    @Query(
        """
        SELECT s.id AS sessionId, b.name AS babyName, s.sessionDate, s.syncStatus
        FROM biometric_sessions s
        INNER JOIN babies b ON b.id = s.babyId
        WHERE s.syncStatus IN ('PENDING', 'ERROR')
        ORDER BY s.sessionDate DESC
        """
    )
    fun observePendingSyncSessions(): Flow<List<PendingSyncProjection>>

    @Query(
        """
        SELECT b.name AS babyName, b.birthDate, u.name AS operatorName, h.name AS hospitalName
        FROM babies b
        INNER JOIN app_users u ON u.id = :operatorId
        INNER JOIN hospitals h ON h.id = b.hospitalId
        WHERE b.id = :babyId
        LIMIT 1
        """
    )
    fun observeSessionContext(babyId: String, operatorId: String): Flow<SessionContextProjection?>

    @Query(
        """
        SELECT id, sessionDate, lifecycleStatus, syncStatus
        FROM biometric_sessions
        WHERE babyId = :babyId
        ORDER BY sessionDate DESC
        LIMIT 1
        """
    )
    fun observeLatestSessionSummary(babyId: String): Flow<LatestBabySessionProjection?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(item: BiometricSessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<BiometricSessionEntity>)

    @Query("SELECT * FROM biometric_sessions WHERE syncStatus IN (:statuses)")
    suspend fun getBySyncStatuses(statuses: List<SyncStatus>): List<BiometricSessionEntity>

    @Query("UPDATE biometric_sessions SET lifecycleStatus = :status, syncStatus = :syncStatus WHERE id = :sessionId")
    suspend fun updateSessionStatus(sessionId: String, status: SessionLifecycleStatus, syncStatus: SyncStatus)

    @Query("UPDATE biometric_sessions SET syncStatus = :newStatus WHERE syncStatus IN (:fromStatuses)")
    suspend fun updateSyncStatusForStatuses(fromStatuses: List<SyncStatus>, newStatus: SyncStatus)

    @Query("SELECT COUNT(*) FROM biometric_sessions")
    suspend fun count(): Int
}

@Dao
interface FingerprintDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: FingerprintEntity)

    @Query("DELETE FROM fingerprints WHERE sessionId = :sessionId AND fingerCode = :fingerCode")
    suspend fun deleteBySessionAndFingerCode(sessionId: String, fingerCode: String)

    @Query(
        """
        SELECT id, fingerCode, imagePath, imageChecksumSha256, qualityScore, fps, resolution, capturedAt
        FROM fingerprints
        WHERE sessionId = :sessionId
        ORDER BY capturedAt DESC, fingerCode ASC
        """
    )
    fun observeBySessionId(sessionId: String): Flow<List<FingerprintHistoryProjection>>

    @Query("SELECT fingerCode FROM fingerprints WHERE sessionId = :sessionId ORDER BY capturedAt DESC")
    suspend fun getFingerCodesBySessionId(sessionId: String): List<String>

    @Query("SELECT * FROM fingerprints WHERE id = :fingerprintId LIMIT 1")
    suspend fun getById(fingerprintId: String): FingerprintEntity?

    @Query("SELECT * FROM fingerprints WHERE sessionId IN (:sessionIds)")
    suspend fun getBySessionIds(sessionIds: List<String>): List<FingerprintEntity>
}

@Dao
interface SyncQueueDao {
    @Query("SELECT COUNT(*) FROM sync_queue WHERE status IN ('PENDING', 'ERROR')")
    fun observePendingCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM sync_queue WHERE status IN ('PENDING', 'ERROR')")
    suspend fun pendingCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: SyncQueueEntity)

    @Query("UPDATE sync_queue SET status = :newStatus, lastAttemptAt = :attemptedAt, errorMessage = :errorMessage WHERE status IN (:fromStatuses)")
    suspend fun updateStatuses(
        fromStatuses: List<SyncStatus>,
        newStatus: SyncStatus,
        attemptedAt: String,
        errorMessage: String?,
    )
}

@Dao
interface AuditLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: AuditLogEntity)
}

@Database(
    entities = [
        HospitalEntity::class,
        UserEntity::class,
        BabyEntity::class,
        GuardianEntity::class,
        BiometricSessionEntity::class,
        FingerprintEntity::class,
        SyncQueueEntity::class,
        AuditLogEntity::class,
    ],
    version = 4,
    exportSchema = true,
)
@TypeConverters(AppConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun hospitalDao(): HospitalDao
    abstract fun userDao(): UserDao
    abstract fun babyDao(): BabyDao
    abstract fun guardianDao(): GuardianDao
    abstract fun sessionDao(): SessionDao
    abstract fun fingerprintDao(): FingerprintDao
    abstract fun syncQueueDao(): SyncQueueDao
    abstract fun auditLogDao(): AuditLogDao

    companion object {
        fun build(
            context: Context,
            databasePassphraseProvider: DatabasePassphraseProvider,
            appRuntimeConfig: AppRuntimeConfig,
        ): AppDatabase {
            return runCatching {
                SqlCipherDatabase.loadLibs(context)
                val databaseFile = context.getDatabasePath(DATABASE_NAME)
                val passphrase = if (!databaseFile.exists() || isPlaintextSqliteDatabase(databaseFile)) {
                    val passphraseText = databasePassphraseProvider.getOrCreatePassphraseText()
                    migrateLegacyPlaintextDatabaseIfNeeded(context, passphraseText)
                    passphraseText.toByteArray(StandardCharsets.UTF_8)
                } else {
                    databasePassphraseProvider.getOrCreatePassphrase()
                }
                openEncryptedDatabase(context, passphrase)
            }.getOrElse { error ->
                if (!appRuntimeConfig.offlineDemoMode) {
                    throw error
                }
                Log.e(TAG, "Encrypted database startup failed in offline/demo mode. Recreating local database from zero.", error)
                resetDatabaseFiles(context)
                val passphrase = databasePassphraseProvider.getOrCreatePassphraseText()
                    .toByteArray(StandardCharsets.UTF_8)
                openEncryptedDatabase(context, passphrase)
            }
        }

        private fun openEncryptedDatabase(
            context: Context,
            passphrase: ByteArray,
        ): AppDatabase {
            val database = Room.databaseBuilder(
                context,
                AppDatabase::class.java,
                DATABASE_NAME,
            ).openHelperFactory(
                SupportFactory(passphrase),
            ).addMigrations(*DatabaseMigrations.ALL)
                .build()
            database.openHelper.writableDatabase
            return database
        }

        private fun migrateLegacyPlaintextDatabaseIfNeeded(
            context: Context,
            passphraseText: String,
        ) {
            val databaseFile = context.getDatabasePath(DATABASE_NAME)
            if (!databaseFile.exists()) return

            restoreBackupIfNeeded(databaseFile)

            if (!isPlaintextSqliteDatabase(databaseFile)) {
                return
            }

            Log.i(TAG, "Legacy plaintext database detected. Starting SQLCipher migration.")
            val tempEncryptedFile = File(databaseFile.parentFile, "$DATABASE_NAME.sqlcipher.tmp")
            val backupPlaintextFile = File(databaseFile.parentFile, "$DATABASE_NAME.plaintext.bak")
            deleteDatabaseArtifacts(tempEncryptedFile)
            deleteDatabaseArtifacts(backupPlaintextFile)

            checkpointPlaintextWal(databaseFile)
            exportPlaintextToEncrypted(
                plaintextDatabaseFile = databaseFile,
                encryptedDatabaseFile = tempEncryptedFile,
                passphraseText = passphraseText,
            )
            swapMigratedDatabaseFiles(
                plaintextDatabaseFile = databaseFile,
                encryptedDatabaseFile = tempEncryptedFile,
                backupPlaintextFile = backupPlaintextFile,
            )
            Log.i(TAG, "SQLCipher migration completed successfully.")
        }

        private fun restoreBackupIfNeeded(databaseFile: File) {
            val backupPlaintextFile = File(databaseFile.parentFile, "$DATABASE_NAME.plaintext.bak")
            if (!backupPlaintextFile.exists() || databaseFile.exists()) return
            check(backupPlaintextFile.renameTo(databaseFile)) {
                "Failed to restore backup plaintext database before SQLCipher migration."
            }
        }

        private fun isPlaintextSqliteDatabase(databaseFile: File): Boolean {
            if (!databaseFile.exists() || databaseFile.length() < SQLITE_HEADER.size.toLong()) return false
            val header = ByteArray(SQLITE_HEADER.size)
            BufferedInputStream(databaseFile.inputStream()).use { input ->
                if (input.read(header) != SQLITE_HEADER.size) return false
            }
            return header.contentEquals(SQLITE_HEADER)
        }

        private fun checkpointPlaintextWal(databaseFile: File) {
            if (!databaseFile.exists()) return
            runCatching {
                FrameworkSQLiteDatabase.openDatabase(
                    databaseFile.absolutePath,
                    null,
                    FrameworkSQLiteDatabase.OPEN_READWRITE,
                ).use { database ->
                    database.execSQL("PRAGMA wal_checkpoint(FULL)")
                }
            }.onFailure { error ->
                Log.w(TAG, "Unable to checkpoint plaintext WAL before SQLCipher migration.", error)
            }
        }

        private fun exportPlaintextToEncrypted(
            plaintextDatabaseFile: File,
            encryptedDatabaseFile: File,
            passphraseText: String,
        ) {
            val encryptedPath = encryptedDatabaseFile.absolutePath.toSqlStringLiteral()
            val escapedPassphrase = passphraseText.toSqlStringLiteral()

            encryptedDatabaseFile.parentFile?.mkdirs()
            if (!encryptedDatabaseFile.exists()) {
                encryptedDatabaseFile.createNewFile()
            }

            val plaintextDatabase = SqlCipherDatabase.openDatabase(
                plaintextDatabaseFile.absolutePath,
                "",
                null,
                SqlCipherDatabase.OPEN_READWRITE,
            )

            plaintextDatabase.use { database ->
                val userVersion = database.compileStatement("PRAGMA user_version;").simpleQueryForLong().toInt()
                database.rawExecSQL("ATTACH DATABASE '$encryptedPath' AS encrypted KEY '$escapedPassphrase'")
                database.rawExecSQL("SELECT sqlcipher_export('encrypted')")
                database.rawExecSQL("PRAGMA encrypted.user_version = $userVersion")
                database.rawExecSQL("DETACH DATABASE encrypted")
            }

            check(!isPlaintextSqliteDatabase(encryptedDatabaseFile)) {
                "SQLCipher migration produced an unencrypted database file."
            }
        }

        private fun swapMigratedDatabaseFiles(
            plaintextDatabaseFile: File,
            encryptedDatabaseFile: File,
            backupPlaintextFile: File,
        ) {
            check(encryptedDatabaseFile.exists()) { "Encrypted database temp file not found after migration." }
            deleteDatabaseSidecars(backupPlaintextFile)
            check(plaintextDatabaseFile.renameTo(backupPlaintextFile)) {
                "Failed to backup plaintext database during SQLCipher migration."
            }
            try {
                check(encryptedDatabaseFile.renameTo(plaintextDatabaseFile)) {
                    "Failed to replace plaintext database with encrypted database."
                }
                deleteDatabaseArtifacts(backupPlaintextFile)
                deleteDatabaseSidecars(plaintextDatabaseFile)
            } catch (error: Throwable) {
                deleteDatabaseArtifacts(plaintextDatabaseFile)
                backupPlaintextFile.renameTo(plaintextDatabaseFile)
                throw error
            }
        }

        private fun deleteDatabaseArtifacts(databaseFile: File) {
            if (databaseFile.exists()) {
                databaseFile.delete()
            }
            deleteDatabaseSidecars(databaseFile)
        }

        private fun deleteDatabaseSidecars(databaseFile: File) {
            File(databaseFile.absolutePath + "-wal").delete()
            File(databaseFile.absolutePath + "-shm").delete()
            File(databaseFile.absolutePath + "-journal").delete()
        }

        private fun resetDatabaseFiles(context: Context) {
            deleteDatabaseArtifacts(context.getDatabasePath(DATABASE_NAME))
            deleteDatabaseArtifacts(context.getDatabasePath("$DATABASE_NAME.sqlcipher.tmp"))
            deleteDatabaseArtifacts(context.getDatabasePath("$DATABASE_NAME.plaintext.bak"))
        }


        private fun String.toSqlStringLiteral(): String {
            return replace("'", "''")
        }

        private const val DATABASE_NAME = "biometria-neonatal.db"
        private const val TAG = "AppDatabase"
        private val SQLITE_HEADER = "SQLite format 3\u0000".toByteArray(StandardCharsets.US_ASCII)
    }
}


