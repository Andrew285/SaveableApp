package com.rainyday.saveableapp.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.vector.ImageVector

/** Curated set of icons users can attach to lists, categories and info blocks. */
object IconCatalog {
    val icons: Map<String, ImageVector> = linkedMapOf(
        "checklist" to Icons.Filled.Checklist,
        "star" to Icons.Filled.Star,
        "movie" to Icons.Filled.Movie,
        "book" to Icons.AutoMirrored.Filled.MenuBook,
        "quote" to Icons.Filled.FormatQuote,
        "restaurant" to Icons.Filled.Restaurant,
        "fitness" to Icons.Filled.FitnessCenter,
        "home" to Icons.Filled.Home,
        "work" to Icons.Filled.Work,
        "school" to Icons.Filled.School,
        "flight" to Icons.Filled.Flight,
        "shopping" to Icons.Filled.ShoppingCart,
        "pets" to Icons.Filled.Pets,
        "music" to Icons.Filled.MusicNote,
        "palette" to Icons.Filled.Palette,
        "code" to Icons.Filled.Terminal,
        "money" to Icons.Filled.AttachMoney,
        "health" to Icons.Filled.LocalHospital,
        "car" to Icons.Filled.DirectionsCar,
        "badge" to Icons.Filled.Badge,
        "document" to Icons.Filled.Description,
        "card" to Icons.Filled.CreditCard,
        "fingerprint" to Icons.Filled.Fingerprint,
        "key" to Icons.Filled.Key,
        "wallet" to Icons.Filled.Wallet
    )

    fun resolve(key: String): ImageVector = icons[key] ?: Icons.Filled.Checklist

    val defaultKey: String = "checklist"
}
