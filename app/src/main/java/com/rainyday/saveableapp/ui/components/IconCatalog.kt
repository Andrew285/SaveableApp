package com.rainyday.saveableapp.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Bed
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsBoat
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Hiking
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.Icecream
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.LocalBar
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.LocalGroceryStore
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocalLibrary
import androidx.compose.material.icons.filled.LocalPizza
import androidx.compose.material.icons.filled.Luggage
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Nightlife
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Piano
import androidx.compose.material.icons.filled.Pool
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.SportsBasketball
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.SportsFootball
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.SportsTennis
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Theaters
import androidx.compose.material.icons.filled.Train
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Umbrella
import androidx.compose.material.icons.filled.VideogameAsset
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.Yard
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
        "wallet" to Icons.Filled.Wallet,
        // Food & drink
        "cake" to Icons.Filled.Cake,
        "bar" to Icons.Filled.LocalBar,
        "cafe" to Icons.Filled.LocalCafe,
        "pizza" to Icons.Filled.LocalPizza,
        "icecream" to Icons.Filled.Icecream,
        "nightlife" to Icons.Filled.Nightlife,
        "grocery" to Icons.Filled.LocalGroceryStore,
        // Entertainment & hobbies
        "theater" to Icons.Filled.Theaters,
        "piano" to Icons.Filled.Piano,
        "mic" to Icons.Filled.Mic,
        "headphones" to Icons.Filled.Headphones,
        "brush" to Icons.Filled.Brush,
        "camera" to Icons.Filled.Camera,
        "gamepad" to Icons.Filled.Gamepad,
        "videogame" to Icons.Filled.VideogameAsset,
        "esports" to Icons.Filled.SportsEsports,
        "casino" to Icons.Filled.Casino,
        "puzzle" to Icons.Filled.Extension,
        "reading" to Icons.Filled.AutoStories,
        "library" to Icons.Filled.LocalLibrary,
        // Sports & fitness
        "basketball" to Icons.Filled.SportsBasketball,
        "soccer" to Icons.Filled.SportsSoccer,
        "football" to Icons.Filled.SportsFootball,
        "tennis" to Icons.Filled.SportsTennis,
        "hiking" to Icons.Filled.Hiking,
        "bike" to Icons.Filled.DirectionsBike,
        "pool" to Icons.Filled.Pool,
        "yoga" to Icons.Filled.SelfImprovement,
        "spa" to Icons.Filled.Spa,
        // Travel & places
        "boat" to Icons.Filled.DirectionsBoat,
        "bus" to Icons.Filled.DirectionsBus,
        "train" to Icons.Filled.Train,
        "hotel" to Icons.Filled.Hotel,
        "luggage" to Icons.Filled.Luggage,
        "map" to Icons.Filled.Map,
        "globe" to Icons.Filled.Public,
        "language" to Icons.Filled.Language,
        // Nature & outdoors
        "florist" to Icons.Filled.LocalFlorist,
        "park" to Icons.Filled.Park,
        "eco" to Icons.Filled.Eco,
        "yard" to Icons.Filled.Yard,
        "water" to Icons.Filled.WaterDrop,
        "sunny" to Icons.Filled.WbSunny,
        "cloud" to Icons.Filled.Cloud,
        "umbrella" to Icons.Filled.Umbrella,
        // Home & tools
        "bed" to Icons.Filled.Bed,
        "handyman" to Icons.Filled.Handyman,
        "bolt" to Icons.Filled.Bolt,
        // Wellness
        "medical" to Icons.Filled.MedicalServices,
        "medication" to Icons.Filled.Medication,
        // Finance
        "bank" to Icons.Filled.AccountBalance,
        "savings" to Icons.Filled.Savings,
        "trending" to Icons.Filled.TrendingUp,
        "receipt" to Icons.Filled.Receipt,
        "gift" to Icons.Filled.CardGiftcard,
        // People & social
        "face" to Icons.Filled.Face,
        "groups" to Icons.Filled.Groups,
        "celebration" to Icons.Filled.Celebration,
        "mood" to Icons.Filled.Mood,
        // Tech & science
        "laptop" to Icons.Filled.Laptop,
        "science" to Icons.Filled.Science
    )

    fun resolve(key: String): ImageVector = icons[key] ?: Icons.Filled.Checklist

    val defaultKey: String = "checklist"
}
