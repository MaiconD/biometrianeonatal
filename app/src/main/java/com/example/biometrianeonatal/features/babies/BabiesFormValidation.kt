package com.example.biometrianeonatal.features.babies

import com.example.biometrianeonatal.domain.model.GuardianDraft
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.ResolverStyle
import java.util.Locale

/**
 * Lista restrita de UFs aceitas no formulário de responsáveis.
 */
internal val BrazilianStateOptions = listOf(
    "AC", "AL", "AP", "AM", "BA", "CE", "DF", "ES", "GO", "MA",
    "MT", "MS", "MG", "PA", "PB", "PR", "PE", "PI", "RJ", "RN",
    "RS", "RO", "RR", "SC", "SP", "SE", "TO",
)

private val brazilianDateFormatter: DateTimeFormatter = DateTimeFormatter
    .ofPattern("dd/MM/uuuu", Locale.forLanguageTag("pt-BR"))
    .withResolverStyle(ResolverStyle.STRICT)

private val isoDateFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/** Mantém apenas dígitos e limita o peso ao tamanho esperado na interface. */
internal fun normalizeWeightInput(value: String): String = value.filter(Char::isDigit).take(5)

/** Mantém apenas dígitos e limita a altura ao tamanho esperado na interface. */
internal fun normalizeHeightInput(value: String): String = value.filter(Char::isDigit).take(3)

internal fun normalizeDocumentInput(value: String): String = formatCpf(value)

internal fun normalizePhoneInput(value: String): String = formatPhoneBr(value)

internal fun normalizeStateCode(value: String): String = value.trim().uppercase().take(2)

internal fun formatGuardianDraftForUi(draft: GuardianDraft): GuardianDraft {
    return draft.copy(
        document = formatCpf(draft.document),
        phone = formatPhoneBr(draft.phone),
        addressState = normalizeStateCode(draft.addressState),
    )
}

/** Converte datas salvas em ISO para o formato amigável exibido no formulário. */
internal fun toDisplayBirthDate(rawValue: String): String {
    val trimmed = rawValue.trim()
    if (trimmed.isBlank()) return ""
    parseDisplayBirthDate(trimmed)?.let { return it.format(brazilianDateFormatter) }
    parseStorageBirthDate(trimmed)?.let { return it.format(brazilianDateFormatter) }
    return trimmed
}

/** Converte a data digitada na UI para o formato estável persistido no banco. */
internal fun toStorageBirthDate(rawValue: String): String {
    val trimmed = rawValue.trim()
    if (trimmed.isBlank()) return ""
    return parseDisplayBirthDate(trimmed)?.format(isoDateFormatter)
        ?: parseStorageBirthDate(trimmed)?.format(isoDateFormatter)
        ?: trimmed
}

/** Rejeita datas vazias, inválidas ou futuras para proteger a consistência clínica. */
internal fun isValidBirthDate(rawValue: String): Boolean {
    val trimmed = rawValue.trim()
    if (trimmed.isBlank()) return false
    val date = parseDisplayBirthDate(trimmed) ?: parseStorageBirthDate(trimmed) ?: return false
    return !date.isAfter(LocalDate.now())
}

/** Padroniza a hora em HH:mm para manter a mesma exibição em criação e edição. */
internal fun toDisplayBirthTime(rawValue: String): String {
    val trimmed = rawValue.trim()
    if (trimmed.isBlank()) return ""
    return parseBirthTime(trimmed)?.format(timeFormatter) ?: trimmed.take(5)
}

internal fun isValidBirthTime(rawValue: String): Boolean {
    return parseBirthTime(rawValue.trim()) != null
}

internal fun formatBirthTime(hourOfDay: Int, minute: Int): String {
    return LocalTime.of(hourOfDay, minute).format(timeFormatter)
}

internal fun formatCpf(rawValue: String): String {
    val digits = rawValue.filter(Char::isDigit).take(11)
    return buildString {
        digits.forEachIndexed { index, char ->
            when (index) {
                3, 6 -> append('.')
                9 -> append('-')
            }
            append(char)
        }
    }
}

/** Implementa a validação matemática do CPF para evitar documentos obviamente inválidos. */
internal fun isValidCpf(rawValue: String): Boolean {
    val digits = rawValue.filter(Char::isDigit)
    if (digits.length != 11) return false
    if (digits.all { it == digits.first() }) return false

    val numbers = digits.map(Char::digitToInt)
    val firstDigit = calculateCpfCheckDigit(numbers.take(9), startWeight = 10)
    val secondDigit = calculateCpfCheckDigit(numbers.take(10), startWeight = 11)
    return numbers[9] == firstDigit && numbers[10] == secondDigit
}

/** Formata números brasileiros com 10 ou 11 dígitos para leitura humana no formulário. */
internal fun formatPhoneBr(rawValue: String): String {
    val digits = rawValue.filter(Char::isDigit).take(11)
    return when {
        digits.isEmpty() -> ""
        digits.length < 3 -> "(${digits}"
        digits.length < 7 -> "(${digits.take(2)}) ${digits.drop(2)}"
        digits.length < 11 -> "(${digits.take(2)}) ${digits.drop(2).take(4)}-${digits.drop(6)}"
        else -> "(${digits.take(2)}) ${digits.drop(2).take(5)}-${digits.drop(7)}"
    }
}

internal fun isValidPhoneNumber(rawValue: String): Boolean {
    val digits = rawValue.filter(Char::isDigit)
    return digits.length == 10 || digits.length == 11
}

private fun parseDisplayBirthDate(rawValue: String): LocalDate? {
    return try {
        LocalDate.parse(rawValue, brazilianDateFormatter)
    } catch (_: DateTimeParseException) {
        null
    }
}

private fun parseStorageBirthDate(rawValue: String): LocalDate? {
    return try {
        LocalDate.parse(rawValue, isoDateFormatter)
    } catch (_: DateTimeParseException) {
        null
    }
}

private fun parseBirthTime(rawValue: String): LocalTime? {
    val candidate = rawValue.take(5)
    return try {
        LocalTime.parse(candidate, timeFormatter)
    } catch (_: DateTimeParseException) {
        null
    }
}

private fun calculateCpfCheckDigit(numbers: List<Int>, startWeight: Int): Int {
    val sum = numbers.mapIndexed { index, number -> number * (startWeight - index) }.sum()
    val remainder = (sum * 10) % 11
    return if (remainder == 10) 0 else remainder
}


