package com.look4counter.modeleditor

import com.intellij.openapi.ide.CopyPasteManager
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException

class CopiedPropertyRows(val rows: List<ActivityProperty>)

object PropertyRowsClipboard {
    private val flavor = DataFlavor(
        "${DataFlavor.javaJVMLocalObjectMimeType};class=${CopiedPropertyRows::class.java.name}",
        "Activity property rows", CopiedPropertyRows::class.java.classLoader)

    fun write(rows: List<ActivityProperty>) {
        if (rows.isEmpty()) return
        val snapshot = CopiedPropertyRows(rows.map { it.copy(sourceIndex = null) })
        fun cell(value: String) = if (value.any { it == '\t' || it == '\n' || it == '\r' || it == '"' })
            "\"${value.replace("\"", "\"\"")}\"" else value
        val text = snapshot.rows.joinToString("\n") { "${cell(it.key)}\t${cell(it.value)}" }
        CopyPasteManager.getInstance().setContents(object : Transferable {
            override fun getTransferDataFlavors() = arrayOf(flavor, DataFlavor.stringFlavor)
            override fun isDataFlavorSupported(candidate: DataFlavor) = candidate == flavor || candidate == DataFlavor.stringFlavor
            override fun getTransferData(candidate: DataFlavor): Any = when (candidate) {
                flavor -> snapshot
                DataFlavor.stringFlavor -> text
                else -> throw UnsupportedFlavorException(candidate)
            }
        })
    }

    fun read(): List<ActivityProperty>? {
        val clipboard = CopyPasteManager.getInstance()
        clipboard.getContents<CopiedPropertyRows>(flavor)?.let { return it.rows.map { row -> row.copy(sourceIndex = null) } }
        val text = clipboard.getContents<String>(DataFlavor.stringFlavor) ?: return null
        val rows = mutableListOf<List<String>>()
        val cells = mutableListOf<String>()
        val value = StringBuilder()
        var quoted = false
        var closedQuote = false
        var i = 0
        fun endField() { cells.add(value.toString()); value.setLength(0); closedQuote = false }
        fun endRow() { endField(); rows.add(cells.toList()); cells.clear() }
        while (i < text.length) {
            val c = text[i]
            if (quoted) {
                if (c == '"') {
                    if (i + 1 < text.length && text[i + 1] == '"') { value.append('"'); i++ }
                    else { quoted = false; closedQuote = true }
                } else value.append(c)
            } else {
                if (closedQuote && c != '\t' && c != '\r' && c != '\n') return null
                when (c) {
                    '\t' -> endField()
                    '\r', '\n' -> {
                        endRow()
                        if (c == '\r' && i + 1 < text.length && text[i + 1] == '\n') i++
                    }
                    '"' -> if (value.isEmpty()) quoted = true else value.append(c)
                    else -> value.append(c)
                }
            }
            i++
        }
        if (quoted) return null
        if (value.isNotEmpty() || cells.isNotEmpty() || closedQuote) endRow()
        if (rows.isEmpty() || rows.any { it.size != 2 }) return null
        return rows.map { ActivityProperty(it[0], it[1]) }
    }
}
