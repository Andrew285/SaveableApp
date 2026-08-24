package com.rainyday.saveableapp.navigation

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.rainyday.saveableapp.R
import com.rainyday.saveableapp.ui.theme.Dimens
import com.rainyday.saveableapp.ui.screens.flashcards.FlashCardDeckDetailScreen
import com.rainyday.saveableapp.ui.screens.flashcards.FlashCardDecksScreen
import com.rainyday.saveableapp.ui.screens.flashcards.FlashCardStudyScreen
import com.rainyday.saveableapp.ui.screens.info.InfoCategoriesScreen
import com.rainyday.saveableapp.ui.screens.info.InfoCategoryDetailScreen
import com.rainyday.saveableapp.ui.screens.lists.SimpleListDetailScreen
import com.rainyday.saveableapp.ui.screens.lists.SimpleListsScreen
import com.rainyday.saveableapp.ui.screens.search.SearchScreen
import com.rainyday.saveableapp.ui.screens.settings.SettingsScreen
import com.rainyday.saveableapp.ui.screens.todo.TasksScreen

private data class BottomTab(
    val screen: Screen,
    @StringRes val labelRes: Int,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

private val bottomTabs = listOf(
    BottomTab(Screen.Tasks, R.string.nav_tasks, Icons.Filled.Checklist),
    BottomTab(Screen.SimpleLists, R.string.nav_lists, Icons.Filled.Folder),
    BottomTab(Screen.FlashCardDecks, R.string.nav_cards, Icons.Filled.Style),
    BottomTab(Screen.InfoCategories, R.string.nav_vault, Icons.Filled.Lock),
    BottomTab(Screen.Settings, R.string.nav_settings, Icons.Filled.Tune)
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .navigationBarsPadding()
                        .height(Dimens.d64),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    bottomTabs.forEach { tab ->
                        val selected = tab.screen::class.qualifiedName == currentRoute
                        val label = stringResource(tab.labelRes)
                        val color = if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(1f)
                                .selectable(
                                    selected = selected,
                                    role = Role.Tab,
                                    onClick = {
                                        navController.navigate(tab.screen) {
                                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                ),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(tab.icon, contentDescription = label, tint = color)
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                color = color,
                                modifier = Modifier.padding(top = Dimens.d2)
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Tasks,
            modifier = Modifier
                .padding(padding)
                .consumeWindowInsets(WindowInsets.navigationBars)
        ) {
            composable<Screen.Tasks> {
                TasksScreen(onOpenSearch = { navController.navigate(Screen.Search) })
            }
            composable<Screen.SimpleLists> {
                SimpleListsScreen(
                    onOpenList = { navController.navigate(Screen.SimpleListDetail(it)) },
                    onOpenSearch = { navController.navigate(Screen.Search) }
                )
            }
            composable<Screen.SimpleListDetail> { entry ->
                val args = entry.toRoute<Screen.SimpleListDetail>()
                SimpleListDetailScreen(listId = args.listId, onBack = { navController.popBackStack() })
            }
            composable<Screen.FlashCardDecks> {
                FlashCardDecksScreen(
                    onOpenDeck = { navController.navigate(Screen.FlashCardDeckDetail(it)) },
                    onOpenSearch = { navController.navigate(Screen.Search) }
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
                    onOpenSearch = { navController.navigate(Screen.Search) }
                )
            }
            composable<Screen.InfoCategoryDetail> { entry ->
                val args = entry.toRoute<Screen.InfoCategoryDetail>()
                InfoCategoryDetailScreen(categoryId = args.categoryId, onBack = { navController.popBackStack() })
            }
            composable<Screen.Search> {
                SearchScreen(
                    onBack = { navController.popBackStack() },
                    onOpenTodoList = {
                        navController.navigate(Screen.Tasks) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                        }
                    },
                    onOpenSimpleList = { navController.navigate(Screen.SimpleListDetail(it)) }
                )
            }
            composable<Screen.Settings> {
                SettingsScreen()
            }
        }
    }
}
