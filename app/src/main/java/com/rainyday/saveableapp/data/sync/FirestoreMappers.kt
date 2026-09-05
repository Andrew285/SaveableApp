package com.rainyday.saveableapp.data.sync

import com.rainyday.saveableapp.data.local.FieldDefinitionEntity
import com.rainyday.saveableapp.data.local.FieldType
import com.rainyday.saveableapp.data.local.FieldValueEntity
import com.rainyday.saveableapp.data.local.FlashCardDeckEntity
import com.rainyday.saveableapp.data.local.FlashCardEntity
import com.rainyday.saveableapp.data.local.InfoBlockEntity
import com.rainyday.saveableapp.data.local.InfoCategoryEntity
import com.rainyday.saveableapp.data.local.Priority
import com.rainyday.saveableapp.data.local.RecurrenceRule
import com.rainyday.saveableapp.data.local.SimpleListEntity
import com.rainyday.saveableapp.data.local.SimpleListItemEntity
import com.rainyday.saveableapp.data.local.SyncEntityType
import com.rainyday.saveableapp.data.local.TagEntity
import com.rainyday.saveableapp.data.local.TaskWithTags
import com.rainyday.saveableapp.data.local.TodoListEntity
import com.rainyday.saveableapp.data.local.TodoTaskEntity

/**
 * Manual entity <-> Firestore document mapping (a `Map<String, Any?>` in, a `Map<String, Any?>` out)
 * rather than Firestore's reflection-based POJO mapper — these entities are already `@Serializable`
 * for local JSON backup, and keeping this conversion explicit avoids a second, less predictable
 * reflection path fighting the first. Enums are written as their [Enum.name] (e.g. "MEDIUM"), per
 * the readable-in-console tradeoff over ordinal ints. `id` is included in the document body too
 * (redundant with the Firestore doc id, but keeps every map self-contained for the pull path).
 */
fun SyncEntityType.firestoreCollectionName(): String = when (this) {
    SyncEntityType.TODO_LIST -> "todoLists"
    SyncEntityType.TODO_TASK -> "todoTasks"
    SyncEntityType.TAG -> "tags"
    SyncEntityType.SIMPLE_LIST -> "simpleLists"
    SyncEntityType.SIMPLE_LIST_ITEM -> "simpleListItems"
    SyncEntityType.INFO_CATEGORY -> "infoCategories"
    SyncEntityType.INFO_BLOCK -> "infoBlocks"
    SyncEntityType.FLASH_CARD_DECK -> "flashCardDecks"
    SyncEntityType.FLASH_CARD -> "flashCards"
    SyncEntityType.FIELD_DEFINITION -> "fieldDefinitions"
    SyncEntityType.FIELD_VALUE -> "fieldValues"
}

private fun Map<String, Any?>.str(key: String): String = this[key] as String
private fun Map<String, Any?>.strOrNull(key: String): String? = this[key] as String?
private fun Map<String, Any?>.long(key: String): Long = (this[key] as Number).toLong()
private fun Map<String, Any?>.longOrNull(key: String): Long? = (this[key] as Number?)?.toLong()
private fun Map<String, Any?>.int(key: String): Int = (this[key] as Number).toInt()
private fun Map<String, Any?>.double(key: String): Double = (this[key] as Number).toDouble()
private fun Map<String, Any?>.bool(key: String): Boolean = this[key] as Boolean

fun TodoListEntity.toFirestoreMap(): Map<String, Any?> = mapOf(
    "id" to id, "name" to name, "colorHex" to colorHex, "icon" to icon,
    "position" to position, "createdAt" to createdAt, "updatedAt" to updatedAt
)

fun Map<String, Any?>.toTodoListEntity(): TodoListEntity = TodoListEntity(
    id = str("id"), name = str("name"), colorHex = str("colorHex"), icon = str("icon"),
    position = int("position"), createdAt = long("createdAt"), updatedAt = long("updatedAt")
)

/** Embeds tag membership as `tagIds` — see the note on [SyncEntityType] about why there's no separate cross-ref collection. */
fun TaskWithTags.toFirestoreMap(): Map<String, Any?> = mapOf(
    "id" to task.id, "listId" to task.listId, "title" to task.title, "notes" to task.notes,
    "isDone" to task.isDone, "priority" to task.priority.name, "dueDate" to task.dueDate,
    "colorHex" to task.colorHex, "position" to task.position, "createdAt" to task.createdAt,
    "updatedAt" to task.updatedAt, "completedAt" to task.completedAt, "isArchived" to task.isArchived,
    "recurrence" to task.recurrence.name, "tagIds" to tags.map { it.id }
)

fun Map<String, Any?>.toTodoTaskEntity(): TodoTaskEntity = TodoTaskEntity(
    id = str("id"), listId = str("listId"), title = str("title"), notes = strOrNull("notes"),
    isDone = bool("isDone"), priority = Priority.valueOf(str("priority")), dueDate = longOrNull("dueDate"),
    colorHex = strOrNull("colorHex"), position = int("position"), createdAt = long("createdAt"),
    updatedAt = long("updatedAt"), completedAt = longOrNull("completedAt"), isArchived = bool("isArchived"),
    recurrence = RecurrenceRule.valueOf(str("recurrence"))
)

@Suppress("UNCHECKED_CAST")
fun Map<String, Any?>.remoteTagIds(): List<String> = (this["tagIds"] as? List<String>).orEmpty()

fun TagEntity.toFirestoreMap(): Map<String, Any?> = mapOf(
    "id" to id, "name" to name, "colorHex" to colorHex, "updatedAt" to updatedAt
)

fun Map<String, Any?>.toTagEntity(): TagEntity = TagEntity(
    id = str("id"), name = str("name"), colorHex = str("colorHex"), updatedAt = long("updatedAt")
)

fun SimpleListEntity.toFirestoreMap(): Map<String, Any?> = mapOf(
    "id" to id, "name" to name, "icon" to icon, "colorHex" to colorHex, "showCheckbox" to showCheckbox,
    "position" to position, "createdAt" to createdAt, "updatedAt" to updatedAt
)

fun Map<String, Any?>.toSimpleListEntity(): SimpleListEntity = SimpleListEntity(
    id = str("id"), name = str("name"), icon = str("icon"), colorHex = str("colorHex"),
    showCheckbox = bool("showCheckbox"), position = int("position"), createdAt = long("createdAt"),
    updatedAt = long("updatedAt")
)

fun SimpleListItemEntity.toFirestoreMap(): Map<String, Any?> = mapOf(
    "id" to id, "listId" to listId, "text" to text, "note" to note, "url" to url, "imageUrl" to imageUrl,
    "isChecked" to isChecked, "position" to position, "createdAt" to createdAt, "updatedAt" to updatedAt
)

fun Map<String, Any?>.toSimpleListItemEntity(): SimpleListItemEntity = SimpleListItemEntity(
    id = str("id"), listId = str("listId"), text = str("text"), note = strOrNull("note"),
    url = strOrNull("url"), imageUrl = strOrNull("imageUrl"), isChecked = bool("isChecked"), position = int("position"),
    createdAt = long("createdAt"), updatedAt = long("updatedAt")
)

fun InfoCategoryEntity.toFirestoreMap(): Map<String, Any?> = mapOf(
    "id" to id, "name" to name, "icon" to icon, "colorHex" to colorHex, "position" to position, "updatedAt" to updatedAt
)

fun Map<String, Any?>.toInfoCategoryEntity(): InfoCategoryEntity = InfoCategoryEntity(
    id = str("id"), name = str("name"), icon = str("icon"), colorHex = str("colorHex"),
    position = int("position"), updatedAt = long("updatedAt")
)

fun InfoBlockEntity.toFirestoreMap(): Map<String, Any?> = mapOf(
    "id" to id, "categoryId" to categoryId, "title" to title, "content" to content,
    "isSensitive" to isSensitive, "isFavorite" to isFavorite, "position" to position,
    "createdAt" to createdAt, "updatedAt" to updatedAt, "expiryDate" to expiryDate
)

fun Map<String, Any?>.toInfoBlockEntity(): InfoBlockEntity = InfoBlockEntity(
    id = str("id"), categoryId = str("categoryId"), title = str("title"), content = str("content"),
    isSensitive = bool("isSensitive"), isFavorite = bool("isFavorite"), position = int("position"),
    createdAt = long("createdAt"), updatedAt = long("updatedAt"), expiryDate = longOrNull("expiryDate")
)

fun FlashCardDeckEntity.toFirestoreMap(): Map<String, Any?> = mapOf(
    "id" to id, "name" to name, "icon" to icon, "colorHex" to colorHex,
    "position" to position, "createdAt" to createdAt, "updatedAt" to updatedAt
)

fun Map<String, Any?>.toFlashCardDeckEntity(): FlashCardDeckEntity = FlashCardDeckEntity(
    id = str("id"), name = str("name"), icon = str("icon"), colorHex = str("colorHex"),
    position = int("position"), createdAt = long("createdAt"), updatedAt = long("updatedAt")
)

fun FlashCardEntity.toFirestoreMap(): Map<String, Any?> = mapOf(
    "id" to id, "deckId" to deckId, "front" to front, "back" to back, "position" to position,
    "createdAt" to createdAt, "updatedAt" to updatedAt, "intervalDays" to intervalDays,
    "easeFactor" to easeFactor, "repetitions" to repetitions, "dueAt" to dueAt
)

fun Map<String, Any?>.toFlashCardEntity(): FlashCardEntity = FlashCardEntity(
    id = str("id"), deckId = str("deckId"), front = str("front"), back = str("back"),
    position = int("position"), createdAt = long("createdAt"), updatedAt = long("updatedAt"),
    intervalDays = int("intervalDays"), easeFactor = double("easeFactor"),
    repetitions = int("repetitions"), dueAt = long("dueAt")
)

fun FieldDefinitionEntity.toFirestoreMap(): Map<String, Any?> = mapOf(
    "id" to id, "listId" to listId, "name" to name, "type" to type.name, "colorHex" to colorHex,
    "position" to position, "createdAt" to createdAt, "updatedAt" to updatedAt
)

fun Map<String, Any?>.toFieldDefinitionEntity(): FieldDefinitionEntity = FieldDefinitionEntity(
    id = str("id"), listId = str("listId"), name = str("name"), type = FieldType.valueOf(str("type")),
    colorHex = str("colorHex"), position = int("position"), createdAt = long("createdAt"), updatedAt = long("updatedAt")
)

fun FieldValueEntity.toFirestoreMap(): Map<String, Any?> = mapOf(
    "id" to id, "itemId" to itemId, "fieldId" to fieldId, "value" to value, "updatedAt" to updatedAt
)

fun Map<String, Any?>.toFieldValueEntity(): FieldValueEntity = FieldValueEntity(
    id = str("id"), itemId = str("itemId"), fieldId = str("fieldId"), value = str("value"), updatedAt = long("updatedAt")
)
