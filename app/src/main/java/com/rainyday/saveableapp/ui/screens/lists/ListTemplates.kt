package com.rainyday.saveableapp.ui.screens.lists

import com.rainyday.saveableapp.data.local.FieldTemplate
import com.rainyday.saveableapp.data.local.FieldType

data class SimpleListTemplate(
    val label: String,
    val name: String,
    val icon: String,
    val colorHex: String,
    val showCheckbox: Boolean,
    val fields: List<FieldTemplate> = emptyList()
)

val simpleListTemplates = listOf(
    SimpleListTemplate(
        "Movies", "Movies to watch", "movie", "#7C5CC7", true,
        listOf(
            FieldTemplate("Rating", FieldType.RATING, "#E8B23A"),
            FieldTemplate("Genre", FieldType.TEXT, "#4C8FE0")
        )
    ),
    SimpleListTemplate(
        "Books", "Books to read", "book", "#4C8FE0", true,
        listOf(
            FieldTemplate("Author", FieldType.TEXT, "#4C8FE0"),
            FieldTemplate("Rating", FieldType.RATING, "#E8B23A")
        )
    ),
    SimpleListTemplate(
        "TV Shows", "TV shows to watch", "theater", "#D2609C", true,
        listOf(
            FieldTemplate("Rating", FieldType.RATING, "#E8B23A"),
            FieldTemplate("Season", FieldType.NUMBER, "#3FA796")
        )
    ),
    SimpleListTemplate(
        "Shopping", "Shopping list", "shopping", "#6FB668", true,
        listOf(FieldTemplate("Price", FieldType.NUMBER, "#E4574C"))
    ),
    SimpleListTemplate(
        "Quotes", "Favorite quotes", "quote", "#EF8A3D", false,
        listOf(FieldTemplate("Author", FieldType.TEXT, "#4C8FE0"))
    ),
    SimpleListTemplate(
        "Bucket list", "Bucket list", "star", "#E4574C", true,
        listOf(FieldTemplate("Target date", FieldType.DATE, "#7C5CC7"))
    ),
    SimpleListTemplate(
        "Recipes", "Recipes to try", "restaurant", "#EF8A3D", true,
        listOf(
            FieldTemplate("Cuisine", FieldType.TEXT, "#3FA796"),
            FieldTemplate("Rating", FieldType.RATING, "#E8B23A")
        )
    ),
    SimpleListTemplate(
        "Restaurants", "Restaurants to try", "cafe", "#D2609C", true,
        listOf(
            FieldTemplate("Rating", FieldType.RATING, "#E8B23A"),
            FieldTemplate("Cuisine", FieldType.TEXT, "#3FA796")
        )
    ),
    SimpleListTemplate(
        "Wishlist", "Wishlist", "gift", "#D2609C", true,
        listOf(FieldTemplate("Price", FieldType.NUMBER, "#E4574C"))
    ),
    SimpleListTemplate(
        "Games", "Games to play", "esports", "#4C5FD5", true,
        listOf(
            FieldTemplate("Platform", FieldType.TEXT, "#3FA796"),
            FieldTemplate("Rating", FieldType.RATING, "#E8B23A")
        )
    ),
    SimpleListTemplate(
        "Podcasts", "Podcasts", "headphones", "#3FA796", true,
        listOf(FieldTemplate("Rating", FieldType.RATING, "#E8B23A"))
    ),
    SimpleListTemplate(
        "Wine & Drinks", "Wine & drinks", "bar", "#E4574C", true,
        listOf(
            FieldTemplate("Rating", FieldType.RATING, "#E8B23A"),
            FieldTemplate("Region", FieldType.TEXT, "#3FA796")
        )
    ),
    SimpleListTemplate(
        "Plants", "Plants to water", "florist", "#6FB668", true,
        listOf(FieldTemplate("Next watering", FieldType.DATE, "#3FA796"))
    ),
    SimpleListTemplate(
        "Workouts", "Workouts", "fitness", "#E4574C", true,
        listOf(FieldTemplate("Duration (min)", FieldType.NUMBER, "#3FA796"))
    )
)
