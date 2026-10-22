package com.look4counter.queryeditor

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

class QueryDesignEditorProvider : FileEditorProvider, DumbAware {
    override fun accept(project: Project, file: VirtualFile): Boolean = file.fileType == GlueSqlFileType.INSTANCE
    override fun createEditor(project: Project, file: VirtualFile): FileEditor = QueryDesignEditor(project, file)
    override fun getEditorTypeId(): String = "glue-query-design"
    override fun getPolicy(): FileEditorPolicy = FileEditorPolicy.PLACE_BEFORE_DEFAULT_EDITOR
}
