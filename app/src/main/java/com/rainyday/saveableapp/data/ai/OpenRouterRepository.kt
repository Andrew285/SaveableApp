package com.rainyday.saveableapp.data.ai

import com.rainyday.saveableapp.BuildConfig
import com.rainyday.saveableapp.data.auth.FirebaseAuthRepository
import com.rainyday.saveableapp.data.local.FieldType
import com.rainyday.saveableapp.data.local.Priority
import com.rainyday.saveableapp.data.local.RecurrenceRule
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

/** Per-flavor Cloud Function URLs — see the "stage"/"prod" product flavors in app/build.gradle.kts. */
private val ENDPOINT = BuildConfig.AI_PARSE_ENDPOINT
private val ENRICH_ENDPOINT = BuildConfig.AI_ENRICH_ENDPOINT
private val ISO_DATE = "yyyy-MM-dd"

/** Fields extracted from free-form task text. Any field the model can't determine is left null. */
data class ParsedTask(
    val title: String,
    val notes: String?,
    val priority: Priority?,
    val dueDate: Long?,
    val listName: String?,
    val tagNames: List<String>,
    val recurrence: RecurrenceRule,
    /** A brand-new list name the model suggests when none of the existing lists fit, or null. */
    val suggestedNewListName: String?
)

/** Fields extracted from free-form simple-list item text. Any field the model can't determine is left null. */
data class ParsedListItem(
    val text: String,
    val note: String?,
    val url: String?,
    val listName: String?,
    /** Custom field values keyed by the field's own name (as passed into [OpenRouterRepository.parseListItem]). */
    val fieldValues: Map<String, String>,
    /** A brand-new list name the model suggests when none of the existing lists fit, or null. */
    val suggestedNewListName: String?,
    /** What real-world thing [text] names, so the caller can look up a poster/photo — see [EntityType]. */
    val entityType: EntityType
)

/** What kind of real-world thing an item's title names, used to route enrichment lookups. */
enum class EntityType {
    MOVIE, TV,
    /** Some other real, look-up-able thing (a place, book, person, historical topic, ...). */
    GENERAL,
    /** A plain task/grocery/generic item with no real-world identity worth looking up. */
    NONE
}

/** A poster/photo and short description resolved for an item's title from a real-world data source. */
data class EntityEnrichment(val imageUrl: String?, val description: String?)

/** One custom field a simple list defines, e.g. "Rating" (RATING). */
data class FieldSpec(val name: String, val type: FieldType)

/** An existing simple list and the custom fields it defines, given as context for AI parsing. */
data class SimpleListContext(val name: String, val fields: List<FieldSpec> = emptyList())

/**
 * Calls our Firebase Cloud Function ("aiParse"), which itself calls OpenRouter's OpenAI-compatible
 * chat completions endpoint (`openai/gpt-oss-120b`), to turn quick-add text into structured fields.
 *
 * The OpenRouter API key never lives on-device: the Cloud Function holds it as a secret and forwards
 * the request. The caller must be signed in with Google (see [FirebaseAuthRepository]) — its Firebase
 * ID token is sent as a bearer token so the function can identify the user and enforce their
 * subscription's daily call quota.
 */
class OpenRouterRepository(private val firebaseAuthRepository: FirebaseAuthRepository) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    suspend fun parseTask(
        input: String,
        existingListNames: List<String>,
        existingTagNames: List<String>
    ): Result<ParsedTask> = withContext(Dispatchers.IO) {
        runCatching {
            val idToken = firebaseAuthRepository.getIdToken()
            require(!idToken.isNullOrEmpty()) { "Sign in with Google in Settings to use AI task parsing." }

            val systemPrompt = buildSystemPrompt(Date(), existingListNames, existingTagNames)
            val responseBody = postChatCompletion(idToken, systemPrompt, input)

            val chatResponse = json.decodeFromString<AiChatResponse>(responseBody)
            val content = chatResponse.choices.firstOrNull()?.message?.content
            require(!content.isNullOrBlank()) { "The AI parser returned an empty response." }

            json.decodeFromString<ParsedTaskDto>(content).toParsedTask(fallbackTitle = input.trim())
        }
    }

    suspend fun parseListItem(
        input: String,
        existingLists: List<SimpleListContext>
    ): Result<ParsedListItem> = withContext(Dispatchers.IO) {
        runCatching {
            val idToken = firebaseAuthRepository.getIdToken()
            require(!idToken.isNullOrEmpty()) { "Sign in with Google in Settings to use AI task parsing." }

            val systemPrompt = buildListItemSystemPrompt(Date(), existingLists)
            val responseBody = postChatCompletion(idToken, systemPrompt, input)

            val chatResponse = json.decodeFromString<AiChatResponse>(responseBody)
            val content = chatResponse.choices.firstOrNull()?.message?.content
            require(!content.isNullOrBlank()) { "The AI parser returned an empty response." }

            json.decodeFromString<ParsedListItemDto>(content)
                .toParsedListItem(fallbackText = input.trim(), existingLists = existingLists)
        }
    }

    private fun buildListItemSystemPrompt(referenceDate: Date, existingLists: List<SimpleListContext>): String {
        val datesTable = buildUpcomingDatesTable(referenceDate)
        val listNames = existingLists.joinToString { it.name }
        val fieldsTable = if (existingLists.isEmpty()) {
            "  (no lists yet)"
        } else {
            existingLists.joinToString("\n") { list ->
                val fieldsDesc = if (list.fields.isEmpty()) {
                    "no custom fields"
                } else {
                    list.fields.joinToString(", ") { field ->
                        "${field.name} (${field.type.name}${if (field.type == FieldType.RATING) " 1-5" else ""})"
                    }
                }
                "  - ${list.name}: $fieldsDesc"
            }
        }
        return """
        You are a parsing assistant for a simple-lists app (e.g. movies to watch, books to read,
        groceries, gift ideas). Extract structured fields from the user's raw item text and respond
        with ONLY a JSON object with exactly these keys:
        - "text": short item title (string, required, never empty). Strip a leading category word that
          only names the kind of thing (e.g. "Movie", "Book", "Фільм", "Книга") from the title — that
          word should only help you pick the right list below, it must not remain part of the title.
        - "note": a short additional detail mentioned in the text, or null if there is none
        - "url": a URL mentioned in the text, or null if none is present
        - "list": the single best-matching list name from this exact set of existing lists: [$listNames], or null if none of them clearly fit
        - "suggested_list": ONLY set this when "list" above is null because nothing existing fits — a
          short new list name you'd propose creating for this item (e.g. "Recipes", "Wishlist"), based
          on the item's real category. Null whenever "list" is non-null, or when the item is too vague
          to suggest a sensible category.
        - "entity_type": classify what "text" actually names, so a poster/photo can be looked up for it —
          one of "movie" (a film), "tv" (a TV/streaming series), "general" (some other real,
          look-up-able thing with its own identity — a place, book, person, historical event, animal,
          product, etc.), or "none" (a plain task/grocery/generic item with no real-world identity worth
          looking up, e.g. "buy milk" or "call the dentist"). Base this only on what the title itself
          names, not on which list it belongs to.

        If the item text is just a URL (or a URL plus very little else), use the URL itself — its
        domain, path, and slug — plus what you know about that site to infer a real, human-readable
        title and to guess which list fits best (e.g. a movie/streaming/reviews URL suggests a movies
        list, a bookstore or reading-tracker URL suggests a books list, a recipe site suggests a
        recipes list, a shop/product URL suggests a shopping or wishlist). Only fall back to a generic
        label like the bare domain name for "text" if nothing more specific can reasonably be inferred.

        If the item text includes a parenthetical like `(The link's actual page title is: "...")`, that
        title was fetched directly from the page, so treat it as the authoritative answer for "text" —
        use it verbatim (only stripping a leading category word as above) instead of guessing your own,
        and use it to help judge "list" and "fields" too. Never copy that parenthetical annotation itself
        into "note" — it is context for you, not something the user wrote.

        Existing lists and the custom fields each one defines (only relevant once you've picked a list above):
        $fieldsTable

        - "fields": a JSON array of {"name": <field name>, "value": <string>} — ONLY for the custom
          fields that belong to the list you picked in "list" above, and ONLY when the item text
          clearly implies a value for that field. Skip a field entirely (do not include it) if the
          text says nothing about it — never guess a value just to fill it in. Format "value" to match
          the field's declared type:
            TEXT -> plain text; NUMBER -> digits only, no units; RATING -> a whole number from 1 to 5;
            DATE -> as "$ISO_DATE", resolved via this table instead of computing it yourself (always
            match to the nearest upcoming date):
          $datesTable
          If the list you picked has no custom fields, or none of them clearly apply, "fields" MUST be
          an empty array.

        The item text may be written in any language (e.g. English or Ukrainian) — keep "text", "note",
        and any TEXT-type field value in that same language.

        Respond with raw JSON only. No markdown, no code fences, no explanation.
        """.trimIndent()
    }

    private fun buildSystemPrompt(referenceDate: Date, existingLists: List<String>, existingTags: List<String>): String {
        val todayIso = SimpleDateFormat(ISO_DATE, Locale.US).format(referenceDate)
        val datesTable = buildUpcomingDatesTable(referenceDate)
        return """
        You are a task-parsing assistant for a to-do app. Today's date is $todayIso.
        Extract structured fields from the user's raw task text and respond with ONLY a JSON object
        with exactly these keys:
        - "title": short task title (string, required, never empty)
        - "notes": additional detail, or null if there is none
        - "priority": one of "LOW", "MEDIUM", "HIGH", "URGENT", or null if unclear

        - "due_date": due date as "$ISO_DATE", or null.
          To resolve a relative date ("today", "tomorrow", a weekday name, "next <weekday>", etc.), look it
          up in this table instead of computing it yourself — always match to the nearest upcoming date:
          $datesTable
          If the text gives an explicit calendar date instead, convert that. If the text mentions no date
          or time reference at all, "due_date" MUST be null — never invent one.

        - "list": the single best-matching list name from this exact set of existing lists: [${existingLists.joinToString()}], or null if none of them clearly fit
        - "suggested_list": ONLY set this when "list" above is null because nothing existing fits — a
          short new list name you'd propose creating for this task (e.g. "Errands", "Bills"), based on
          the task's real category. Null whenever "list" is non-null, or when the task is too vague to
          suggest a sensible category.

        - "recurrence": one of "DAILY", "WEEKLY", "MONTHLY", "YEARLY", or null if the task doesn't
          repeat. Recognize phrases like "every day"/"щодня" -> DAILY, "every Monday"/"щопонеділка" or
          any single specific weekday -> WEEKLY, "every month"/"щомісяця" -> MONTHLY, "every year"/
          "щороку" -> YEARLY. Only set this when the text clearly implies repetition — a one-off task
          with just a due date is not recurring.

        - "tags": a JSON array of 1-3 short lowercase topic tags describing what the task is actually about.
          Tags are expected on most tasks. First check whether an existing tag already means the same
          thing: [${existingTags.joinToString()}]. Reuse it ONLY if its meaning genuinely matches the
          task's subject — never reuse an existing tag just because it happens to be on that list.
          If none of the existing tags fit, invent a short new one yourself for the task's real topic
          (e.g. "groceries", "health", "finance", "work", "car") — inventing a new, well-fitting tag is
          the normal expected outcome, not a last resort. Only leave the array empty when the task text
          is genuinely too vague or generic to have any identifiable topic (e.g. "do the thing").
          The task text may be written in any language (e.g. English or Ukrainian). Keep "title" and
          "notes" in that same language. For "tags" specifically: write any new tag in the same language
          as the majority of the existing tags listed above (if there are none yet, use the language of
          the task text itself), and never translate or respell an existing tag when reusing it verbatim.
          Keep every tag in one single consistent language — do not mix languages across the tags array.

        Respond with raw JSON only. No markdown, no code fences, no explanation.
        """.trimIndent()
    }

    /** A day-by-day lookup the model can match relative dates against instead of doing date arithmetic. */
    private fun buildUpcomingDatesTable(referenceDate: Date): String {
        val calendar = Calendar.getInstance().apply { time = referenceDate }
        val dayFormat = SimpleDateFormat(ISO_DATE, Locale.US)
        val weekdayFormat = SimpleDateFormat("EEEE", Locale.US)
        return (0..7).joinToString("\n") { offset ->
            val weekday = weekdayFormat.format(calendar.time)
            val label = when (offset) {
                0 -> "today, $weekday"
                1 -> "tomorrow, $weekday"
                else -> weekday
            }
            val line = "  $label -> ${dayFormat.format(calendar.time)}"
            calendar.add(Calendar.DAY_OF_MONTH, 1)
            line
        }
    }

    /** Posts to our Cloud Function, authenticated with the caller's Firebase ID token (not an OpenRouter key). */
    private fun postChatCompletion(idToken: String, systemPrompt: String, userInput: String): String {
        val requestBody = buildJsonObject {
            put("temperature", 0.2)
            putJsonObject("response_format") { put("type", "json_object") }
            putJsonArray("messages") {
                addJsonObject {
                    put("role", "system")
                    put("content", systemPrompt)
                }
                addJsonObject {
                    put("role", "user")
                    put("content", userInput)
                }
            }
        }
        return postJson(ENDPOINT, idToken, requestBody.toString())
    }

    /**
     * Looks up a real poster/photo and short description for an item's title via our "enrichItem"
     * Cloud Function, which itself queries TMDb (movies/TV) or Wikipedia (everything else) depending
     * on [entityType]. Either result field may come back null if nothing was found.
     */
    suspend fun enrichEntity(entityType: EntityType, query: String): Result<EntityEnrichment> =
        withContext(Dispatchers.IO) {
            runCatching {
                val idToken = firebaseAuthRepository.getIdToken()
                require(!idToken.isNullOrEmpty()) { "Sign in with Google in Settings to use AI task parsing." }

                val requestBody = buildJsonObject {
                    put("entityType", entityType.name.lowercase(Locale.US))
                    put("query", query)
                }
                val responseBody = postJson(ENRICH_ENDPOINT, idToken, requestBody.toString())
                val dto = json.decodeFromString<EntityEnrichmentDto>(responseBody)
                EntityEnrichment(imageUrl = dto.imageUrl?.trim()?.takeIf { it.isNotBlank() }, description = dto.description?.trim()?.takeIf { it.isNotBlank() })
            }
        }

    /** Posts [jsonBody] to [endpoint], authenticated with the caller's Firebase ID token. */
    private fun postJson(endpoint: String, idToken: String, jsonBody: String): String {
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Authorization", "Bearer $idToken")
            setRequestProperty("Content-Type", "application/json")
            doOutput = true
            connectTimeout = 15_000
            readTimeout = 20_000
        }

        return try {
            connection.outputStream.use { it.write(jsonBody.toByteArray(Charsets.UTF_8)) }
            val responseCode = connection.responseCode
            val stream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
            val responseText = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            check(responseCode in 200..299) { describeError(responseCode, responseText) }
            responseText
        } finally {
            connection.disconnect()
        }
    }

    /** Surfaces the Cloud Function's `{"error": "..."}` message (e.g. quota exceeded) when present. */
    private fun describeError(responseCode: Int, responseText: String): String {
        val message = runCatching { json.decodeFromString<AiErrorDto>(responseText).error }.getOrNull()
        return message ?: "AI request failed ($responseCode): ${responseText.take(300)}"
    }

    private fun ParsedTaskDto.toParsedTask(fallbackTitle: String): ParsedTask {
        val resolvedPriority = priority?.trim()?.uppercase(Locale.US)
            ?.let { p -> runCatching { Priority.valueOf(p) }.getOrNull() }
        val resolvedDueDate = dueDate?.let { parseIsoDate(it) }
        val resolvedRecurrence = recurrence?.trim()?.uppercase(Locale.US)
            ?.let { r -> runCatching { RecurrenceRule.valueOf(r) }.getOrNull() }
            ?: RecurrenceRule.NONE
        return ParsedTask(
            title = title?.trim().takeUnless { it.isNullOrBlank() } ?: fallbackTitle,
            notes = notes?.trim()?.takeIf { it.isNotBlank() },
            priority = resolvedPriority,
            dueDate = resolvedDueDate,
            listName = list?.trim()?.takeIf { it.isNotBlank() },
            tagNames = tags.map { it.trim() }.filter { it.isNotBlank() }.distinct(),
            recurrence = resolvedRecurrence,
            suggestedNewListName = suggestedList?.trim()?.takeIf { it.isNotBlank() }
        )
    }

    private fun parseIsoDate(text: String): Long? = runCatching {
        val format = SimpleDateFormat(ISO_DATE, Locale.US).apply { isLenient = false }
        format.parse(text)?.time
    }.getOrNull()

    private fun ParsedListItemDto.toParsedListItem(
        fallbackText: String,
        existingLists: List<SimpleListContext>
    ): ParsedListItem {
        val resolvedListName = list?.trim()?.takeIf { it.isNotBlank() }
        val matchedList = resolvedListName?.let { name -> existingLists.firstOrNull { it.name.equals(name, ignoreCase = true) } }
        val resolvedFieldValues = fields.mapNotNull { field ->
            val fieldName = field.name?.trim().orEmpty()
            val rawValue = field.value?.trim().orEmpty()
            if (fieldName.isEmpty() || rawValue.isEmpty()) return@mapNotNull null
            val spec = matchedList?.fields?.firstOrNull { it.name.equals(fieldName, ignoreCase = true) } ?: return@mapNotNull null
            val converted = convertFieldValue(rawValue, spec.type) ?: return@mapNotNull null
            spec.name to converted
        }.toMap()

        val resolvedEntityType = entityType?.trim()?.uppercase(Locale.US)
            ?.let { t -> runCatching { EntityType.valueOf(t) }.getOrNull() } ?: EntityType.NONE

        return ParsedListItem(
            text = text?.trim().takeUnless { it.isNullOrBlank() } ?: fallbackText,
            note = note?.trim()?.takeIf { it.isNotBlank() },
            url = url?.trim()?.takeIf { it.isNotBlank() },
            listName = resolvedListName,
            fieldValues = resolvedFieldValues,
            suggestedNewListName = suggestedList?.trim()?.takeIf { it.isNotBlank() },
            entityType = resolvedEntityType
        )
    }

    /** Converts a model-provided field value into the raw string format the app stores for [type]. */
    private fun convertFieldValue(raw: String, type: FieldType): String? = when (type) {
        FieldType.TEXT -> raw
        FieldType.NUMBER -> Regex("-?\\d+(\\.\\d+)?").find(raw)?.value
        FieldType.RATING -> raw.toIntOrNull()?.coerceIn(1, 5)?.toString()
        FieldType.DATE -> parseIsoDate(raw)?.toString()
    }
}

@Serializable
private data class AiChatResponse(val choices: List<AiChoice> = emptyList())

@Serializable
private data class AiChoice(val message: AiMessage)

@Serializable
private data class AiMessage(val content: String? = null)

@Serializable
private data class AiErrorDto(val error: String? = null)

@Serializable
private data class ParsedTaskDto(
    val title: String? = null,
    val notes: String? = null,
    val priority: String? = null,
    @SerialName("due_date") val dueDate: String? = null,
    val list: String? = null,
    @SerialName("suggested_list") val suggestedList: String? = null,
    val recurrence: String? = null,
    val tags: List<String> = emptyList()
)

@Serializable
private data class ParsedListItemDto(
    val text: String? = null,
    val note: String? = null,
    val url: String? = null,
    val list: String? = null,
    @SerialName("suggested_list") val suggestedList: String? = null,
    val fields: List<ParsedFieldDto> = emptyList(),
    @SerialName("entity_type") val entityType: String? = null
)

@Serializable
private data class ParsedFieldDto(
    val name: String? = null,
    val value: String? = null
)

@Serializable
private data class EntityEnrichmentDto(
    val imageUrl: String? = null,
    val description: String? = null
)
