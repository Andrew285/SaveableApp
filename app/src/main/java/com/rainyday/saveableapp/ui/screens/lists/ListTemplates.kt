package com.rainyday.saveableapp.ui.screens.lists

data class SimpleListTemplate(
    val label: String,
    val name: String,
    val icon: String,
    val colorHex: String,
    val showCheckbox: Boolean
)

val simpleListTemplates = listOf(
    SimpleListTemplate("Movies", "Movies to watch", "movie", "#7C5CC7", true),
    SimpleListTemplate("Books", "Books to read", "book", "#4C8FE0", true),
    SimpleListTemplate("Shopping", "Shopping list", "shopping", "#6FB668", true),
    SimpleListTemplate("Quotes", "Favorite quotes", "quote", "#EF8A3D", false),
    SimpleListTemplate("Bucket list", "Bucket list", "star", "#E4574C", true)
)
