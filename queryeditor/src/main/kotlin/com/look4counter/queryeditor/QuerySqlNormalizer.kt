package com.look4counter.queryeditor

import com.intellij.lang.xml.XMLLanguage
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlFile
import com.intellij.psi.xml.XmlText

internal object QuerySqlNormalizer {
    fun normalize(project: Project, document: Document) {
        val file = FileDocumentManager.getInstance().getFile(document) ?: return
        if (!file.isValid || file.fileType != GlueSqlFileType.INSTANCE || !file.isWritable || !document.isWritable) return
        if (project.isDisposed) return
        val stamp = document.modificationStamp
        val replacements = ReadAction.compute<List<Replacement>, RuntimeException> {
            val xml = PsiFileFactory.getInstance(project)
                .createFileFromText(file.name, XMLLanguage.INSTANCE, document.charsSequence) as? XmlFile
                ?: return@compute emptyList()
            val root = xml.rootTag ?: return@compute emptyList()
            if (root.localName != "queryMap" || PsiTreeUtil.findChildOfType(xml, PsiErrorElement::class.java) != null) {
                return@compute emptyList()
            }
            root.subTags.mapNotNull { tag ->
                if (tag.localName != "query" || tag.namespace != root.namespace || tag.value.children.any { it !is XmlText }) {
                    return@mapNotNull null
                }
                val sql = tag.value.textElements.joinToString("") { it.value }
                val trimmed = sql.trim()
                if (sql == trimmed) return@mapNotNull null
                val range = tag.value.textRange
                Replacement(range.startOffset, range.endOffset, querySqlCdata(trimmed))
            }
        }
        if (replacements.isEmpty()) return
        WriteCommandAction.writeCommandAction(project).withName("SQL 앞뒤 공백 정리").run<RuntimeException> {
            if (!document.isWritable || document.modificationStamp != stamp) return@run
            replacements.asReversed().forEach { document.replaceString(it.start, it.end, it.text) }
        }
    }

    private data class Replacement(val start: Int, val end: Int, val text: String)
}
