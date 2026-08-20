package com.eduk.app.model

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
    val countries = listOf(
        Country("United States", "US", "🇺🇸"),
        Country("United Kingdom", "GB", "🇬🇧"),
        Country("Spain", "ES", "🇪🇸"),
        Country("Mexico", "MX", "🇲🇽"),
        Country("Colombia", "CO", "🇨🇴"),
        Country("Argentina", "AR", "🇦🇷"),
        Country("Brazil", "BR", "🇧🇷"),
        Country("France", "FR", "🇫🇷"),
        Country("Germany", "DE", "🇩🇪"),
        Country("Italy", "IT", "🇮🇹"),
        Country("Canada", "CA", "🇨🇦"),
        Country("Australia", "AU", "🇦🇺")
    )

    val languages = listOf(
        Language("English", "en"),
        Language("Español", "es"),
        Language("Français", "fr"),
        Language("Deutsch", "de"),
        Language("Português", "pt"),
        Language("Italiano", "it")
    )
}
