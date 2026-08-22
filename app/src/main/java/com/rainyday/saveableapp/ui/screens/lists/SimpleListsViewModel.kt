package com.rainyday.saveableapp.ui.screens.lists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rainyday.saveableapp.data.ai.FieldSpec
import com.rainyday.saveableapp.data.ai.GroqRepository
import com.rainyday.saveableapp.data.ai.SimpleListContext
import com.rainyday.saveableapp.data.local.FieldDefinitionEntity
import com.rainyday.saveableapp.data.local.FieldTemplate
import com.rainyday.saveableapp.data.local.SimpleListEntity
import com.rainyday.saveableapp.data.repository.ListsRepository
import com.rainyday.saveableapp.data.repository.SimpleListSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SimpleListUiModel(val list: SimpleListEntity, val checked: Int, val total: Int)

/** Item fields resolved from AI parsing, ready to prefill the AI review sheet. */
data class AiListItemDraft(
    val listId: Long,
    val text: String,
    val note: String?,
    val url: String?,
    val fieldValues: Map<Long, String>
)

sealed interface AiListItemOutcome {
    data class Success(val draft: AiListItemDraft) : AiListItemOutcome
    data class Error(val message: String) : AiListItemOutcome
}

private const val UNCATEGORIZED_LIST_NAME = "Uncategorized"
private const val UNCATEGORIZED_LIST_COLOR_HEX = "#9E9E9E"
private const val UNCATEGORIZED_LIST_ICON_KEY = "checklist"

class SimpleListsViewModel(
    private val repository: ListsRepository,
    private val groqRepository: GroqRepository
) : ViewModel() {
    // null while the first Room emission hasn't arrived yet, so the UI can tell "loading" apart from "empty".
    private val _lists = MutableStateFlow<List<SimpleListUiModel>?>(null)
    val lists: StateFlow<List<SimpleListUiModel>?> = _lists

    private val _aiParsing = MutableStateFlow(false)
    val aiParsing: StateFlow<Boolean> = _aiParsing

    val fieldsByListId: StateFlow<Map<Long, List<FieldDefinitionEntity>>> = repository.observeAllFields()
        .map { fields -> fields.groupBy { it.listId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    init {
        viewModelScope.launch {
            combine(repository.observeLists(), repository.observeProgress()) { lists, progress ->
                val progressByList = progress.associateBy { it.listId }
                lists.map { list ->
                    val p = progressByList[list.id]
                    SimpleListUiModel(list, checked = p?.checked ?: 0, total = p?.total ?: 0)
                }
            }.collect { _lists.value = it }
        }
    }

    fun createList(
        name: String,
        icon: String,
        colorHex: String,
        showCheckbox: Boolean,
        fieldTemplates: List<FieldTemplate> = emptyList()
    ) {
        viewModelScope.launch { repository.createList(name, icon, colorHex, showCheckbox, fieldTemplates) }
    }

    fun updateList(list: SimpleListEntity, name: String, icon: String, colorHex: String, showCheckbox: Boolean) {
        viewModelScope.launch {
            repository.updateList(
                list.copy(name = name, icon = icon, colorHex = colorHex, showCheckbox = showCheckbox)
            )
        }
    }

    suspend fun deleteListWithUndo(list: SimpleListEntity): SimpleListSnapshot =
        repository.deleteListWithSnapshot(list)

    suspend fun restoreList(snapshot: SimpleListSnapshot) = repository.restoreList(snapshot)

    fun createItem(listId: Long, text: String, note: String?, url: String?, fieldValues: Map<Long, String> = emptyMap()) {
        viewModelScope.launch {
            val itemId = repository.createItem(listId, text, note, url)
            if (fieldValues.isNotEmpty()) repository.setItemFieldValues(itemId, fieldValues)
        }
    }

    /** Sends [input] to Groq to extract an item's title, note, link, destination list, and custom field values. */
    suspend fun parseItemWithAi(input: String): AiListItemOutcome {
        _aiParsing.value = true
        return try {
            val fieldsSnapshot = fieldsByListId.value
            val contexts = lists.value.orEmpty().map { entry ->
                SimpleListContext(
                    name = entry.list.name,
                    fields = fieldsSnapshot[entry.list.id].orEmpty().map { FieldSpec(it.name, it.type) }
                )
            }
            val result = groqRepository.parseListItem(input = input, existingLists = contexts)
            result.fold(
                onSuccess = { parsed ->
                    val listId = resolveListId(parsed.listName)
                    val fieldValues = resolveFieldValues(listId, parsed.fieldValues, fieldsSnapshot)
                    AiListItemOutcome.Success(
                        AiListItemDraft(
                            listId = listId,
                            text = parsed.text,
                            note = parsed.note,
                            url = parsed.url,
                            fieldValues = fieldValues
                        )
                    )
                },
                onFailure = { e -> AiListItemOutcome.Error(e.message ?: "AI parsing failed") }
            )
        } finally {
            _aiParsing.value = false
        }
    }

    /** Maps AI field values (keyed by field name) to the resolved list's actual field ids. */
    private fun resolveFieldValues(
        listId: Long,
        byName: Map<String, String>,
        fieldsSnapshot: Map<Long, List<FieldDefinitionEntity>>
    ): Map<Long, String> {
        if (byName.isEmpty()) return emptyMap()
        val fieldsForList = fieldsSnapshot[listId].orEmpty()
        return byName.mapNotNull { (name, value) ->
            fieldsForList.firstOrNull { it.name.equals(name, ignoreCase = true) }?.let { it.id to value }
        }.toMap()
    }

    /** Matches [name] against existing lists case-insensitively; falls back to (creating) Uncategorized. */
    private suspend fun resolveListId(name: String?): Long {
        val current = lists.value.orEmpty().map { it.list }
        val trimmed = name?.trim().orEmpty()
        if (trimmed.isNotEmpty()) {
            current.firstOrNull { it.name.equals(trimmed, ignoreCase = true) }?.let { return it.id }
        }
        current.firstOrNull { it.name.equals(UNCATEGORIZED_LIST_NAME, ignoreCase = true) }?.let { return it.id }
        return repository.createList(
            name = UNCATEGORIZED_LIST_NAME,
            icon = UNCATEGORIZED_LIST_ICON_KEY,
            colorHex = UNCATEGORIZED_LIST_COLOR_HEX,
            showCheckbox = true
        )
    }
}
