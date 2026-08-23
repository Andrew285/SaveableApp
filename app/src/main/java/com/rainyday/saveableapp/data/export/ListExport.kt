package com.rainyday.saveableapp.data.export

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import com.rainyday.saveableapp.data.local.FieldDefinitionEntity
import com.rainyday.saveableapp.data.local.FieldValueEntity
import com.rainyday.saveableapp.data.local.SimpleListItemEntity

private const val PAGE_WIDTH = 595 // A4 @ 72dpi
private const val PAGE_HEIGHT = 842
private const val MARGIN = 40f
private const val LINE_HEIGHT = 18f

/** Builds a CSV export of [items] — one column per field, in addition to text/note/URL/checked. */
fun buildListCsv(
    items: List<SimpleListItemEntity>,
    fields: List<FieldDefinitionEntity>,
    valuesByItem: Map<String, List<FieldValueEntity>>
): String {
    val header = listOf("Text", "Note", "URL", "Checked") + fields.map { it.name }
    val rows = mutableListOf(header)
    items.forEach { item ->
        val valuesById = valuesByItem[item.id].orEmpty().associate { it.fieldId to it.value }
        val row = mutableListOf(item.text, item.note.orEmpty(), item.url.orEmpty(), item.isChecked.toString())
        fields.forEach { field -> row += valuesById[field.id].orEmpty() }
        rows += row
    }
    return rows.joinToString("\r\n") { row -> row.joinToString(",") { csvEscape(it) } }
}

private fun csvEscape(value: String): String =
    if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
        "\"" + value.replace("\"", "\"\"") + "\""
    } else {
        value
    }

/** Renders [items] as a simple paginated PDF (title, then one entry per item) and writes it to [uri]. */
fun writeListPdf(
    context: Context,
    uri: Uri,
    listName: String,
    items: List<SimpleListItemEntity>,
    fields: List<FieldDefinitionEntity>,
    valuesByItem: Map<String, List<FieldValueEntity>>
) {
    val document = PdfDocument()
    val titlePaint = Paint().apply { textSize = 18f; isFakeBoldText = true }
    val textPaint = Paint().apply { textSize = 12f }
    val subPaint = Paint().apply { textSize = 10f; color = Color.DKGRAY }

    var pageNumber = 1
    var page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
    var canvas = page.canvas
    var y = MARGIN + 12f
    canvas.drawText(listName, MARGIN, y, titlePaint)
    y += LINE_HEIGHT * 1.8f

    fun newPageIfNeeded() {
        if (y + LINE_HEIGHT > PAGE_HEIGHT - MARGIN) {
            document.finishPage(page)
            pageNumber += 1
            page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
            canvas = page.canvas
            y = MARGIN
        }
    }

    fun drawLine(text: String, indent: Float, paint: Paint) {
        newPageIfNeeded()
        canvas.drawText(text, MARGIN + indent, y, paint)
        y += LINE_HEIGHT * (if (paint === textPaint) 1f else 0.85f)
    }

    items.forEach { item ->
        val checkbox = if (item.isChecked) "[x] " else "[ ] "
        drawLine("$checkbox${item.text}", 0f, textPaint)
        if (!item.note.isNullOrBlank()) drawLine(item.note, 16f, subPaint)
        if (!item.url.isNullOrBlank()) drawLine(item.url, 16f, subPaint)
        val valuesById = valuesByItem[item.id].orEmpty().associate { it.fieldId to it.value }
        fields.forEach { field ->
            val raw = valuesById[field.id]
            if (!raw.isNullOrBlank()) drawLine("${field.name}: $raw", 16f, subPaint)
        }
        y += LINE_HEIGHT * 0.5f
    }
    document.finishPage(page)

    context.contentResolver.openOutputStream(uri)?.use { out -> document.writeTo(out) }
    document.close()
}
