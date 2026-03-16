package com.example.biometrianeonatal.data.seed

import androidx.room.withTransaction
import com.example.biometrianeonatal.core.database.AppDatabase
import com.example.biometrianeonatal.core.database.BabyEntity
import com.example.biometrianeonatal.core.database.BiometricSessionEntity
import com.example.biometrianeonatal.core.database.GuardianEntity
import com.example.biometrianeonatal.core.database.GuardianRelation
import com.example.biometrianeonatal.core.database.HospitalEntity
import com.example.biometrianeonatal.core.database.SessionLifecycleStatus
import com.example.biometrianeonatal.core.database.Sex
import com.example.biometrianeonatal.core.database.SyncStatus
import com.example.biometrianeonatal.core.database.UserEntity
import com.example.biometrianeonatal.core.database.UserRole
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class DatabaseSeeder(
    private val database: AppDatabase,
) {
    suspend fun seedIfNeeded() {
        if (database.hospitalDao().count() > 0 || database.userDao().count() > 0) {
            return
        }

        val today = LocalDate.now()
        database.withTransaction {
            database.hospitalDao().insertAll(
                listOf(
                    HospitalEntity(
                        id = "hospital-pb",
                        name = "Hospital UTFPR Pato Branco",
                        city = "Pato Branco",
                        state = "PR",
                    ),
                    HospitalEntity(
                        id = "hospital-parceiro",
                        name = "Maternidade Parceira",
                        city = "Pato Branco",
                        state = "PR",
                    ),
                )
            )

            database.userDao().insertAll(
                listOf(
                    UserEntity(
                        id = "user-operador",
                        name = "João Paulo",
                        email = "operador@utfpr.edu.br",
                        password = "123456",
                        role = UserRole.OPERADOR,
                        hospitalId = "hospital-pb",
                    ),
                    UserEntity(
                        id = "user-admin",
                        name = "Ana Admin",
                        email = "admin@utfpr.edu.br",
                        password = "123456",
                        role = UserRole.ADMINISTRADOR,
                        hospitalId = "hospital-pb",
                    ),
                    UserEntity(
                        id = "user-pesquisa",
                        name = "Carlos Pesquisa",
                        email = "pesquisa@utfpr.edu.br",
                        password = "123456",
                        role = UserRole.PESQUISADOR,
                        hospitalId = "hospital-pb",
                    ),
                )
            )

            database.babyDao().insertAll(
                listOf(
                    BabyEntity(
                        id = "baby-maria",
                        name = "Maria Silva",
                        birthDate = today.toString(),
                        birthTime = "08:12",
                        sex = Sex.FEMININO,
                        weightGrams = "3200",
                        heightCm = "49",
                        hospitalId = "hospital-pb",
                        medicalRecord = "PB-2026-001",
                        observations = "Aguardando coleta biométrica.",
                        createdAt = LocalDateTime.now().minusHours(2).toString(),
                        updatedAt = LocalDateTime.now().minusHours(2).toString(),
                        deletedAt = null,
                    ),
                    BabyEntity(
                        id = "baby-joao",
                        name = "João Pedro",
                        birthDate = today.minusDays(1).toString(),
                        birthTime = "14:05",
                        sex = Sex.MASCULINO,
                        weightGrams = "3450",
                        heightCm = "50",
                        hospitalId = "hospital-pb",
                        medicalRecord = "PB-2026-002",
                        observations = "Coleta inicial concluída.",
                        createdAt = LocalDateTime.now().minusDays(1).toString(),
                        updatedAt = LocalDateTime.now().minusDays(1).toString(),
                        deletedAt = null,
                    ),
                    BabyEntity(
                        id = "baby-ana",
                        name = "Ana Julia",
                        birthDate = today.minusDays(2).toString(),
                        birthTime = "20:20",
                        sex = Sex.FEMININO,
                        weightGrams = "2980",
                        heightCm = "47",
                        hospitalId = "hospital-parceiro",
                        medicalRecord = "MP-2026-014",
                        observations = "Triagem concluída. Em espera para coleta.",
                        createdAt = LocalDateTime.now().minusDays(2).toString(),
                        updatedAt = LocalDateTime.now().minusDays(2).toString(),
                        deletedAt = null,
                    ),
                )
            )

            database.guardianDao().insertAll(
                listOf(
                    GuardianEntity(
                        id = "guard-maria-mae",
                        babyId = "baby-maria",
                        name = "Fernanda Silva",
                        document = "111.111.111-11",
                        phone = "(46) 99999-1111",
                        relation = GuardianRelation.MAE,
                        addressCity = "Pato Branco",
                        addressState = "PR",
                        addressLine = "Rua das Araucárias, 123",
                        signatureBase64 = "demo-signature-mae",
                        createdAt = LocalDateTime.now().minusHours(2).toString(),
                        updatedAt = LocalDateTime.now().minusHours(2).toString(),
                        deletedAt = null,
                    ),
                    GuardianEntity(
                        id = "guard-maria-pai",
                        babyId = "baby-maria",
                        name = "Marcos Silva",
                        document = "222.222.222-22",
                        phone = "(46) 99999-2222",
                        relation = GuardianRelation.PAI,
                        addressCity = "Pato Branco",
                        addressState = "PR",
                        addressLine = "Rua das Araucárias, 123",
                        signatureBase64 = "demo-signature-pai",
                        createdAt = LocalDateTime.now().minusHours(2).toString(),
                        updatedAt = LocalDateTime.now().minusHours(2).toString(),
                        deletedAt = null,
                    ),
                )
            )

            database.sessionDao().insertAll(
                listOf(
                    BiometricSessionEntity(
                        id = "session-joao-01",
                        babyId = "baby-joao",
                        operatorId = "user-operador",
                        sessionDate = LocalDateTime.of(today.minusDays(1), LocalTime.of(16, 0)).toString(),
                        deviceId = "tablet-01",
                        sensorSerial = "INFANT-240-A",
                        lifecycleStatus = SessionLifecycleStatus.COMPLETED,
                        syncStatus = SyncStatus.SYNCED,
                        notes = "Sessão sincronizada com sucesso.",
                    ),
                    BiometricSessionEntity(
                        id = "session-maria-01",
                        babyId = "baby-maria",
                        operatorId = "user-operador",
                        sessionDate = LocalDateTime.of(today, LocalTime.of(10, 30)).toString(),
                        deviceId = "tablet-01",
                        sensorSerial = "INFANT-240-A",
                        lifecycleStatus = SessionLifecycleStatus.IN_PROGRESS,
                        syncStatus = SyncStatus.PENDING,
                        notes = "Pré-cadastro para coleta.",
                    ),
                )
            )
        }
    }
}

