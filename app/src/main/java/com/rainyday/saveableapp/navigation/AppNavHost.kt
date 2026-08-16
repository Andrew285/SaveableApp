package com.rainyday.saveableapp.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.rainyday.saveableapp.ui.screens.flashcards.FlashCardDeckDetailScreen
import com.rainyday.saveableapp.ui.screens.flashcards.FlashCardDecksScreen
import com.rainyday.saveableapp.ui.screens.flashcards.FlashCardStudyScreen
import com.rainyday.saveableapp.ui.screens.info.InfoCategoriesScreen
import com.rainyday.saveableapp.ui.screens.info.InfoCategoryDetailScreen
import com.rainyday.saveableapp.ui.screens.lists.SimpleListDetailScreen
import com.rainyday.saveableapp.ui.screens.lists.SimpleListsScreen
import com.rainyday.saveableapp.ui.screens.search.SearchScreen
import com.rainyday.saveableapp.ui.screens.settings.SettingsScreen
import com.rainyday.saveableapp.ui.screens.todo.TodoListDetailScreen
import com.rainyday.saveableapp.ui.screens.todo.TodoListsScreen

private data class BottomTab(val screen: Screen, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val bottomTabs = listOf(
    BottomTab(Screen.TodoLists, "To-Do", Icons.Filled.Checklist),
    BottomTab(Screen.SimpleLists, "Lists", Icons.AutoMirrored.Filled.MenuBook),
    BottomTab(Screen.FlashCardDecks, "Cards", Icons.Filled.Style),
    BottomTab(Screen.InfoCategories, "Info", Icons.Filled.Badge)
)

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    val currentRoute = currentDestination?.route
    val showBottomBar = bottomTabs.any { it.screen::class.qualifiedName == currentRoute }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomTabs.forEach { tab ->
                        val selected = tab.screen::class.qualifiedName == currentRoute
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(tab.screen) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.TodoLists,
            modifier = Modifier.padding(padding)
        ) {
            composable<Screen.TodoLists> {
                TodoListsScreen(
                    onOpenList = { navController.navigate(Screen.TodoListDetail(it)) },
                    onOpenSearch = { navController.navigate(Screen.Search) },
                    onOpenSettings = { navController.navigate(Screen.Settings) }
                )
            }
            composable<Screen.TodoListDetail> { entry ->
                val args = entry.toRoute<Screen.TodoListDetail>()
                TodoListDetailScreen(listId = args.listId, onBack = { navController.popBackStack() })
            }
            composable<Screen.SimpleLists> {
                SimpleListsScreen(
                    onOpenList = { navController.navigate(Screen.SimpleListDetail(it)) },
                    onOpenSearch = { navController.navigate(Screen.Search) },
                    onOpenSettings = { navController.navigate(Screen.Settings) }
                )
            }
            composable<Screen.SimpleListDetail> { entry ->
                val args = entry.toRoute<Screen.SimpleListDetail>()
                SimpleListDetailScreen(listId = args.listId, onBack = { navController.popBackStack() })
            }
            composable<Screen.FlashCardDecks> {
                FlashCardDecksScreen(
                    onOpenDeck = { navController.navigate(Screen.FlashCardDeckDetail(it)) },
                    onOpenSearch = { navController.navigate(Screen.Search) },
                    onOpenSettings = { navController.navigate(Screen.Settings) }
                )
            }
            composable<Screen.FlashCardDeckDetail> { entry ->
                val args = entry.toRoute<Screen.FlashCardDeckDetail>()
                FlashCardDeckDetailScreen(
                    deckId = args.deckId,
                    onBack = { navController.popBackStack() },
                    onStudy = { navController.navigate(Screen.FlashCardStudy(args.deckId)) }
                )
            }
            composable<Screen.FlashCardStudy> { entry ->
                val args = entry.toRoute<Screen.FlashCardStudy>()
                FlashCardStudyScreen(deckId = args.deckId, onBack = { navController.popBackStack() })
            }
            composable<Screen.InfoCategories> {
                InfoCategoriesScreen(
                    onOpenCategory = { navController.navigate(Screen.InfoCategoryDetail(it)) },
                    onOpenSearch = { navController.navigate(Screen.Search) },
                    onOpenSettings = { navController.navigate(Screen.Settings) }
                )
            }
            composable<Screen.InfoCategoryDetail> { entry ->
                val args = entry.toRoute<Screen.InfoCategoryDetail>()
                InfoCategoryDetailScreen(categoryId = args.categoryId, onBack = { navController.popBackStack() })
            }
            composable<Screen.Search> {
                SearchScreen(
                    onBack = { navController.popBackStack() },
                    onOpenTodoList = { navController.navigate(Screen.TodoListDetail(it)) },
                    onOpenSimpleList = { navController.navigate(Screen.SimpleListDetail(it)) }
                )
            }
            composable<Screen.Settings> {
                SettingsScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
