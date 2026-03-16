package com.example.biometrianeonatal.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.time.LocalDateTime

/**
 * Catalogo de migrations do Room com estrategias de reconstrucao segura para tabelas legadas.
 */
object DatabaseMigrations {
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            val migrationTimestamp = LocalDateTime.now().toString().toSqlStringLiteral()

            rebuildAppUsers(db)
            rebuildBabies(db, migrationTimestamp)
            rebuildGuardians(db, migrationTimestamp)
            rebuildBiometricSessions(db)
            rebuildFingerprints(db, migrationTimestamp)
            rebuildSyncQueue(db)
            createAuditLogsTable(db)
        }
    }

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE guardians ADD COLUMN consentFilePath TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE guardians ADD COLUMN consentChecksumSha256 TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE fingerprints ADD COLUMN imageChecksumSha256 TEXT NOT NULL DEFAULT ''")
        }
    }

    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE guardians RENAME TO guardians_legacy")
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS guardians (
                    id TEXT NOT NULL,
                    babyId TEXT NOT NULL,
                    name TEXT NOT NULL,
                    document TEXT NOT NULL,
                    phone TEXT NOT NULL,
                    relation TEXT NOT NULL,
                    addressCity TEXT NOT NULL,
                    addressState TEXT NOT NULL,
                    addressLine TEXT NOT NULL,
                    signatureBase64 TEXT,
                    createdAt TEXT NOT NULL,
                    updatedAt TEXT NOT NULL,
                    deletedAt TEXT,
                    PRIMARY KEY(id)
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                INSERT INTO guardians(
                    id, babyId, name, document, phone, relation,
                    addressCity, addressState, addressLine, signatureBase64,
                    createdAt, updatedAt, deletedAt
                )
                SELECT
                    id, babyId, name, document, phone, relation,
                    addressCity, addressState, addressLine, NULL,
                    createdAt, updatedAt, deletedAt
                FROM guardians_legacy
                """.trimIndent(),
            )
            db.execSQL("DROP TABLE guardians_legacy")
        }
    }

    val ALL = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)

    private fun rebuildAppUsers(database: SupportSQLiteDatabase) {
        val tableName = "app_users"
        if (!database.hasTable(tableName)) {
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS app_users (
                    id TEXT NOT NULL,
                    name TEXT NOT NULL,
                    email TEXT NOT NULL,
                    password TEXT NOT NULL,
                    role TEXT NOT NULL,
                    hospitalId TEXT NOT NULL,
                    PRIMARY KEY(id)
                )
                """.trimIndent(),
            )
            return
        }

        database.execSQL("ALTER TABLE app_users RENAME TO app_users_legacy")
        val legacyColumns = database.getColumnNames("app_users_legacy")
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS app_users (
                id TEXT NOT NULL,
                name TEXT NOT NULL,
                email TEXT NOT NULL,
                password TEXT NOT NULL,
                role TEXT NOT NULL,
                hospitalId TEXT NOT NULL,
                PRIMARY KEY(id)
            )
            """.trimIndent(),
        )
        database.execSQL(
            """
            INSERT INTO app_users(id, name, email, password, role, hospitalId)
            SELECT
                ${legacyColumns.columnExpr("id")},
                ${legacyColumns.columnExpr("name")},
                ${legacyColumns.columnExpr("name", fallbackSql = "''")},
                ${legacyColumns.columnExpr("password", fallbackSql = "'REMOTE_AUTH_ONLY'")},
                ${legacyColumns.columnExpr("role", fallbackSql = "'OPERADOR'")},
                ${legacyColumns.columnExpr("hospitalId", "hospital_id", fallbackSql = "''")}
            FROM app_users_legacy
            """.trimIndent(),
        )
        database.execSQL("DROP TABLE app_users_legacy")
    }

    private fun rebuildBabies(database: SupportSQLiteDatabase, migrationTimestamp: String) {
        val tableName = "babies"
        if (!database.hasTable(tableName)) {
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS babies (
                    id TEXT NOT NULL,
                    name TEXT NOT NULL,
                    birthDate TEXT NOT NULL,
                    birthTime TEXT NOT NULL,
                    sex TEXT NOT NULL,
                    weightGrams TEXT NOT NULL,
                    heightCm TEXT NOT NULL,
                    hospitalId TEXT NOT NULL,
                    medicalRecord TEXT NOT NULL,
                    observations TEXT NOT NULL,
                    createdAt TEXT NOT NULL,
                    updatedAt TEXT NOT NULL,
                    deletedAt TEXT,
                    PRIMARY KEY(id)
                )
                """.trimIndent(),
            )
            return
        }

        database.execSQL("ALTER TABLE babies RENAME TO babies_legacy")
        val legacyColumns = database.getColumnNames("babies_legacy")
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS babies (
                id TEXT NOT NULL,
                name TEXT NOT NULL,
                birthDate TEXT NOT NULL,
                birthTime TEXT NOT NULL,
                sex TEXT NOT NULL,
                weightGrams TEXT NOT NULL,
                heightCm TEXT NOT NULL,
                hospitalId TEXT NOT NULL,
                medicalRecord TEXT NOT NULL,
                observations TEXT NOT NULL,
                createdAt TEXT NOT NULL,
                updatedAt TEXT NOT NULL,
                deletedAt TEXT,
                PRIMARY KEY(id)
            )
            """.trimIndent(),
        )
        database.execSQL(
            """
            INSERT INTO babies(
                id, name, birthDate, birthTime, sex, weightGrams, heightCm,
                hospitalId, medicalRecord, observations, createdAt, updatedAt, deletedAt
            )
            SELECT
                ${legacyColumns.columnExpr("id")},
                ${legacyColumns.columnExpr("name", fallbackSql = "''")},
                ${legacyColumns.columnExpr("birthDate", "birth_date", fallbackSql = "''")},
                ${legacyColumns.columnExpr("birthTime", "birth_time", fallbackSql = "''")},
                ${legacyColumns.columnExpr("sex", fallbackSql = "'NAO_INFORMADO'")},
                ${legacyColumns.columnExpr("weightGrams", "weight", fallbackSql = "''")},
                ${legacyColumns.columnExpr("heightCm", "height", fallbackSql = "''")},
                ${legacyColumns.columnExpr("hospitalId", "hospital_id", fallbackSql = "''")},
                ${legacyColumns.columnExpr("medicalRecord", "medical_record", fallbackSql = "''")},
                ${legacyColumns.columnExpr("observations", fallbackSql = "''")},
                ${legacyColumns.columnExpr("createdAt", fallbackSql = "'$migrationTimestamp'")},
                ${legacyColumns.columnExpr("updatedAt", fallbackSql = "'$migrationTimestamp'")},
                ${legacyColumns.columnExpr("deletedAt", fallbackSql = "NULL")}
            FROM babies_legacy
            """.trimIndent(),
        )
        database.execSQL("DROP TABLE babies_legacy")
    }

    private fun rebuildGuardians(database: SupportSQLiteDatabase, migrationTimestamp: String) {
        val tableName = "guardians"
        if (!database.hasTable(tableName)) {
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS guardians (
                    id TEXT NOT NULL,
                    babyId TEXT NOT NULL,
                    name TEXT NOT NULL,
                    document TEXT NOT NULL,
                    phone TEXT NOT NULL,
                    relation TEXT NOT NULL,
                    addressCity TEXT NOT NULL,
                    addressState TEXT NOT NULL,
                    addressLine TEXT NOT NULL,
                    consentFileName TEXT NOT NULL,
                    consentAcceptedAt TEXT NOT NULL,
                    createdAt TEXT NOT NULL,
                    updatedAt TEXT NOT NULL,
                    deletedAt TEXT,
                    PRIMARY KEY(id)
                )
                """.trimIndent(),
            )
            return
        }

        database.execSQL("ALTER TABLE guardians RENAME TO guardians_legacy")
        val legacyColumns = database.getColumnNames("guardians_legacy")
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS guardians (
                id TEXT NOT NULL,
                babyId TEXT NOT NULL,
                name TEXT NOT NULL,
                document TEXT NOT NULL,
                phone TEXT NOT NULL,
                relation TEXT NOT NULL,
                addressCity TEXT NOT NULL,
                addressState TEXT NOT NULL,
                addressLine TEXT NOT NULL,
                consentFileName TEXT NOT NULL,
                consentAcceptedAt TEXT NOT NULL,
                createdAt TEXT NOT NULL,
                updatedAt TEXT NOT NULL,
                deletedAt TEXT,
                PRIMARY KEY(id)
            )
            """.trimIndent(),
        )
        database.execSQL(
            """
            INSERT INTO guardians(
                id, babyId, name, document, phone, relation, addressCity, addressState,
                addressLine, consentFileName, consentAcceptedAt, createdAt, updatedAt, deletedAt
            )
            SELECT
                ${legacyColumns.columnExpr("id")},
                ${legacyColumns.columnExpr("babyId", "baby_id", fallbackSql = "''")},
                ${legacyColumns.columnExpr("name", fallbackSql = "''")},
                ${legacyColumns.columnExpr("document", fallbackSql = "''")},
                ${legacyColumns.columnExpr("phone", fallbackSql = "''")},
                ${legacyColumns.columnExpr("relation", fallbackSql = "'RESPONSAVEL'")},
                ${legacyColumns.columnExpr("addressCity", "address_city", fallbackSql = "''")},
                ${legacyColumns.columnExpr("addressState", "address_state", fallbackSql = "''")},
                ${legacyColumns.columnExpr("addressLine", "address_line", fallbackSql = "''")},
                ${legacyColumns.columnExpr("consentFileName", "consent_file_name", fallbackSql = "''")},
                ${legacyColumns.columnExpr("consentAcceptedAt", "consent_accepted_at", fallbackSql = "'$migrationTimestamp'")},
                ${legacyColumns.columnExpr("createdAt", fallbackSql = "'$migrationTimestamp'")},
                ${legacyColumns.columnExpr("updatedAt", fallbackSql = "'$migrationTimestamp'")},
                ${legacyColumns.columnExpr("deletedAt", fallbackSql = "NULL")}
            FROM guardians_legacy
            """.trimIndent(),
        )
        database.execSQL("DROP TABLE guardians_legacy")
    }

    private fun rebuildBiometricSessions(database: SupportSQLiteDatabase) {
        val tableName = "biometric_sessions"
        if (!database.hasTable(tableName)) {
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS biometric_sessions (
                    id TEXT NOT NULL,
                    babyId TEXT NOT NULL,
                    operatorId TEXT NOT NULL,
                    sessionDate TEXT NOT NULL,
                    deviceId TEXT NOT NULL,
                    sensorSerial TEXT NOT NULL,
                    lifecycleStatus TEXT NOT NULL,
                    syncStatus TEXT NOT NULL,
                    notes TEXT NOT NULL,
                    PRIMARY KEY(id)
                )
                """.trimIndent(),
            )
            return
        }

        database.execSQL("ALTER TABLE biometric_sessions RENAME TO biometric_sessions_legacy")
        val legacyColumns = database.getColumnNames("biometric_sessions_legacy")
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS biometric_sessions (
                id TEXT NOT NULL,
                babyId TEXT NOT NULL,
                operatorId TEXT NOT NULL,
                sessionDate TEXT NOT NULL,
                deviceId TEXT NOT NULL,
                sensorSerial TEXT NOT NULL,
                lifecycleStatus TEXT NOT NULL,
                syncStatus TEXT NOT NULL,
                notes TEXT NOT NULL,
                PRIMARY KEY(id)
            )
            """.trimIndent(),
        )
        database.execSQL(
            """
            INSERT INTO biometric_sessions(
                id, babyId, operatorId, sessionDate, deviceId, sensorSerial,
                lifecycleStatus, syncStatus, notes
            )
            SELECT
                ${legacyColumns.columnExpr("id")},
                ${legacyColumns.columnExpr("babyId", "baby_id", fallbackSql = "''")},
                ${legacyColumns.columnExpr("operatorId", "operator_id", fallbackSql = "''")},
                ${legacyColumns.columnExpr("sessionDate", "session_date", fallbackSql = "''")},
                ${legacyColumns.columnExpr("deviceId", "device_id", fallbackSql = "''")},
                ${legacyColumns.columnExpr("sensorSerial", "sensor_serial", fallbackSql = "''")},
                ${legacyColumns.columnExpr("lifecycleStatus", fallbackSql = biometricLifecycleFallback(legacyColumns))},
                ${legacyColumns.columnExpr("syncStatus", "sync_status", fallbackSql = "'PENDING'")},
                ${legacyColumns.columnExpr("notes", fallbackSql = "'Migrated from legacy schema.'")}
            FROM biometric_sessions_legacy
            """.trimIndent(),
        )
        database.execSQL("DROP TABLE biometric_sessions_legacy")
    }

    private fun rebuildFingerprints(database: SupportSQLiteDatabase, migrationTimestamp: String) {
        val tableName = "fingerprints"
        if (!database.hasTable(tableName)) {
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS fingerprints (
                    id TEXT NOT NULL,
                    sessionId TEXT NOT NULL,
                    fingerCode TEXT NOT NULL,
                    imagePath TEXT NOT NULL,
                    templateBase64 TEXT NOT NULL,
                    qualityScore INTEGER NOT NULL,
                    fps INTEGER NOT NULL,
                    resolution TEXT NOT NULL,
                    capturedAt TEXT NOT NULL,
                    PRIMARY KEY(id)
                )
                """.trimIndent(),
            )
            return
        }

        database.execSQL("ALTER TABLE fingerprints RENAME TO fingerprints_legacy")
        val legacyColumns = database.getColumnNames("fingerprints_legacy")
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS fingerprints (
                id TEXT NOT NULL,
                sessionId TEXT NOT NULL,
                fingerCode TEXT NOT NULL,
                imagePath TEXT NOT NULL,
                templateBase64 TEXT NOT NULL,
                qualityScore INTEGER NOT NULL,
                fps INTEGER NOT NULL,
                resolution TEXT NOT NULL,
                capturedAt TEXT NOT NULL,
                PRIMARY KEY(id)
            )
            """.trimIndent(),
        )
        database.execSQL(
            """
            INSERT INTO fingerprints(
                id, sessionId, fingerCode, imagePath, templateBase64,
                qualityScore, fps, resolution, capturedAt
            )
            SELECT
                ${legacyColumns.columnExpr("id")},
                ${legacyColumns.columnExpr("sessionId", "session_id", fallbackSql = "''")},
                ${legacyColumns.columnExpr("fingerCode", "finger", fallbackSql = "''")},
                ${legacyColumns.columnExpr("imagePath", "image_path", fallbackSql = "''")},
                ${legacyColumns.columnExpr("templateBase64", "template_base64", fallbackSql = "''")},
                ${legacyColumns.columnExpr("qualityScore", "quality_score", fallbackSql = "0")},
                ${legacyColumns.columnExpr("fps", fallbackSql = "0")},
                ${legacyColumns.columnExpr("resolution", fallbackSql = "''")},
                ${legacyColumns.columnExpr("capturedAt", "captured_at", fallbackSql = "'$migrationTimestamp'")}
            FROM fingerprints_legacy
            """.trimIndent(),
        )
        database.execSQL("DROP TABLE fingerprints_legacy")
    }

    private fun rebuildSyncQueue(database: SupportSQLiteDatabase) {
        val tableName = "sync_queue"
        if (!database.hasTable(tableName)) {
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS sync_queue (
                    id TEXT NOT NULL,
                    entityType TEXT NOT NULL,
                    entityId TEXT NOT NULL,
                    status TEXT NOT NULL,
                    createdAt TEXT NOT NULL,
                    lastAttemptAt TEXT,
                    errorMessage TEXT,
                    PRIMARY KEY(id)
                )
                """.trimIndent(),
            )
            return
        }

        database.execSQL("ALTER TABLE sync_queue RENAME TO sync_queue_legacy")
        val legacyColumns = database.getColumnNames("sync_queue_legacy")
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS sync_queue (
                id TEXT NOT NULL,
                entityType TEXT NOT NULL,
                entityId TEXT NOT NULL,
                status TEXT NOT NULL,
                createdAt TEXT NOT NULL,
                lastAttemptAt TEXT,
                errorMessage TEXT,
                PRIMARY KEY(id)
            )
            """.trimIndent(),
        )
        database.execSQL(
            """
            INSERT INTO sync_queue(id, entityType, entityId, status, createdAt, lastAttemptAt, errorMessage)
            SELECT
                ${legacyColumns.columnExpr("id")},
                ${legacyColumns.columnExpr("entityType", "entity_type", fallbackSql = "''")},
                ${legacyColumns.columnExpr("entityId", "entity_id", fallbackSql = "''")},
                ${legacyColumns.columnExpr("status", fallbackSql = "'PENDING'")},
                ${legacyColumns.columnExpr("createdAt", "created_at", fallbackSql = "''")},
                ${legacyColumns.columnExpr("lastAttemptAt", "last_attempt_at", fallbackSql = "NULL")},
                ${legacyColumns.columnExpr("errorMessage", "error_message", fallbackSql = "NULL")}
            FROM sync_queue_legacy
            """.trimIndent(),
        )
        database.execSQL("DROP TABLE sync_queue_legacy")
    }

    private fun createAuditLogsTable(database: SupportSQLiteDatabase) {
        if (database.hasTable("audit_logs")) return
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS audit_logs (
                id TEXT NOT NULL,
                actorUserId TEXT NOT NULL,
                action TEXT NOT NULL,
                targetEntity TEXT NOT NULL,
                targetEntityId TEXT NOT NULL,
                createdAt TEXT NOT NULL,
                PRIMARY KEY(id)
            )
            """.trimIndent(),
        )
    }

    private fun SupportSQLiteDatabase.hasTable(tableName: String): Boolean {
        query("SELECT name FROM sqlite_master WHERE type='table' AND name=?", arrayOf(tableName)).use { cursor ->
            return cursor.moveToFirst()
        }
    }

    private fun SupportSQLiteDatabase.getColumnNames(tableName: String): Set<String> {
        query("PRAGMA table_info(`$tableName`)").use { cursor ->
            val nameIndex = cursor.getColumnIndex("name")
            val names = linkedSetOf<String>()
            while (cursor.moveToNext()) {
                names += cursor.getString(nameIndex)
            }
            return names
        }
    }

    private fun Set<String>.columnExpr(vararg candidates: String, fallbackSql: String = "''"): String {
        val column = candidates.firstOrNull { it in this } ?: return fallbackSql
        return "COALESCE(`$column`, $fallbackSql)"
    }

    private fun biometricLifecycleFallback(columns: Set<String>): String {
        val syncStatusExpr = columns.columnExpr("syncStatus", "sync_status", fallbackSql = "'PENDING'")
        return "CASE WHEN $syncStatusExpr = 'SYNCED' THEN 'COMPLETED' ELSE 'IN_PROGRESS' END"
    }

    private fun String.toSqlStringLiteral(): String {
        return replace("'", "''")
    }
}



