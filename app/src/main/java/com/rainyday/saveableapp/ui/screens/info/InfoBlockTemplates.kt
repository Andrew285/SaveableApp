package com.rainyday.saveableapp.ui.screens.info

data class InfoBlockTemplate(
    val label: String,
    val title: String,
    val isSensitive: Boolean
)

val infoBlockTemplates = listOf(
    InfoBlockTemplate("Passport", "Passport number", isSensitive = true),
    InfoBlockTemplate("Driver's license", "Driver's license number", isSensitive = true),
    InfoBlockTemplate("ID card", "ID card number", isSensitive = true),
    InfoBlockTemplate("Bank card", "Bank card number", isSensitive = true),
    InfoBlockTemplate("Wi-Fi password", "Wi-Fi password", isSensitive = true),
    InfoBlockTemplate("Membership", "Membership number", isSensitive = false)
)
