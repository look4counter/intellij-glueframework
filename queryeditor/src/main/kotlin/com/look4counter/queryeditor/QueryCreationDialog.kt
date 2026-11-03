package com.look4counter.queryeditor

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import javax.swing.JComponent

data class NewQueryData(val id: String, val description: String = "", val resultType: String = "", val isNamed: Boolean = false) {
    fun validationError(existingIds: Set<String>): String? = when {
        id.isBlank() -> "ID를 입력하세요."
        id.trim() in existingIds -> "이미 존재하는 ID입니다."
        else -> null
    }
}

interface QueryCreationDialogs {
    fun create(existingIds: Set<String>): NewQueryData?
}

class DefaultQueryCreationDialogs(private val project: Project) : QueryCreationDialogs {
    override fun create(existingIds: Set<String>): NewQueryData? {
        val dialog = QueryCreationDialog(project, existingIds)
        return if (dialog.showAndGet()) dialog.editor.result() else null
    }
}

class QueryCreationPanel {
    val id = JBTextField(28).apply { name = "query.create.id" }
    val description = JBTextField(28).apply { name = "query.create.description" }
    val resultType = JBTextField(28).apply { name = "query.create.resultType" }
    val isNamed = JBCheckBox("IsNamed").apply { name = "query.create.isNamed" }
    val component = FormBuilder.createFormBuilder()
        .addLabeledComponent("ID", id)
        .addLabeledComponent("Description", description)
        .addLabeledComponent("ResultType", resultType)
        .addComponent(isNamed).panel

    fun result() = NewQueryData(id.text.trim(), description.text, resultType.text, isNamed.isSelected)
}

class QueryCreationDialog(project: Project, private val existingIds: Set<String>) : DialogWrapper(project) {
    val editor = QueryCreationPanel()
    init {
        title = "Query 추가"
        setOKButtonText("추가")
        setCancelButtonText("취소")
        init()
    }
    override fun createCenterPanel(): JComponent = editor.component
    override fun getPreferredFocusedComponent(): JComponent = editor.id
    override fun doValidate(): ValidationInfo? = editor.result().validationError(existingIds)?.let { ValidationInfo(it, editor.id) }
}
