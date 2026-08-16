package com.rainyday.saveableapp.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface Screen {
    @Serializable data object TodoLists : Screen
    @Serializable data class TodoListDetail(val listId: Long) : Screen
    @Serializable data object SimpleLists : Screen
    @Serializable data class SimpleListDetail(val listId: Long) : Screen
    @Serializable data object InfoCategories : Screen
    @Serializable data class InfoCategoryDetail(val categoryId: Long) : Screen
    @Serializable data object FlashCardDecks : Screen
    @Serializable data class FlashCardDeckDetail(val deckId: Long) : Screen
    @Serializable data class FlashCardStudy(val deckId: Long) : Screen
    @Serializable data object Search : Screen
    @Serializable data object Settings : Screen
}
