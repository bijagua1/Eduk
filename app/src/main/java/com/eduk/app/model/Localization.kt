package com.eduk.app.model

import java.util.Locale

data class Country(
    val name: String,
    val code: String,
    val flag: String
)

data class Language(
    val name: String,
    val code: String
)

object LocalizationData {
    val countries: List<Country> = Locale.getISOCountries()
        .mapNotNull { code ->
            val countryName = Locale("", code).getDisplayCountry(Locale.ENGLISH)
            if (countryName.isBlank()) {
                null
            } else {
                Country(countryName, code, flagFor(code))
            }
        }
        .sortedBy { it.name }

    val languages: List<Language> = Locale.getISOLanguages()
        .mapNotNull { code ->
            val englishName = Locale(code).getDisplayLanguage(Locale.ENGLISH)
            if (englishName.isBlank()) {
                null
            } else {
                val nativeName = Locale(code).getDisplayLanguage(Locale(code))
                val name = if (nativeName.isBlank() || nativeName == englishName) {
                    englishName
                } else {
                    "$nativeName ($englishName)"
                }
                Language(name, code)
            }
        }
        .sortedBy { it.name }

    private fun flagFor(countryCode: String): String = countryCode
        .uppercase(Locale.ROOT)
        .map { character -> String(Character.toChars(character.code + 0x1F1A5)) }
        .joinToString("")
}
