package com.example.biometrianeonatal.features.babies

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Garante que as conversões de data do formulário permaneçam consistentes entre UI e persistência.
 */
class BabiesFormValidationTest {

    @Test
    fun toDisplayBirthDate_formats_iso_value_for_ui() {
        assertEquals("08/08/2024", toDisplayBirthDate("2024-08-08"))
    }

    @Test
    fun toStorageBirthDate_formats_display_value_for_persistence() {
        assertEquals("2024-08-08", toStorageBirthDate("08/08/2024"))
    }

    @Test
    fun birthDate_round_trip_preserves_selected_day() {
        // O valor retornado ao usuário após salvar e reabrir o formulário deve representar o mesmo dia.
        val selectedDate = "08/08/2024"

        val storedDate = toStorageBirthDate(selectedDate)
        val displayedDate = toDisplayBirthDate(storedDate)

        assertEquals(selectedDate, displayedDate)
    }

    @Test
    fun isValidBirthDate_accepts_today_and_past_dates() {
        assertTrue(isValidBirthDate(LocalDate.now().toString()))
        assertTrue(isValidBirthDate("08/08/2024"))
    }

    @Test
    fun isValidBirthDate_rejects_blank_and_future_dates() {
        assertFalse(isValidBirthDate(""))
        assertFalse(isValidBirthDate(LocalDate.now().plusDays(1).toString()))
    }
}

