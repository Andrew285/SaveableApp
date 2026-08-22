package com.rainyday.saveableapp.data.ai

import com.rainyday.saveableapp.data.local.FieldType
import com.rainyday.saveableapp.data.local.Priority
import com.rainyday.saveableapp.data.prefs.PreferencesRepository
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

private const val ENDPOINT = "https://api.groq.com/openai/v1/chat/completions"
private const val MODEL = "openai/gpt-oss-20b"
private val ISO_DATE = "yyyy-MM-dd"

/** Fields extracted from free-form task text. Any field the model can't determine is left null. */
data class ParsedTask(
    val title: String,
    val notes: String?,
    val priority: Priority?,
    val dueDate: Long?,
    val listName: String?,
    val tagNames: List<String>
)

/** Fields extracted from free-form simple-list item text. Any field the model can't determine is left null. */
data class ParsedListItem(
    val text: String,
    val note: String?,
    val url: String?,
    val listName: String?,
    /** Custom field values keyed by the field's own name (as passed into [GroqRepository.parseListItem]). */
    val fieldValues: Map<String, String>
)

/** One custom field a simple list defines, e.g. "Rating" (RATING). */
data class FieldSpec(val name: String, val type: FieldType)

/** An existing simple list and the custom fields it defines, given as context for AI parsing. */
data class SimpleListContext(val name: String, val fields: List<FieldSpec> = emptyList())

/**
 * Calls Groq's OpenAI-compatible chat completions endpoint (free-tier `openai/gpt-oss-20b`) to turn
 * a quick-add task string into structured fields. The API key is user-supplied (Settings, testing only).
 */
class GroqRepository(private val preferencesRepository: PreferencesRepository) {
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
            val apiKey = preferencesRepository.groqApiKey.first()?.trim()
            require(!apiKey.isNullOrEmpty()) { "Add a Groq API key in Settings to use AI task parsing." }

            val systemPrompt = buildSystemPrompt(Date(), existingListNames, existingTagNames)
            val responseBody = postChatCompletion(apiKey, systemPrompt, input)

            val chatResponse = json.decodeFromString<GroqChatResponse>(responseBody)
            val content = chatResponse.choices.firstOrNull()?.message?.content
            require(!content.isNullOrBlank()) { "Groq returned an empty response." }

            json.decodeFromString<ParsedTaskDto>(content).toParsedTask(fallbackTitle = input.trim())
        }
    }

    suspend fun parseListItem(
        input: String,
        existingLists: List<SimpleListContext>
    ): Result<ParsedListItem> = withContext(Dispatchers.IO) {
        runCatching {
            val apiKey = preferencesRepository.groqApiKey.first()?.trim()
            require(!apiKey.isNullOrEmpty()) { "Add a Groq API key in Settings to use AI task parsing." }

            val systemPrompt = buildListItemSystemPrompt(Date(), existingLists)
            val responseBody = postChatCompletion(apiKey, systemPrompt, input)

            val chatResponse = json.decodeFromString<GroqChatResponse>(responseBody)
            val content = chatResponse.choices.firstOrNull()?.message?.content
            require(!content.isNullOrBlank()) { "Groq returned an empty response." }

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

    private fun postChatCompletion(apiKey: String, systemPrompt: String, userInput: String): String {
        val requestBody = buildJsonObject {
            put("model", MODEL)
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

        val connection = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Authorization", "Bearer $apiKey")
            setRequestProperty("Content-Type", "application/json")
            doOutput = true
            connectTimeout = 15_000
            readTimeout = 20_000
        }

        return try {
            connection.outputStream.use { it.write(requestBody.toString().toByteArray(Charsets.UTF_8)) }
            val responseCode = connection.responseCode
            val stream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
            val responseText = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            check(responseCode in 200..299) { "Groq request failed ($responseCode): ${responseText.take(300)}" }
            responseText
        } finally {
            connection.disconnect()
        }
    }

    private fun ParsedTaskDto.toParsedTask(fallbackTitle: String): ParsedTask {
        val resolvedPriority = priority?.trim()?.uppercase(Locale.US)
            ?.let { p -> runCatching { Priority.valueOf(p) }.getOrNull() }
        val resolvedDueDate = dueDate?.let { parseIsoDate(it) }
        return ParsedTask(
            title = title?.trim().takeUnless { it.isNullOrBlank() } ?: fallbackTitle,
            notes = notes?.trim()?.takeIf { it.isNotBlank() },
            priority = resolvedPriority,
            dueDate = resolvedDueDate,
            listName = list?.trim()?.takeIf { it.isNotBlank() },
            tagNames = tags.map { it.trim() }.filter { it.isNotBlank() }.distinct()
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

        return ParsedListItem(
            text = text?.trim().takeUnless { it.isNullOrBlank() } ?: fallbackText,
            note = note?.trim()?.takeIf { it.isNotBlank() },
            url = url?.trim()?.takeIf { it.isNotBlank() },
            listName = resolvedListName,
            fieldValues = resolvedFieldValues
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
private data class GroqChatResponse(val choices: List<GroqChoice> = emptyList())

@Serializable
private data class GroqChoice(val message: GroqMessage)

@Serializable
private data class GroqMessage(val content: String? = null)

@Serializable
private data class ParsedTaskDto(
    val title: String? = null,
    val notes: String? = null,
    val priority: String? = null,
    @SerialName("due_date") val dueDate: String? = null,
    val list: String? = null,
    val tags: List<String> = emptyList()
)

@Serializable
private data class ParsedListItemDto(
    val text: String? = null,
    val note: String? = null,
    val url: String? = null,
    val list: String? = null,
    val fields: List<ParsedFieldDto> = emptyList()
)

@Serializable
private data class ParsedFieldDto(
    val name: String? = null,
    val value: String? = null
)
