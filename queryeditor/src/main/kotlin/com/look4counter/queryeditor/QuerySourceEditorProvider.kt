package com.look4counter.queryeditor

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.fileEditor.FileEditorStateLevel
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import org.jdom.Element

class QuerySourceEditorProvider : FileEditorProvider, DumbAware {
    override fun accept(project: Project, file: VirtualFile): Boolean = file.fileType == GlueSqlFileType.INSTANCE

    override fun createEditor(project: Project, file: VirtualFile): FileEditor =
        QuerySourceEditor(TextEditorProvider.getInstance().createEditor(project, file) as TextEditor)

    override fun getEditorTypeId(): String = "glue-query-source"
    override fun getPolicy(): FileEditorPolicy = FileEditorPolicy.HIDE_DEFAULT_EDITOR

    override fun readState(sourceElement: Element, project: Project, file: VirtualFile): FileEditorState =
        TextEditorProvider.getInstance().readState(sourceElement, project, file)

    override fun writeState(state: FileEditorState, project: Project, targetElement: Element) =
        TextEditorProvider.getInstance().writeState(state, project, targetElement)
}

private class QuerySourceEditor(private val delegate: TextEditor) : TextEditor by delegate {
    init {
        Disposer.register(this, delegate)
    }

    override fun getName(): String = "Source"
    override fun getState(level: FileEditorStateLevel): FileEditorState = delegate.getState(level)
    override fun setState(state: FileEditorState, exactState: Boolean) = delegate.setState(state, exactState)
    override fun selectNotify() = delegate.selectNotify()
    override fun deselectNotify() = delegate.deselectNotify()
    override fun getBackgroundHighlighter() = delegate.backgroundHighlighter
    override fun getCurrentLocation() = delegate.currentLocation
    override fun getStructureViewBuilder() = delegate.structureViewBuilder
    override fun getFile() = delegate.file
    override fun getFilesToRefresh() = delegate.filesToRefresh
    override fun getTabActions() = delegate.tabActions
    override fun isEditorLoaded(): Boolean = delegate.isEditorLoaded
    override fun dispose() = Unit
}
