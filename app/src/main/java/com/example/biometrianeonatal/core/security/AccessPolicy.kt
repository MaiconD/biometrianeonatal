package com.example.biometrianeonatal.core.security

import com.example.biometrianeonatal.core.database.UserRole

object AccessPolicy {
    fun canViewBabySummary(role: UserRole?): Boolean {
        return canAccessReadOnlyBabies(role)
    }

    fun canViewSensitiveGuardianData(role: UserRole?): Boolean {
        return role == UserRole.OPERADOR || role == UserRole.ADMINISTRADOR
    }

    fun canCreateOrEditBaby(role: UserRole?): Boolean {
        return role == UserRole.OPERADOR || role == UserRole.ADMINISTRADOR
    }

    fun canManageGuardians(role: UserRole?): Boolean {
        return role == UserRole.OPERADOR || role == UserRole.ADMINISTRADOR
    }

    fun canCollectBiometrics(role: UserRole?): Boolean {
        return role == UserRole.OPERADOR || role == UserRole.ADMINISTRADOR
    }

    fun canSync(role: UserRole?): Boolean {
        return role == UserRole.OPERADOR || role == UserRole.ADMINISTRADOR
    }

    fun canViewHistory(role: UserRole?): Boolean {
        return canAccessReadOnlyBabies(role)
    }

    fun canViewGuardians(role: UserRole?): Boolean {
        return role == UserRole.OPERADOR || role == UserRole.ADMINISTRADOR
    }

    fun canAccessReadOnlyBabies(role: UserRole?): Boolean {
        return role == UserRole.OPERADOR || role == UserRole.ADMINISTRADOR || role == UserRole.PESQUISADOR
    }
}

