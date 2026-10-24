package com.look4counter.modeleditor

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.CharFilter
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFileFactory
import com.intellij.ui.EditorTextField
import com.intellij.util.textCompletion.TextCompletionProvider
import com.intellij.util.textCompletion.TextCompletionUtil
import com.look4counter.queryeditor.queryindex.QueryIdIndexService
import java.awt.Component
import javax.swing.AbstractCellEditor
import javax.swing.JTable
import javax.swing.table.TableCellEditor

class QueryIdCellEditor(private val project: Project) : AbstractCellEditor(), TableCellEditor {
    private var field: EditorTextField? = null

    override fun getTableCellEditorComponent(table: JTable, value: Any?, selected: Boolean, row: Int, column: Int): Component {
        val file = PsiFileFactory.getInstance(project).createFileFromText(
            "glue-query-id.txt", PlainTextFileType.INSTANCE, value?.toString().orEmpty(), System.currentTimeMillis(), true)
        TextCompletionUtil.installProvider(file, QueryIdCompletionProvider(project), true)
        val document = requireNotNull(PsiDocumentManager.getInstance(project).getDocument(file))
        return EditorTextField(document, project, PlainTextFileType.INSTANCE).apply {
            setOneLineMode(true)
            setFontInheritedFromLAF(true)
            font = table.font
            background = table.background
            field = this
        }
    }

    override fun getCellEditorValue(): Any = field?.text.orEmpty()
}

class QueryIdCompletionProvider(private val project: Project) : TextCompletionProvider, DumbAware {
    override fun getAdvertisement(): String? = null
    override fun getPrefix(text: String, offset: Int) = text.substring(0, offset.coerceIn(0, text.length))
    override fun applyPrefixMatcher(result: CompletionResultSet, prefix: String) = result.withPrefixMatcher(prefix).caseInsensitive()
    override fun acceptChar(char: Char) = if (char.isLetterOrDigit() || char in "._-:/$") CharFilter.Result.ADD_TO_PREFIX else CharFilter.Result.HIDE_LOOKUP
    override fun fillCompletionVariants(parameters: CompletionParameters, prefix: String, result: CompletionResultSet) {
        if (!project.isDisposed && !parameters.editor.isViewer && parameters.editor.document.isWritable &&
            (prefix.isNotEmpty() || parameters.invocationCount > 0)) {
            QueryIdIndexService.getInstance(project).snapshot(prefix).forEach { query ->
                result.addElement(LookupElementBuilder.create(query.id).withCaseSensitivity(false)
                    .withTailText(if (query.description.isBlank()) "" else "  ${query.description}", true)
                    .withTypeText(query.fileUrl.substringAfter("://"), true))
            }
        }
        result.stopHere()
    }
}
