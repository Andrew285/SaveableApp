package com.rainyday.saveableapp.ui.screens.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.rainyday.saveableapp.R
import com.rainyday.saveableapp.data.local.Priority
import com.rainyday.saveableapp.data.local.SimpleListItemEntity
import com.rainyday.saveableapp.data.local.TagEntity
import com.rainyday.saveableapp.data.local.TaskWithTags
import com.rainyday.saveableapp.data.local.TodoTaskEntity
import com.rainyday.saveableapp.ui.theme.Dimens
import com.rainyday.saveableapp.ui.theme.SaveableAppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onOpenTodoList: (String) -> Unit,
    onOpenSimpleList: (String) -> Unit
) {
    val viewModel: SearchViewModel = hiltViewModel()
    val query by viewModel.query.collectAsState()
    val results by viewModel.results.collectAsState()

    SearchScreenContent(
        query = query,
        results = results,
        onBack = onBack,
        onOpenTodoList = onOpenTodoList,
        onOpenSimpleList = onOpenSimpleList,
        onQueryChange = viewModel::setQuery
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchScreenContent(
    query: String,
    results: SearchResults,
    onBack: () -> Unit,
    onOpenTodoList: (String) -> Unit,
    onOpenSimpleList: (String) -> Unit,
    onQueryChange: (String) -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.search_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                placeholder = { Text(stringResource(R.string.search_placeholder)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Dimens.d16)
            )

            if (query.isNotBlank() && results.tasks.isEmpty() && results.items.isEmpty()) {
                Text(
                    text = stringResource(R.string.search_no_results, query),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(Dimens.d16)
                )
            }

            LazyColumn(
                contentPadding = PaddingValues(Dimens.d16, Dimens.d0, Dimens.d16, Dimens.d24),
                verticalArrangement = Arrangement.spacedBy(Dimens.d8)
            ) {
                if (results.tasks.isNotEmpty()) {
                    item { SearchSectionLabel(stringResource(R.string.search_section_tasks)) }
                    items(results.tasks, key = { "task-${it.task.id}" }) { result ->
                        SearchResultRow(
                            icon = Icons.Filled.Checklist,
                            title = result.task.title,
                            subtitle = result.task.notes,
                            onClick = { onOpenTodoList(result.task.listId) }
                        )
                    }
                }
                if (results.items.isNotEmpty()) {
                    item { SearchSectionLabel(stringResource(R.string.search_section_list_items)) }
                    items(results.items, key = { "item-${it.id}" }) { result ->
                        SearchResultRow(
                            icon = Icons.AutoMirrored.Filled.MenuBook,
                            title = result.text,
                            subtitle = result.note,
                            onClick = { onOpenSimpleList(result.listId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchSectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = Dimens.d8)
    )
}

@Composable
private fun SearchResultRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String?,
    onClick: () -> Unit
) {
    ElevatedCard(
        onClick = onClick,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = Dimens.d0),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(Dimens.d12)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private val previewSearchResults = SearchResults(
    tasks = listOf(
        TaskWithTags(
            task = TodoTaskEntity(
                id = "task-1",
                listId = "list-1",
                title = "Buy groceries",
                notes = "Milk, eggs, bread",
                priority = Priority.MEDIUM,
                createdAt = 0L,
                updatedAt = 0L
            ),
            tags = listOf(TagEntity(id = "tag-1", name = "home", colorHex = "#43A047", updatedAt = 0L))
        )
    ),
    items = listOf(
        SimpleListItemEntity(
            id = "item-1",
            listId = "list-2",
            text = "The Hobbit",
            note = "J.R.R. Tolkien",
            createdAt = 0L,
            updatedAt = 0L
        )
    )
)

@Preview(showBackground = true)
@Composable
private fun SearchScreenPreview() {
    SaveableAppTheme {
        SearchScreenContent(
            query = "book",
            results = previewSearchResults,
            onBack = {},
            onOpenTodoList = {},
            onOpenSimpleList = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SearchScreenEmptyPreview() {
    SaveableAppTheme {
        SearchScreenContent(
            query = "xyz",
            results = SearchResults(),
            onBack = {},
            onOpenTodoList = {},
            onOpenSimpleList = {}
        )
    }
}
