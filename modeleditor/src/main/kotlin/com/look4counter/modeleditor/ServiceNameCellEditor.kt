package com.look4counter.modeleditor

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.CharFilter
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.util.textCompletion.TextCompletionProvider
import com.intellij.util.textCompletion.TextFieldWithCompletion
import com.intellij.ui.EditorTextField
import java.awt.Component
import javax.swing.AbstractCellEditor
import javax.swing.JTable
import javax.swing.table.TableCellEditor

class ServiceNameCellEditor(private val project: Project) : AbstractCellEditor(), TableCellEditor {
    private var input: EditorTextField? = null
    override fun getTableCellEditorComponent(table: JTable, value: Any?, selected: Boolean, row: Int, column: Int): Component {
        val provider = object : TextCompletionProvider, DumbAware {
            override fun getAdvertisement(): String? = null
            override fun getPrefix(text: String, offset: Int) = text.substring(0, offset.coerceIn(0, text.length))
            override fun applyPrefixMatcher(result: CompletionResultSet, prefix: String) = result.withPrefixMatcher(prefix).caseInsensitive()
            override fun acceptChar(char: Char) = if (char.isLetterOrDigit() || char in "._-") CharFilter.Result.ADD_TO_PREFIX else CharFilter.Result.HIDE_LOOKUP
            override fun fillCompletionVariants(parameters: CompletionParameters, prefix: String, result: CompletionResultSet) {
                if (!project.isDisposed && !parameters.editor.isViewer && parameters.editor.document.isWritable) {
                    ServiceFiles.names(project).filter { it.startsWith(prefix, true) || "$it.xml".startsWith(prefix, true) }.forEach {
                        result.addElement(LookupElementBuilder.create(it).withLookupString("$it.xml")
                            .withCaseSensitivity(false).withTypeText("Service XML", true))
                    }
                }
                result.stopHere()
            }
        }
        return TextFieldWithCompletion(project, provider, value?.toString().orEmpty(), true, true, true, false).apply {
            setFontInheritedFromLAF(true); font = table.font; background = table.background
            input = this
        }
    }
    override fun getCellEditorValue(): Any = input?.text.orEmpty()
}
