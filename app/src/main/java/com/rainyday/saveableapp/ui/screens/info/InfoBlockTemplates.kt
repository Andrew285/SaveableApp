package com.rainyday.saveableapp.ui.screens.info

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.rainyday.saveableapp.R

data class InfoBlockTemplate(
    val label: String,
    val title: String,
    val isSensitive: Boolean
)

@Composable
fun infoBlockTemplates(): List<InfoBlockTemplate> {
    val wifiPasswordLabel = stringResource(R.string.info_template_wifi_password_label)
    return listOf(
        InfoBlockTemplate(
            stringResource(R.string.info_template_passport_label),
            stringResource(R.string.info_template_passport_title),
            isSensitive = true
        ),
        InfoBlockTemplate(
            stringResource(R.string.info_template_drivers_license_label),
            stringResource(R.string.info_template_drivers_license_title),
            isSensitive = true
        ),
        InfoBlockTemplate(
            stringResource(R.string.info_template_id_card_label),
            stringResource(R.string.info_template_id_card_title),
            isSensitive = true
        ),
        InfoBlockTemplate(
            stringResource(R.string.info_template_bank_card_label),
            stringResource(R.string.info_template_bank_card_title),
            isSensitive = true
        ),
        InfoBlockTemplate(wifiPasswordLabel, wifiPasswordLabel, isSensitive = true),
        InfoBlockTemplate(
            stringResource(R.string.info_template_membership_label),
            stringResource(R.string.info_template_membership_title),
            isSensitive = false
        )
    )
}
