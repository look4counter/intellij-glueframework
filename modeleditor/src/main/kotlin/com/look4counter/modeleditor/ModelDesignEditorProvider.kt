package com.look4counter.modeleditor

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import java.awt.BorderLayout
import java.beans.PropertyChangeListener
import java.beans.PropertyChangeSupport
import javax.swing.JComponent
import javax.swing.SwingConstants

class ModelDesignEditorProvider : FileEditorProvider, DumbAware {
    override fun accept(project: Project, file: VirtualFile): Boolean = file.fileType == GlueUmlAdFileType.INSTANCE
    override fun createEditor(project: Project, file: VirtualFile): FileEditor = ModelDesignEditor(file)
    override fun getEditorTypeId(): String = "glue-model-design"
    override fun getPolicy(): FileEditorPolicy = FileEditorPolicy.PLACE_BEFORE_DEFAULT_EDITOR
}

private class ModelDesignEditor(private val file: VirtualFile) : UserDataHolderBase(), FileEditor {
    private val changes = PropertyChangeSupport(this)
    private val panel = JBPanel<JBPanel<*>>(BorderLayout()).apply {
        isFocusable = true
        add(JBLabel("모델 Design 화면입니다. XML은 Source 탭에서 편집하세요.", SwingConstants.CENTER), BorderLayout.CENTER)
    }

    override fun getComponent(): JComponent = panel
    override fun getPreferredFocusedComponent(): JComponent = panel
    override fun getName(): String = "Design"
    override fun getFile(): VirtualFile = file
    override fun setState(state: FileEditorState) = Unit
    override fun isModified(): Boolean = false
    override fun isValid(): Boolean = file.isValid
    override fun addPropertyChangeListener(listener: PropertyChangeListener) = changes.addPropertyChangeListener(listener)
    override fun removePropertyChangeListener(listener: PropertyChangeListener) = changes.removePropertyChangeListener(listener)
    override fun dispose() = Unit
}
