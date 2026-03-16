package com.example.biometrianeonatal.core.database

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import net.sqlcipher.database.SupportFactory
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDatabaseMigrationTest {

    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @After
    fun tearDown() {
        context.deleteDatabase(DB_V1_TO_V4)
        context.deleteDatabase(DB_V3_TO_V4)
    }

    @Test
    fun migrateFromV1ToV4_preservesLegacyRowsAndAddsModernColumns() {
        createEncryptedLegacyDatabase(
            name = DB_V1_TO_V4,
            version = 1,
        ) { database ->
            database.execSQL("CREATE TABLE IF NOT EXISTS hospitals (id TEXT NOT NULL, name TEXT NOT NULL, city TEXT NOT NULL, state TEXT NOT NULL, PRIMARY KEY(id))")
            database.execSQL("CREATE TABLE IF NOT EXISTS app_users (id TEXT NOT NULL, name TEXT NOT NULL, email TEXT NOT NULL, role TEXT NOT NULL, hospital_id TEXT NOT NULL, PRIMARY KEY(id))")
            database.execSQL("CREATE TABLE IF NOT EXISTS babies (id TEXT NOT NULL, name TEXT NOT NULL, birth_date TEXT NOT NULL, birth_time TEXT NOT NULL, sex TEXT NOT NULL, weight TEXT NOT NULL, height TEXT NOT NULL, hospital_id TEXT NOT NULL, medical_record TEXT NOT NULL, PRIMARY KEY(id))")
            database.execSQL("CREATE TABLE IF NOT EXISTS guardians (id TEXT NOT NULL, baby_id TEXT NOT NULL, name TEXT NOT NULL, document TEXT NOT NULL, phone TEXT NOT NULL, relation TEXT NOT NULL, PRIMARY KEY(id))")
            database.execSQL("CREATE TABLE IF NOT EXISTS biometric_sessions (id TEXT NOT NULL, baby_id TEXT NOT NULL, operator_id TEXT NOT NULL, session_date TEXT NOT NULL, device_id TEXT NOT NULL, sync_status TEXT NOT NULL, PRIMARY KEY(id))")
            database.execSQL("CREATE TABLE IF NOT EXISTS fingerprints (id TEXT NOT NULL, session_id TEXT NOT NULL, finger TEXT NOT NULL, image_path TEXT NOT NULL, quality_score INTEGER NOT NULL, PRIMARY KEY(id))")
            database.execSQL("CREATE TABLE IF NOT EXISTS sync_queue (id TEXT NOT NULL, entity_type TEXT NOT NULL, entity_id TEXT NOT NULL, status TEXT NOT NULL, created_at TEXT NOT NULL, PRIMARY KEY(id))")

            database.execSQL("INSERT INTO hospitals(id, name, city, state) VALUES ('hospital-pb', 'Hospital UTFPR Pato Branco', 'Pato Branco', 'PR')")
            database.execSQL("INSERT INTO app_users(id, name, email, role, hospital_id) VALUES ('user-1', 'Operador 1', 'operador@utfpr.edu.br', 'OPERADOR', 'hospital-pb')")
            database.execSQL("INSERT INTO babies(id, name, birth_date, birth_time, sex, weight, height, hospital_id, medical_record) VALUES ('baby-1', 'Bebê 1', '2026-03-15', '08:30', 'FEMININO', '3200', '49', 'hospital-pb', 'MR-01')")
            database.execSQL("INSERT INTO guardians(id, baby_id, name, document, phone, relation) VALUES ('guardian-1', 'baby-1', 'Mãe Teste', '12345678901', '46999991111', 'MAE')")
            database.execSQL("INSERT INTO biometric_sessions(id, baby_id, operator_id, session_date, device_id, sync_status) VALUES ('session-1', 'baby-1', 'user-1', '2026-03-15T09:00:00', 'tablet-01', 'PENDING')")
            database.execSQL("INSERT INTO fingerprints(id, session_id, finger, image_path, quality_score) VALUES ('finger-1', 'session-1', 'POLEGAR_DIREITO', 'secure://artifacts/captures/finger-1', 91)")
            database.execSQL("INSERT INTO sync_queue(id, entity_type, entity_id, status, created_at) VALUES ('sync-1', 'baby', 'baby-1', 'PENDING', '2026-03-15T09:00:00')")
        }

        val migrated = openMigratedRoomDatabase(DB_V1_TO_V4)
        try {
            val readableDatabase = migrated.openHelper.readableDatabase
            assertEquals(1, readableDatabase.countRows("app_users"))
            assertEquals(1, readableDatabase.countRows("babies"))
            assertEquals(1, readableDatabase.countRows("biometric_sessions"))
            assertGuardianColumns(readableDatabase)
            assertFingerprintColumns(readableDatabase)
            assertAuditLogsTableExists(readableDatabase)
        } finally {
            migrated.close()
        }
    }

    @Test
    fun migrateFromV3ToV4_replacesConsentColumnsWithSignatureColumn() {
        createEncryptedLegacyDatabase(
            name = DB_V3_TO_V4,
            version = 3,
        ) { database ->
            database.execSQL("CREATE TABLE IF NOT EXISTS hospitals (id TEXT NOT NULL, name TEXT NOT NULL, city TEXT NOT NULL, state TEXT NOT NULL, PRIMARY KEY(id))")
            database.execSQL("CREATE TABLE IF NOT EXISTS app_users (id TEXT NOT NULL, name TEXT NOT NULL, email TEXT NOT NULL, password TEXT NOT NULL, role TEXT NOT NULL, hospitalId TEXT NOT NULL, PRIMARY KEY(id))")
            database.execSQL("CREATE TABLE IF NOT EXISTS babies (id TEXT NOT NULL, name TEXT NOT NULL, birthDate TEXT NOT NULL, birthTime TEXT NOT NULL, sex TEXT NOT NULL, weightGrams TEXT NOT NULL, heightCm TEXT NOT NULL, hospitalId TEXT NOT NULL, medicalRecord TEXT NOT NULL, observations TEXT NOT NULL, createdAt TEXT NOT NULL, updatedAt TEXT NOT NULL, deletedAt TEXT, PRIMARY KEY(id))")
            database.execSQL("CREATE TABLE IF NOT EXISTS guardians (id TEXT NOT NULL, babyId TEXT NOT NULL, name TEXT NOT NULL, document TEXT NOT NULL, phone TEXT NOT NULL, relation TEXT NOT NULL, addressCity TEXT NOT NULL, addressState TEXT NOT NULL, addressLine TEXT NOT NULL, consentFileName TEXT NOT NULL, consentAcceptedAt TEXT NOT NULL, createdAt TEXT NOT NULL, updatedAt TEXT NOT NULL, deletedAt TEXT, PRIMARY KEY(id))")
            database.execSQL("CREATE TABLE IF NOT EXISTS biometric_sessions (id TEXT NOT NULL, babyId TEXT NOT NULL, operatorId TEXT NOT NULL, sessionDate TEXT NOT NULL, deviceId TEXT NOT NULL, sensorSerial TEXT NOT NULL, lifecycleStatus TEXT NOT NULL, syncStatus TEXT NOT NULL, notes TEXT NOT NULL, PRIMARY KEY(id))")
            database.execSQL("CREATE TABLE IF NOT EXISTS fingerprints (id TEXT NOT NULL, sessionId TEXT NOT NULL, fingerCode TEXT NOT NULL, imagePath TEXT NOT NULL, templateBase64 TEXT NOT NULL, qualityScore INTEGER NOT NULL, fps INTEGER NOT NULL, resolution TEXT NOT NULL, capturedAt TEXT NOT NULL, PRIMARY KEY(id))")
            database.execSQL("CREATE TABLE IF NOT EXISTS sync_queue (id TEXT NOT NULL, entityType TEXT NOT NULL, entityId TEXT NOT NULL, status TEXT NOT NULL, createdAt TEXT NOT NULL, lastAttemptAt TEXT, errorMessage TEXT, PRIMARY KEY(id))")
            database.execSQL("CREATE TABLE IF NOT EXISTS audit_logs (id TEXT NOT NULL, actorUserId TEXT NOT NULL, action TEXT NOT NULL, targetEntity TEXT NOT NULL, targetEntityId TEXT NOT NULL, createdAt TEXT NOT NULL, PRIMARY KEY(id))")
        }

        val migrated = openMigratedRoomDatabase(DB_V3_TO_V4)
        try {
            val readableDatabase = migrated.openHelper.readableDatabase
            assertGuardianColumns(readableDatabase)
            assertFingerprintColumns(readableDatabase)
        } finally {
            migrated.close()
        }
    }

    private fun createEncryptedLegacyDatabase(
        name: String,
        version: Int,
        onCreateSchema: (SupportSQLiteDatabase) -> Unit,
    ) {
        context.deleteDatabase(name)
        val helper = SupportFactory(passphrase()).create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(name)
                .callback(object : SupportSQLiteOpenHelper.Callback(version) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        onCreateSchema(db)
                    }

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                })
                .build(),
        )
        helper.writableDatabase.close()
        helper.close()
    }

    private fun openMigratedRoomDatabase(name: String): AppDatabase {
        return Room.databaseBuilder(context, AppDatabase::class.java, name)
            .openHelperFactory(SupportFactory(passphrase()))
            .addMigrations(*DatabaseMigrations.ALL)
            .build()
    }

    private fun assertGuardianColumns(database: SupportSQLiteDatabase) {
        val columnNames = database.columnNamesOf("guardians")
        assertTrue(columnNames.contains("signatureBase64"))
        assertTrue(!columnNames.contains("consentFileName"))
        assertTrue(!columnNames.contains("consentChecksumSha256"))
    }

    private fun assertFingerprintColumns(database: SupportSQLiteDatabase) {
        val columnNames = database.columnNamesOf("fingerprints")
        assertTrue(columnNames.contains("imageChecksumSha256"))
    }

    private fun assertAuditLogsTableExists(database: SupportSQLiteDatabase) {
        database.query("SELECT name FROM sqlite_master WHERE type='table' AND name='audit_logs'").use { cursor ->
            assertTrue(cursor.moveToFirst())
        }
    }

    private fun SupportSQLiteDatabase.countRows(tableName: String): Int {
        query("SELECT COUNT(*) FROM `$tableName`").use { cursor ->
            check(cursor.moveToFirst())
            return cursor.getInt(0)
        }
    }

    private fun SupportSQLiteDatabase.columnNamesOf(tableName: String): Set<String> {
        query("PRAGMA table_info(`$tableName`)").use { cursor ->
            val nameIndex = cursor.getColumnIndex("name")
            val result = mutableSetOf<String>()
            while (cursor.moveToNext()) {
                result += cursor.getString(nameIndex)
            }
            return result
        }
    }

    private fun passphrase(): ByteArray {
        return DatabasePassphraseProvider(context).getOrCreatePassphrase()
    }

    private companion object {
        const val DB_V1_TO_V4 = "migration-test-v1-v4.db"
        const val DB_V3_TO_V4 = "migration-test-v3-v4.db"
    }
}


