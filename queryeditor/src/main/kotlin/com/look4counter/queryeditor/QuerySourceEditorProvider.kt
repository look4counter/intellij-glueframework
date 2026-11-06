package com.look4counter.queryeditor

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.fileEditor.FileEditorStateLevel
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider
import com.intellij.openapi.fileEditor.impl.text.AsyncEditorLoader
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.editor.ScrollType
import com.intellij.psi.PsiDocumentManager
import org.jdom.Element

class QuerySourceEditorProvider : FileEditorProvider, DumbAware {
    override fun accept(project: Project, file: VirtualFile): Boolean = file.fileType == GlueSqlFileType.INSTANCE

    override fun createEditor(project: Project, file: VirtualFile): FileEditor =
        com.intellij.openapi.application.ApplicationManager.getApplication().runWriteIntentReadAction<FileEditor, RuntimeException> {
            QuerySourceEditor(project, file, TextEditorProvider.getInstance().createEditor(project, file) as TextEditor)
        }

    override fun getEditorTypeId(): String = "glue-query-source"
    override fun getPolicy(): FileEditorPolicy = FileEditorPolicy.HIDE_DEFAULT_EDITOR

    override fun readState(sourceElement: Element, project: Project, file: VirtualFile): FileEditorState =
        TextEditorProvider.getInstance().readState(sourceElement, project, file)

    override fun writeState(state: FileEditorState, project: Project, targetElement: Element) =
        TextEditorProvider.getInstance().writeState(state, project, targetElement)
}

private class QuerySourceEditor(private val project: Project, private val sourceFile: VirtualFile,
                                private val delegate: TextEditor) : TextEditor by delegate {
    private var disposed = false
    private var tabRevision = 0L
    init {
        Disposer.register(this, delegate)
    }

    override fun getName(): String = "Source"
    override fun getState(level: FileEditorStateLevel): FileEditorState = delegate.getState(level)
    override fun setState(state: FileEditorState, exactState: Boolean) = delegate.setState(state, exactState)
    override fun selectNotify() {
        delegate.selectNotify()
        val revision = ++tabRevision
        AsyncEditorLoader.performWhenLoaded(editor) {
            QueryTabNavigation.whenWriteSafe(project, sourceFile, { !disposed && revision == tabRevision && !editor.isDisposed }) {
                val document = editor.document
                PsiDocumentManager.getInstance(project).commitDocument(document)
                val queries = QueryMapDocumentModel(project, sourceFile).snapshot().queries
                val selected = document.getUserData(QueryTabNavigation.designQuery)?.element
                val query = queries.firstOrNull { it.target.element == selected } ?: queries.firstOrNull()
                val tag = query?.target?.element ?: return@whenWriteSafe
                val caret = document.getUserData(QueryTabNavigation.designCaret)
                val sqlOffset = caret?.takeIf { it.target.element == tag }?.offset ?: 0
                val offset = QueryTabNavigation.sourceOffset(tag, sqlOffset)
                editor.caretModel.removeSecondaryCarets()
                editor.selectionModel.removeSelection()
                editor.caretModel.moveToOffset(offset)
                editor.scrollingModel.scrollToCaret(ScrollType.CENTER)
            }
        }
    }
    override fun deselectNotify() {
        ++tabRevision
        if (!disposed && !editor.isDisposed) editor.document.putUserData(QueryTabNavigation.sourceOffset, editor.caretModel.offset)
        delegate.deselectNotify()
    }
    override fun getBackgroundHighlighter() = delegate.backgroundHighlighter
    override fun getCurrentLocation() = delegate.currentLocation
    override fun getStructureViewBuilder() = delegate.structureViewBuilder
    override fun getFile() = delegate.file
    override fun getFilesToRefresh() = delegate.filesToRefresh
    override fun getTabActions() = delegate.tabActions
    override fun isEditorLoaded(): Boolean = delegate.isEditorLoaded
    override fun dispose() {
        disposed = true
        ++tabRevision
    }
}
