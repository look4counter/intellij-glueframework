package com.look4counter.modeleditor

import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.refactoring.ui.ClassNameReferenceEditor
import com.intellij.ui.EditorTextField
import com.intellij.ui.JavaReferenceEditorUtil
import java.awt.Component
import javax.swing.AbstractCellEditor
import javax.swing.JTable
import javax.swing.table.TableCellEditor

class ActivityClassCellEditor(private val project: Project) : AbstractCellEditor(), TableCellEditor {
    private var field: EditorTextField? = null

    override fun getTableCellEditorComponent(table: JTable, value: Any?, selected: Boolean, row: Int, column: Int): Component {
        val document = requireNotNull(JavaReferenceEditorUtil.createDocument(value?.toString().orEmpty(), project, true))
        PsiDocumentManager.getInstance(project).getPsiFile(document)
            ?.putUserData(ClassNameReferenceEditor.CLASS_NAME_REFERENCE_FRAGMENT, true)
        return EditorTextField(document, project, JavaFileType.INSTANCE).apply {
            setOneLineMode(true)
            setFontInheritedFromLAF(true)
            font = table.font
            background = table.background
            field = this
        }
    }

    override fun getCellEditorValue(): Any = field?.text.orEmpty()
}
