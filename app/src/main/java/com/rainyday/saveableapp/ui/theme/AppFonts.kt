package com.rainyday.saveableapp.ui.theme

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont

private val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = com.rainyday.saveableapp.R.array.com_google_android_gms_fonts_certs
)

private val spaceGroteskName = GoogleFont("Space Grotesk")
private val jetBrainsMonoName = GoogleFont("JetBrains Mono")

/** Display/headline face — bold geometric sans used for large screen titles. */
val SpaceGrotesk = FontFamily(
    Font(googleFont = spaceGroteskName, fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = spaceGroteskName, fontProvider = provider, weight = FontWeight.SemiBold),
    Font(googleFont = spaceGroteskName, fontProvider = provider, weight = FontWeight.Bold)
)

/** Mono face — eyebrow labels, meta text, tags, badges, buttons, nav labels. */
val JetBrainsMono = FontFamily(
    Font(googleFont = jetBrainsMonoName, fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = jetBrainsMonoName, fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = jetBrainsMonoName, fontProvider = provider, weight = FontWeight.Bold)
)
