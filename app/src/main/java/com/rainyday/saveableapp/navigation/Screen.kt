package com.rainyday.saveableapp.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface Screen {
    @Serializable data object Tasks : Screen
    @Serializable data object SimpleLists : Screen
    @Serializable data class SimpleListDetail(val listId: String) : Screen
    @Serializable data object InfoCategories : Screen
    @Serializable data class InfoCategoryDetail(val categoryId: String) : Screen
    @Serializable data object FlashCardDecks : Screen
    @Serializable data class FlashCardDeckDetail(val deckId: String) : Screen
    @Serializable data class FlashCardStudy(val deckId: String) : Screen
    @Serializable data object Search : Screen
    @Serializable data object Settings : Screen
}
